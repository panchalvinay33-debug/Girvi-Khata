package com.girvikhata.app.backup

import com.girvikhata.app.data.AppSnapshot
import com.girvikhata.app.data.CategoryRecord
import com.girvikhata.app.data.CustomerRecord
import com.girvikhata.app.data.GirviRecord
import com.girvikhata.app.domain.MasterCatalog
import com.girvikhata.app.domain.MasterEntry
import com.girvikhata.app.domain.MasterKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class PortableAppBundleCodecTest {
    private val snapshot = AppSnapshot(
        customers = listOf(CustomerRecord(id = "c1", name = "Test Customer", createdAt = 1L)),
        categories = listOf(CategoryRecord(id = "cat1", name = "Gold")),
        girvis = listOf(
            GirviRecord(
                id = "g1",
                girviNumber = "GK-1",
                customerId = "c1",
                customerName = "Test Customer",
                categoryName = "Gold",
                itemName = "Ring",
                weightGrams = "10",
                principalPaise = 10_000L,
                monthlyRateBasisPoints = 200,
                createdAt = 1L,
            ),
        ),
    )

    @Test fun newBundleRoundTripsBusinessAndMasters() {
        val masters = MasterCatalog(listOf(MasterEntry(id = "m1", kind = MasterKind.LOCKER, name = "Locker A")))
        val decoded = PortableAppBundleCodec.decode(PortableAppBundleCodec.encode(snapshot, masters))
        assertCanonicalSnapshotEquals(snapshot, decoded.snapshot)
        assertEquals(masters, decoded.masterCatalog)
        assertTrue(decoded.containsPortableMasters)
    }

    @Test fun legacySnapshotOnlyPayloadRemainsReadable() {
        val decoded = PortableAppBundleCodec.decode(SnapshotPortableCodec.encode(snapshot))
        assertCanonicalSnapshotEquals(snapshot, decoded.snapshot)
        assertFalse(decoded.containsPortableMasters)
        assertTrue(decoded.masterCatalog.entries.isNotEmpty())
    }

    @Test fun damagedBundleBase64IsRejected() {
        val damaged = "{\"bundleVersion\":1,\"snapshot\":\"%%%\",\"masterCatalog\":\"%%%\"}".toByteArray()
        assertThrows(IllegalArgumentException::class.java) { PortableAppBundleCodec.decode(damaged) }
    }

    @Test fun duplicateMasterIdsAreRejected() {
        val json = """{"version":1,"entries":[{"id":"x","kind":"UNIT","name":"gram"},{"id":"x","kind":"UNIT","name":"kg"}]}"""
        assertThrows(IllegalArgumentException::class.java) { MasterCatalogPortableCodec.decode(json.toByteArray()) }
    }

    private fun assertCanonicalSnapshotEquals(expected: AppSnapshot, actual: AppSnapshot) {
        assertTrue(SnapshotPortableCodec.encode(expected).contentEquals(SnapshotPortableCodec.encode(actual)))
    }
}
