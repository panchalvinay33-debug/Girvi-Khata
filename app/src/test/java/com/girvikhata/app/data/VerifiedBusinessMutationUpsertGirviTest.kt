package com.girvikhata.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class VerifiedBusinessMutationUpsertGirviTest {
    private val customer = CustomerRecord(
        id = "customer-1",
        name = "Test Customer",
        createdAt = 100L,
    )

    private fun girvi(id: String, number: String, adjustmentPaise: Long = 0L) = GirviRecord(
        id = id,
        girviNumber = number,
        customerId = customer.id,
        customerName = customer.name,
        categoryName = "Jewellery",
        itemName = "Ring",
        weightGrams = "10",
        principalPaise = 10_000L,
        monthlyRateBasisPoints = 200,
        createdAt = 200L,
        manualInterestAdjustmentPaise = adjustmentPaise,
    )

    @Test
    fun upsertGirviReplacesOnlyMatchingRecord() {
        val target = girvi("girvi-1", "G-1")
        val untouched = girvi("girvi-2", "G-2")
        val snapshot = AppSnapshot(
            customers = listOf(customer),
            girvis = listOf(target, untouched),
        )
        val adjusted = target.copy(manualInterestAdjustmentPaise = -1_500L)

        val result = VerifiedBusinessMutation.UpsertGirvi(adjusted).apply(snapshot)

        assertEquals(-1_500L, result.girvis.single { it.id == target.id }.manualInterestAdjustmentPaise)
        assertSame(untouched, result.girvis.single { it.id == untouched.id })
        assertEquals(snapshot.customers, result.customers)
    }

    @Test
    fun upsertGirviDoesNotDuplicateMatchingId() {
        val target = girvi("girvi-1", "G-1")
        val snapshot = AppSnapshot(customers = listOf(customer), girvis = listOf(target))

        val result = VerifiedBusinessMutation.UpsertGirvi(
            target.copy(manualInterestAdjustmentPaise = 500L),
        ).apply(snapshot)

        assertEquals(1, result.girvis.count { it.id == target.id })
        assertEquals(500L, result.girvis.single().manualInterestAdjustmentPaise)
    }
}
