package com.girvikhata.app.data

import android.content.Context
import com.girvikhata.app.domain.MasterCatalog
import java.util.UUID

/** Coordinates encrypted staging, verified business activation, master activation and restart recovery. */
class RestoreGenerationCoordinator(
    context: Context,
    private val records: EncryptedRecordStore = EncryptedRecordStore(context.applicationContext),
    private val masters: EncryptedMasterCatalogStore = EncryptedMasterCatalogStore(context.applicationContext),
    private val businessWrites: VerifiedBusinessWriteCoordinator = VerifiedBusinessWriteCoordinator(
        context.applicationContext,
        records = records,
    ),
    private val intents: RestoreGenerationIntentStore = RestoreGenerationIntentStore(context.applicationContext),
    private val stages: RestoreGenerationStageStore = RestoreGenerationStageStore(context.applicationContext),
) {
    data class Result(
        val generationId: String,
        val businessFingerprint: String,
        val masterFingerprint: String,
        val completedAt: Long,
    )

    @Synchronized
    fun restore(
        targetSnapshot: AppSnapshot,
        importedMasters: MasterCatalog,
        containsPortableMasters: Boolean,
        backupSha256: String,
    ): Result {
        require(intents.load() == null) { "Another restore generation requires recovery" }
        val beforeSnapshot = records.load()
        val beforeMasters = masters.load()
        val targetMasters = if (containsPortableMasters) importedMasters else beforeMasters
        val generationId = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        val intent = RestoreGenerationIntent(
            generationId = generationId,
            phase = RestoreGenerationPhase.PREPARED,
            beforeBusinessFingerprint = RelationalShadowFingerprint.sha256(beforeSnapshot),
            targetBusinessFingerprint = RelationalShadowFingerprint.sha256(targetSnapshot),
            beforeMasterFingerprint = MasterCatalogFingerprint.sha256(beforeMasters),
            targetMasterFingerprint = MasterCatalogFingerprint.sha256(targetMasters),
            containsPortableMasters = containsPortableMasters,
            backupSha256Prefix = backupSha256.lowercase().replace(":", "").take(32),
            createdAt = now,
            updatedAt = now,
        )
        stages.stage(
            RestoreGenerationStageStore.StagedTarget(
                generationId = generationId,
                snapshot = targetSnapshot,
                masterCatalog = targetMasters,
            ),
        )
        intents.save(intent)
        return reconcile(requireGenerationId = generationId)
    }

    @Synchronized
    fun reconcileOnStartup(): Result? {
        val intent = intents.load() ?: return null
        if (intent.phase == RestoreGenerationPhase.COMPLETED) {
            stages.clear(intent.generationId)
            intents.clearCompleted()
            return null
        }
        return reconcile(requireGenerationId = intent.generationId)
    }

    fun pendingIntent(): RestoreGenerationIntent? = intents.load()

    private fun reconcile(requireGenerationId: String): Result {
        repeat(MAX_TRANSITIONS) {
            val intent = intents.load() ?: error("Restore generation metadata missing")
            require(intent.generationId == requireGenerationId) { "Restore generation changed during reconciliation" }
            val currentBusiness = RelationalShadowFingerprint.sha256(records.load())
            val currentMasters = MasterCatalogFingerprint.sha256(masters.load())
            val decision = RestoreGenerationPolicy.decide(intent, currentBusiness, currentMasters)

            when (decision.action) {
                RestoreGenerationRecoveryAction.ACTIVATE_BUSINESS -> {
                    val staged = loadVerifiedStage(intent)
                    businessWrites.execute(
                        VerifiedBusinessWriteRequest(
                            expectedFingerprint = currentBusiness,
                            mutation = VerifiedBusinessMutation.ReplaceSnapshotForRestore(staged.snapshot),
                            title = "Restore generation ${intent.generationId.take(12)} business activation",
                            restoreGenerationId = intent.generationId,
                        ),
                    )
                    intents.transition(intent, RestoreGenerationPhase.BUSINESS_ACTIVATED, decision.reason)
                }

                RestoreGenerationRecoveryAction.ACTIVATE_MASTERS -> {
                    val staged = loadVerifiedStage(intent)
                    masters.save(staged.masterCatalog)
                    check(MasterCatalogFingerprint.sha256(masters.load()) == intent.targetMasterFingerprint) {
                        "Restore master activation verification failed"
                    }
                    intents.transition(intent, RestoreGenerationPhase.MASTERS_ACTIVATED, decision.reason)
                }

                RestoreGenerationRecoveryAction.MARK_COMPLETED,
                RestoreGenerationRecoveryAction.NO_ACTION,
                -> {
                    check(currentBusiness == intent.targetBusinessFingerprint) {
                        "Restore business completion verification failed"
                    }
                    check(currentMasters == intent.targetMasterFingerprint) {
                        "Restore master completion verification failed"
                    }
                    val completedAt = System.currentTimeMillis()
                    val completed = if (intent.phase == RestoreGenerationPhase.COMPLETED) intent else {
                        intents.transition(intent, RestoreGenerationPhase.COMPLETED, decision.reason, completedAt)
                    }
                    stages.clear(completed.generationId)
                    intents.clearCompleted()
                    return Result(
                        generationId = completed.generationId,
                        businessFingerprint = currentBusiness,
                        masterFingerprint = currentMasters,
                        completedAt = completedAt,
                    )
                }

                RestoreGenerationRecoveryAction.BLOCK_AND_REQUIRE_RECOVERY -> {
                    intents.transition(intent, RestoreGenerationPhase.BLOCKED, decision.reason)
                    error("Restore generation blocked: ${decision.reason}")
                }
            }
        }
        error("Restore generation exceeded reconciliation transition limit")
    }

    private fun loadVerifiedStage(intent: RestoreGenerationIntent): RestoreGenerationStageStore.StagedTarget {
        val staged = stages.load(intent.generationId)
        check(RelationalShadowFingerprint.sha256(staged.snapshot) == intent.targetBusinessFingerprint) {
            "Staged business restore fingerprint mismatch"
        }
        check(MasterCatalogFingerprint.sha256(staged.masterCatalog) == intent.targetMasterFingerprint) {
            "Staged master restore fingerprint mismatch"
        }
        return staged
    }

    private companion object {
        const val MAX_TRANSITIONS = 4
    }
}
