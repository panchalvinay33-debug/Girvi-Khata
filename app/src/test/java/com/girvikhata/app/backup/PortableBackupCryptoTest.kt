package com.girvikhata.app.backup

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class PortableBackupCryptoTest {
    private val passphrase = "MeriTijori2026".toCharArray()

    @Test
    fun roundTripPreservesPayloadAndMetadata() {
        val payload = "encrypted snapshot bytes".toByteArray()
        val packed = PortableBackupCrypto.encrypt(payload, passphrase, schemaVersion = 3, createdAt = 123456789L)
        val restored = PortableBackupCrypto.decrypt(packed, passphrase)

        assertArrayEquals(payload, restored.payload)
        assertEquals(3, restored.schemaVersion)
        assertEquals(123456789L, restored.createdAt)
        assertEquals(PortableBackupCrypto.sha256(payload), restored.payloadSha256)
    }

    @Test
    fun samePayloadProducesDifferentPackagesBecauseNonceAndSaltAreRandom() {
        val payload = "same".toByteArray()
        val first = PortableBackupCrypto.encrypt(payload, passphrase, 3)
        val second = PortableBackupCrypto.encrypt(payload, passphrase, 3)
        assertNotEquals(first.toList(), second.toList())
    }

    @Test
    fun wrongPassphraseIsRejected() {
        val packed = PortableBackupCrypto.encrypt("data".toByteArray(), passphrase, 3)
        assertThrows(IllegalArgumentException::class.java) {
            PortableBackupCrypto.decrypt(packed, "WrongPassphrase9".toCharArray())
        }
    }

    @Test
    fun tamperedCiphertextIsRejected() {
        val packed = PortableBackupCrypto.encrypt("important data".toByteArray(), passphrase, 3)
        packed[packed.lastIndex] = (packed.last().toInt() xor 1).toByte()
        assertThrows(IllegalArgumentException::class.java) {
            PortableBackupCrypto.decrypt(packed, passphrase)
        }
    }

    @Test
    fun weakPassphraseIsRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            PortableBackupCrypto.encrypt("data".toByteArray(), "short1".toCharArray(), 3)
        }
    }

    @Test
    fun trailingBytesAreRejected() {
        val packed = PortableBackupCrypto.encrypt("data".toByteArray(), passphrase, 3) + byteArrayOf(1)
        assertThrows(IllegalArgumentException::class.java) {
            PortableBackupCrypto.decrypt(packed, passphrase)
        }
    }
}
