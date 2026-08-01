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
        val status = shadowFactory().use { shadow ->
            shadow.replaceAll(authoritative)
            val dual = shadow.dualReadComparison(authoritative)
            check(dual.matches) { dual.reason ?: "Recovered relational shadow does not match encrypted khata" }
            shadow.statusAgainst(authoritative)
        }
        check(status.healthy) { status.reason ?: "Recovered relational shadow is unhealthy" }
        check(status.actualFingerprint == fingerprint) { "Recovered relational fingerprint mismatch" }

        intentStore.fail("Stale interrupted write reconciled from verified authoritative snapshot")
        val finalDecision = recoveryCoordinator.reconcileOnStartup()
        check(finalDecision.action != InterruptedWriteRecoveryAction.BLOCK_AND_REQUIRE_RECOVERY) {
            finalDecision.reason
        }

        runCatching {
            journal.recordNamedEvent(
                type = "VERIFIED_WRITE_STALE_BLOCK_REPAIRED",
                title = "Stale save block repaired",
                detail = "${pending.transactionId} • ${pending.mutationLabel} • ${fingerprint.take(12)} • ${status.syncMode ?: "FULL_REBUILD"}",
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
