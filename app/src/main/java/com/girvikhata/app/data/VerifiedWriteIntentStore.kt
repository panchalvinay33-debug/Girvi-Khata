package com.girvikhata.app.data

import android.content.Context

enum class VerifiedWriteIntentState { PENDING, COMMITTED, FAILED }

data class VerifiedWriteIntent(
    val transactionId: String,
    val mutationLabel: String,
    val expectedFingerprint: String,
    val targetFingerprint: String? = null,
    val state: VerifiedWriteIntentState,
    val startedAt: Long,
    val finishedAt: Long? = null,
    val reason: String? = null,
)

object VerifiedWriteIntentReducer {
    fun begin(transactionId: String, mutationLabel: String, expectedFingerprint: String, startedAt: Long): VerifiedWriteIntent =
        VerifiedWriteIntent(
            transactionId = transactionId,
            mutationLabel = mutationLabel,
            expectedFingerprint = expectedFingerprint,
            state = VerifiedWriteIntentState.PENDING,
            startedAt = startedAt,
        )

    fun commit(current: VerifiedWriteIntent, targetFingerprint: String, finishedAt: Long): VerifiedWriteIntent = current.copy(
        targetFingerprint = targetFingerprint,
        state = VerifiedWriteIntentState.COMMITTED,
        finishedAt = finishedAt,
        reason = null,
    )

    fun fail(current: VerifiedWriteIntent, reason: String, finishedAt: Long): VerifiedWriteIntent = current.copy(
        state = VerifiedWriteIntentState.FAILED,
        finishedAt = finishedAt,
        reason = reason.take(240),
    )
}

/** Stores only transaction/fingerprint metadata; no customer or ledger content. */
class VerifiedWriteIntentStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        "verified_write_intent_v1",
        Context.MODE_PRIVATE,
    )

    @Synchronized
    fun load(): VerifiedWriteIntent? {
        val transactionId = preferences.getString(KEY_TRANSACTION_ID, null) ?: return null
        val mutationLabel = preferences.getString(KEY_MUTATION_LABEL, null) ?: return null
        val expectedFingerprint = preferences.getString(KEY_EXPECTED_FINGERPRINT, null) ?: return null
        val state = runCatching {
            VerifiedWriteIntentState.valueOf(preferences.getString(KEY_STATE, null).orEmpty())
        }.getOrNull() ?: return null
        return VerifiedWriteIntent(
            transactionId = transactionId,
            mutationLabel = mutationLabel,
            expectedFingerprint = expectedFingerprint,
            targetFingerprint = preferences.getString(KEY_TARGET_FINGERPRINT, null),
            state = state,
            startedAt = preferences.getLong(KEY_STARTED_AT, 0L),
            finishedAt = if (preferences.contains(KEY_FINISHED_AT)) preferences.getLong(KEY_FINISHED_AT, 0L) else null,
            reason = preferences.getString(KEY_REASON, null),
        )
    }

    @Synchronized
    fun begin(transactionId: String, mutationLabel: String, expectedFingerprint: String, startedAt: Long = System.currentTimeMillis()): VerifiedWriteIntent {
        val intent = VerifiedWriteIntentReducer.begin(transactionId, mutationLabel, expectedFingerprint, startedAt)
        write(intent)
        return intent
    }

    @Synchronized
    fun commit(targetFingerprint: String, finishedAt: Long = System.currentTimeMillis()): VerifiedWriteIntent {
        val current = requireNotNull(load()) { "Verified write intent missing" }
        val intent = VerifiedWriteIntentReducer.commit(current, targetFingerprint, finishedAt)
        write(intent)
        return intent
    }

    @Synchronized
    fun fail(reason: String, finishedAt: Long = System.currentTimeMillis()): VerifiedWriteIntent? {
        val current = load() ?: return null
        val intent = VerifiedWriteIntentReducer.fail(current, reason, finishedAt)
        write(intent)
        return intent
    }

    @Synchronized
    fun clearCompleted() {
        val current = load() ?: return
        if (current.state != VerifiedWriteIntentState.PENDING) preferences.edit().clear().apply()
    }

    private fun write(intent: VerifiedWriteIntent) {
        val editor = preferences.edit()
            .putString(KEY_TRANSACTION_ID, intent.transactionId)
            .putString(KEY_MUTATION_LABEL, intent.mutationLabel)
            .putString(KEY_EXPECTED_FINGERPRINT, intent.expectedFingerprint)
            .putString(KEY_TARGET_FINGERPRINT, intent.targetFingerprint)
            .putString(KEY_STATE, intent.state.name)
            .putLong(KEY_STARTED_AT, intent.startedAt)
            .putString(KEY_REASON, intent.reason)
        if (intent.finishedAt == null) editor.remove(KEY_FINISHED_AT) else editor.putLong(KEY_FINISHED_AT, intent.finishedAt)
        editor.apply()
    }

    private companion object {
        const val KEY_TRANSACTION_ID = "transaction_id"
        const val KEY_MUTATION_LABEL = "mutation_label"
        const val KEY_EXPECTED_FINGERPRINT = "expected_fingerprint"
        const val KEY_TARGET_FINGERPRINT = "target_fingerprint"
        const val KEY_STATE = "state"
        const val KEY_STARTED_AT = "started_at"
        const val KEY_FINISHED_AT = "finished_at"
        const val KEY_REASON = "reason"
    }
}
