package com.girvikhata.app.data

import android.content.Context
import java.util.UUID

/** Safety-first authoritative snapshot write with relational verification. */
class VerifiedBusinessWriteCoordinator(
    context: Context,
    private val records: EncryptedRecordStore = EncryptedRecordStore(context.applicationContext),
    private val shadowFactory: () -> EncryptedRelationalShadowStore = {
        EncryptedRelationalShadowStore(context.applicationContext)
    },
    private val journal: DataSafetyJournal = DataSafetyJournal(context.applicationContext),
    private val observationStore: VerifiedWriteObservationStore = VerifiedWriteObservationStore(context.applicationContext),
    private val intentStore: VerifiedWriteIntentStore = VerifiedWriteIntentStore(context.applicationContext),
    private val recoveryCoordinator: InterruptedWriteRecoveryCoordinator = InterruptedWriteRecoveryCoordinator(context.applicationContext),
    private val recoveryRepair: VerifiedWriteRecoveryRepair = VerifiedWriteRecoveryRepair(context.applicationContext, records = records),
) {
    @Synchronized
    fun execute(request: VerifiedBusinessWriteRequest): VerifiedBusinessWriteResult {
        var recovery = recoveryCoordinator.reconcileOnStartup()
        if (recovery.action == InterruptedWriteRecoveryAction.BLOCK_AND_REQUIRE_RECOVERY) {
            recoveryRepair.repairIfBlocked()
            recovery = recoveryCoordinator.reconcileOnStartup()
        }
        require(recovery.action != InterruptedWriteRecoveryAction.BLOCK_AND_REQUIRE_RECOVERY) {
            "Unresolved interrupted transaction ${recovery.transactionId ?: "unknown"}; recovery required before new writes"
        }

        // IMPORTANT: validate the caller's fingerprint before creating a PENDING intent.
        // A stale screen snapshot is not a transaction and must never poison future writes.
        val before = records.load()
        val beforeFingerprint = RelationalShadowFingerprint.sha256(before)
        require(request.expectedFingerprint == beforeFingerprint) {
            "Business data changed before transaction ${request.transactionId}; refresh and retry"
        }

        intentStore.begin(
            transactionId = request.transactionId,
            mutationLabel = request.mutation.auditLabel,
            expectedFingerprint = beforeFingerprint,
        )
        return try {
            val target = request.mutation.apply(before)
            require(target != before) { "Transaction ${request.transactionId} produced no business change" }
            validateTarget(target)
            val targetFingerprint = RelationalShadowFingerprint.sha256(target)

            intentStore.prepareTarget(targetFingerprint)

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

            val result = VerifiedBusinessWriteResult(
                transactionId = request.transactionId,
                beforeFingerprint = beforeFingerprint,
                afterFingerprint = targetFingerprint,
                relationalFingerprint = relationalStatus.actualFingerprint,
                syncMode = relationalStatus.syncMode,
                changedRows = relationalStatus.changedRows ?: 0,
                committedAt = System.currentTimeMillis(),
            )
            intentStore.commit(targetFingerprint, result.committedAt)
            val observation = observationStore.recordSuccess(result)

            runCatching {
                journal.recordNamedEvent(
                    type = "VERIFIED_BUSINESS_WRITE",
                    title = request.title.take(80),
                    detail = "${request.transactionId} • ${request.mutation.auditLabel} • ${beforeFingerprint.take(12)}→${targetFingerprint.take(12)} • ${relationalStatus.syncMode ?: "SYNC"} • proof ${observation.successfulWrites}/${VerifiedWriteCutoverPolicy.MINIMUM_COORDINATED_WRITES}",
                )
            }
            result
        } catch (failure: Throwable) {
            val reason = failure.message ?: failure::class.java.simpleName
            val intent = runCatching { intentStore.load() }.getOrNull()
            val currentFingerprint = runCatching {
                RelationalShadowFingerprint.sha256(records.load())
            }.getOrNull()
            val safelyPreCommit = InterruptedWriteRecoveryPolicy.mayMarkFailed(intent, currentFingerprint)

            if (safelyPreCommit) runCatching { intentStore.fail(reason) }

            val recoveryState = when {
                safelyPreCommit -> "pre-commit failure"
                currentFingerprint == intent?.targetFingerprint -> "authoritative committed; recovery pending"
                else -> "authoritative state unknown; recovery required"
            }
            val observation = runCatching {
                observationStore.recordFailure(request.transactionId, "$reason • $recoveryState")
            }.getOrNull()
            runCatching {
                journal.recordNamedEvent(
                    type = "VERIFIED_BUSINESS_WRITE_FAILED",
                    title = request.title.take(80),
                    detail = "${request.transactionId} • ${request.mutation.auditLabel} • ${reason.take(140)} • $recoveryState • successful ${observation?.successfulWrites ?: 0}",
                )
            }
            throw failure
        }
    }

    fun currentFingerprint(): String = RelationalShadowFingerprint.sha256(records.load())

    fun observation(): VerifiedWriteObservation = observationStore.load()

    fun latestIntent(): VerifiedWriteIntent? = intentStore.load()

    fun clearResolvedIntent() = intentStore.clearCompleted()

    private fun validateTarget(snapshot: AppSnapshot) {
        require(snapshot.customers.map { it.id }.distinct().size == snapshot.customers.size) { "Duplicate customer ID" }
        require(snapshot.girvis.map { it.id }.distinct().size == snapshot.girvis.size) { "Duplicate girvi ID" }
        require(snapshot.girvis.map { it.girviNumber }.distinct().size == snapshot.girvis.size) { "Duplicate girvi number" }
        require(snapshot.categories.map { it.id }.distinct().size == snapshot.categories.size) { "Duplicate category ID" }
        require(snapshot.categories.map { it.name.lowercase() }.distinct().size == snapshot.categories.size) { "Duplicate category name" }
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

    data class ReversePayment(
        val girviId: String,
        val originalPaymentId: String,
        val reversal: PaymentRecord,
    ) : VerifiedBusinessMutation {
        override val auditLabel: String = "PAYMENT_REVERSAL"

        override fun apply(snapshot: AppSnapshot): AppSnapshot {
            require(reversal.isReversal) { "Reversal entry required" }
            require(reversal.reversedPaymentId == originalPaymentId) { "Reversal payment identity mismatch" }
            require(snapshot.girvis.flatMap { it.payments }.none { it.id == reversal.id || it.receiptNumber == reversal.receiptNumber }) {
                "Duplicate reversal or receipt"
            }
            return snapshot.copy(girvis = snapshot.girvis.map { girvi ->
                if (girvi.id != girviId) girvi else {
                    require(girvi.status == "ACTIVE") { "Released girvi reversal blocked" }
                    val original = girvi.payments.firstOrNull { it.id == originalPaymentId && !it.isReversal }
                        ?: error("Original payment missing")
                    require(girvi.payments.none { it.isReversal && it.reversedPaymentId == originalPaymentId }) {
                        "Payment already reversed"
                    }
                    require(reversal.amountPaise == original.amountPaise) { "Reversal amount mismatch" }
                    require(reversal.principalPaise == original.principalPaise) { "Reversal principal mismatch" }
                    require(reversal.interestPaise == original.interestPaise) { "Reversal interest mismatch" }
                    require(reversal.chargesPaise == original.chargesPaise) { "Reversal charges mismatch" }
                    girvi.copy(payments = girvi.payments + reversal)
                }
            })
        }
    }

    data class ReleaseGirvi(val released: GirviRecord) : VerifiedBusinessMutation {
        override val auditLabel: String = "GIRVI_RELEASE"

        override fun apply(snapshot: AppSnapshot): AppSnapshot {
            val current = snapshot.girvis.firstOrNull { it.id == released.id } ?: error("Girvi missing")
            require(current.status == "ACTIVE") { "Only active girvi can be released" }
            require(released.status == "RELEASED") { "Released status required" }
            require(released.releasedAt != null) { "Release timestamp required" }
            require(released.customerId == current.customerId) { "Release customer cannot change" }
            require(released.girviNumber == current.girviNumber) { "Release girvi number cannot change" }
            require(released.payments == current.payments) { "Release cannot alter payment ledger" }
            require(released.principalPaise == current.principalPaise) { "Release cannot alter principal" }
            return snapshot.copy(girvis = snapshot.girvis.map { if (it.id == released.id) released else it })
        }
    }

    data class AddCategory(val category: CategoryRecord) : VerifiedBusinessMutation {
        override val auditLabel: String = "CATEGORY_ADD"

        override fun apply(snapshot: AppSnapshot): AppSnapshot {
            val clean = category.name.trim()
            require(clean.isNotBlank()) { "Category name required" }
            require(snapshot.categories.none { it.id == category.id }) { "Duplicate category ID" }
            require(snapshot.categories.none { it.name.equals(clean, ignoreCase = true) }) { "Duplicate category name" }
            return snapshot.copy(categories = snapshot.categories + category.copy(name = clean))
        }
    }

    data class SetCategoryActive(val categoryId: String, val active: Boolean) : VerifiedBusinessMutation {
        override val auditLabel: String = if (active) "CATEGORY_ACTIVATE" else "CATEGORY_DEACTIVATE"

        override fun apply(snapshot: AppSnapshot): AppSnapshot {
            val category = snapshot.categories.firstOrNull { it.id == categoryId } ?: error("Category missing")
            require(category.active != active) { "Category already ${if (active) "active" else "inactive"}" }
            if (!active) {
                val activeCategoryNames = snapshot.girvis
                    .filter { it.status == "ACTIVE" }
                    .flatMap { it.effectiveItems }
                    .map { it.categoryName }
                require(activeCategoryNames.none { it.equals(category.name, ignoreCase = true) }) {
                    "Active girvi uses this category"
                }
            }
            return snapshot.copy(categories = snapshot.categories.map {
                if (it.id == categoryId) it.copy(active = active) else it
            })
        }
    }

    data class ReplaceSnapshotForRestore(val target: AppSnapshot) : VerifiedBusinessMutation {
        override val auditLabel: String = "RESTORE_REPLACE"
        override fun apply(snapshot: AppSnapshot): AppSnapshot = target
    }
}
