package com.girvikhata.app.backup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExternalBackupVerificationTest {
    private val passphrase = "VinayBackup2026".toCharArray()
    private val payload = "{\"schemaVersion\":3,\"customers\":[]}".toByteArray()

    @Test
    fun exactWrittenPackageDecryptsAndVerifies() {
        val encrypted = PortableBackupCrypto.encrypt(payload, passphrase, schemaVersion = 3)

        val result = ExternalBackupVerification.verify(
            expectedPackage = encrypted,
            writtenPackage = encrypted.copyOf(),
            expectedPayload = payload,
            expectedSchemaVersion = 3,
            passphrase = passphrase,
        )

        assertEquals(3, result.schemaVersion)
        assertEquals(encrypted.size, result.sizeBytes)
        assertEquals(64, result.sha256.length)
    }

    @Test
    fun changedWrittenByteIsRejectedBeforeSuccess() {
        val encrypted = PortableBackupCrypto.encrypt(payload, passphrase, schemaVersion = 3)
        val changed = encrypted.copyOf().also { it[it.lastIndex] = (it.last() xor 1) }

        val failure = runCatching {
            ExternalBackupVerification.verify(encrypted, changed, payload, 3, passphrase)
        }.exceptionOrNull()

        assertTrue(failure is IllegalArgumentException)
    }

    @Test
    fun truncatedExternalWriteIsRejected() {
        val encrypted = PortableBackupCrypto.encrypt(payload, passphrase, schemaVersion = 3)
        val truncated = encrypted.copyOf(encrypted.size - 1)

        val failure = runCatching {
            ExternalBackupVerification.verify(encrypted, truncated, payload, 3, passphrase)
        }.exceptionOrNull()

        assertTrue(failure is IllegalArgumentException)
    }

    @Test
    fun wrongExpectedSchemaIsRejected() {
        val encrypted = PortableBackupCrypto.encrypt(payload, passphrase, schemaVersion = 3)

        val failure = runCatching {
            ExternalBackupVerification.verify(encrypted, encrypted, payload, 4, passphrase)
        }.exceptionOrNull()

        assertTrue(failure is IllegalArgumentException)
    }

    @Test
    fun wrongRecoveryPhraseCannotVerifyPackage() {
        val encrypted = PortableBackupCrypto.encrypt(payload, passphrase, schemaVersion = 3)

        val failure = runCatching {
            ExternalBackupVerification.verify(
                encrypted,
                encrypted,
                payload,
                3,
                "WrongBackup2026".toCharArray(),
            )
        }.exceptionOrNull()

        assertTrue(failure != null)
    }
}

private infix fun Byte.xor(other: Int): Byte = (toInt() xor other).toByte()
