package com.girvikhata.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.Locale

class BusinessOperationsTest {
    @Test
    fun customerMatcherPrefersExactMobile() {
        val customers = listOf(
            CustomerCandidate("1", "Ramesh Patel", "9999999999", "Pitol"),
            CustomerCandidate("2", "Ramesh Patel", "8888888888", "Dahod"),
        )
        assertEquals("2", CustomerMatcher.findBestMatch(customers, "Ramesh Patel", "88888 88888")?.id)
    }

    @Test
    fun customerMatcherNormalizesNameWhitespaceAndCase() {
        val customers = listOf(CustomerCandidate("1", "Ramesh   Patel", "", "Pitol"))
        assertEquals("1", CustomerMatcher.findBestMatch(customers, " ramesh patel ", "")?.id)
    }

    @Test
    fun customerSearchIncludesAddress() {
        val customers = listOf(CustomerCandidate("1", "Ramesh", "9999999999", "Pitol"))
        assertEquals(1, CustomerMatcher.search(customers, "pitol").size)
    }

    @Test
    fun invalidDeductionIsRejected() {
        val item = DraftItem(
            categoryName = "Gold",
            itemName = "Ring",
            grossWeightGrams = BigDecimal("5"),
            deductionWeightGrams = BigDecimal("6"),
        )
        assertTrue(item.validate().any { it.contains("exceed") })
    }

    @Test
    fun netWeightIsCalculated() {
        val item = DraftItem(
            categoryName = "Gold",
            itemName = "Ring",
            grossWeightGrams = BigDecimal("8.5"),
            deductionWeightGrams = BigDecimal("0.5"),
        )
        assertEquals(BigDecimal("8.0"), item.netWeightGrams)
    }

    @Test
    fun sequenceUsesHighestExistingNumberForToday() {
        val date = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse("2026-07-28")!!
        val number = GirviSequence.nextNumber(
            listOf("GK-20260728-0001", "GK-20260728-0004", "GK-20260727-0099"),
            date,
        )
        assertEquals("GK-20260728-0005", number)
    }

    @Test
    fun interestBreakdownIsTransparentAndExact() {
        val result = SimpleInterestPreview.calculate(
            principalPaise = 100_000,
            monthlyRateBasisPoints = 200,
            months = 6,
        )
        assertEquals(12_000, result.totalInterestPaise)
        assertEquals(112_000, result.totalPayablePaise)
        assertEquals(2_000, result.rows.first().interestPaise)
        assertEquals(112_000, result.rows.last().closingPayablePaise)
    }

    @Test
    fun categoryWithActiveGirviCannotBeDeactivated() {
        assertFalse(CategoryRules.canDeactivate("Gold", listOf("Gold", "Silver")))
        assertTrue(CategoryRules.canDeactivate("Electronics", listOf("Gold", "Silver")))
    }

    @Test
    fun noCustomerMatchReturnsNull() {
        assertNull(CustomerMatcher.findBestMatch(emptyList(), "Ramesh", "9999999999"))
    }
}
