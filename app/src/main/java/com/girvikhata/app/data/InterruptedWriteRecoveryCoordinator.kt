package com.girvikhata.app.data

import android.content.Context

/** Non-destructive startup reconciliation for interrupted coordinated writes. */
class InterruptedWriteRecoveryCoordinator(
    context: Context,
    private val records: EncryptedRecordStore = EncryptedRecordStore(context.applicationContext),
    private val intentStore: VerifiedWriteIntentStore = VerifiedWriteIntentStore(context.applicationContext),
    private val restoreIntentStore: RestoreGenerationIntentStore = RestoreGenerationIntentStore(context.applicationContext),
    private val observationStore: VerifiedWriteObservationStore = VerifiedWriteObservationStore(context.applicationContext),
    private val statusStore: InterruptedWriteRecoveryStatusStore = InterruptedWriteRecoveryStatusStore(context.applicationContext),
    private val journal: DataSafetyJournal = DataSafetyJournal(context.applicationContext),
    private val shadowFactory: () -> EncryptedRelationalShadowStore = {
        EncryptedRelationalShadowStore(context.applicationContext)
    },
) {
    @Synchronized
    fun reconcileOnStartup(): InterruptedWriteRecoveryDecision {
        val snapshot = records.load()
        val currentFingerprint = RelationalShadowFingerprint.sha256(snapshot)
        val restoreIntent = restoreIntentStore.load()
        if (
            restoreIntent != null &&
            restoreIntent.phase != RestoreGenerationPhase.COMPLETED &&
            !RestoreGenerationExecutionScope.allows(restoreIntent.generationId)
        ) {
            val blocked = InterruptedWriteRecoveryDecision(
                action = InterruptedWriteRecoveryAction.BLOCK_AND_REQUIRE_RECOVERY,
                transactionId = restoreIntent.generationId,
                reason = "Restore generation ${restoreIntent.phase.name.lowercase()} must reconcile before business writes",
            )
            statusStore.record(blocked, null, currentFingerprint)
            record(
                "RESTORE_GENERATION_RECOVERY_REQUIRED",
                "Restore generation blocks business writes",
                "${restoreIntent.generationId} • ${restoreIntent.phase.name} • ${blocked.reason}",
            )
            return blocked
        }

        val intent = intentStore.load()
        val initialDecision = InterruptedWriteRecoveryPolicy.evaluate(intent, currentFingerprint)

        val finalDecision = runCatching {
            applyDecision(initialDecision, intent, snapshot, currentFingerprint)
            initialDecision
        }.getOrElse { error ->
            val blocked = InterruptedWriteRecoveryPolicy.executionFailure(intent, error)
            record(
                "INTERRUPTED_WRITE_RECONCILIATION_FAILED",
                "Interrupted write recovery blocked safely",
                "${intent?.transactionId ?: "unknown"} • ${intent?.mutationLabel ?: "unknown"} • ${blocked.reason.take(180)}",
            )
            blocked
        }

        statusStore.record(finalDecision, intent, currentFingerprint)
        return finalDecision
    }

    private fun applyDecision(
        decision: InterruptedWriteRecoveryDecision,
        intent: VerifiedWriteIntent?,
        snapshot: AppSnapshot,
        currentFingerprint: String,
    ) {
        when (decision.action) {
            InterruptedWriteRecoveryAction.NONE -> Unit
            InterruptedWriteRecoveryAction.SAFE_TO_RETRY -> {
                val tx = requireNotNull(intent)
                val reason = "Interrupted before authoritative snapshot commit; safe to retry"
                intentStore.fail(reason)
                observationStore.recordFailure(tx.transactionId, reason)
                record(
                    "INTERRUPTED_WRITE_SAFE_TO_RETRY",
                    "Interrupted write did not commit",
                    "${tx.transactionId} • ${tx.mutationLabel} • ${currentFingerprint.take(12)}",
                )
            }
            InterruptedWriteRecoveryAction.VERIFY_RELATIONAL_AND_COMPLETE -> {
                val tx = requireNotNull(intent)
                val status = shadowFactory().use { shadow ->
                    shadow.syncIncremental(snapshot)
                    val dual = shadow.dualReadComparison(snapshot)
                    check(dual.matches) { dual.reason ?: "Interrupted-write relational verification failed" }
                    shadow.statusAgainst(snapshot)
                }
                check(status.healthy) { status.reason ?: "Interrupted-write relational status unhealthy" }
                val completedAt = System.currentTimeMillis()
                intentStore.commit(currentFingerprint, completedAt)
                observationStore.recordSuccess(
                    VerifiedBusinessWriteResult(
                        transactionId = tx.transactionId,
                        beforeFingerprint = tx.expectedFingerprint,
                        afterFingerprint = currentFingerprint,
                        relationalFingerprint = status.actualFingerprint,
                        syncMode = status.syncMode,
                        changedRows = status.changedRows ?: 0,
                        committedAt = completedAt,
                    ),
                )
                record(
                    "INTERRUPTED_WRITE_COMPLETED",
                    "Interrupted write proof completed",
                    "${tx.transactionId} • ${tx.mutationLabel} • ${currentFingerprint.take(12)} • ${status.syncMode ?: "SYNC"}",
                )
            }
            InterruptedWriteRecoveryAction.BLOCK_AND_REQUIRE_RECOVERY -> {
                record(
                    "INTERRUPTED_WRITE_RECOVERY_REQUIRED",
                    "Interrupted write needs recovery",
                    "${intent?.transactionId ?: "unknown"} • ${intent?.mutationLabel ?: "unknown"} • ${decision.reason.take(180)}",
                )
            }
        }
    }

    fun latestStatus(): InterruptedWriteRecoveryStatus? = statusStore.load()

    private fun record(type: String, title: String, detail: String) {
        runCatching { journal.recordNamedEvent(type, title, detail) }
    }
}
