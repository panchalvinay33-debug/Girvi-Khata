package com.girvikhata.app.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate

class InterestCalculatorTest {
    private val calculator = InterestCalculator()
    private val start = LocalDate.of(2026, 1, 1)

    @Test
    fun simpleMonthly_fullSixMonths() {
        val result = calculator.calculate(
            principal = BigDecimal("50000"),
            startDate = start,
            calculationDate = start.plusDays(180),
            plan = InterestPlan(
                name = "2 percent",
                kind = InterestKind.SIMPLE_MONTHLY_PERCENT,
                ratePercent = BigDecimal("2"),
                partialMonthRule = PartialMonthRule.EXACT_DAYS_30,
            ),
        )
        assertEquals(BigDecimal("6000"), result.automaticInterest)
        assertEquals(BigDecimal("56000"), result.totalPayable)
    }

    @Test
    fun extraDayBecomesFullMonth() {
        val result = calculator.calculate(
            principal = BigDecimal("10000"),
            startDate = start,
            calculationDate = start.plusDays(31),
            plan = InterestPlan(
                name = "full month rule",
                kind = InterestKind.SIMPLE_MONTHLY_PERCENT,
                ratePercent = BigDecimal("2"),
                partialMonthRule = PartialMonthRule.EXTRA_DAY_FULL_MONTH,
            ),
        )
        assertEquals(BigDecimal("400"), result.automaticInterest)
    }

    @Test
    fun halfMonthSlab() {
        val result = calculator.calculate(
            principal = BigDecimal("10000"),
            startDate = start,
            calculationDate = start.plusDays(40),
            plan = InterestPlan(
                name = "half slab",
                kind = InterestKind.SIMPLE_MONTHLY_PERCENT,
                ratePercent = BigDecimal("2"),
                partialMonthRule = PartialMonthRule.HALF_MONTH_SLAB,
            ),
        )
        assertEquals(BigDecimal("300"), result.automaticInterest)
    }

    @Test
    fun compoundEverySixMonths() {
        val result = calculator.calculate(
            principal = BigDecimal("50000"),
            startDate = start,
            calculationDate = start.plusDays(360),
            plan = InterestPlan(
                name = "compound",
                kind = InterestKind.COMPOUND,
                ratePercent = BigDecimal("2"),
                compoundEveryMonths = 6,
                partialMonthRule = PartialMonthRule.EXACT_DAYS_30,
            ),
        )
        assertEquals(BigDecimal("12720"), result.automaticInterest)
        assertEquals(BigDecimal("62720"), result.totalPayable)
        assertEquals(2, result.periods.size)
    }

    @Test
    fun manualDiscountDoesNotOverwriteAutomaticInterest() {
        val result = calculator.calculate(
            principal = BigDecimal("50000"),
            startDate = start,
            calculationDate = start.plusDays(180),
            plan = InterestPlan(
                name = "simple",
                kind = InterestKind.SIMPLE_MONTHLY_PERCENT,
                ratePercent = BigDecimal("2"),
                partialMonthRule = PartialMonthRule.EXACT_DAYS_30,
            ),
            adjustments = listOf(ManualAdjustment(BigDecimal("-500"), "Old customer settlement")),
        )
        assertEquals(BigDecimal("6000"), result.automaticInterest)
        assertEquals(BigDecimal("55500"), result.totalPayable)
    }

    @Test(expected = IllegalArgumentException::class)
    fun calculationBeforeStartIsRejected() {
        calculator.calculate(
            principal = BigDecimal("1000"),
            startDate = start,
            calculationDate = start.minusDays(1),
            plan = InterestPlan(name = "none", kind = InterestKind.NONE),
        )
    }
}
