package com.girvikhata.app.data

import com.girvikhata.app.domain.MasterCatalog
import com.girvikhata.app.domain.MasterEntry
import com.girvikhata.app.domain.MasterKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RelationalMasterSchemaTest {
    @Test fun `upgrade adds all master foreign key columns without destructive SQL`() {
        val sql = RelationalMasterSchema.upgradeV1ToV2.joinToString("\n").uppercase()
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS MASTERS"))
        assertTrue(sql.contains("ITEM_MASTER_ID"))
        assertTrue(sql.contains("UNIT_ID"))
        assertTrue(sql.contains("LOCKER_ID"))
        assertTrue(sql.contains("INTEREST_PLAN_ID"))
        assertTrue(sql.contains("PAYMENT_MODE_ID"))
        assertFalse(sql.contains("DROP TABLE"))
        assertFalse(sql.contains("DELETE FROM"))
    }

    @Test fun `catalog normalization is deterministic`() {
        val entries = listOf(
            MasterEntry(id = "z", kind = MasterKind.UNIT, name = "Gram"),
            MasterEntry(id = "a", kind = MasterKind.ITEM, name = "Ring", categoryName = "Gold"),
            MasterEntry(id = "p", kind = MasterKind.INTEREST_PLAN, name = "Two", rateBasisPoints = 200),
        )
        val one = RelationalMasterSchema.normalized(MasterCatalog(entries))
        val two = RelationalMasterSchema.normalized(MasterCatalog(entries.reversed()))
        assertEquals(one, two)
        assertEquals(listOf("a", "p", "z"), one.map { it.id })
    }

    @Test(expected = IllegalArgumentException::class)
    fun `duplicate master ids are rejected`() {
        RelationalMasterSchema.normalized(MasterCatalog(listOf(
            MasterEntry(id = "same", kind = MasterKind.UNIT, name = "Gram"),
            MasterEntry(id = "same", kind = MasterKind.LOCKER, name = "Locker"),
        )))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `rate on non interest master is rejected`() {
        RelationalMasterSchema.normalized(MasterCatalog(listOf(
            MasterEntry(id = "unit", kind = MasterKind.UNIT, name = "Gram", rateBasisPoints = 200),
        )))
    }

    @Test fun `coverage only completes when every required link resolves`() {
        assertFalse(RelationalMasterCoverage(2, 2, 2, 1, 1, 1, 3, 3).complete)
        assertTrue(RelationalMasterCoverage(2, 2, 2, 2, 1, 1, 3, 3).complete)
    }
}
