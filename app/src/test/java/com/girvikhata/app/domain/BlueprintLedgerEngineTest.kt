package com.girvikhata.app.domain

import com.girvikhata.app.data.GirviItemRecord
import com.girvikhata.app.data.GirviRecord
import com.girvikhata.app.data.PaymentRecord
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar

class BlueprintLedgerEngineTest {
    private fun day(year: Int, month: Int, date: Int): Long = Calendar.getInstance().apply {
        clear()
        set(year, month - 1, date, 12, 0, 0)
    }.timeInMillis

    private val terms = InterestTerms(
        mode = InterestMode.PERCENT_PER_MONTH,
        monthlyRateBasisPoints = 200,
        periodRule = InterestPeriodRule.EXACT_DAYS,
    )

    @Test
    fun `original and additional advances remain separate and payments reduce totals`() {
        val start = day(2026, 1, 1)
        val additionalAt = day(2026, 1, 16)
        val settlement = day(2026, 1, 31)
        val item = GirviItemRecord(
            id = "i1",
            categoryName = "Gold",
            itemName = "Ring",
            grossWeightGrams = "5",
            description = GirviInterestMetadata.attach("ring", terms),
        )
        val advance = GirviAdvanceMetadata.Advance(
            id = "a1",
            amountPaise = 50_000L,
            createdAt = additionalAt,
            terms = terms,
            note = "extra cash",
        )
        val payment = PaymentRecord(
            id = "p1",
            receiptNumber = "R1",
            amountPaise = 30_000L,
            principalPaise = 20_000L,
            interestPaise = 10_000L,
            createdAt = day(2026, 1, 25),
        )
        val girvi = GirviRecord(
            id = "g1",
            girviNumber = "G-001",
            customerId = "c1",
            customerName = "Ravi",
            categoryName = "Gold",
            itemName = "Ring",
            weightGrams = "5",
            principalPaise = 100_000L,
            monthlyRateBasisPoints = 200,
            createdAt = start,
            items = listOf(item),
            payments = listOf(payment),
            releaseNote = GirviAdvanceMetadata.attach("", listOf(advance)),
        )

        val result = BlueprintLedgerEngine.project(girvi, settlement)

        assertEquals(150_000L, result.totalAdvancedPaise)
        assertEquals(20_000L, result.principalReturnedPaise)
        assertEquals(130_000L, result.principalOutstandingPaise)
        // Original: 2,000 paise for 30 days. Additional: 500 paise for 15 days.
        assertEquals(2_500L, result.grossInterestAccruedPaise)
        assertEquals(10_000L, result.interestReceivedPaise)
        assertEquals(0L, result.interestOutstandingPaise)
        assertEquals(130_000L, result.totalDuePaise)
        assertEquals(3, result.lines.size)
    }

    @Test
    fun `reversal removes original payment from effective ledger`() {
        val original = PaymentRecord(
            id = "p1",
            receiptNumber = "R1",
            amountPaise = 10_000L,
            principalPaise = 10_000L,
            interestPaise = 0,
            createdAt = day(2026, 2, 1),
        )
        val reversal = PaymentRecord(
            id = "p2",
            receiptNumber = "R2",
            amountPaise = 10_000L,
            principalPaise = 10_000L,
            interestPaise = 0,
            createdAt = day(2026, 2, 2),
            isReversal = true,
            reversedPaymentId = "p1",
        )

        assertEquals(emptyList<PaymentRecord>(), BlueprintLedgerEngine.effectivePayments(listOf(original, reversal)))
    }
}
