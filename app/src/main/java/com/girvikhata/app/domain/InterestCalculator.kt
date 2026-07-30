package com.girvikhata.app.domain

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.ceil

data class CalculationPeriod(
    val label: String,
    val openingPrincipal: Money,
    val interest: Money,
    val closingPrincipal: Money,
    val days: Long,
)

data class ManualAdjustment(
    val amount: Money,
    val reason: String,
)

data class InterestResult(
    val calculationDate: LocalDate,
    val principal: Money,
    val automaticInterest: Money,
    val adjustments: List<ManualAdjustment>,
    val totalPayable: Money,
    val periods: List<CalculationPeriod>,
)

class InterestCalculator {
    fun calculate(
        principal: Money,
        startDate: LocalDate,
        calculationDate: LocalDate,
        plan: InterestPlan,
        adjustments: List<ManualAdjustment> = emptyList(),
    ): InterestResult {
        require(principal >= BigDecimal.ZERO)
        require(!calculationDate.isBefore(startDate))
        adjustments.forEach { require(it.reason.isNotBlank()) }

        val raw = when (plan.kind) {
            InterestKind.SIMPLE_MONTHLY_PERCENT -> simpleMonthly(principal, startDate, calculationDate, plan)
            InterestKind.SIMPLE_DAILY_PERCENT -> simpleDaily(principal, startDate, calculationDate, plan)
            InterestKind.SIMPLE_YEARLY_PERCENT -> simpleYearly(principal, startDate, calculationDate, plan)
            InterestKind.FIXED_MONTHLY -> fixedMonthly(principal, startDate, calculationDate, plan)
            InterestKind.FIXED_PERIOD -> fixedPeriod(principal, startDate, calculationDate, plan)
            InterestKind.COMPOUND -> compound(principal, startDate, calculationDate, plan)
            InterestKind.MANUAL, InterestKind.NONE -> RawCalculation(BigDecimal.ZERO, emptyList())
        }

        val roundedInterest = round(raw.interest, plan.roundingRule)
        val adjustmentTotal = adjustments.fold(BigDecimal.ZERO) { sum, item -> sum.add(item.amount) }
        val total = principal.add(roundedInterest).add(adjustmentTotal).max(BigDecimal.ZERO)

        return InterestResult(
            calculationDate = calculationDate,
            principal = principal,
            automaticInterest = roundedInterest,
            adjustments = adjustments,
            totalPayable = total,
            periods = raw.periods,
        )
    }

    private fun simpleMonthly(principal: Money, start: LocalDate, end: LocalDate, plan: InterestPlan): RawCalculation {
        val monthUnits = billableMonthUnits(start, end, plan)
        val interest = principal
            .multiply(plan.ratePercent)
            .divide(BigDecimal(100), 12, RoundingMode.HALF_UP)
            .multiply(monthUnits)
        return RawCalculation(interest, listOf(CalculationPeriod("Simple monthly", principal, interest, principal, ChronoUnit.DAYS.between(start, end))))
    }

    private fun simpleDaily(principal: Money, start: LocalDate, end: LocalDate, plan: InterestPlan): RawCalculation {
        val days = chargeableDays(start, end, plan.graceDays)
        val interest = principal.multiply(plan.ratePercent).divide(BigDecimal(100), 12, RoundingMode.HALF_UP).multiply(BigDecimal(days))
        return RawCalculation(interest, listOf(CalculationPeriod("$days days", principal, interest, principal, days)))
    }

    private fun simpleYearly(principal: Money, start: LocalDate, end: LocalDate, plan: InterestPlan): RawCalculation {
        val days = chargeableDays(start, end, plan.graceDays)
        val years = BigDecimal(days).divide(BigDecimal("365"), 12, RoundingMode.HALF_UP)
        val interest = principal.multiply(plan.ratePercent).divide(BigDecimal(100), 12, RoundingMode.HALF_UP).multiply(years)
        return RawCalculation(interest, listOf(CalculationPeriod("Yearly prorated", principal, interest, principal, days)))
    }

    private fun fixedMonthly(principal: Money, start: LocalDate, end: LocalDate, plan: InterestPlan): RawCalculation {
        val units = billableMonthUnits(start, end, plan)
        val interest = plan.fixedAmount.multiply(units)
        return RawCalculation(interest, listOf(CalculationPeriod("Fixed monthly", principal, interest, principal, ChronoUnit.DAYS.between(start, end))))
    }

