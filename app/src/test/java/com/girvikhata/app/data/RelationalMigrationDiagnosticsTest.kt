package com.girvikhata.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RelationalMigrationDiagnosticsTest {
    @Test fun `space policy requires absolute and database multiplier headroom`() {
        assertFalse(RelationalSpacePolicy.hasSafeHeadroom(63L * 1024 * 1024, 1L))
        assertFalse(RelationalSpacePolicy.hasSafeHeadroom(100L * 1024 * 1024, 40L * 1024 * 1024))
        assertTrue(RelationalSpacePolicy.hasSafeHeadroom(128L * 1024 * 1024, 40L * 1024 * 1024))
    }

    @Test fun `master links resolve case insensitively from schema compatible metadata`() {
        val catalog = MasterCatalog(entries = listOf(
            MasterEntry(id = "item-1", kind = MasterKind.ITEM, name = "Ring", categoryName = "Gold"),
            MasterEntry(id = "unit-1", kind = MasterKind.UNIT, name = "Gram"),
            MasterEntry(id = "locker-1", kind = MasterKind.LOCKER, name = "Locker A"),
        ))
        val item = GirviItemRecord(categoryName = "gold", itemName = "RING", description = "Unit: gram • Locker: locker a • note")
        val links = RelationalMasterLinkResolver.resolve(item, catalog)
        assertEquals("item-1", links.itemMasterId)
        assertEquals("unit-1", links.unitId)
        assertEquals("locker-1", links.lockerId)
    }

    @Test fun `unknown metadata remains safely unresolved`() {
        val links = RelationalMasterLinkResolver.resolve(
            GirviItemRecord(categoryName = "Other", itemName = "Watch", description = "manual note"),
            MasterCatalog(),
        )
        assertNull(links.itemMasterId)
        assertNull(links.unitId)
        assertNull(links.lockerId)
    }
}
