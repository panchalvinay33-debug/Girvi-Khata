package com.girvikhata.app.backup

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * Portable backup envelope protected by an owner recovery passphrase.
 * It is independent from the Android Keystore so it can be restored after reinstall/device loss.
 */
object PortableBackupCrypto {
    private const val MAGIC = 0x474B4250 // GKBP
    private const val FORMAT_VERSION = 1
    private const val SALT_BYTES = 16
    private const val NONCE_BYTES = 12
    private const val KEY_BITS = 256
    private const val PBKDF2_ITERATIONS = 310_000
    private const val MAX_PAYLOAD_BYTES = 128 * 1024 * 1024
    private val aad = "GirviKhataPortableBackup/v1".toByteArray(Charsets.UTF_8)

    data class DecryptedPackage(
        val schemaVersion: Int,
        val createdAt: Long,
        val payload: ByteArray,
        val payloadSha256: String,
    )

    fun encrypt(
        plainPayload: ByteArray,
        passphrase: CharArray,
        schemaVersion: Int,
        createdAt: Long = System.currentTimeMillis(),
        random: SecureRandom = SecureRandom(),
    ): ByteArray {
        require(plainPayload.isNotEmpty()) { "Backup payload is empty" }
        require(plainPayload.size <= MAX_PAYLOAD_BYTES) { "Backup payload is too large" }
        validatePassphrase(passphrase)
        require(schemaVersion > 0) { "Invalid schema version" }

        val salt = ByteArray(SALT_BYTES).also(random::nextBytes)
        val nonce = ByteArray(NONCE_BYTES).also(random::nextBytes)
        val key = deriveKey(passphrase, salt)
        val cipherText = try {
            Cipher.getInstance("AES/GCM/NoPadding").run {
                init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(128, nonce))
                updateAAD(aad)
                doFinal(plainPayload)
            }
        } finally {
            key.encoded?.fill(0)
        }

        return ByteArrayOutputStream().use { bytes ->
            DataOutputStream(bytes).use { out ->
                out.writeInt(MAGIC)
                out.writeInt(FORMAT_VERSION)
                out.writeInt(schemaVersion)
                out.writeLong(createdAt)
                out.writeInt(PBKDF2_ITERATIONS)
                out.writeInt(salt.size)
                out.write(salt)
                out.writeInt(nonce.size)
                out.write(nonce)
                out.writeInt(cipherText.size)
                out.write(cipherText)
            }
            bytes.toByteArray()
        }
    }

    fun decrypt(packageBytes: ByteArray, passphrase: CharArray): DecryptedPackage {
        validatePassphrase(passphrase)
        require(packageBytes.size >= 64) { "Backup package is incomplete" }
        return DataInputStream(ByteArrayInputStream(packageBytes)).use { input ->
            require(input.readInt() == MAGIC) { "Not a Girvi Khata backup" }
            require(input.readInt() == FORMAT_VERSION) { "Unsupported backup format" }
            val schemaVersion = input.readInt().also { require(it > 0) { "Invalid backup schema" } }
            val createdAt = input.readLong().also { require(it > 0) { "Invalid backup timestamp" } }
            val iterations = input.readInt().also { require(it == PBKDF2_ITERATIONS) { "Unsupported key derivation settings" } }
            val salt = readBounded(input, SALT_BYTES, SALT_BYTES, "salt")
            val nonce = readBounded(input, NONCE_BYTES, NONCE_BYTES, "nonce")
            val cipherLength = input.readInt()
            require(cipherLength in 17..(MAX_PAYLOAD_BYTES + 32)) { "Invalid encrypted payload length" }
            val cipherText = ByteArray(cipherLength).also(input::readFully)
            require(input.available() == 0) { "Unexpected trailing backup data" }

            val key = deriveKey(passphrase, salt, iterations)
            val payload = try {
                Cipher.getInstance("AES/GCM/NoPadding").run {
                    init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, nonce))
                    updateAAD(aad)
                    doFinal(cipherText)
                }
            } catch (_: AEADBadTagException) {
                throw IllegalArgumentException("Wrong recovery passphrase or damaged backup")
            } finally {
                key.encoded?.fill(0)
            }
            require(payload.isNotEmpty() && payload.size <= MAX_PAYLOAD_BYTES) { "Invalid decrypted payload" }
            DecryptedPackage(schemaVersion, createdAt, payload, sha256(payload))
        }
    }

    fun sha256(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256")
        .digest(bytes)
        .joinToString("") { "%02x".format(it) }

    private fun readBounded(input: DataInputStream, min: Int, max: Int, label: String): ByteArray {
        val size = input.readInt()
        require(size in min..max) { "Invalid $label length" }
        return ByteArray(size).also(input::readFully)
    }

    private fun deriveKey(passphrase: CharArray, salt: ByteArray, iterations: Int = PBKDF2_ITERATIONS): SecretKeySpec {
        val spec = PBEKeySpec(passphrase, salt, iterations, KEY_BITS)
        return try {
            SecretKeySpec(SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded, "AES")
        } finally {
            spec.clearPassword()
        }
    }

    private fun validatePassphrase(passphrase: CharArray) {
        require(passphrase.size >= 12) { "Recovery passphrase must be at least 12 characters" }
        require(passphrase.any(Char::isLetter) && passphrase.any(Char::isDigit)) {
            "Recovery passphrase must include letters and digits"
        }
    }
}
