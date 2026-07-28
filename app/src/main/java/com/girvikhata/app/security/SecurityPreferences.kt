package com.girvikhata.app.security

import android.content.Context
import android.util.Base64

/**
 * Stores only a salted PIN verifier and lockout metadata.
 * The raw PIN is never persisted.
 */
class SecurityPreferences(context: Context) {
    private val preferences = context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)

    fun hasPin(): Boolean = preferences.contains(KEY_HASH) && preferences.contains(KEY_SALT)

    fun savePin(pin: CharArray) {
        val record = PinHasher.create(pin)
        preferences.edit()
            .putString(KEY_HASH, Base64.encodeToString(record.hash, Base64.NO_WRAP))
            .putString(KEY_SALT, Base64.encodeToString(record.salt, Base64.NO_WRAP))
            .putInt(KEY_ITERATIONS, record.iterations)
            .putInt(KEY_FAILURES, 0)
            .putLong(KEY_LOCKED_UNTIL, 0L)
            .apply()
        pin.fill('\u0000')
    }

    fun verify(pin: CharArray, nowMillis: Long = System.currentTimeMillis()): PinVerificationResult {
        val lockedUntil = preferences.getLong(KEY_LOCKED_UNTIL, 0L)
        if (lockedUntil > nowMillis) {
            pin.fill('\u0000')
            return PinVerificationResult.Locked(lockedUntil)
        }

        val record = readRecord() ?: run {
            pin.fill('\u0000')
            return PinVerificationResult.NotConfigured
        }

        val valid = PinHasher.verify(pin, record)
        pin.fill('\u0000')
        if (valid) {
            preferences.edit().putInt(KEY_FAILURES, 0).putLong(KEY_LOCKED_UNTIL, 0L).apply()
            return PinVerificationResult.Success
        }

        val failures = preferences.getInt(KEY_FAILURES, 0) + 1
        val delayMillis = LockoutPolicy.delayMillis(failures)
        val nextLockedUntil = if (delayMillis > 0) nowMillis + delayMillis else 0L
        preferences.edit()
            .putInt(KEY_FAILURES, failures)
            .putLong(KEY_LOCKED_UNTIL, nextLockedUntil)
            .apply()
        return PinVerificationResult.Failure(failures, nextLockedUntil)
    }

    fun clearForDevelopmentOnly() {
        preferences.edit().clear().apply()
    }

    private fun readRecord(): PinRecord? {
        val hash = preferences.getString(KEY_HASH, null) ?: return null
        val salt = preferences.getString(KEY_SALT, null) ?: return null
        return runCatching {
            PinRecord(
                hash = Base64.decode(hash, Base64.NO_WRAP),
                salt = Base64.decode(salt, Base64.NO_WRAP),
                iterations = preferences.getInt(KEY_ITERATIONS, PinHasher.DEFAULT_ITERATIONS),
            )
        }.getOrNull()
    }

    companion object {
        private const val FILE_NAME = "security_state"
        private const val KEY_HASH = "pin_hash"
        private const val KEY_SALT = "pin_salt"
        private const val KEY_ITERATIONS = "pin_iterations"
        private const val KEY_FAILURES = "pin_failures"
        private const val KEY_LOCKED_UNTIL = "locked_until"
    }
}

sealed interface PinVerificationResult {
    data object Success : PinVerificationResult
    data object NotConfigured : PinVerificationResult
    data class Locked(val untilMillis: Long) : PinVerificationResult
    data class Failure(val attempts: Int, val lockedUntilMillis: Long) : PinVerificationResult
}
