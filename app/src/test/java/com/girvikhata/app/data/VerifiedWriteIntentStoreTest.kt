package com.girvikhata.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VerifiedWriteIntentStoreTest {
    @Test
    fun beginCreatesPendingIntent() {
        val intent = VerifiedWriteIntentReducer.begin("tx-1", "PAYMENT_APPEND", "before", 100L)
        assertEquals(VerifiedWriteIntentState.PENDING, intent.state)
        assertEquals("tx-1", intent.transactionId)
        assertEquals("before", intent.expectedFingerprint)
        assertNull(intent.finishedAt)
    }

    @Test
    fun commitPreservesIdentityAndAddsTargetProof() {
        val pending = VerifiedWriteIntentReducer.begin("tx-2", "RESTORE_REPLACE", "before", 100L)
        val committed = VerifiedWriteIntentReducer.commit(pending, "after", 200L)
        assertEquals(VerifiedWriteIntentState.COMMITTED, committed.state)
        assertEquals("tx-2", committed.transactionId)
        assertEquals("after", committed.targetFingerprint)
        assertEquals(200L, committed.finishedAt)
        assertNull(committed.reason)
    }

    @Test
    fun failurePreservesExpectedFingerprintAndTruncatesReason() {
        val pending = VerifiedWriteIntentReducer.begin("tx-3", "CUSTOMER_GIRVI_CREATE", "before", 100L)
        val failed = VerifiedWriteIntentReducer.fail(pending, "x".repeat(500), 250L)
        assertEquals(VerifiedWriteIntentState.FAILED, failed.state)
        assertEquals("before", failed.expectedFingerprint)
        assertEquals(240, failed.reason?.length)
        assertTrue(failed.finishedAt == 250L)
    }
}
