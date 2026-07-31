package com.girvikhata.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RestoreSafetyPolicyTest {
    @Test
    fun smallPackageStillRequires64Mb() {
        val required = RestoreSafetyPolicy.requiredBytes(1L * 1024L * 1024L)
        assertEquals(64L * 1024L * 1024L, required)
    }

    @Test
    fun largerPackageRequiresThreeTimesWorkspace() {
        val required = RestoreSafetyPolicy.requiredBytes(40L * 1024L * 1024L)
        assertEquals(120L * 1024L * 1024L, required)
    }

    @Test
    fun exactRequiredStorageIsAllowed() {
        val required = RestoreSafetyPolicy.requiredBytes(30L * 1024L * 1024L)
        assertTrue(RestoreSafetyPolicy.evaluate(30L * 1024L * 1024L, required).allowed)
    }

    @Test
    fun oneByteShortIsBlockedWithEvidence() {
        val required = RestoreSafetyPolicy.requiredBytes(30L * 1024L * 1024L)
        val decision = RestoreSafetyPolicy.evaluate(30L * 1024L * 1024L, required - 1L)
        assertFalse(decision.allowed)
        assertEquals(required, decision.requiredBytes)
        assertTrue(decision.reason.orEmpty().contains("free space"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun emptyPackageIsRejected() {
        RestoreSafetyPolicy.requiredBytes(0L)
    }

    @Test(expected = IllegalArgumentException::class)
    fun packageOver128MbIsRejected() {
        RestoreSafetyPolicy.requiredBytes(RestoreSafetyPolicy.MAX_PACKAGE_BYTES + 1L)
    }
}
