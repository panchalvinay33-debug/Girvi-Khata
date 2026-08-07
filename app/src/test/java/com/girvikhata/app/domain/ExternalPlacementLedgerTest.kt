package com.girvikhata.app.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar

class ExternalPlacementLedgerTest {
    @Test
    fun exactDays_keepsExternalInterestSeparate() {
        val start = noon(2026, Calendar.JANUARY, 1)
        val at = noon(2026, Calendar.JANUARY, 31)
        val projection = ExternalPlacementLedger.project(
            advances = listOf(ExternalFundingAdvance(amountPaise = 80_000_00, monthlyRateBasisPoints = 150, createdAt = start)),
            payments = emptyList(),
            at = at,
        )
        assertEquals(80_000_00, projection.principalOutstandingPaise)
        assertEquals(1_200_00, projection.grossInterestPaise)
        assertEquals(81_200_00, projection.totalDuePaise)
    }

    @Test
    fun payment_appliesInterestFirstThenPrincipal() {
        val start = noon(2026, Calendar.JANUARY, 1)
        val at = noon(2026, Calendar.JANUARY, 31)
        val paymentAt = noon(2026, Calendar.JANUARY, 31)
        val projection = ExternalPlacementLedger.project(
            advances = listOf(ExternalFundingAdvance(amountPaise = 80_000_00, monthlyRateBasisPoints = 150, createdAt = start)),
            payments = listOf(ExternalFundingPayment(id = "p1", amountPaise = 21_200_00, createdAt = paymentAt)),
            at = at,
        )
        assertEquals(60_000_00, projection.principalOutstandingPaise)
        assertEquals(0, projection.interestOutstandingPaise)
        assertEquals(60_000_00, projection.totalDuePaise)
    }

    @Test
    fun reversal_restoresPaymentEffect() {
        val start = noon(2026, Calendar.JANUARY, 1)
        val at = noon(2026, Calendar.JANUARY, 31)
        val payments = listOf(
            ExternalFundingPayment(id = "p1", amountPaise = 21_200_00, createdAt = at),
            ExternalFundingPayment(id = "rev", amountPaise = 21_200_00, createdAt = at, isReversal = true, reversedPaymentId = "p1"),
        )
        val projection = ExternalPlacementLedger.project(
            advances = listOf(ExternalFundingAdvance(amountPaise = 80_000_00, monthlyRateBasisPoints = 150, createdAt = start)),
            payments = payments,
            at = at,
        )
        assertEquals(81_200_00, projection.totalDuePaise)
    }

    private fun noon(year: Int, month: Int, day: Int): Long = Calendar.getInstance().apply {
        clear(); set(year, month, day, 12, 0, 0)
    }.timeInMillis
}
