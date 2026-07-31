package com.girvikhata.app.data

import android.content.Context

data class InterruptedWriteRecoveryStatus(
    val action: InterruptedWriteRecoveryAction,
    val transactionId: String? = null,
    val mutationLabel: String? = null,
    val reason: String,
    val snapshotFingerprint: String? = null,
    val evaluatedAt: Long,
) {
    val blocksBusinessWrites: Boolean
        get() = action == InterruptedWriteRecoveryAction.BLOCK_AND_REQUIRE_RECOVERY
}

object InterruptedWriteRecoveryStatusReducer {
    fun fromDecision(
        decision: InterruptedWriteRecoveryDecision,
        intent: VerifiedWriteIntent?,
        snapshotFingerprint: String,
        evaluatedAt: Long,
    ): InterruptedWriteRecoveryStatus = InterruptedWriteRecoveryStatus(
        action = decision.action,
        transactionId = decision.transactionId,
        mutationLabel = intent?.mutationLabel,
        reason = decision.reason.take(240),
        snapshotFingerprint = snapshotFingerprint,
        evaluatedAt = evaluatedAt,
    )
}

/** Persists only recovery metadata; no customer, item, payment, PIN or passphrase data. */
class InterruptedWriteRecoveryStatusStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        "interrupted_write_recovery_status_v1",
        Context.MODE_PRIVATE,
    )

    @Synchronized
    fun load(): InterruptedWriteRecoveryStatus? {
        val action = runCatching {
            InterruptedWriteRecoveryAction.valueOf(preferences.getString(KEY_ACTION, null).orEmpty())
        }.getOrNull() ?: return null
        return InterruptedWriteRecoveryStatus(
            action = action,
            transactionId = preferences.getString(KEY_TRANSACTION_ID, null),
            mutationLabel = preferences.getString(KEY_MUTATION_LABEL, null),
            reason = preferences.getString(KEY_REASON, "Recovery status unavailable").orEmpty(),
            snapshotFingerprint = preferences.getString(KEY_SNAPSHOT_FINGERPRINT, null),
            evaluatedAt = preferences.getLong(KEY_EVALUATED_AT, 0L),
        )
    }

    @Synchronized
    fun record(
        decision: InterruptedWriteRecoveryDecision,
        intent: VerifiedWriteIntent?,
        snapshotFingerprint: String,
        evaluatedAt: Long = System.currentTimeMillis(),
    ): InterruptedWriteRecoveryStatus {
        val status = InterruptedWriteRecoveryStatusReducer.fromDecision(
            decision = decision,
            intent = intent,
            snapshotFingerprint = snapshotFingerprint,
            evaluatedAt = evaluatedAt,
        )
        preferences.edit()
            .putString(KEY_ACTION, status.action.name)
            .putString(KEY_TRANSACTION_ID, status.transactionId)
            .putString(KEY_MUTATION_LABEL, status.mutationLabel)
            .putString(KEY_REASON, status.reason)
            .putString(KEY_SNAPSHOT_FINGERPRINT, status.snapshotFingerprint)
            .putLong(KEY_EVALUATED_AT, status.evaluatedAt)
            .apply()
        return status
    }

    private companion object {
        const val KEY_ACTION = "action"
        const val KEY_TRANSACTION_ID = "transaction_id"
        const val KEY_MUTATION_LABEL = "mutation_label"
        const val KEY_REASON = "reason"
        const val KEY_SNAPSHOT_FINGERPRINT = "snapshot_fingerprint"
        const val KEY_EVALUATED_AT = "evaluated_at"
    }
}
