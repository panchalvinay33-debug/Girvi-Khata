package com.girvikhata.app.data

import com.girvikhata.app.custody.CustodyPlacementSnapshot
import com.girvikhata.app.custody.StorageLocation
import com.girvikhata.app.domain.MasterCatalog
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BlueprintRestoreCoordinatorTest {
    @Test
    fun `restore activates business masters and media together`() {
        var business = AppSnapshot.defaults()
        var masters = MasterCatalog()
        var media = emptyMap<String, ByteArray>()
        var masterSaves = 0
        val targetBusiness = AppSnapshot(
            customers = listOf(CustomerRecord(id = "c1", name = "Ravi", createdAt = 1L)),
        )
        val targetMasters = MasterCatalog()
        val targetMedia = mapOf("customer-c1.gkm" to byteArrayOf(1, 2, 3))
        val coordinator = BlueprintRestoreCoordinator(
            loadBusiness = { business }, saveBusiness = { business = it },
            loadMasters = { masters }, saveMasters = { masterSaves += 1; masters = it },
            loadMedia = { media }, saveMedia = { media = copyMedia(it) },
        )

        val result = coordinator.restore(targetBusiness, targetMasters, true, targetMedia)

        assertEquals(targetBusiness, business)
        assertEquals(targetMasters, masters)
        assertTrue(masterSaves > 0)
        assertArrayEquals(targetMedia.getValue("customer-c1.gkm"), media.getValue("customer-c1.gkm"))
        assertEquals(1, result.mediaCount)
    }

    @Test
    fun `media activation failure rolls business masters and media back`() {
        val beforeBusiness = AppSnapshot.defaults()
        val beforeMasters = MasterCatalog()
        val beforeMedia = mapOf("old.gkm" to byteArrayOf(7))
        var business = beforeBusiness
        var masters = beforeMasters
        var media = copyMedia(beforeMedia)
        var failTargetMediaOnce = true
        val targetBusiness = AppSnapshot(customers = listOf(CustomerRecord(id = "c2", name = "Sita", createdAt = 2L)))
        val targetMasters = MasterCatalog()
        val targetMedia = mapOf("new.gkm" to byteArrayOf(9))
        val coordinator = BlueprintRestoreCoordinator(
            loadBusiness = { business }, saveBusiness = { business = it },
            loadMasters = { masters }, saveMasters = { masters = it },
            loadMedia = { media },
            saveMedia = {
                if (it.keys == targetMedia.keys && failTargetMediaOnce) {
                    failTargetMediaOnce = false
                    error("simulated media activation failure")
                }
                media = copyMedia(it)
            },
        )

        val failure = runCatching { coordinator.restore(targetBusiness, targetMasters, true, targetMedia) }.exceptionOrNull()

        assertNotNull(failure)
        assertEquals(beforeBusiness, business)
        assertEquals(beforeMasters, masters)
        assertArrayEquals(beforeMedia.getValue("old.gkm"), media.getValue("old.gkm"))
    }

    @Test
    fun `custody activation is verified and failure rolls every generation back`() {
        val beforeBusiness = AppSnapshot.defaults()
        val beforeMasters = MasterCatalog()
        val beforeMedia = mapOf("old.gkm" to byteArrayOf(4))
        val beforeCustody = CustodyPlacementSnapshot(
            locations = listOf(StorageLocation(id = "old-locker", name = "Old Locker", createdAt = 1L)),
        )
        var business = beforeBusiness
        var masters = beforeMasters
        var media = copyMedia(beforeMedia)
        var custody = beforeCustody
        var failCustodyOnce = true

        val targetBusiness = AppSnapshot(customers = listOf(CustomerRecord(id = "c3", name = "Asha", createdAt = 3L)))
        val targetMedia = mapOf("new.gkm" to byteArrayOf(8))
        val targetCustody = CustodyPlacementSnapshot(
            locations = listOf(StorageLocation(id = "new-locker", name = "Shop Locker", createdAt = 2L)),
        )
        val coordinator = BlueprintRestoreCoordinator(
            loadBusiness = { business }, saveBusiness = { business = it },
            loadMasters = { masters }, saveMasters = { masters = it },
            loadMedia = { media }, saveMedia = { media = copyMedia(it) },
            loadCustody = { custody },
            saveCustody = {
                if (it == targetCustody && failCustodyOnce) {
                    failCustodyOnce = false
                    error("simulated custody activation failure")
                }
                custody = it
            },
        )

        val failure = runCatching {
            coordinator.restore(
                targetBusiness = targetBusiness,
                importedMasters = MasterCatalog(),
                containsPortableMasters = true,
                targetMedia = targetMedia,
                targetCustody = targetCustody,
                containsPortableCustody = true,
            )
        }.exceptionOrNull()

        assertNotNull(failure)
        assertEquals(beforeBusiness, business)
        assertEquals(beforeMasters, masters)
        assertArrayEquals(beforeMedia.getValue("old.gkm"), media.getValue("old.gkm"))
        assertEquals(beforeCustody, custody)
    }

    private fun copyMedia(source: Map<String, ByteArray>): Map<String, ByteArray> =
        source.mapValues { (_, value) -> value.copyOf() }
}
