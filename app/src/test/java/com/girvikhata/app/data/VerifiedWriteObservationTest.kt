package com.girvikhata.app.data

import org.junit.Assert.assertTrue
import org.junit.Test

class VerifiedWriteObservationTest {
    @Test fun `cutover remains blocked before minimum writes`() {
        val blockers = VerifiedWriteCutoverPolicy.blockers(
            VerifiedWriteObservation(
                successfulWrites = 24,
                lastSnapshotFingerprint = "same",
                lastRelationalFingerprint = "same",
            ),
        )
        assertTrue(blockers.any { it.contains("24/25") })
    }

    @Test fun `matching fingerprints and minimum writes clear blockers`() {
        val blockers = VerifiedWriteCutoverPolicy.blockers(
            VerifiedWriteObservation(
                successfulWrites = 25,
                lastCommittedAt = 200,
                lastSnapshotFingerprint = "same",
                lastRelationalFingerprint = "same",
                lastFailureAt = 100,
            ),
        )
        assertTrue(blockers.isEmpty())
    }

    @Test fun `newer failure blocks cutover`() {
        val blockers = VerifiedWriteCutoverPolicy.blockers(
            VerifiedWriteObservation(
                successfulWrites = 25,
                lastCommittedAt = 100,
                lastSnapshotFingerprint = "same",
                lastRelationalFingerprint = "same",
                lastFailureAt = 200,
            ),
        )
        assertTrue(blockers.any { it.contains("failed") })
    }
}
