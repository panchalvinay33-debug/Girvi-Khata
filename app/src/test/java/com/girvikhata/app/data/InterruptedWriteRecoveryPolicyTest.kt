package com.girvikhata.app.data

import org.junit.Assert.assertEquals
import org.junit.Test

class InterruptedWriteRecoveryPolicyTest {
    private fun pending(target: String? = null) = VerifiedWriteIntent(
        transactionId = "tx-1",
        mutationLabel = "PAYMENT_APPEND",
        expectedFingerprint = "before",
        targetFingerprint = target,
        state = VerifiedWriteIntentState.PENDING,
        startedAt = 100L,
    )

    @Test
    fun noIntentNeedsNoRecovery() {
        assertEquals(
            InterruptedWriteRecoveryAction.NONE,
            InterruptedWriteRecoveryPolicy.evaluate(null, "current").action,
        )
    }

    @Test
    fun completedIntentNeedsNoRecovery() {
        val committed = pending("after").copy(state = VerifiedWriteIntentState.COMMITTED, finishedAt = 200L)
        assertEquals(
            InterruptedWriteRecoveryAction.NONE,
            InterruptedWriteRecoveryPolicy.evaluate(committed, "after").action,
        )
    }

    @Test
    fun pendingWithBeforeFingerprintIsSafeToRetry() {
        assertEquals(
            InterruptedWriteRecoveryAction.SAFE_TO_RETRY,
            InterruptedWriteRecoveryPolicy.evaluate(pending(), "before").action,
        )
    }

    @Test
    fun pendingWithTargetFingerprintNeedsRelationalCompletion() {
        assertEquals(
            InterruptedWriteRecoveryAction.VERIFY_RELATIONAL_AND_COMPLETE,
            InterruptedWriteRecoveryPolicy.evaluate(pending("after"), "after").action,
        )
    }

    @Test
    fun unknownFingerprintBlocksBusinessWrites() {
        assertEquals(
            InterruptedWriteRecoveryAction.BLOCK_AND_REQUIRE_RECOVERY,
            InterruptedWriteRecoveryPolicy.evaluate(pending("after"), "unexpected").action,
        )
    }
}
