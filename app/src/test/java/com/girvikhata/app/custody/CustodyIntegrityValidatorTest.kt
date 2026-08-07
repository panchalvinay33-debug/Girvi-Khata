package com.girvikhata.app.custody

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CustodyIntegrityValidatorTest {
    @Test
    fun `valid locker and external history has no integrity issue`() {
        val location = StorageLocation(id = "loc-1", name = "Home Locker 1")
        val party = ExternalParty(id = "party-1", name = "Finance A")
        val lot = PlacementLot(
            id = "lot-1",
            lotNumber = "LOT-1",
            partyId = party.id,
            openedAt = 100L,
            amountReceivedPaise = 10_000L,
            monthlyRateBasisPoints = 100,
            items = listOf(PlacementItem("g1", "i1", 100L, removedAt = 200L)),
        )
        val snapshot = CustodyPlacementSnapshot(
            locations = listOf(location),
            parties = listOf(party),
            lots = listOf(lot),
            movements = listOf(
                CustodyMovement(girviId = "g1", itemId = "i1", destinationType = "EXTERNAL", destinationId = party.id, lotId = lot.id, movedAt = 100L),
                CustodyMovement(girviId = "g1", itemId = "i1", destinationType = "LOCATION", destinationId = location.id, movedAt = 200L),
            ),
        )

        val issues = CustodyIntegrityValidator.validate(setOf("g1" to "i1"), emptySet(), snapshot)

        assertTrue(issues.isEmpty())
    }

    @Test
    fun `same item in two active lots is flagged`() {
        val party = ExternalParty(id = "p1", name = "Finance")
        val snapshot = CustodyPlacementSnapshot(
            parties = listOf(party),
            lots = listOf(
                PlacementLot(id = "l1", lotNumber = "LOT-1", partyId = party.id, openedAt = 1L, amountReceivedPaise = 0L, monthlyRateBasisPoints = 0, items = listOf(PlacementItem("g1", "i1", 1L))),
                PlacementLot(id = "l2", lotNumber = "LOT-2", partyId = party.id, openedAt = 2L, amountReceivedPaise = 0L, monthlyRateBasisPoints = 0, items = listOf(PlacementItem("g1", "i1", 2L))),
            ),
        )

        val issues = CustodyIntegrityValidator.validate(setOf("g1" to "i1"), emptySet(), snapshot)

        assertTrue(issues.any { it.code == "ITEM_MULTI_ACTIVE_LOTS" })
    }

    @Test
    fun `released girvi still external and unknown references are flagged`() {
        val party = ExternalParty(id = "p1", name = "Finance")
        val lot = PlacementLot(
            id = "l1",
            lotNumber = "LOT-1",
            partyId = party.id,
            openedAt = 1L,
            amountReceivedPaise = 0L,
            monthlyRateBasisPoints = 0,
            items = listOf(
                PlacementItem("g1", "i1", 1L),
                PlacementItem("missing-g", "missing-i", 1L),
            ),
        )
        val snapshot = CustodyPlacementSnapshot(parties = listOf(party), lots = listOf(lot))

        val issues = CustodyIntegrityValidator.validate(setOf("g1" to "i1"), setOf("g1"), snapshot)

        assertTrue(issues.any { it.code == "RELEASED_GIRVI_EXTERNAL" })
        assertTrue(issues.any { it.code == "LOT_ITEM_UNKNOWN" })
        assertFalse(issues.isEmpty())
    }
}
