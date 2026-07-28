package com.girvikhata.app.security

import java.security.MessageDigest
import java.security.SecureRandom
import java.time.Duration
import java.time.Instant
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

data class StoredPin(
    val salt: ByteArray,
    val hash: ByteArray,
    val iterations: Int,
)

data class LockState(
    val failedAttempts: Int = 0,
    val lockedUntil: Instant? = null,
) {
    fun isLocked(now: Instant): Boolean = lockedUntil?.isAfter(now) == true
}

class PinHasher(
    private val secureRandom: SecureRandom = SecureRandom(),
    private val iterations: Int = 210_000,
) {
    fun create(pin: CharArray): StoredPin {
        validatePin(pin)
        val salt = ByteArray(16).also(secureRandom::nextBytes)
        return StoredPin(salt, derive(pin, salt, iterations), iterations)
    }

    fun verify(pin: CharArray, stored: StoredPin): Boolean {
        validatePin(pin)
        val candidate = derive(pin, stored.salt, stored.iterations)
        return MessageDigest.isEqual(candidate, stored.hash)
    }

    private fun derive(pin: CharArray, salt: ByteArray, iterations: Int): ByteArray {
        val spec = PBEKeySpec(pin, salt, iterations, 256)
        return try {
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
            pin.fill('\u0000')
        }
    }

    private fun validatePin(pin: CharArray) {
        require(pin.size == 6) { "PIN must contain six digits" }
        require(pin.all(Char::isDigit)) { "PIN must contain digits only" }
        require(pin.toSet().size > 1) { "Repeated-digit PIN is not allowed" }
        require(String(pin) !in weakPins) { "Common PIN is not allowed" }
    }

    private companion object {
        val weakPins = setOf("123456", "654321", "111111", "000000", "121212", "112233")
    }
}

class PinAttemptPolicy {
    fun onSuccess(): LockState = LockState()

    fun onFailure(previous: LockState, now: Instant): LockState {
        val attempts = previous.failedAttempts + 1
        val delay = when {
            attempts < 5 -> Duration.ZERO
            attempts == 5 -> Duration.ofMinutes(1)
            attempts == 6 -> Duration.ofMinutes(5)
            attempts == 7 -> Duration.ofMinutes(15)
            attempts == 8 -> Duration.ofHours(1)
            else -> Duration.ofHours(24)
        }
        return LockState(attempts, if (delay.isZero) null else now.plus(delay))
    }
}
