package com.girvikhata.app.data

import android.content.Context
import java.util.UUID

/**
 * Safety-first write coordinator.
 *
 * The encrypted snapshot remains authoritative. A write succeeds only when:
 * 1. the caller's expected fingerprint matches the latest snapshot,
 * 2. the mutation produces a valid deterministic target snapshot,
 * 3. the encrypted snapshot save/read-back succeeds,
 * 4. the relational shadow independently reconstructs the same fingerprint.
 *
 * Alpha 23 does not cut normal reads over to SQLite.
 */
class VerifiedBusinessWriteCoordinator(
    context: Context,
    private val records: EncryptedRecordStore = EncryptedRecordStore(context.applicationContext),
    private val shadowFactory: () -> EncryptedRelationalShadowStore = {
        EncryptedRelationalShadowStore(context.applicationContext)
    },
    private val journal: DataSafetyJournal = DataSafetyJournal(context.applicationContext),
) {
    @Synchronized
    fun execute(request: VerifiedBusinessWriteRequest): VerifiedBusinessWriteResult {
        val before = records.load()
        val beforeFingerprint = RelationalShadowFingerprint.sha256(before)
        require(request.expectedFingerprint == beforeFingerprint) {
            "Business data changed before transaction ${request.transactionId}; refresh and retry"
        }

        val target = request.mutation.apply(before)
        require(target != before) { "Transaction ${request.transactionId} produced no business change" }
        validateTarget(target)
        val targetFingerprint = RelationalShadowFingerprint.sha256(target)

        records.save(target)
        val persisted = records.load()
        val persistedFingerprint = RelationalShadowFingerprint.sha256(persisted)
        check(persistedFingerprint == targetFingerprint) {
            "Authoritative snapshot verification failed for ${request.transactionId}"
        }

        val relationalStatus = shadowFactory().use { shadow ->
            shadow.syncIncremental(persisted)
            val dual = shadow.dualReadComparison(persisted)
            check(dual.matches) { dual.reason ?: "Relational dual-read verification failed" }
            shadow.statusAgainst(persisted)
        }
        check(relationalStatus.healthy) {
            relationalStatus.reason ?: "Relational status unhealthy after ${request.transactionId}"
        }

        runCatching {
            journal.recordNamedEvent(
                type = "VERIFIED_BUSINESS_WRITE",
                title = request.title.take(80),
                detail = "${request.transactionId} • ${request.mutation.auditLabel} • ${beforeFingerprint.take(12)}→${targetFingerprint.take(12)} • ${relationalStatus.syncMode ?: "SYNC"}",
            )
        }

        return VerifiedBusinessWriteResult(
            transactionId = request.transactionId,
            beforeFingerprint = beforeFingerprint,
            afterFingerprint = targetFingerprint,
            relationalFingerprint = relationalStatus.actualFingerprint,
            syncMode = relationalStatus.syncMode,
            changedRows = relationalStatus.changedRows ?: 0,
            committedAt = System.currentTimeMillis(),
        )
    }

    fun currentFingerprint(): String = RelationalShadowFingerprint.sha256(records.load())

    private fun validateTarget(snapshot: AppSnapshot) {
        require(snapshot.customers.map { it.id }.distinct().size == snapshot.customers.size) { "Duplicate customer ID" }
        require(snapshot.girvis.map { it.id }.distinct().size == snapshot.girvis.size) { "Duplicate girvi ID" }
        require(snapshot.girvis.map { it.girviNumber }.distinct().size == snapshot.girvis.size) { "Duplicate girvi number" }
        val customerIds = snapshot.customers.map { it.id }.toSet()
        require(snapshot.girvis.all { it.customerId in customerIds }) { "Girvi customer link missing" }
        snapshot.girvis.forEach { girvi ->
            require(girvi.principalPaise > 0L) { "Girvi principal invalid" }
            require(girvi.status in setOf("ACTIVE", "RELEASED")) { "Girvi status invalid" }
            require(girvi.payments.map { it.id }.distinct().size == girvi.payments.size) { "Duplicate payment ID" }
            require(girvi.payments.map { it.receiptNumber }.distinct().size == girvi.payments.size) { "Duplicate receipt number" }
        }
    }
}

