package com.girvikhata.app.backup

import android.content.Context
import com.girvikhata.app.security.DeviceKeyManager
import com.girvikhata.app.security.EncryptedPayload
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

/**
 * Portable owner recovery key.
 *
 * The key is shown to the owner and must be kept outside the phone. A wrapped copy is stored locally
 * under Android Keystore only so automatic backups can run without asking for the key every time.
 * Losing both the phone and the external copy of this key intentionally makes encrypted backups
 * unrecoverable.
 */
class RecoveryKeyStore(
    context: Context,
    private val keyManager: DeviceKeyManager = DeviceKeyManager(),
) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun hasRecoveryKey(): Boolean = prefs.contains(KEY_CIPHERTEXT) && prefs.contains(KEY_IV)

    @Synchronized
    fun createIfMissing(): String {
        if (hasRecoveryKey()) return reveal()
        val key = generate()
        store(key)
        return key
    }

    @Synchronized
    fun importAndStore(value: String): String {
        val normalized = normalize(value)
        require(isValid(normalized)) { "Recovery key invalid" }
        store(normalized)
        return normalized
    }

    @Synchronized
    fun reveal(): String {
        val cipher = prefs.getString(KEY_CIPHERTEXT, null) ?: error("Recovery key not configured")
        val iv = prefs.getString(KEY_IV, null) ?: error("Recovery key not configured")
        val plain = keyManager.decrypt(
            EncryptedPayload(
                ciphertext = Base64.getDecoder().decode(cipher),
                iv = Base64.getDecoder().decode(iv),
            ),
            AAD,
        )
        return String(plain, Charsets.UTF_8).also {
            plain.fill(0)
            require(isValid(it)) { "Stored recovery key invalid" }
        }
    }

    fun fingerprint(): String? = runCatching { reveal() }.getOrNull()?.let(::fingerprintOf)

    private fun store(value: String) {
        val bytes = value.toByteArray(Charsets.UTF_8)
        val encrypted = try { keyManager.encrypt(bytes, AAD) } finally { bytes.fill(0) }
        prefs.edit()
            .putString(KEY_CIPHERTEXT, Base64.getEncoder().encodeToString(encrypted.ciphertext))
            .putString(KEY_IV, Base64.getEncoder().encodeToString(encrypted.iv))
            .putString(KEY_FINGERPRINT, fingerprintOf(value))
            .apply()
    }

    companion object {
        private const val PREFS = "recovery_key_v1"
        private const val KEY_CIPHERTEXT = "ciphertext"
        private const val KEY_IV = "iv"
        private const val KEY_FINGERPRINT = "fingerprint"
        private val AAD = "girvi-khata-recovery-key-v1".toByteArray(Charsets.UTF_8)
        private const val LETTERS = "ABCDEFGHJKLMNPQRSTUVWXYZ"
        private const val DIGITS = "23456789"
        private const val ALPHABET = LETTERS + DIGITS
        private const val RANDOM_CHARS = 24

        fun normalize(value: String): String = value.trim().uppercase().replace(" ", "")

        fun isValid(value: String): Boolean {
            val normalized = normalize(value)
            val parts = normalized.split('-')
            if (parts.size != 8 || parts.first() != "GK") return false
            if (parts.drop(1).any { it.length != 4 || it.any { ch -> ch !in ALPHABET } }) return false
            val random = parts.drop(1).take(6).joinToString("")
            return parts.last() == checksum(random)
        }

        fun fingerprintOf(value: String): String = MessageDigest.getInstance("SHA-256")
            .digest(normalize(value).toByteArray(Charsets.UTF_8))
            .take(6)
            .joinToString("") { "%02X".format(it) }

        fun generate(random: SecureRandom = SecureRandom()): String {
            val chars = CharArray(RANDOM_CHARS) { ALPHABET[random.nextInt(ALPHABET.length)] }
            // PortableBackupCrypto requires both letters and digits. Guarantee that invariant rather
            // than relying on probability, so every generated recovery key is immediately usable.
            chars[0] = LETTERS[random.nextInt(LETTERS.length)]
            chars[1] = DIGITS[random.nextInt(DIGITS.length)]
            val body = String(chars)
            val groups = body.chunked(4)
            return (listOf("GK") + groups + checksum(body)).joinToString("-")
        }

        private fun checksum(body: String): String {
            val digest = MessageDigest.getInstance("SHA-256").digest(body.toByteArray(Charsets.US_ASCII))
            var value = ((digest[0].toInt() and 0xFF) shl 24) or
                ((digest[1].toInt() and 0xFF) shl 16) or
                ((digest[2].toInt() and 0xFF) shl 8) or
                (digest[3].toInt() and 0xFF)
            if (value == Int.MIN_VALUE) value = 0
            value = kotlin.math.abs(value)
            return buildString(4) {
                repeat(4) {
                    append(ALPHABET[value % ALPHABET.length])
                    value /= ALPHABET.length
                }
            }
        }
    }
}
