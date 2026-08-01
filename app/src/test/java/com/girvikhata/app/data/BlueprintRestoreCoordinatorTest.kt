package com.girvikhata.app.data

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

    private fun copyMedia(source: Map<String, ByteArray>): Map<String, ByteArray> =
        source.mapValues { (_, value) -> value.copyOf() }
}
