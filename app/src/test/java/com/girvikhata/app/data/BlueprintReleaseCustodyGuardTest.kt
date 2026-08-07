package com.girvikhata.app.data

import com.girvikhata.app.custody.CustodyPlacementSnapshot
import com.girvikhata.app.custody.PlacementItem
import com.girvikhata.app.custody.PlacementLot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BlueprintReleaseCustodyGuardTest {
    @Test
    fun `active item in external lot blocks girvi release`() {
        val custody = CustodyPlacementSnapshot(
            lots = listOf(
                PlacementLot(
                    id = "lot-1",
                    lotNumber = "LOT-101",
                    partyId = "party-1",
                    openedAt = 100L,
                    amountReceivedPaise = 10_000L,
                    monthlyRateBasisPoints = 200,
                    items = listOf(
                        PlacementItem(girviId = "g-1", itemId = "item-1", addedAt = 100L),
                    ),
                ),
            ),
        )

        assertEquals(listOf("LOT-101"), activeExternalLotNumbers("g-1", custody))
    }

    @Test
    fun `returned item no longer blocks girvi release`() {
        val custody = CustodyPlacementSnapshot(
            lots = listOf(
                PlacementLot(
                    id = "lot-1",
                    lotNumber = "LOT-101",
                    partyId = "party-1",
                    openedAt = 100L,
                    amountReceivedPaise = 10_000L,
                    monthlyRateBasisPoints = 200,
                    items = listOf(
                        PlacementItem(girviId = "g-1", itemId = "item-1", addedAt = 100L, removedAt = 200L),
                    ),
                ),
            ),
        )

        assertTrue(activeExternalLotNumbers("g-1", custody).isEmpty())
    }
}
