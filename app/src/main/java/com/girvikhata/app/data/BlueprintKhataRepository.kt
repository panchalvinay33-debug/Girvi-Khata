package com.girvikhata.app.data

import com.girvikhata.app.GirviKhataApplication
import com.girvikhata.app.custody.CustodyPlacementStore
import com.girvikhata.app.domain.GirviAdvanceMetadata
import com.girvikhata.app.domain.GirviSequence
import com.girvikhata.app.domain.InterestTerms
import java.util.UUID

/**
 * Single business API for the rebuilt app.
 * UI screens must call this repository instead of saving snapshots independently.
 */
class BlueprintKhataRepository(
    private val records: EncryptedRecordStore,
    private val writer: AuthoritativeBusinessWriter = AuthoritativeBusinessWriter(records),
) {
    fun snapshot(): AppSnapshot = records.load()

    @Synchronized
    fun createGirvi(customer: CustomerRecord, draft: GirviRecord): AppSnapshot {
        val latest = records.load()
        val number = if (latest.girvis.none { it.girviNumber == draft.girviNumber }) {
            draft.girviNumber
        } else {
            GirviSequence.nextNumber(latest.girvis.map { it.girviNumber })
        }
        val girvi = draft.copy(girviNumber = number, customerId = customer.id, customerName = customer.name)
        return writer.execute(CreateGirviWithCustomerUpsertMutation(customer, girvi))
    }

    @Synchronized
    fun upsertCustomer(customer: CustomerRecord): AppSnapshot =
        writer.execute(VerifiedBusinessMutation.UpsertCustomer(customer))

    @Synchronized
    fun addAdditionalAdvance(
        girviId: String,
        amountPaise: Long,
        createdAt: Long,
        terms: InterestTerms,
        note: String = "",
        eventId: String = UUID.randomUUID().toString(),
    ): AppSnapshot {
        require(amountPaise > 0L) { "Advance amount must be positive" }
        val current = records.load().girvis.firstOrNull { it.id == girviId } ?: error("Girvi missing")
        require(current.status == "ACTIVE") { "Only active girvi can receive additional advance" }
        require(createdAt >= current.createdAt) { "Advance date cannot be before original girvi date" }
        val advance = GirviAdvanceMetadata.Advance(
            id = eventId,
            amountPaise = amountPaise,
            createdAt = createdAt,
            terms = terms,
            note = note.trim(),
        )
        val updated = current.copy(
            releaseNote = GirviAdvanceMetadata.append(current.releaseNote, advance),
        )
        return writer.execute(VerifiedBusinessMutation.UpsertGirvi(updated))
    }

    @Synchronized
    fun appendPayment(girviId: String, payment: PaymentRecord): AppSnapshot =
        writer.execute(VerifiedBusinessMutation.AppendPayment(girviId, payment))

    @Synchronized
    fun reversePayment(
        girviId: String,
        originalPaymentId: String,
        reversal: PaymentRecord,
    ): AppSnapshot = writer.execute(
        VerifiedBusinessMutation.ReversePayment(
            girviId = girviId,
            originalPaymentId = originalPaymentId,
            reversal = reversal,
        ),
    )

    @Synchronized
    fun releaseGirvi(girviId: String, releasedAt: Long, note: String = ""): AppSnapshot {
        val current = records.load().girvis.firstOrNull { it.id == girviId } ?: error("Girvi missing")
        require(current.status == "ACTIVE") { "Girvi already released" }
        require(releasedAt >= current.createdAt) { "Release date cannot be before girvi date" }
        validateNoActiveExternalPlacement(girviId)
        val metadata = GirviAdvanceMetadata.read(current.releaseNote)
        val releaseNote = GirviAdvanceMetadata.attach(note.trim(), metadata)
        return writer.execute(
            VerifiedBusinessMutation.ReleaseGirvi(
                current.copy(
                    status = "RELEASED",
                    releasedAt = releasedAt,
                    releaseNote = releaseNote,
                ),
            ),
        )
    }

    private fun validateNoActiveExternalPlacement(girviId: String) {
        val context = GirviKhataApplication.appContextOrNull() ?: return
        val custody = CustodyPlacementStore(context).load()
        val blockingLots = custody.lots.filter { lot ->
            lot.items.any { item -> item.girviId == girviId && item.removedAt == null }
        }
        require(blockingLots.isEmpty()) {
            val lots = blockingLots.joinToString { it.lotNumber }
            "Girvi ka item external lot ($lots) me active hai. Release se pehle item ko locker/location me wapas move karein."
        }
    }

    @Synchronized
    fun addCategory(category: CategoryRecord): AppSnapshot =
        writer.execute(VerifiedBusinessMutation.AddCategory(category))

    @Synchronized
    fun setCategoryActive(categoryId: String, active: Boolean): AppSnapshot =
        writer.execute(VerifiedBusinessMutation.SetCategoryActive(categoryId, active))
}
