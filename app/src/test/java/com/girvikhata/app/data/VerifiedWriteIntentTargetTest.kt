package com.girvikhata.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class VerifiedWriteIntentTargetTest {
    private val pending = VerifiedWriteIntentReducer.begin(
        transactionId = "tx-prepare",
        mutationLabel = "RESTORE_REPLACE",
        expectedFingerprint = "before",
        startedAt = 100L,
    )

    @Test
    fun targetIsPersistableBeforeCommitState() {
        val prepared = VerifiedWriteIntentReducer.prepareTarget(pending, "after")

        assertEquals(VerifiedWriteIntentState.PENDING, prepared.state)
        assertEquals("before", prepared.expectedFingerprint)
        assertEquals("after", prepared.targetFingerprint)
    }

    @Test
    fun blankTargetIsRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            VerifiedWriteIntentReducer.prepareTarget(pending, "")
        }
    }

    @Test
    fun completedIntentCannotPrepareAnotherTarget() {
        val committed = VerifiedWriteIntentReducer.commit(pending, "after", 200L)

        assertThrows(IllegalArgumentException::class.java) {
            VerifiedWriteIntentReducer.prepareTarget(committed, "other")
        }
    }
}
