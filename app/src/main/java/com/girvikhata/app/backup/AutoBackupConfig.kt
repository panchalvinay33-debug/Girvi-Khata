package com.girvikhata.app.backup

import android.content.Context
import android.net.Uri

class AutoBackupConfig(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    data class Status(
        val enabled: Boolean,
        val folderUri: String?,
        val lastSuccessAt: Long,
        val lastAttemptAt: Long,
        val lastSha256: String?,
        val lastError: String?,
        val generationCount: Int,
    )

    fun status(): Status = Status(
        enabled = prefs.getBoolean(KEY_ENABLED, false),
        folderUri = prefs.getString(KEY_FOLDER_URI, null),
        lastSuccessAt = prefs.getLong(KEY_LAST_SUCCESS, 0L),
        lastAttemptAt = prefs.getLong(KEY_LAST_ATTEMPT, 0L),
        lastSha256 = prefs.getString(KEY_LAST_SHA, null),
        lastError = prefs.getString(KEY_LAST_ERROR, null),
        generationCount = prefs.getInt(KEY_GENERATIONS, 0),
    )

    fun configure(folder: Uri, enabled: Boolean = true) {
        prefs.edit()
            .putString(KEY_FOLDER_URI, folder.toString())
            .putBoolean(KEY_ENABLED, enabled)
            .remove(KEY_LAST_ERROR)
            .apply()
    }

    fun setEnabled(enabled: Boolean) = prefs.edit().putBoolean(KEY_ENABLED, enabled).apply()

    fun recordAttempt() = prefs.edit().putLong(KEY_LAST_ATTEMPT, System.currentTimeMillis()).apply()

    fun recordSuccess(sha256: String, generationCount: Int) = prefs.edit()
        .putLong(KEY_LAST_SUCCESS, System.currentTimeMillis())
        .putLong(KEY_LAST_ATTEMPT, System.currentTimeMillis())
        .putString(KEY_LAST_SHA, sha256)
        .putInt(KEY_GENERATIONS, generationCount)
        .remove(KEY_LAST_ERROR)
        .apply()

    fun recordFailure(message: String) = prefs.edit()
        .putLong(KEY_LAST_ATTEMPT, System.currentTimeMillis())
        .putString(KEY_LAST_ERROR, message.take(300))
        .apply()

    companion object {
        private const val PREFS = "auto_backup_v1"
        private const val KEY_ENABLED = "enabled"
        private const val KEY_FOLDER_URI = "folder_uri"
        private const val KEY_LAST_SUCCESS = "last_success"
        private const val KEY_LAST_ATTEMPT = "last_attempt"
        private const val KEY_LAST_SHA = "last_sha"
        private const val KEY_LAST_ERROR = "last_error"
        private const val KEY_GENERATIONS = "generations"
    }
}
