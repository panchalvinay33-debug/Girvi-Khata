package com.girvikhata.app.data

import android.content.Context

/** Persists restore-generation metadata only; never backup payloads, business rows, PINs or passphrases. */
class RestoreGenerationIntentStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        "restore_generation_intent_v1",
        Context.MODE_PRIVATE,
    )

    @Synchronized
    fun load(): RestoreGenerationIntent? {
        val generationId = preferences.getString(KEY_GENERATION_ID, null) ?: return null
        val phase = runCatching {
            RestoreGenerationPhase.valueOf(preferences.getString(KEY_PHASE, null).orEmpty())
        }.getOrNull() ?: return null
        return RestoreGenerationIntent(
            generationId = generationId,
            phase = phase,
            beforeBusinessFingerprint = preferences.getString(KEY_BEFORE_BUSINESS, "").orEmpty(),
            targetBusinessFingerprint = preferences.getString(KEY_TARGET_BUSINESS, "").orEmpty(),
            beforeMasterFingerprint = preferences.getString(KEY_BEFORE_MASTER, "").orEmpty(),
            targetMasterFingerprint = preferences.getString(KEY_TARGET_MASTER, "").orEmpty(),
            containsPortableMasters = preferences.getBoolean(KEY_PORTABLE_MASTERS, false),
            backupSha256Prefix = preferences.getString(KEY_BACKUP_PREFIX, "").orEmpty(),
            createdAt = preferences.getLong(KEY_CREATED_AT, 0L),
            updatedAt = preferences.getLong(KEY_UPDATED_AT, 0L),
            reason = preferences.getString(KEY_REASON, null),
        ).also(::validate)
    }

    @Synchronized
    fun save(intent: RestoreGenerationIntent): RestoreGenerationIntent {
        validate(intent)
        preferences.edit()
            .putString(KEY_GENERATION_ID, intent.generationId)
            .putString(KEY_PHASE, intent.phase.name)
            .putString(KEY_BEFORE_BUSINESS, intent.beforeBusinessFingerprint)
            .putString(KEY_TARGET_BUSINESS, intent.targetBusinessFingerprint)
            .putString(KEY_BEFORE_MASTER, intent.beforeMasterFingerprint)
            .putString(KEY_TARGET_MASTER, intent.targetMasterFingerprint)
            .putBoolean(KEY_PORTABLE_MASTERS, intent.containsPortableMasters)
            .putString(KEY_BACKUP_PREFIX, intent.backupSha256Prefix)
            .putLong(KEY_CREATED_AT, intent.createdAt)
            .putLong(KEY_UPDATED_AT, intent.updatedAt)
            .putString(KEY_REASON, intent.reason?.take(240))
            .commit()
            .also { check(it) { "Restore generation metadata persistence failed" } }
        return intent
    }

    @Synchronized
    fun transition(
        intent: RestoreGenerationIntent,
        phase: RestoreGenerationPhase,
        reason: String? = null,
        updatedAt: Long = System.currentTimeMillis(),
    ): RestoreGenerationIntent = save(
        intent.copy(
            phase = phase,
            reason = reason?.take(240),
            updatedAt = updatedAt,
        ),
    )

    @Synchronized
    fun clearCompleted() {
        val current = load() ?: return
        require(current.phase == RestoreGenerationPhase.COMPLETED) {
            "Incomplete restore generation cannot be cleared"
        }
        check(preferences.edit().clear().commit()) { "Restore generation metadata cleanup failed" }
    }

    private fun validate(intent: RestoreGenerationIntent) {
        require(intent.generationId.isNotBlank() && intent.generationId.length <= 80) { "Restore generation ID invalid" }
        require(intent.beforeBusinessFingerprint.length == 64) { "Before business fingerprint invalid" }
        require(intent.targetBusinessFingerprint.length == 64) { "Target business fingerprint invalid" }
        require(intent.beforeMasterFingerprint.length == 64) { "Before master fingerprint invalid" }
        require(intent.targetMasterFingerprint.length == 64) { "Target master fingerprint invalid" }
        require(intent.backupSha256Prefix.length in 8..32) { "Backup fingerprint prefix invalid" }
        require(intent.createdAt > 0L && intent.updatedAt >= intent.createdAt) { "Restore generation timestamp invalid" }
    }

    private companion object {
        const val KEY_GENERATION_ID = "generation_id"
        const val KEY_PHASE = "phase"
        const val KEY_BEFORE_BUSINESS = "before_business"
        const val KEY_TARGET_BUSINESS = "target_business"
        const val KEY_BEFORE_MASTER = "before_master"
        const val KEY_TARGET_MASTER = "target_master"
        const val KEY_PORTABLE_MASTERS = "portable_masters"
        const val KEY_BACKUP_PREFIX = "backup_prefix"
        const val KEY_CREATED_AT = "created_at"
        const val KEY_UPDATED_AT = "updated_at"
        const val KEY_REASON = "reason"
    }
}
