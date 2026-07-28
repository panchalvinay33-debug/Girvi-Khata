package com.girvikhata.app.security

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionSecurityTest {
    @Test
    fun sessionDoesNotLockWithoutBackgroundTimestamp() {
        assertFalse(SessionAutoLockPolicy(30_000).shouldLock(null, 100_000))
    }

    @Test
    fun sessionLocksAfterTimeout() {
        assertTrue(SessionAutoLockPolicy(30_000).shouldLock(10_000, 40_000))
    }

    @Test
    fun sessionStaysUnlockedBeforeTimeout() {
        assertFalse(SessionAutoLockPolicy(30_000).shouldLock(10_000, 39_999))
    }

    @Test
    fun clockRollbackLocksConservatively() {
        assertTrue(SessionAutoLockPolicy(30_000).shouldLock(50_000, 40_000))
    }

    @Test(expected = IllegalArgumentException::class)
    fun negativeTimeoutIsRejected() {
        SessionAutoLockPolicy(-1)
    }
}
