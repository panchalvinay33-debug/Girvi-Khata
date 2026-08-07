package com.girvikhata.app.data

import android.content.Context
import com.girvikhata.app.backup.MediaBackupSupport
import com.girvikhata.app.backup.PortableMediaSupport
import com.girvikhata.app.custody.CustodyPlacementSnapshot
import com.girvikhata.app.custody.CustodyPlacementStore
import com.girvikhata.app.domain.MasterCatalog

/**
 * Rebuild restore path. Business, masters, media and v4 custody data are activated as one verified generation.
 * New v3+ backups use portable photo bytes and re-encrypt them under the destination device key.
 * Legacy v2 device-encrypted media remains supported for same-device recovery.
 */
class BlueprintRestoreCoordinator internal constructor(
    private val loadBusiness: () -> AppSnapshot,
    private val saveBusiness: (AppSnapshot) -> Unit,
    private val loadMasters: () -> MasterCatalog,
    private val saveMasters: (MasterCatalog) -> Unit,
    private val loadMedia: () -> Map<String, ByteArray>,
    private val saveMedia: (Map<String, ByteArray>) -> Unit,
    private val loadPortableMedia: () -> Map<String, ByteArray> = loadMedia,
    private val savePortableMedia: (Map<String, ByteArray>) -> Unit = saveMedia,
    private val loadCustody: () -> CustodyPlacementSnapshot = { CustodyPlacementSnapshot() },
    private val saveCustody: (CustodyPlacementSnapshot) -> Unit = {},
) {
    constructor(context: Context) : this(
        loadBusiness = EncryptedRecordStore(context.applicationContext)::load,
        saveBusiness = EncryptedRecordStore(context.applicationContext)::save,
        loadMasters = EncryptedMasterCatalogStore(context.applicationContext)::load,
        saveMasters = EncryptedMasterCatalogStore(context.applicationContext)::save,
        loadMedia = { MediaBackupSupport.collect(context.applicationContext.filesDir) },
        saveMedia = { MediaBackupSupport.restore(context.applicationContext.filesDir, it) },
        loadPortableMedia = { PortableMediaSupport.collect(context.applicationContext) },
        savePortableMedia = { PortableMediaSupport.restore(context.applicationContext, it) },
        loadCustody = CustodyPlacementStore(context.applicationContext)::load,
        saveCustody = CustodyPlacementStore(context.applicationContext)::save,
    )

    data class Result(
        val customerCount: Int,
        val girviCount: Int,
        val masterCount: Int,
        val mediaCount: Int,
        val custodyLocationCount: Int = 0,
        val custodyLotCount: Int = 0,
    )

    @Synchronized
    fun restore(
        targetBusiness: AppSnapshot,
        importedMasters: MasterCatalog,
        containsPortableMasters: Boolean,
        targetMedia: Map<String, ByteArray>,
        targetPortableMedia: Map<String, ByteArray> = emptyMap(),
        targetCustody: CustodyPlacementSnapshot = CustodyPlacementSnapshot(),
        containsPortableCustody: Boolean = false,
    ): Result {
        require(targetMedia.isEmpty() || targetPortableMedia.isEmpty()) { "Backup contains conflicting media generations" }
        val portableMode = targetPortableMedia.isNotEmpty()
        val beforeBusiness = loadBusiness()
        val beforeMasters = loadMasters()
        val beforePortableMedia = loadPortableMedia()
        val beforeLegacyMedia = if (portableMode) emptyMap() else loadMedia()
        val beforeCustody = if (containsPortableCustody) loadCustody() else CustodyPlacementSnapshot()
        val targetMasters = if (containsPortableMasters) importedMasters else beforeMasters

        try {
            saveBusiness(targetBusiness)
            check(loadBusiness() == targetBusiness) { "Restored business verification failed" }

            saveMasters(targetMasters)
            check(loadMasters() == targetMasters) { "Restored masters verification failed" }

            if (portableMode) {
                savePortableMedia(targetPortableMedia)
                check(sameMedia(loadPortableMedia(), targetPortableMedia)) { "Portable media re-encryption verification failed" }
            } else {
                saveMedia(targetMedia)
                check(sameMedia(loadMedia(), targetMedia)) { "Restored encrypted media verification failed" }
            }

            if (containsPortableCustody) {
                saveCustody(targetCustody)
                check(loadCustody() == targetCustody) { "Restored custody verification failed" }
            }

            return Result(
                customerCount = targetBusiness.customers.size,
                girviCount = targetBusiness.girvis.size,
                masterCount = targetMasters.entries.size,
                mediaCount = if (portableMode) targetPortableMedia.size else targetMedia.size,
                custodyLocationCount = if (containsPortableCustody) targetCustody.locations.size else 0,
                custodyLotCount = if (containsPortableCustody) targetCustody.lots.size else 0,
            )
        } catch (failure: Throwable) {
            val rollbackFailures = mutableListOf<Throwable>()
            runCatching { saveBusiness(beforeBusiness); check(loadBusiness() == beforeBusiness) }
                .exceptionOrNull()?.let(rollbackFailures::add)
            runCatching { saveMasters(beforeMasters); check(loadMasters() == beforeMasters) }
                .exceptionOrNull()?.let(rollbackFailures::add)
            runCatching {
                if (portableMode) {
                    savePortableMedia(beforePortableMedia)
                    check(sameMedia(loadPortableMedia(), beforePortableMedia))
                } else {
                    saveMedia(beforeLegacyMedia)
                    check(sameMedia(loadMedia(), beforeLegacyMedia))
                }
            }.exceptionOrNull()?.let(rollbackFailures::add)
            if (containsPortableCustody) {
                runCatching {
                    saveCustody(beforeCustody)
                    check(loadCustody() == beforeCustody)
                }.exceptionOrNull()?.let(rollbackFailures::add)
            }
            rollbackFailures.forEach(failure::addSuppressed)
            if (rollbackFailures.isNotEmpty()) {
                throw IllegalStateException("Restore failed and rollback was incomplete", failure)
            }
            throw failure
        } finally {
            if (portableMode) PortableMediaSupport.clear(beforePortableMedia)
        }
    }

    private fun sameMedia(a: Map<String, ByteArray>, b: Map<String, ByteArray>): Boolean =
        a.keys == b.keys && a.all { (name, bytes) -> b[name]?.contentEquals(bytes) == true }
}
