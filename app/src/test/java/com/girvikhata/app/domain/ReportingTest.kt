package com.girvikhata.app.domain

import com.girvikhata.app.data.AppSnapshot
import com.girvikhata.app.data.CustomerRecord
import com.girvikhata.app.data.GirviRecord
import com.girvikhata.app.data.PaymentRecord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReportingTest {
    private val customer = CustomerRecord(id = "c1", name = "Ramesh")

    private fun girvi(payments: List<PaymentRecord> = emptyList(), status: String = "ACTIVE") = GirviRecord(
        id = "g1",
        girviNumber = "GK-1",
        customerId = customer.id,
        customerName = customer.name,
        categoryName = "Gold",
        itemName = "Ring",
        weightGrams = "10",
        principalPaise = 100_000,
        monthlyRateBasisPoints = 200,
        status = status,
        payments = payments,
    )

    @Test fun reversedPaymentIsExcludedFromEffectiveCollections() {
        val original = PaymentRecord(
            id = "p1", receiptNumber = "PAY-1", amountPaise = 10_000,
            principalPaise = 8_000, interestPaise = 2_000, createdAt = 100,
        )
        val reversal = PaymentRecord(
            id = "p2", receiptNumber = "PAY-2", amountPaise = 10_000,
            principalPaise = 8_000, interestPaise = 2_000, createdAt = 200,
            isReversal = true, reversedPaymentId = original.id,
        )
        assertEquals(0, EffectiveLedger.receivedPaise(girvi(listOf(original, reversal))))
    }

    @Test fun collectionsRespectDateRangeAndIgnoreReversals() {
        val payment = PaymentRecord(
            id = "p1", receiptNumber = "PAY-1", amountPaise = 5_000,
            principalPaise = 5_000, interestPaise = 0, createdAt = 500,
        )
        val snapshot = AppSnapshot(customers = listOf(customer), girvis = listOf(girvi(listOf(payment))))
        assertEquals(1, ReportingEngine.collections(snapshot, DateRange(400, 600)).size)
        assertTrue(ReportingEngine.collections(snapshot, DateRange(1, 100)).isEmpty())
    }

    @Test fun statusAndQueryFiltersWorkTogether() {
        val active = girvi().copy(id = "a", girviNumber = "GK-A", itemName = "Ring")
        val released = girvi(status = "RELEASED").copy(id = "r", girviNumber = "GK-R", itemName = "Chain")
        val snapshot = AppSnapshot(customers = listOf(customer), girvis = listOf(active, released))
        assertEquals(listOf("GK-A"), ReportingEngine.filterGirvi(snapshot, GirviStatusFilter.ACTIVE).map { it.girviNumber })
        assertEquals(listOf("GK-R"), ReportingEngine.filterGirvi(snapshot, GirviStatusFilter.ALL, "Chain").map { it.girviNumber })
    }

    @Test fun customerLedgerCalculatesOutstanding() {
        val payment = PaymentRecord(
            id = "p1", receiptNumber = "PAY-1", amountPaise = 20_000,
            principalPaise = 18_000, interestPaise = 2_000,
        )
        val snapshot = AppSnapshot(customers = listOf(customer), girvis = listOf(girvi(listOf(payment))))
        val result = ReportingEngine.customerLedger(snapshot, customer.id, 1)
        assertEquals(82_000, result.outstandingPrincipalPaise)
        assertEquals(0, result.outstandingInterestPaise)
        assertEquals(20_000, result.effectiveReceivedPaise)
    }
}
