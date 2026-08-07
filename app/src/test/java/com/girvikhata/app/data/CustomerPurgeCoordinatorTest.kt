package com.girvikhata.app.data

import com.girvikhata.app.custody.CustodyMovement
import com.girvikhata.app.custody.CustodyPlacementSnapshot
import com.girvikhata.app.custody.PlacementItem
import com.girvikhata.app.custody.PlacementLot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CustomerPurgeCoordinatorTest {
    @Test
    fun `complete mutation removes customer and all own girvis only`() {
        val c1 = CustomerRecord(id = "c1", name = "One", mobile = "", address = "")
        val c2 = CustomerRecord(id = "c2", name = "Two", mobile = "", address = "")
        val g1 = girvi("g1", "GK-1", "c1", "One")
        val g2 = girvi("g2", "GK-2", "c1", "One")
        val g3 = girvi("g3", "GK-3", "c2", "Two")
        val before = AppSnapshot(customers = listOf(c1, c2), girvis = listOf(g1, g2, g3))

        val after = DeleteCustomerCompleteMutation("c1").apply(before)

        assertEquals(listOf("c2"), after.customers.map { it.id })
        assertEquals(listOf("g3"), after.girvis.map { it.id })
    }

    @Test
    fun `custody purge removes only deleted customer item links from shared financed lot`() {
        val lot = PlacementLot(
            id = "lot1",
            lotNumber = "LOT-1",
            partyId = "party1",
            openedAt = 100L,
            amountReceivedPaise = 50000L,
            monthlyRateBasisPoints = 100,
            items = listOf(
                PlacementItem(girviId = "g1", itemId = "i1", addedAt = 100L),
                PlacementItem(girviId = "g2", itemId = "i2", addedAt = 100L),
            ),
        )
        val before = CustodyPlacementSnapshot(
            lots = listOf(lot),
            movements = listOf(
                CustodyMovement(girviId = "g1", itemId = "i1", destinationType = "EXTERNAL", destinationId = "party1", lotId = "lot1", movedAt = 100L),
                CustodyMovement(girviId = "g2", itemId = "i2", destinationType = "EXTERNAL", destinationId = "party1", lotId = "lot1", movedAt = 100L),
            ),
        )

        val after = purgeCustomerCustody(before, setOf("g1"))

        assertEquals(1, after.lots.size)
        assertEquals(listOf("i2"), after.lots.single().items.map { it.itemId })
        assertEquals(listOf("g2"), after.movements.map { it.girviId })
        assertEquals(50000L, after.lots.single().amountReceivedPaise)
    }

    @Test
    fun `empty non financial lot is removed with deleted customer`() {
        val before = CustodyPlacementSnapshot(
            lots = listOf(
                PlacementLot(
                    id = "lot1",
                    lotNumber = "LOT-1",
                    partyId = "party1",
                    openedAt = 100L,
                    amountReceivedPaise = 0L,
                    monthlyRateBasisPoints = 0,
                    items = listOf(PlacementItem(girviId = "g1", itemId = "i1", addedAt = 100L)),
                ),
            ),
        )

        val after = purgeCustomerCustody(before, setOf("g1"))

        assertTrue(after.lots.isEmpty())
    }

    @Test
    fun `purge with no girvi ids leaves custody unchanged`() {
        val before = CustodyPlacementSnapshot()
        assertTrue(purgeCustomerCustody(before, emptySet()) === before)
    }

    private fun girvi(id: String, number: String, customerId: String, customerName: String) = GirviRecord(
        id = id,
        girviNumber = number,
        customerId = customerId,
        customerName = customerName,
        categoryName = "Gold",
        itemName = "Ring",
        weightGrams = "10",
        principalPaise = 10000L,
        monthlyRateBasisPoints = 100,
        createdAt = 1L,
    )
}
