package com.girvikhata.app.data

import com.girvikhata.app.domain.MasterCatalog
import com.girvikhata.app.domain.MasterEntry
import com.girvikhata.app.domain.MasterKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class MasterCatalogFingerprintTest {
    private val first = MasterEntry(
        id = "m1",
        kind = MasterKind.entries.first(),
        name = "Gold",
        active = true,
        categoryName = "Jewellery",
        rateBasisPoints = 200,
    )

    @Test
    fun `same catalog has stable fingerprint`() {
        val catalog = MasterCatalog(entries = listOf(first))
        assertEquals(
            MasterCatalogFingerprint.sha256(catalog),
            MasterCatalogFingerprint.sha256(catalog.copy()),
        )
    }

    @Test
    fun `business relevant field change changes fingerprint`() {
        val before = MasterCatalog(entries = listOf(first))
        val after = MasterCatalog(entries = listOf(first.copy(rateBasisPoints = 250)))
        assertNotEquals(
            MasterCatalogFingerprint.sha256(before),
            MasterCatalogFingerprint.sha256(after),
        )
    }

    @Test
    fun `entry order is part of restore generation identity`() {
        val second = first.copy(id = "m2", name = "Silver")
        assertNotEquals(
            MasterCatalogFingerprint.sha256(MasterCatalog(entries = listOf(first, second))),
            MasterCatalogFingerprint.sha256(MasterCatalog(entries = listOf(second, first))),
        )
    }
}
