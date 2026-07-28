package com.girvikhata.app.security

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class PinSecurityTest {
    @Test
    fun validPinCanBeVerified() {
        val hasher = PinHasher(iterations = 10_000)
        val stored = hasher.create("482719".toCharArray())
        assertTrue(hasher.verify("482719".toCharArray(), stored))
        assertFalse(hasher.verify("482718".toCharArray(), stored))
    }

    @Test(expected = IllegalArgumentException::class)
    fun commonPinIsRejected() {
        PinHasher(iterations = 10_000).create("123456".toCharArray())
    }

    @Test
    fun fifthFailureStartsLockout() {
        val policy = PinAttemptPolicy()
        val now = Instant.parse("2026-07-28T10:00:00Z")
        var state = LockState()
        repeat(5) { state = policy.onFailure(state, now) }
        assertTrue(state.isLocked(now.plusSeconds(30)))
        assertFalse(state.isLocked(now.plusSeconds(61)))
    }
}
