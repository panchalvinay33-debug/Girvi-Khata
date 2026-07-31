package com.girvikhata.app.data

enum class RestoreGenerationPhase {
    PREPARED,
    BUSINESS_ACTIVATED,
    MASTERS_ACTIVATED,
    COMPLETED,
    BLOCKED,
}

data class RestoreGenerationIntent(
    val generationId: String,
    val phase: RestoreGenerationPhase,
    val beforeBusinessFingerprint: String,
    val targetBusinessFingerprint: String,
    val beforeMasterFingerprint: String,
    val targetMasterFingerprint: String,
    val containsPortableMasters: Boolean,
    val backupSha256Prefix: String,
    val createdAt: Long,
    val updatedAt: Long,
    val reason: String? = null,
)

enum class RestoreGenerationRecoveryAction {
    ACTIVATE_BUSINESS,
    ACTIVATE_MASTERS,
    MARK_COMPLETED,
    NO_ACTION,
    BLOCK_AND_REQUIRE_RECOVERY,
}

data class RestoreGenerationRecoveryDecision(
    val action: RestoreGenerationRecoveryAction,
    val reason: String,
)

/** Pure fail-closed policy for reconciling an interrupted cross-store restore generation. */
object RestoreGenerationPolicy {
    fun decide(
        intent: RestoreGenerationIntent,
        currentBusinessFingerprint: String,
        currentMasterFingerprint: String,
    ): RestoreGenerationRecoveryDecision {
        if (intent.phase == RestoreGenerationPhase.BLOCKED) {
            return block(intent.reason ?: "Restore generation is already blocked")
        }

        val businessBefore = currentBusinessFingerprint == intent.beforeBusinessFingerprint
        val businessTarget = currentBusinessFingerprint == intent.targetBusinessFingerprint
        val mastersBefore = currentMasterFingerprint == intent.beforeMasterFingerprint
        val mastersTarget = currentMasterFingerprint == intent.targetMasterFingerprint

        if ((!businessBefore && !businessTarget) || (!mastersBefore && !mastersTarget)) {
            return block("Restore generation fingerprints are ambiguous")
        }

        if (!intent.containsPortableMasters) {
            if (!mastersBefore) return block("Legacy restore unexpectedly changed master catalog")
            return when {
                businessBefore && intent.phase == RestoreGenerationPhase.PREPARED -> decision(
                    RestoreGenerationRecoveryAction.ACTIVATE_BUSINESS,
                    "Legacy restore is prepared and business snapshot is unchanged",
                )
                businessTarget -> decision(
                    RestoreGenerationRecoveryAction.MARK_COMPLETED,
                    "Legacy restore business snapshot is active and current masters are preserved",
                )
                else -> block("Legacy restore generation state is inconsistent")
            }
        }

        return when {
            businessBefore && mastersBefore && intent.phase == RestoreGenerationPhase.PREPARED -> decision(
                RestoreGenerationRecoveryAction.ACTIVATE_BUSINESS,
                "Portable restore is prepared and neither store is activated",
            )
            businessTarget && mastersBefore -> decision(
                RestoreGenerationRecoveryAction.ACTIVATE_MASTERS,
                "Business snapshot is active but master catalog still needs activation",
            )
            businessTarget && mastersTarget -> when (intent.phase) {
                RestoreGenerationPhase.COMPLETED -> decision(
                    RestoreGenerationRecoveryAction.NO_ACTION,
                    "Restore generation is already complete",
                )
                else -> decision(
                    RestoreGenerationRecoveryAction.MARK_COMPLETED,
                    "Both restore targets are active and can be marked complete",
                )
            }
            businessBefore && mastersTarget -> block("Master catalog activated before business snapshot")
            else -> block("Restore generation state is inconsistent")
        }
    }

    private fun decision(
        action: RestoreGenerationRecoveryAction,
        reason: String,
    ) = RestoreGenerationRecoveryDecision(action, reason)

    private fun block(reason: String) = decision(
        RestoreGenerationRecoveryAction.BLOCK_AND_REQUIRE_RECOVERY,
        reason,
    )
}
