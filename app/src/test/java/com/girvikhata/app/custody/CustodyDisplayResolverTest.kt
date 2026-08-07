package com.girvikhata.app.custody

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CustodyDisplayResolverTest {
    @Test
    fun `latest locker movement becomes current display`() {
        val shop = StorageLocation(id = "shop", name = "Shop Locker")
        val home = StorageLocation(id = "home", name = "Home Locker 1")
        val snapshot = CustodyPlacementSnapshot(
            locations = listOf(shop, home),
            movements = listOf(
                CustodyMovement(girviId = "g1", itemId = "i1", destinationType = "LOCATION", destinationId = shop.id, movedAt = 10L),
                CustodyMovement(girviId = "g1", itemId = "i1", destinationType = "LOCATION", destinationId = home.id, movedAt = 20L),
            ),
        )

        val current = CustodyDisplayResolver.currentItem(snapshot, "i1")

        assertEquals("Home Locker 1", current.label)
        assertEquals(20L, current.movedAt)
        assertFalse(current.isExternal)
    }

    @Test
    fun `external placement display contains party and lot`() {
        val party = ExternalParty(id = "p1", name = "Sharma Finance")
        val lot = PlacementLot(id = "l1", lotNumber = "LOT-008", partyId = party.id, openedAt = 100L, amountReceivedPaise = 0L, monthlyRateBasisPoints = 0)
        val snapshot = CustodyPlacementSnapshot(
            parties = listOf(party),
            lots = listOf(lot),
            movements = listOf(
                CustodyMovement(girviId = "g1", itemId = "i1", destinationType = "EXTERNAL", destinationId = party.id, lotId = lot.id, movedAt = 100L),
            ),
        )

        val current = CustodyDisplayResolver.currentItem(snapshot, "i1")

        assertEquals("Sharma Finance • LOT-008", current.label)
        assertTrue(current.isExternal)
        assertEquals("LOT-008", current.lotNumber)
    }

    @Test
    fun `girvi summary groups same current location`() {
        val locker = StorageLocation(id = "loc", name = "Home Locker 1")
        val snapshot = CustodyPlacementSnapshot(
            locations = listOf(locker),
            movements = listOf(
                CustodyMovement(girviId = "g", itemId = "i1", destinationType = "LOCATION", destinationId = locker.id, movedAt = 1L),
                CustodyMovement(girviId = "g", itemId = "i2", destinationType = "LOCATION", destinationId = locker.id, movedAt = 2L),
            ),
        )

        assertEquals("2× Home Locker 1", CustodyDisplayResolver.girviSummary(snapshot, listOf("i1", "i2")))
        assertEquals("Not assigned", CustodyDisplayResolver.girviSummary(snapshot, listOf("i3")))
    }
}
