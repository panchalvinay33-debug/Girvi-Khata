package com.girvikhata.app.custody

import com.girvikhata.app.domain.ExternalFundingAdvance
import com.girvikhata.app.domain.ExternalInterestRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CustodyReportEngineTest {
    @Test
    fun `build groups current locker items external party due and unassigned items`() {
        val locker = StorageLocation(id = "loc1", name = "Home Locker 1")
        val party = ExternalParty(id = "p1", name = "Finance A")
        val lot = PlacementLot(
            id = "lot1",
            lotNumber = "LOT-1",
            partyId = party.id,
            openedAt = 1_000L,
            amountReceivedPaise = 100_000L,
            monthlyRateBasisPoints = 100,
            items = listOf(PlacementItem("g2", "i2", 1_000L)),
            fundingAdvances = listOf(
                ExternalFundingAdvance(
                    id = "a1",
                    amountPaise = 100_000L,
                    monthlyRateBasisPoints = 100,
                    createdAt = 1_000L,
                    interestRule = ExternalInterestRule.EXACT_DAYS,
                ),
            ),
        )
        val snapshot = CustodyPlacementSnapshot(
            locations = listOf(locker),
            parties = listOf(party),
            lots = listOf(lot),
            movements = listOf(
                CustodyMovement(girviId = "g1", itemId = "i1", destinationType = "LOCATION", destinationId = locker.id, movedAt = 2_000L),
                CustodyMovement(girviId = "g2", itemId = "i2", destinationType = "EXTERNAL", destinationId = party.id, lotId = lot.id, movedAt = 1_000L),
            ),
        )

        val report = CustodyReportEngine.build(snapshot, setOf("i1", "i2", "i3"), at = 31L * 24L * 60L * 60L * 1000L)

        assertEquals(1, report.storage.single().itemCount)
        assertEquals(1, report.externalParties.single().activeLotCount)
        assertEquals(1, report.externalParties.single().activeItemCount)
        assertTrue(report.externalParties.single().totalDuePaise >= 100_000L)
        assertEquals(setOf("i3"), report.unassignedItemIds)
        assertEquals(2, report.recentMovements.size)
    }

    @Test
    fun `latest movement decides current locker count`() {
        val first = StorageLocation(id = "a", name = "Shop")
        val second = StorageLocation(id = "b", name = "Home")
        val snapshot = CustodyPlacementSnapshot(
            locations = listOf(first, second),
            movements = listOf(
                CustodyMovement(girviId = "g", itemId = "i", destinationType = "LOCATION", destinationId = first.id, movedAt = 10L),
                CustodyMovement(girviId = "g", itemId = "i", destinationType = "LOCATION", destinationId = second.id, movedAt = 20L),
            ),
        )

        val report = CustodyReportEngine.build(snapshot, setOf("i"), at = 20L)

        assertEquals("Home", report.storage.single().locationName)
        assertTrue(report.unassignedItemIds.isEmpty())
    }
}
