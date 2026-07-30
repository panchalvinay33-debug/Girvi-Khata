package com.girvikhata.app.domain

import com.girvikhata.app.data.GirviRecord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SettlementDocumentsTest {
    private fun girvi(status: String = "ACTIVE") = GirviRecord(
        girviNumber = "GK-20260730-0001",
        customerId = "c1",
        customerName = "Test Customer",
        categoryName = "Jewellery",
        itemName = "Ring",
        weightGrams = "5",
        principalPaise = 100_000,
        monthlyRateBasisPoints = 200,
        status = status,
    )

    @Test fun positiveAndNegativeAdjustmentsAreCumulative() {
        val first = ManualInterestAdjustment.apply(girvi(), "150", "Owner approved correction")
        assertEquals(15_000, first.cumulativePaise)
        val second = ManualInterestAdjustment.apply(first.updatedGirvi, "-50.50", "Customer concession recorded")
        assertEquals(9_950, second.cumulativePaise)
    }

    @Test(expected = IllegalArgumentException::class)
    fun adjustmentRequiresReason() {
        ManualInterestAdjustment.apply(girvi(), "100", "no")
    }

    @Test(expected = IllegalArgumentException::class)
    fun releasedGirviCannotBeAdjusted() {
        ManualInterestAdjustment.apply(girvi("RELEASED"), "100", "Late correction reason")
    }

    @Test fun receiptContainsSettlementAndAdjustment() {
        val adjusted = ManualInterestAdjustment.apply(girvi(), "100", "Owner correction reason").updatedGirvi
        val receipt = SettlementReceiptText.create(adjusted, 2)
        assertTrue(receipt.contains("GK-20260730-0001"))
        assertTrue(receipt.contains("Manual interest adjustment: +₹100.00"))
        assertTrue(receipt.contains("Total due at receipt"))
    }
}
