package com.girvikhata.app.data

import android.content.Context

/**
 * Persists only non-customer migration evidence. No names, mobile numbers,
 * addresses, item notes, payment notes, PINs or secrets are stored here.
 */
class VerifiedWriteObservationStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        "verified_write_observation_v1",
        Context.MODE_PRIVATE,
    )

    @Synchronized
    fun load(): VerifiedWriteObservation = VerifiedWriteObservation(
        successfulWrites = preferences.getInt(KEY_SUCCESSFUL_WRITES, 0),
        lastTransactionId = preferences.getString(KEY_LAST_TRANSACTION_ID, null),
        lastCommittedAt = preferences.longOrNull(KEY_LAST_COMMITTED_AT),
        lastSnapshotFingerprint = preferences.getString(KEY_LAST_SNAPSHOT_FINGERPRINT, null),
        lastRelationalFingerprint = preferences.getString(KEY_LAST_RELATIONAL_FINGERPRINT, null),
        lastFailureAt = preferences.longOrNull(KEY_LAST_FAILURE_AT),
        lastFailureReason = preferences.getString(KEY_LAST_FAILURE_REASON, null),
    )

    @Synchronized
    fun recordSuccess(result: VerifiedBusinessWriteResult): VerifiedWriteObservation {
        val next = VerifiedWriteObservationReducer.success(load(), result)
        write(next)
        return next
    }

    @Synchronized
    fun recordFailure(transactionId: String, reason: String, failedAt: Long = System.currentTimeMillis()): VerifiedWriteObservation {
        val next = VerifiedWriteObservationReducer.failure(load(), transactionId, reason, failedAt)
        write(next)
        return next
    }

    private fun write(value: VerifiedWriteObservation) {
        preferences.edit()
            .putInt(KEY_SUCCESSFUL_WRITES, value.successfulWrites)
            .putString(KEY_LAST_TRANSACTION_ID, value.lastTransactionId)
            .putLongOrRemove(KEY_LAST_COMMITTED_AT, value.lastCommittedAt)
            .putString(KEY_LAST_SNAPSHOT_FINGERPRINT, value.lastSnapshotFingerprint)
            .putString(KEY_LAST_RELATIONAL_FINGERPRINT, value.lastRelationalFingerprint)
            .putLongOrRemove(KEY_LAST_FAILURE_AT, value.lastFailureAt)
            .putString(KEY_LAST_FAILURE_REASON, value.lastFailureReason)
            .apply()
    }

    private fun android.content.SharedPreferences.longOrNull(key: String): Long? =
        if (contains(key)) getLong(key, 0L) else null

    private fun android.content.SharedPreferences.Editor.putLongOrRemove(key: String, value: Long?): android.content.SharedPreferences.Editor =
        if (value == null) remove(key) else putLong(key, value)

    private companion object {
        const val KEY_SUCCESSFUL_WRITES = "successful_writes"
        const val KEY_LAST_TRANSACTION_ID = "last_transaction_id"
        const val KEY_LAST_COMMITTED_AT = "last_committed_at"
        const val KEY_LAST_SNAPSHOT_FINGERPRINT = "last_snapshot_fingerprint"
        const val KEY_LAST_RELATIONAL_FINGERPRINT = "last_relational_fingerprint"
        const val KEY_LAST_FAILURE_AT = "last_failure_at"
        const val KEY_LAST_FAILURE_REASON = "last_failure_reason"
    }
}

object VerifiedWriteObservationReducer {
    fun success(
        current: VerifiedWriteObservation,
        result: VerifiedBusinessWriteResult,
    ): VerifiedWriteObservation = current.copy(
        successfulWrites = current.successfulWrites + 1,
        lastTransactionId = result.transactionId,
        lastCommittedAt = result.committedAt,
        lastSnapshotFingerprint = result.afterFingerprint,
        lastRelationalFingerprint = result.relationalFingerprint,
        lastFailureReason = null,
    )

    fun failure(
        current: VerifiedWriteObservation,
        transactionId: String,
        reason: String,
        failedAt: Long,
    ): VerifiedWriteObservation = current.copy(
        lastTransactionId = transactionId,
        lastFailureAt = failedAt,
        lastFailureReason = reason.take(240),
    )
}
