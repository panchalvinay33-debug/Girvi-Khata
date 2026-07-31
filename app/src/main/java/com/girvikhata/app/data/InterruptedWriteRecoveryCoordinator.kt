package com.girvikhata.app.data

import android.content.Context

/**
 * Non-destructive process-start reconciliation for an interrupted coordinated write.
 * The authoritative snapshot is never replaced here.
 */
class InterruptedWriteRecoveryCoordinator(
    context: Context,
    private val records: EncryptedRecordStore = EncryptedRecordStore(context.applicationContext),
    private val intentStore: VerifiedWriteIntentStore = VerifiedWriteIntentStore(context.applicationContext),
    private val observationStore: VerifiedWriteObservationStore = VerifiedWriteObservationStore(context.applicationContext),
    private val journal: DataSafetyJournal = DataSafetyJournal(context.applicationContext),
    private val shadowFactory: () -> EncryptedRelationalShadowStore = {
        EncryptedRelationalShadowStore(context.applicationContext)
    },
) {
    @Synchronized
    fun reconcileOnStartup(): InterruptedWriteRecoveryDecision {
        val intent = intentStore.load()
        val snapshot = records.load()
        val currentFingerprint = RelationalShadowFingerprint.sha256(snapshot)
        val decision = InterruptedWriteRecoveryPolicy.evaluate(intent, currentFingerprint)

        when (decision.action) {
            InterruptedWriteRecoveryAction.NONE -> Unit
            InterruptedWriteRecoveryAction.SAFE_TO_RETRY -> {
                val tx = requireNotNull(intent)
                val reason = "Interrupted before authoritative snapshot commit; safe to retry"
                intentStore.fail(reason)
                observationStore.recordFailure(tx.transactionId, reason)
                record(
                    type = "INTERRUPTED_WRITE_SAFE_TO_RETRY",
                    title = "Interrupted write did not commit",
                    detail = "${tx.transactionId} • ${tx.mutationLabel} • ${currentFingerprint.take(12)}",
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
                    type = "INTERRUPTED_WRITE_COMPLETED",
                    title = "Interrupted write proof completed",
                    detail = "${tx.transactionId} • ${tx.mutationLabel} • ${currentFingerprint.take(12)} • ${status.syncMode ?: "SYNC"}",
                )
            }
            InterruptedWriteRecoveryAction.BLOCK_AND_REQUIRE_RECOVERY -> {
                val tx = intent
                record(
                    type = "INTERRUPTED_WRITE_RECOVERY_REQUIRED",
                    title = "Interrupted write needs recovery",
                    detail = "${tx?.transactionId ?: "unknown"} • ${tx?.mutationLabel ?: "unknown"} • ${decision.reason.take(180)}",
                )
            }
        }
        return decision
    }

    private fun record(type: String, title: String, detail: String) {
        runCatching { journal.recordNamedEvent(type, title, detail) }
    }
}
