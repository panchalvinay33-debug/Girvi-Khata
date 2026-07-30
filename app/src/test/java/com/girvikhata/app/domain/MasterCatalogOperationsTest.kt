package com.girvikhata.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MasterCatalogOperationsTest {
    @Test
    fun addNormalizesAndRejectsDuplicate() {
        val base = MasterCatalog(emptyList())
        val added = MasterCatalogOperations.add(base, MasterKind.UNIT, "  troy   gram ")
        assertEquals("troy gram", added.entries.single().name)
        runCatching { MasterCatalogOperations.add(added, MasterKind.UNIT, "TROY GRAM") }
            .onSuccess { error("Duplicate should fail") }
    }

    @Test
    fun itemDuplicateIsScopedByCategory() {
        val first = MasterCatalogOperations.add(MasterCatalog(emptyList()), MasterKind.ITEM, "Ring", "Gold")
        val second = MasterCatalogOperations.add(first, MasterKind.ITEM, "Ring", "Silver")
        assertEquals(2, second.entries.size)
    }

    @Test
    fun toggleAndRenamePreserveId() {
        val entry = MasterEntry(kind = MasterKind.LOCKER, name = "Locker A")
        val base = MasterCatalog(listOf(entry))
        val toggled = MasterCatalogOperations.toggle(base, entry.id)
        assertFalse(toggled.entries.single().active)
        val renamed = MasterCatalogOperations.rename(toggled, entry.id, "Strong Room")
        assertEquals(entry.id, renamed.entries.single().id)
        assertEquals("Strong Room", renamed.entries.single().name)
    }

    @Test
    fun moveDoesNotCrossMasterKindBoundary() {
        val unit = MasterEntry(kind = MasterKind.UNIT, name = "piece")
        val payment = MasterEntry(kind = MasterKind.PAYMENT_MODE, name = "CASH")
        val catalog = MasterCatalog(listOf(unit, payment))
        assertEquals(catalog, MasterCatalogOperations.move(catalog, unit.id, 1))
    }

    @Test
    fun defaultsContainOperationalMinimums() {
        val catalog = MasterCatalog()
        assertTrue(MasterCatalogOperations.active(catalog, MasterKind.UNIT).isNotEmpty())
        assertTrue(MasterCatalogOperations.active(catalog, MasterKind.PAYMENT_MODE).isNotEmpty())
        assertTrue(MasterCatalogOperations.active(catalog, MasterKind.INTEREST_PLAN).isNotEmpty())
    }
}
