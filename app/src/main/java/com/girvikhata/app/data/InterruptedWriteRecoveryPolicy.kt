package com.girvikhata.app.data

enum class InterruptedWriteRecoveryAction {
    NONE,
    SAFE_TO_RETRY,
    VERIFY_RELATIONAL_AND_COMPLETE,
    BLOCK_AND_REQUIRE_RECOVERY,
}

data class InterruptedWriteRecoveryDecision(
    val action: InterruptedWriteRecoveryAction,
    val transactionId: String? = null,
    val reason: String,
)

object InterruptedWriteRecoveryPolicy {
    fun evaluate(intent: VerifiedWriteIntent?, currentSnapshotFingerprint: String): InterruptedWriteRecoveryDecision {
        if (intent == null) return InterruptedWriteRecoveryDecision(
            action = InterruptedWriteRecoveryAction.NONE,
            reason = "No coordinated write intent",
        )
        if (intent.state != VerifiedWriteIntentState.PENDING) return InterruptedWriteRecoveryDecision(
            action = InterruptedWriteRecoveryAction.NONE,
            transactionId = intent.transactionId,
            reason = "Latest coordinated write is ${intent.state.name.lowercase()}",
        )
        if (currentSnapshotFingerprint == intent.expectedFingerprint) return InterruptedWriteRecoveryDecision(
            action = InterruptedWriteRecoveryAction.SAFE_TO_RETRY,
            transactionId = intent.transactionId,
            reason = "Authoritative snapshot remained at pre-write fingerprint",
        )
        if (!intent.targetFingerprint.isNullOrBlank() && currentSnapshotFingerprint == intent.targetFingerprint) {
            return InterruptedWriteRecoveryDecision(
                action = InterruptedWriteRecoveryAction.VERIFY_RELATIONAL_AND_COMPLETE,
                transactionId = intent.transactionId,
                reason = "Authoritative snapshot reached target fingerprint; relational proof must be completed",
            )
        }
        return InterruptedWriteRecoveryDecision(
            action = InterruptedWriteRecoveryAction.BLOCK_AND_REQUIRE_RECOVERY,
            transactionId = intent.transactionId,
            reason = "Snapshot fingerprint matches neither pre-write nor known target state",
        )
    }

    fun mayMarkFailed(intent: VerifiedWriteIntent?, currentSnapshotFingerprint: String?): Boolean =
        intent != null &&
            intent.state == VerifiedWriteIntentState.PENDING &&
            currentSnapshotFingerprint != null &&
            currentSnapshotFingerprint == intent.expectedFingerprint

    fun executionFailure(intent: VerifiedWriteIntent?, cause: Throwable): InterruptedWriteRecoveryDecision {
        val detail = cause.message?.takeIf { it.isNotBlank() } ?: cause::class.java.simpleName
        return InterruptedWriteRecoveryDecision(
            action = InterruptedWriteRecoveryAction.BLOCK_AND_REQUIRE_RECOVERY,
            transactionId = intent?.transactionId,
            reason = "Startup reconciliation failed safely: ${detail.take(180)}",
        )
    }
}
