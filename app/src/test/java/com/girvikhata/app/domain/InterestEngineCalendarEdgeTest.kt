package com.girvikhata.app.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar

class InterestEngineCalendarEdgeTest {
    @Test
    fun jan31ToFeb28_countsOneCalendarMonth() {
        val quote = InterestEngine.quote(
            1_000_000,
            date(2026, 1, 31),
            date(2026, 2, 28),
            InterestTerms(monthlyRateBasisPoints = 200),
        )
        assertEquals(1, quote.completedMonths)
        assertEquals(0, quote.remainingDays)
        assertEquals(20_000, quote.interestPaise)
    }

    @Test
    fun leapYearFeb29ToMar29_countsOneCalendarMonth() {
        val quote = InterestEngine.quote(
            1_000_000,
            date(2028, 2, 29),
            date(2028, 3, 29),
            InterestTerms(monthlyRateBasisPoints = 200),
        )
        assertEquals(1, quote.completedMonths)
        assertEquals(20_000, quote.interestPaise)
    }

    @Test
    fun zeroRateReturnsPrincipalOnly() {
        val quote = InterestEngine.quote(
            987_654,
            date(2026, 1, 1),
            date(2027, 1, 1),
            InterestTerms(monthlyRateBasisPoints = 0),
        )
        assertEquals(0, quote.interestPaise)
        assertEquals(987_654, quote.totalPayablePaise)
    }

    @Test
    fun exactDaysFlatChargeWorksWithoutPrincipalDependence() {
        val quote = InterestEngine.quote(
            5_000_000,
            date(2026, 5, 1),
            date(2026, 5, 11),
            InterestTerms(
                mode = InterestMode.FLAT_PER_MONTH,
                flatMonthlyChargePaise = 30_000,
                periodRule = InterestPeriodRule.EXACT_DAYS,
            ),
        )
        assertEquals(10_000, quote.interestPaise)
    }

    private fun date(year: Int, month: Int, day: Int): Long = Calendar.getInstance().apply {
        clear()
        set(year, month - 1, day, 12, 0, 0)
    }.timeInMillis
}