data class VerifiedBusinessWriteRequest(
    val expectedFingerprint: String,
    val mutation: VerifiedBusinessMutation,
    val title: String,
    val transactionId: String = UUID.randomUUID().toString(),
)

data class VerifiedBusinessWriteResult(
    val transactionId: String,
    val beforeFingerprint: String,
    val afterFingerprint: String,
    val relationalFingerprint: String?,
    val syncMode: String?,
    val changedRows: Int,
    val committedAt: Long,
)

sealed interface VerifiedBusinessMutation {
    val auditLabel: String
    fun apply(snapshot: AppSnapshot): AppSnapshot

    data class UpsertCustomer(val customer: CustomerRecord) : VerifiedBusinessMutation {
        override val auditLabel: String = "CUSTOMER_UPSERT"
        override fun apply(snapshot: AppSnapshot): AppSnapshot {
            val next = snapshot.customers.filterNot { it.id == customer.id } + customer
            return snapshot.copy(customers = next.sortedBy { it.id })
        }
    }

    data class UpsertGirvi(val girvi: GirviRecord) : VerifiedBusinessMutation {
        override val auditLabel: String = "GIRVI_UPSERT"
        override fun apply(snapshot: AppSnapshot): AppSnapshot {
            require(snapshot.customers.any { it.id == girvi.customerId }) { "Girvi customer missing" }
            val next = snapshot.girvis.filterNot { it.id == girvi.id } + girvi
            return snapshot.copy(girvis = next.sortedBy { it.id })
        }
    }

    data class CreateGirviWithCustomer(
        val customer: CustomerRecord,
        val girvi: GirviRecord,
    ) : VerifiedBusinessMutation {
        override val auditLabel: String = "CUSTOMER_GIRVI_CREATE"

        override fun apply(snapshot: AppSnapshot): AppSnapshot {
            require(girvi.customerId == customer.id) { "Girvi customer identity mismatch" }
            val existingCustomer = snapshot.customers.firstOrNull { it.id == customer.id }
            require(existingCustomer == null || existingCustomer == customer) {
                "Existing customer changed; refresh and retry"
            }
            require(snapshot.girvis.none { it.id == girvi.id }) { "Duplicate girvi ID" }
            require(snapshot.girvis.none { it.girviNumber == girvi.girviNumber }) { "Duplicate girvi number" }
            val customers = if (existingCustomer == null) snapshot.customers + customer else snapshot.customers
            return snapshot.copy(
                customers = customers.sortedBy { it.id },
                girvis = (snapshot.girvis + girvi).sortedBy { it.id },
            )
        }
    }

    data class AppendPayment(val girviId: String, val payment: PaymentRecord) : VerifiedBusinessMutation {
        override val auditLabel: String = "PAYMENT_APPEND"
        override fun apply(snapshot: AppSnapshot): AppSnapshot {
            require(snapshot.girvis.any { it.id == girviId }) { "Girvi missing" }
            require(snapshot.girvis.flatMap { it.payments }.none { it.id == payment.id || it.receiptNumber == payment.receiptNumber }) {
                "Duplicate payment or receipt"
            }
            return snapshot.copy(girvis = snapshot.girvis.map { girvi ->
                if (girvi.id != girviId) girvi else {
                    require(girvi.status == "ACTIVE") { "Released girvi payment blocked" }
                    girvi.copy(payments = girvi.payments + payment)
                }
            })
        }
    }

    data class ReplaceSnapshotForRestore(val target: AppSnapshot) : VerifiedBusinessMutation {
        override val auditLabel: String = "RESTORE_REPLACE"
        override fun apply(snapshot: AppSnapshot): AppSnapshot = target
    }
}
