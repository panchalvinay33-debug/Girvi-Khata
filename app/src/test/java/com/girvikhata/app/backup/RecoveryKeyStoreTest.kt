package com.girvikhata.app.backup

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.security.SecureRandom

class RecoveryKeyStoreTest {
    @Test
    fun `generated recovery key validates`() {
        val key = RecoveryKeyStore.generate(SecureRandom(byteArrayOf(1, 2, 3, 4)))
        assertTrue(RecoveryKeyStore.isValid(key))
        assertTrue(key.startsWith("GK-"))
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
