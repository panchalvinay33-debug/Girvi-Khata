package com.girvikhata.app.backup

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.security.SecureRandom

class RecoveryKeyStoreTest {
    @Test
    fun `generated recovery keys always satisfy portable character policy`() {
        repeat(100) { seed ->
            val key = RecoveryKeyStore.generate(SecureRandom(byteArrayOf(seed.toByte(), 2, 3, 4)))
            assertTrue(RecoveryKeyStore.isValid(key))
            assertTrue(key.startsWith("GK-"))
            assertTrue(key.any(Char::isLetter))
            assertTrue(key.any(Char::isDigit))
        }
    }

    @Test
    fun `generated recovery key works with portable backup crypto`() {
        val key = RecoveryKeyStore.generate(SecureRandom(byteArrayOf(7, 8, 9, 10)))
        val payload = "new-device-recovery".toByteArray()
        val encrypted = PortableBackupCrypto.encrypt(payload, key.toCharArray(), schemaVersion = 3)
        val decrypted = PortableBackupCrypto.decrypt(encrypted, key.toCharArray())
        assertTrue(decrypted.payload.contentEquals(payload))
    }

    @Test
    fun `lowercase and spaces normalize`() {
        val key = RecoveryKeyStore.generate()
        val noisy = "  ${key.lowercase()}  "
        assertTrue(RecoveryKeyStore.isValid(noisy))
    }

    @Test
    fun `changed checksum or body fails`() {
        val key = RecoveryKeyStore.generate()
        val last = key.last()
        val replacement = if (last == 'A') 'B' else 'A'
        assertFalse(RecoveryKeyStore.isValid(key.dropLast(1) + replacement))
    }
}
