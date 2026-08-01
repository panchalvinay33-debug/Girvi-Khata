package com.girvikhata.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Calendar

class InterestEngineTest {
    @Test
    fun percentMonthly_oneCalendarMonth() {
        val quote = InterestEngine.quote(
            principalPaise = 1_000_000,
            startAtMillis = date(2026, 1, 1),
            endAtMillis = date(2026, 2, 1),
            terms = InterestTerms(monthlyRateBasisPoints = 200),
        )
        assertEquals(20_000, quote.interestPaise)
        assertEquals(1_020_000, quote.totalPayablePaise)
        assertEquals(1, quote.completedMonths)
        assertEquals(0, quote.remainingDays)
    }

    @Test
    fun flatMonthly_threeMonths() {
        val quote = InterestEngine.quote(
            principalPaise = 1_000_000,
            startAtMillis = date(2026, 1, 10),
            endAtMillis = date(2026, 4, 10),
            terms = InterestTerms(
                mode = InterestMode.FLAT_PER_MONTH,
                flatMonthlyChargePaise = 30_000,
            ),
        )
        assertEquals(90_000, quote.interestPaise)
        assertEquals(1_090_000, quote.totalPayablePaise)
    }

    @Test
    fun exactDays_usesMonthlyChargeDividedByThirty() {
        val quote = InterestEngine.quote(
            principalPaise = 1_000_000,
            startAtMillis = date(2026, 1, 1),
            endAtMillis = date(2026, 1, 16),
            terms = InterestTerms(
                monthlyRateBasisPoints = 200,
                periodRule = InterestPeriodRule.EXACT_DAYS,
            ),
        )
        assertEquals(10_000, quote.interestPaise)
        assertEquals(15, quote.elapsedDays)
    }

    @Test
    fun fullMonthStarted_oneDayChargesOneMonth() {
        val quote = InterestEngine.quote(
            principalPaise = 1_000_000,
            startAtMillis = date(2026, 1, 1),
            endAtMillis = date(2026, 1, 2),
            terms = InterestTerms(
                monthlyRateBasisPoints = 200,
                periodRule = InterestPeriodRule.FULL_MONTH_STARTED,
            ),
        )
        assertEquals(20_000, quote.interestPaise)
        assertEquals(1, quote.chargedMonths)
    }

    @Test
    fun completedMonthsPlusDays_keepsPartialDaily() {
        val quote = InterestEngine.quote(
            principalPaise = 1_000_000,
            startAtMillis = date(2026, 1, 1),
            endAtMillis = date(2026, 2, 16),
            terms = InterestTerms(
                monthlyRateBasisPoints = 200,
                periodRule = InterestPeriodRule.COMPLETED_MONTHS_PLUS_DAYS,
            ),
        )
        assertEquals(30_000, quote.interestPaise)
        assertEquals(1, quote.completedMonths)
        assertEquals(15, quote.remainingDays)
    }

    @Test
    fun compoundMonthly_capitalizesEachMonth() {
        val quote = InterestEngine.quote(
            principalPaise = 1_000_000,
            startAtMillis = date(2026, 1, 1),
            endAtMillis = date(2026, 3, 1),
            terms = InterestTerms(
                monthlyRateBasisPoints = 200,
                compoundEveryMonths = 1,
            ),
        )
        assertEquals(40_400, quote.interestPaise)
        assertEquals(1_040_400, quote.totalPayablePaise)
        assertEquals(2, quote.compoundPeriodsApplied)
    }

    @Test
    fun compoundEveryThreeMonths_capitalizesAtIntervalBoundary() {
        val quote = InterestEngine.quote(
            principalPaise = 1_000_000,
            startAtMillis = date(2026, 1, 1),
            endAtMillis = date(2026, 7, 1),
            terms = InterestTerms(
                monthlyRateBasisPoints = 200,
                compoundEveryMonths = 3,
            ),
        )
        // First 3 months = 6% => 1,060,000 paise; next 3 months = 6% of that = 63,600 paise.
        assertEquals(123_600, quote.interestPaise)
        assertEquals(1_123_600, quote.totalPayablePaise)
        assertEquals(2, quote.compoundPeriodsApplied)
    }

    @Test(expected = IllegalArgumentException::class)
    fun endBeforeStart_isRejected() {
        InterestEngine.quote(
            principalPaise = 1_000_000,
            startAtMillis = date(2026, 2, 1),
            endAtMillis = date(2026, 1, 1),
            terms = InterestTerms(monthlyRateBasisPoints = 200),
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun compoundFlatCharge_isRejected() {
        InterestTerms(
            mode = InterestMode.FLAT_PER_MONTH,
            flatMonthlyChargePaise = 30_000,
            compoundEveryMonths = 1,
        )
    }

    @Test
    fun codec_roundTripsAllTerms() {
        val terms = InterestTerms(
            monthlyRateBasisPoints = 275,
            periodRule = InterestPeriodRule.EXACT_DAYS,
            compoundEveryMonths = 6,
        )
        val encoded = InterestTermsCodec.encode(terms)
        assertEquals(terms, InterestTermsCodec.decode(encoded))
        assertNotNull(InterestTermsCodec.decode(encoded))
        assertNull(InterestTermsCodec.decode("bad-value"))
    }

    private fun date(year: Int, month: Int, day: Int): Long = Calendar.getInstance().apply {
        clear()
        set(year, month - 1, day, 12, 0, 0)
    }.timeInMillis
}
