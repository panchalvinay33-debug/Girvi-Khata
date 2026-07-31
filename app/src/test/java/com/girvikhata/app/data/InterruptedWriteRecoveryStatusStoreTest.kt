package com.girvikhata.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InterruptedWriteRecoveryStatusStoreTest {
    @Test
    fun blockedDecisionRemainsExplicitAndPrivacySafe() {
        val intent = VerifiedWriteIntent(
            transactionId = "tx-99",
            mutationLabel = "RESTORE_REPLACE",
            expectedFingerprint = "before",
            targetFingerprint = "after",
            state = VerifiedWriteIntentState.PENDING,
            startedAt = 100L,
        )
        val decision = InterruptedWriteRecoveryDecision(
            action = InterruptedWriteRecoveryAction.BLOCK_AND_REQUIRE_RECOVERY,
            transactionId = "tx-99",
            reason = "Snapshot fingerprint matches neither pre-write nor target",
        )

        val status = InterruptedWriteRecoveryStatusReducer.fromDecision(
            decision = decision,
            intent = intent,
            snapshotFingerprint = "unexpected",
            evaluatedAt = 200L,
        )

        assertTrue(status.blocksBusinessWrites)
        assertEquals("tx-99", status.transactionId)
        assertEquals("RESTORE_REPLACE", status.mutationLabel)
        assertEquals("unexpected", status.snapshotFingerprint)
        assertEquals(200L, status.evaluatedAt)
    }

    @Test
    fun safeRetryDoesNotBlockFutureWrites() {
        val status = InterruptedWriteRecoveryStatusReducer.fromDecision(
            decision = InterruptedWriteRecoveryDecision(
                action = InterruptedWriteRecoveryAction.SAFE_TO_RETRY,
                transactionId = "tx-1",
                reason = "Authoritative snapshot stayed unchanged",
            ),
            intent = null,
            snapshotFingerprint = "before",
            evaluatedAt = 300L,
        )

        assertFalse(status.blocksBusinessWrites)
    }

    @Test
    fun longReasonIsBounded() {
        val status = InterruptedWriteRecoveryStatusReducer.fromDecision(
            decision = InterruptedWriteRecoveryDecision(
                action = InterruptedWriteRecoveryAction.BLOCK_AND_REQUIRE_RECOVERY,
                transactionId = "tx-long",
                reason = "x".repeat(500),
            ),
            intent = null,
            snapshotFingerprint = "fp",
            evaluatedAt = 400L,
        )

        assertEquals(240, status.reason.length)
    }
}
