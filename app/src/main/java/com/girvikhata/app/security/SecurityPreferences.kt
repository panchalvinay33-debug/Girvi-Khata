package com.girvikhata.app.security

import android.content.Context
import android.util.Base64
import java.time.Instant

/** Stores only a salted PIN verifier and lockout metadata. The raw PIN is never persisted. */
class SecurityPreferences(context: Context) {
    private val preferences = context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)
    private val hasher = PinHasher()
    private val policy = PinAttemptPolicy()

    fun hasPin(): Boolean = preferences.contains(KEY_HASH) && preferences.contains(KEY_SALT)

    fun verifierStatus(): PinVerifierStatus {
        if (!hasPin()) return PinVerifierStatus.NOT_CONFIGURED
        return if (readStoredPin() == null) PinVerifierStatus.CORRUPT else PinVerifierStatus.READY
    }

    fun savePin(pin: CharArray) {
        val stored = hasher.create(pin)
        preferences.edit()
            .putString(KEY_HASH, Base64.encodeToString(stored.hash, Base64.NO_WRAP))
            .putString(KEY_SALT, Base64.encodeToString(stored.salt, Base64.NO_WRAP))
            .putInt(KEY_ITERATIONS, stored.iterations)
            .putInt(KEY_FAILURES, 0)
            .putLong(KEY_LOCKED_UNTIL, 0L)
            .commit()
    }

    /**
     * Called only after Android biometric/device authentication succeeds.
     * Clears PIN verifier and lockout metadata, never business records or encryption keys.
     */
    fun clearPinAfterAuthenticatedRecovery() {
        preferences.edit()
            .remove(KEY_HASH)
            .remove(KEY_SALT)
            .remove(KEY_ITERATIONS)
            .putInt(KEY_FAILURES, 0)
            .putLong(KEY_LOCKED_UNTIL, 0L)
            .commit()
    }

    fun verify(pin: CharArray, now: Instant = Instant.now()): PinVerificationResult {
        val current = readLockState()
        if (current.isLocked(now)) {
            pin.fill('\u0000')
            return PinVerificationResult.Locked(current.lockedUntil!!.toEpochMilli())
        }

        val stored = readStoredPin() ?: run {
            pin.fill('\u0000')
            return PinVerificationResult.NotConfigured
        }

        val valid = runCatching { hasher.verify(pin, stored) }.getOrDefault(false)
        if (valid) {
            writeLockState(policy.onSuccess())
            return PinVerificationResult.Success
        }

        val next = policy.onFailure(current, now)
        writeLockState(next)
        return PinVerificationResult.Failure(
            attempts = next.failedAttempts,
            lockedUntilMillis = next.lockedUntil?.toEpochMilli() ?: 0L,
        )
    }

    private fun readStoredPin(): StoredPin? {
        val hash = preferences.getString(KEY_HASH, null) ?: return null
        val salt = preferences.getString(KEY_SALT, null) ?: return null
        return runCatching {
            val decodedHash = Base64.decode(hash, Base64.NO_WRAP)
            val decodedSalt = Base64.decode(salt, Base64.NO_WRAP)
            val iterations = preferences.getInt(KEY_ITERATIONS, 210_000)
            require(decodedHash.size == 32) { "Invalid PIN hash" }
            require(decodedSalt.size >= 16) { "Invalid PIN salt" }
            require(iterations in 100_000..2_000_000) { "Invalid PIN iterations" }
            StoredPin(hash = decodedHash, salt = decodedSalt, iterations = iterations)
        }.getOrNull()
    }

    private fun readLockState(): LockState {
        val until = preferences.getLong(KEY_LOCKED_UNTIL, 0L)
        return LockState(
            failedAttempts = preferences.getInt(KEY_FAILURES, 0).coerceAtLeast(0),
            lockedUntil = until.takeIf { it > 0 }?.let(Instant::ofEpochMilli),
        )
    }

    private fun writeLockState(state: LockState) {
        preferences.edit()
            .putInt(KEY_FAILURES, state.failedAttempts)
            .putLong(KEY_LOCKED_UNTIL, state.lockedUntil?.toEpochMilli() ?: 0L)
            .commit()
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

enum class PinVerifierStatus { NOT_CONFIGURED, READY, CORRUPT }

sealed interface PinVerificationResult {
    data object Success : PinVerificationResult
    data object NotConfigured : PinVerificationResult
    data class Locked(val untilMillis: Long) : PinVerificationResult
    data class Failure(val attempts: Int, val lockedUntilMillis: Long) : PinVerificationResult
}
