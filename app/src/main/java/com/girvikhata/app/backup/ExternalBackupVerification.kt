package com.girvikhata.app.backup

import java.security.MessageDigest

data class ExternalBackupVerificationResult(
    val sha256: String,
    val schemaVersion: Int,
    val sizeBytes: Int,
)

/**
 * Verifies the exact bytes read back from a user-selected external document.
 * A backup is successful only when the written package is byte-identical and
 * decrypts to the expected snapshot payload/schema with the recovery phrase.
 */
object ExternalBackupVerification {
    fun verify(
        expectedPackage: ByteArray,
        writtenPackage: ByteArray,
        expectedPayload: ByteArray,
        expectedSchemaVersion: Int,
        passphrase: CharArray,
    ): ExternalBackupVerificationResult {
        require(expectedPackage.isNotEmpty()) { "Expected backup package empty hai" }
        require(writtenPackage.isNotEmpty()) { "Saved backup file empty hai" }
        require(writtenPackage.contentEquals(expectedPackage)) { "Saved file byte verification failed" }

        val decrypted = PortableBackupCrypto.decrypt(writtenPackage, passphrase)
        require(decrypted.schemaVersion == expectedSchemaVersion) { "Saved backup schema mismatch" }
        require(decrypted.payload.contentEquals(expectedPayload)) { "Saved backup payload mismatch" }

        return ExternalBackupVerificationResult(
            sha256 = sha256(writtenPackage),
            schemaVersion = decrypted.schemaVersion,
            sizeBytes = writtenPackage.size,
        )
    }

    private fun sha256(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256")
            .digest(bytes)
            .joinToString(separator = "") { "%02x".format(it) }
}
