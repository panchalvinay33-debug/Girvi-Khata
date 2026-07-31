package com.girvikhata.app.data

import org.junit.Assert.assertEquals
import org.junit.Test

class RestoreGenerationPolicyTest {
    private val portable = RestoreGenerationIntent(
        generationId = "generation-1",
        phase = RestoreGenerationPhase.PREPARED,
        beforeBusinessFingerprint = "business-before",
        targetBusinessFingerprint = "business-target",
        beforeMasterFingerprint = "masters-before",
        targetMasterFingerprint = "masters-target",
        containsPortableMasters = true,
        backupSha256Prefix = "abcdef123456",
        createdAt = 1L,
        updatedAt = 1L,
    )

    @Test
    fun `prepared portable generation activates business first`() {
        assertAction(
            RestoreGenerationRecoveryAction.ACTIVATE_BUSINESS,
            portable,
            "business-before",
            "masters-before",
        )
    }

    @Test
    fun `process death after business activation completes masters`() {
        assertAction(
            RestoreGenerationRecoveryAction.ACTIVATE_MASTERS,
            portable.copy(phase = RestoreGenerationPhase.BUSINESS_ACTIVATED),
            "business-target",
            "masters-before",
        )
    }

    @Test
    fun `fully active portable generation is marked complete`() {
        assertAction(
            RestoreGenerationRecoveryAction.MARK_COMPLETED,
            portable.copy(phase = RestoreGenerationPhase.MASTERS_ACTIVATED),
            "business-target",
            "masters-target",
        )
    }

    @Test
    fun `completed matching generation needs no action`() {
        assertAction(
            RestoreGenerationRecoveryAction.NO_ACTION,
            portable.copy(phase = RestoreGenerationPhase.COMPLETED),
            "business-target",
            "masters-target",
        )
    }

    @Test
    fun `master activation before business blocks`() {
        assertAction(
            RestoreGenerationRecoveryAction.BLOCK_AND_REQUIRE_RECOVERY,
            portable,
            "business-before",
            "masters-target",
        )
    }

    @Test
    fun `unknown business fingerprint blocks`() {
        assertAction(
            RestoreGenerationRecoveryAction.BLOCK_AND_REQUIRE_RECOVERY,
            portable,
            "business-unknown",
            "masters-before",
        )
    }

    @Test
    fun `legacy restore preserves masters and marks completed`() {
        val legacy = portable.copy(
            containsPortableMasters = false,
            targetMasterFingerprint = portable.beforeMasterFingerprint,
            phase = RestoreGenerationPhase.BUSINESS_ACTIVATED,
        )
        assertAction(
            RestoreGenerationRecoveryAction.MARK_COMPLETED,
            legacy,
            "business-target",
            "masters-before",
        )
    }

    @Test
    fun `legacy restore changing masters blocks`() {
        val legacy = portable.copy(
            containsPortableMasters = false,
            targetMasterFingerprint = portable.beforeMasterFingerprint,
        )
        assertAction(
            RestoreGenerationRecoveryAction.BLOCK_AND_REQUIRE_RECOVERY,
            legacy,
            "business-target",
            "masters-target",
        )
    }

    @Test
    fun `blocked generation remains blocked`() {
        assertAction(
            RestoreGenerationRecoveryAction.BLOCK_AND_REQUIRE_RECOVERY,
            portable.copy(phase = RestoreGenerationPhase.BLOCKED, reason = "Manual recovery required"),
            "business-before",
            "masters-before",
        )
    }

    private fun assertAction(
        expected: RestoreGenerationRecoveryAction,
        intent: RestoreGenerationIntent,
        business: String,
        masters: String,
    ) {
        val decision = RestoreGenerationPolicy.decide(intent, business, masters)
        assertEquals(expected, decision.action)
    }
}
