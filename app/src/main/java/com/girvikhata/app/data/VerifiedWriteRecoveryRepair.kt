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

        // records.load() verifies the encrypted authoritative store before it is trusted.
        val authoritative = records.load()
        val fingerprint = RelationalShadowFingerprint.sha256(authoritative)

        // Rebuild only the relational mirror. The encrypted snapshot is never replaced here.
        val verification = shadowFactory().use { shadow ->
            val status = shadow.replaceAll(authoritative)
            check(status.healthy) { status.reason ?: "Recovered relational shadow is unhealthy" }
            val dual = shadow.dualReadComparison(authoritative)
            check(dual.matches) { dual.reason ?: "Recovered relational shadow does not match encrypted khata" }
            dual
        }
        check(verification.snapshotFingerprint == fingerprint) {
            "Recovered authoritative fingerprint changed during verification"
        }

        // The current encrypted snapshot + rebuilt shadow now agree, so the stale transaction
        // metadata is no longer allowed to block future writes. No business rows are deleted.
        intentStore.fail("Stale interrupted write reconciled from verified authoritative snapshot")
        intentStore.clearCompleted()

        val finalDecision = recoveryCoordinator.reconcileOnStartup()
        check(finalDecision.action != InterruptedWriteRecoveryAction.BLOCK_AND_REQUIRE_RECOVERY) {
            finalDecision.reason
        }

        runCatching {
            journal.recordNamedEvent(
                type = "VERIFIED_WRITE_STALE_BLOCK_REPAIRED",
                title = "Stale save block repaired",
                detail = "${pending.transactionId} • ${pending.mutationLabel} • ${fingerprint.take(12)} • FULL_REBUILD",
            )
        }
        return VerifiedWriteRecoveryRepairResult(
            repaired = true,
            message = "Previous interrupted save was safely reconciled",
        )
    }
}

data class VerifiedWriteRecoveryRepairResult(
    val repaired: Boolean,
    val message: String,
)
