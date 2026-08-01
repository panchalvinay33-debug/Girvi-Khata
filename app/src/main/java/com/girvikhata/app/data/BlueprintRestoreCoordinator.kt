package com.girvikhata.app.data

import android.content.Context
import com.girvikhata.app.backup.MediaBackupSupport
import com.girvikhata.app.domain.MasterCatalog

/**
 * Rebuild restore path: encrypted business snapshot is authoritative and media/masters are activated
 * as one verified generation. Any activation failure attempts an immediate rollback of all three.
 */
class BlueprintRestoreCoordinator internal constructor(
    private val loadBusiness: () -> AppSnapshot,
    private val saveBusiness: (AppSnapshot) -> Unit,
    private val loadMasters: () -> MasterCatalog,
    private val saveMasters: (MasterCatalog) -> Unit,
    private val loadMedia: () -> Map<String, ByteArray>,
    private val saveMedia: (Map<String, ByteArray>) -> Unit,
) {
    constructor(context: Context) : this(
        loadBusiness = EncryptedRecordStore(context.applicationContext)::load,
        saveBusiness = EncryptedRecordStore(context.applicationContext)::save,
        loadMasters = EncryptedMasterCatalogStore(context.applicationContext)::load,
        saveMasters = EncryptedMasterCatalogStore(context.applicationContext)::save,
        loadMedia = { MediaBackupSupport.collect(context.applicationContext.filesDir) },
        saveMedia = { MediaBackupSupport.restore(context.applicationContext.filesDir, it) },
    )

    data class Result(
        val customerCount: Int,
        val girviCount: Int,
        val masterCount: Int,
        val mediaCount: Int,
    )

    @Synchronized
    fun restore(
        targetBusiness: AppSnapshot,
        importedMasters: MasterCatalog,
        containsPortableMasters: Boolean,
        targetMedia: Map<String, ByteArray>,
    ): Result {
        val beforeBusiness = loadBusiness()
        val beforeMasters = loadMasters()
        val beforeMedia = loadMedia()
        val targetMasters = if (containsPortableMasters) importedMasters else beforeMasters

        try {
            saveBusiness(targetBusiness)
            check(loadBusiness() == targetBusiness) { "Restored business verification failed" }

            saveMasters(targetMasters)
            check(loadMasters() == targetMasters) { "Restored masters verification failed" }

            saveMedia(targetMedia)
            check(sameMedia(loadMedia(), targetMedia)) { "Restored encrypted media verification failed" }

            return Result(
                customerCount = targetBusiness.customers.size,
                girviCount = targetBusiness.girvis.size,
                masterCount = targetMasters.entries.size,
                mediaCount = targetMedia.size,
            )
        } catch (failure: Throwable) {
            val rollbackFailures = mutableListOf<Throwable>()
            runCatching { saveBusiness(beforeBusiness); check(loadBusiness() == beforeBusiness) }
                .exceptionOrNull()?.let(rollbackFailures::add)
            runCatching { saveMasters(beforeMasters); check(loadMasters() == beforeMasters) }
                .exceptionOrNull()?.let(rollbackFailures::add)
            runCatching { saveMedia(beforeMedia); check(sameMedia(loadMedia(), beforeMedia)) }
                .exceptionOrNull()?.let(rollbackFailures::add)
            rollbackFailures.forEach(failure::addSuppressed)
            if (rollbackFailures.isNotEmpty()) {
                throw IllegalStateException("Restore failed and rollback was incomplete", failure)
            }
            throw failure
        }
    }

    private fun sameMedia(a: Map<String, ByteArray>, b: Map<String, ByteArray>): Boolean =
        a.keys == b.keys && a.all { (name, bytes) -> b[name]?.contentEquals(bytes) == true }
}
