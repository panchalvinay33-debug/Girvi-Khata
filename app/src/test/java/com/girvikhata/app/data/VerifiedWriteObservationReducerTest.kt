package com.girvikhata.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VerifiedWriteObservationReducerTest {
    @Test
    fun successIncrementsCountAndStoresExactProof() {
        val current = VerifiedWriteObservation(successfulWrites = 4, lastFailureReason = "old failure")
        val result = VerifiedBusinessWriteResult(
            transactionId = "tx-5",
            beforeFingerprint = "before",
            afterFingerprint = "after",
            relationalFingerprint = "after",
            syncMode = "INCREMENTAL",
            changedRows = 3,
            committedAt = 1234L,
        )

        val next = VerifiedWriteObservationReducer.success(current, result)

        assertEquals(5, next.successfulWrites)
        assertEquals("tx-5", next.lastTransactionId)
        assertEquals(1234L, next.lastCommittedAt)
        assertEquals("after", next.lastSnapshotFingerprint)
        assertEquals("after", next.lastRelationalFingerprint)
        assertTrue(next.fingerprintsMatch)
        assertNull(next.lastFailureReason)
    }

    @Test
    fun failureDoesNotEraseLastSuccessfulFingerprintProof() {
        val current = VerifiedWriteObservation(
            successfulWrites = 25,
            lastTransactionId = "tx-good",
            lastCommittedAt = 100L,
            lastSnapshotFingerprint = "same",
            lastRelationalFingerprint = "same",
        )

        val next = VerifiedWriteObservationReducer.failure(
            current = current,
            transactionId = "tx-bad",
            reason = "stale screen",
            failedAt = 200L,
        )

        assertEquals(25, next.successfulWrites)
        assertEquals("tx-bad", next.lastTransactionId)
        assertEquals(200L, next.lastFailureAt)
        assertEquals("stale screen", next.lastFailureReason)
        assertTrue(next.fingerprintsMatch)
        assertTrue(VerifiedWriteCutoverPolicy.blockers(next).contains("Latest coordinated write attempt failed"))
    }

    @Test
    fun failureReasonIsBoundedAndMismatchedProofBlocksCutover() {
        val next = VerifiedWriteObservationReducer.failure(
            current = VerifiedWriteObservation(
                successfulWrites = 25,
                lastSnapshotFingerprint = "snapshot",
                lastRelationalFingerprint = "relational",
            ),
            transactionId = "tx",
            reason = "x".repeat(500),
            failedAt = 1L,
        )

        assertEquals(240, next.lastFailureReason?.length)
        assertFalse(next.fingerprintsMatch)
        assertTrue(VerifiedWriteCutoverPolicy.blockers(next).contains("Latest coordinated write fingerprint proof missing"))
    }
}
