package com.girvikhata.app.domain

import com.girvikhata.app.data.GirviRecord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Locale

class PaymentUseCasesTest {
    private fun girvi() = GirviRecord(
        girviNumber = "GK-20260729-0001",
        customerId = "c1",
        customerName = "Test Customer",
        categoryName = "Jewellery",
        itemName = "Ring",
        weightGrams = "5",
        principalPaise = 100_000,
        monthlyRateBasisPoints = 200,
    )

    @Test
    fun `receipt sequence scans highest number for date`() {
        val date = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse("2026-07-29")!!
        assertEquals(
            "PAY-20260729-0008",
            ReceiptNumberGenerator.next(
                listOf("PAY-20260729-0002", "PAY-20260729-0007", "PAY-20260728-9999"),
                date,
            ),
        )
    }

    @Test
    fun `posting payment updates immutable ledger`() {
        val updated = GirviSettlementUseCase.postPayment(
            girvi = girvi(),
            months = 1,
            amountPaise = 12_000,
            allocationMode = PaymentAllocationMode.INTEREST_FIRST,
            paymentMode = "CASH",
            note = "Part payment",
            allReceiptNumbers = emptyList(),
        )

        assertEquals(1, updated.payments.size)
        assertEquals(2_000, updated.payments.single().interestPaise)
        assertEquals(10_000, updated.payments.single().principalPaise)
        assertEquals(90_000, GirviSettlementUseCase.settlementView(updated, 1).principalDuePaise)
    }

    @Test
    fun `reversal restores effective balance`() {
        val paid = GirviSettlementUseCase.postPayment(
            girvi = girvi(),
            months = 1,
            amountPaise = 12_000,
            allocationMode = PaymentAllocationMode.INTEREST_FIRST,
            paymentMode = "UPI",
            note = "Received",
            allReceiptNumbers = emptyList(),
        )
        val reversed = GirviSettlementUseCase.reversePayment(
            girvi = paid,
            paymentId = paid.payments.single().id,
            reason = "Wrong entry",
            allReceiptNumbers = paid.payments.map { it.receiptNumber },
        )

        assertEquals(2, reversed.payments.size)
        assertTrue(reversed.payments.last().isReversal)
        assertEquals(102_000, GirviSettlementUseCase.settlementView(reversed, 1).totalDuePaise)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `release blocked when balance remains`() {
        GirviSettlementUseCase.release(
            girvi = girvi(),
            months = 1,
            releaseNote = "Item returned",
            explicitOwnerOverride = false,
        )
    }

    @Test
    fun `owner override can release with audit note`() {
        val released = GirviSettlementUseCase.release(
            girvi = girvi(),
            months = 1,
            releaseNote = "Owner approved settlement",
            explicitOwnerOverride = true,
        )

        assertEquals("RELEASED", released.status)
        assertTrue(released.releasedAt != null)
    }
}
