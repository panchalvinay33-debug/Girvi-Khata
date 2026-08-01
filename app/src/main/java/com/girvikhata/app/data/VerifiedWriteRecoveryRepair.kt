package com.girvikhata.app.data

import android.content.Context

/**
 * Repairs a stale coordinated-write block using the encrypted authoritative snapshot.
 *
 * The encrypted snapshot remains the source of truth. This helper never bypasses an active
 * restore-generation recovery: restore recovery must complete through its dedicated flow.
 */
class VerifiedWriteRecoveryRepair(
    context: Context,
    private val records: EncryptedRecordStore = EncryptedRecordStore(context.applicationContext),
    private val intentStore: VerifiedWriteIntentStore = VerifiedWriteIntentStore(context.applicationContext),
    private val recoveryCoordinator: InterruptedWriteRecoveryCoordinator = InterruptedWriteRecoveryCoordinator(context.applicationContext),
    private val shadowFactory: () -> EncryptedRelationalShadowStore = {
        EncryptedRelationalShadowStore(context.applicationContext)
    },
    private val journal: DataSafetyJournal = DataSafetyJournal(context.applicationContext),
) {
    @Synchronized
    fun repairIfBlocked(): VerifiedWriteRecoveryRepairResult {
        val initial = recoveryCoordinator.reconcileOnStartup()
        if (initial.action != InterruptedWriteRecoveryAction.BLOCK_AND_REQUIRE_RECOVERY) {
            return VerifiedWriteRecoveryRepairResult(repaired = false, message = initial.reason)
        }

        require(!initial.reason.contains("Restore generation", ignoreCase = true)) {
            "Backup restore recovery pending; complete restore before new entries"
        }

        val pending = intentStore.load()
        require(pending?.state == VerifiedWriteIntentState.PENDING) {
            "Business write is blocked but no pending transaction can be safely repaired"
        }

        // Limit automatic repair to the practical-entry create transaction. Other unknown business
        // mutations remain fail-closed and require their dedicated recovery path.
        require(pending.mutationLabel in PRACTICAL_CREATE_LABELS) {
            "Unsupported pending transaction ${pending.mutationLabel}; manual recovery required"
        }

        // records.load() verifies the encrypted authoritative store before it is trusted.
        val authoritative = records.load()
        val fingerprint = RelationalShadowFingerprint.sha256(authoritative)

        // Rebuild only the relational mirror. The encrypted snapshot is never replaced here.
        // dualReadComparison is the authoritative cross-store proof. Its optional diagnostic
        // fingerprint is deliberately not compared with the snapshot fingerprint because the two
        // stores use different canonical encodings on some upgraded devices.
        shadowFactory().use { shadow ->
            val status = shadow.replaceAll(authoritative)
            check(status.healthy) { status.reason ?: "Recovered relational shadow is unhealthy" }
            val dual = shadow.dualReadComparison(authoritative)
            check(dual.matches) { dual.reason ?: "Recovered relational shadow does not match encrypted khata" }
        }

        // The verified encrypted snapshot + rebuilt shadow agree, so only stale transaction
        // metadata is cleared. No customer, girvi, payment, category, or photo is deleted.
        intentStore.fail("Stale practical-entry save reconciled from verified authoritative snapshot")
        intentStore.clearCompleted()

        val finalDecision = recoveryCoordinator.reconcileOnStartup()
        check(finalDecision.action != InterruptedWriteRecoveryAction.BLOCK_AND_REQUIRE_RECOVERY) {
            finalDecision.reason
        }

        runCatching {
            journal.recordNamedEvent(
                type = "VERIFIED_WRITE_STALE_BLOCK_REPAIRED",
                title = "Stale practical save block repaired",
                detail = "${pending.transactionId} • ${pending.mutationLabel} • ${fingerprint.take(12)} • FULL_REBUILD",
            )
        }
        return VerifiedWriteRecoveryRepairResult(
            repaired = true,
            message = "Previous interrupted practical-entry save was safely reconciled",
        )
    }

    private companion object {
        val PRACTICAL_CREATE_LABELS = setOf(
            "CUSTOMER_UPSERT_GIRVI_CREATE",
            "CUSTOMER_GIRVI_CREATE",
        )
    }
}

data class VerifiedWriteRecoveryRepairResult(
    val repaired: Boolean,
    val message: String,
)
