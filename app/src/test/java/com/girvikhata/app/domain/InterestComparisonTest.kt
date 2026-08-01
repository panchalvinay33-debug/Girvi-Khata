package com.girvikhata.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class InterestComparisonTest {
    @Test
    fun oneDayShowsExactAndFullMonthDifference() {
        val result = InterestComparisonEngine.compare(
            principalPaise = 1_000_000,
            startAtMillis = date(2026, 1, 1),
            endAtMillis = date(2026, 1, 2),
            baseTerms = InterestTerms(monthlyRateBasisPoints = 200),
        )
        assertEquals(667, result.exactDays.interestPaise)
        assertEquals(20_000, result.fullMonthStarted.interestPaise)
        assertEquals(667, result.completedMonthsPlusDays.interestPaise)
        assertTrue(result.fullMonthStarted.interestPaise > result.exactDays.interestPaise)
    }

    private fun date(year: Int, month: Int, day: Int): Long = Calendar.getInstance().apply {
        clear()
        set(year, month - 1, day, 12, 0, 0)
    }.timeInMillis
}