    private fun fixedPeriod(principal: Money, start: LocalDate, end: LocalDate, plan: InterestPlan): RawCalculation {
        val days = chargeableDays(start, end, plan.graceDays)
        val periods = if (days == 0L) BigDecimal.ZERO else BigDecimal(ceil(days / 30.0).toLong())
        val interest = plan.fixedAmount.multiply(periods)
        return RawCalculation(interest, listOf(CalculationPeriod("Fixed period", principal, interest, principal, days)))
    }

    private fun compound(principal: Money, start: LocalDate, end: LocalDate, plan: InterestPlan): RawCalculation {
        val totalMonths = billableMonthUnits(start, end, plan).setScale(0, RoundingMode.CEILING).toInt()
        if (totalMonths <= 0) return RawCalculation(BigDecimal.ZERO, emptyList())

        var opening = principal
        var consumed = 0
        var totalInterest = BigDecimal.ZERO
        val periods = mutableListOf<CalculationPeriod>()
        var index = 1

        while (consumed < totalMonths) {
            val monthsInPeriod = minOf(plan.compoundEveryMonths, totalMonths - consumed)
            val interest = opening
                .multiply(plan.ratePercent)
                .divide(BigDecimal(100), 12, RoundingMode.HALF_UP)
                .multiply(BigDecimal(monthsInPeriod))
            val closing = opening.add(interest)
            periods += CalculationPeriod(
                label = "Compound period $index ($monthsInPeriod month)",
                openingPrincipal = opening,
                interest = interest,
                closingPrincipal = closing,
                days = monthsInPeriod * 30L,
            )
            totalInterest = totalInterest.add(interest)
            opening = closing
            consumed += monthsInPeriod
            index++
        }
        return RawCalculation(totalInterest, periods)
    }

    private fun billableMonthUnits(start: LocalDate, end: LocalDate, plan: InterestPlan): BigDecimal {
        val totalDays = chargeableDays(start, end, plan.graceDays)
        if (totalDays <= 0) return BigDecimal.ZERO
        val fullMonths = totalDays / 30
        val extraDays = totalDays % 30
        return when (plan.partialMonthRule) {
            PartialMonthRule.EXACT_DAYS_30 -> BigDecimal(totalDays).divide(BigDecimal(30), 12, RoundingMode.HALF_UP)
            PartialMonthRule.EXTRA_DAY_FULL_MONTH -> BigDecimal(fullMonths + if (extraDays > 0) 1 else 0)
            PartialMonthRule.HALF_MONTH_SLAB -> BigDecimal(fullMonths).add(
                when {
                    extraDays == 0L -> BigDecimal.ZERO
                    extraDays <= 15L -> BigDecimal("0.5")
                    else -> BigDecimal.ONE
                },
            )
            PartialMonthRule.GRACE_THEN_FULL_MONTH -> BigDecimal(fullMonths + if (extraDays > 0) 1 else 0)
            PartialMonthRule.MANUAL -> BigDecimal.ZERO
        }
    }

    private fun chargeableDays(start: LocalDate, end: LocalDate, graceDays: Int): Long =
        (ChronoUnit.DAYS.between(start, end) - graceDays).coerceAtLeast(0)

    private fun round(value: BigDecimal, rule: RoundingRule): BigDecimal = when (rule) {
        RoundingRule.NONE -> value.setScale(2, RoundingMode.HALF_UP)
        RoundingRule.NEAREST_RUPEE -> value.setScale(0, RoundingMode.HALF_UP)
        RoundingRule.ROUND_UP_RUPEE -> value.setScale(0, RoundingMode.CEILING)
        RoundingRule.ROUND_DOWN_RUPEE -> value.setScale(0, RoundingMode.FLOOR)
        RoundingRule.NEAREST_FIVE -> nearestMultiple(value, BigDecimal(5))
        RoundingRule.NEAREST_TEN -> nearestMultiple(value, BigDecimal(10))
    }

    private fun nearestMultiple(value: BigDecimal, multiple: BigDecimal): BigDecimal =
        value.divide(multiple, 0, RoundingMode.HALF_UP).multiply(multiple)

    private data class RawCalculation(val interest: Money, val periods: List<CalculationPeriod>)
}
