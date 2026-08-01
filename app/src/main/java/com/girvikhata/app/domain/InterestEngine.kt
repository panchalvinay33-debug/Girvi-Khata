package com.girvikhata.app.domain

import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Calendar

enum class InterestMode {
    PERCENT_PER_MONTH,
    FLAT_PER_MONTH,
}

enum class InterestPeriodRule {
    /** Monthly charge / 30 × actual elapsed calendar days. */
    EXACT_DAYS,

    /** Any started partial month is charged as one full month. */
    FULL_MONTH_STARTED,

    /** Full calendar months + remaining days at monthly charge / 30. */
    COMPLETED_MONTHS_PLUS_DAYS,
}

data class InterestTerms(
    val mode: InterestMode = InterestMode.PERCENT_PER_MONTH,
    val monthlyRateBasisPoints: Int = 0,
    val flatMonthlyChargePaise: Long = 0,
    val periodRule: InterestPeriodRule = InterestPeriodRule.COMPLETED_MONTHS_PLUS_DAYS,
    /** Null means simple interest. Non-null means capitalize interest every N full calendar months. */
    val compoundEveryMonths: Int? = null,
) {
    init {
        require(monthlyRateBasisPoints >= 0) { "Monthly rate cannot be negative" }
        require(flatMonthlyChargePaise >= 0) { "Flat monthly charge cannot be negative" }
        require(compoundEveryMonths == null || compoundEveryMonths > 0) { "Compound interval must be positive" }
        require(mode != InterestMode.FLAT_PER_MONTH || compoundEveryMonths == null) {
            "Compound interest is only supported for percentage-per-month terms"
        }
    }
}

data class InterestQuote(
    val principalPaise: Long,
    val interestPaise: Long,
    val totalPayablePaise: Long,
    val originalPrincipalPaise: Long,
    val elapsedDays: Int,
    val completedMonths: Int,
    val remainingDays: Int,
    val chargedMonths: Int,
    val compoundPeriodsApplied: Int,
    val terms: InterestTerms,
)

object InterestEngine {
    private val THIRTY = BigDecimal(30)
    private val TEN_THOUSAND = BigDecimal(10_000)

    fun quote(
        principalPaise: Long,
        startAtMillis: Long,
        endAtMillis: Long,
        terms: InterestTerms,
    ): InterestQuote {
        require(principalPaise >= 0) { "Principal cannot be negative" }
        val start = day(startAtMillis)
        val end = day(endAtMillis)
        require(!end.before(start)) { "End date cannot be before start date" }

        val elapsedDays = daysBetween(start, end)
        val monthSplit = splitCalendarMonths(start, end)

        if (elapsedDays == 0 || principalPaise == 0L) {
            return InterestQuote(
                principalPaise = principalPaise,
                interestPaise = 0,
                totalPayablePaise = principalPaise,
                originalPrincipalPaise = principalPaise,
                elapsedDays = elapsedDays,
                completedMonths = monthSplit.first,
                remainingDays = monthSplit.second,
                chargedMonths = 0,
                compoundPeriodsApplied = 0,
                terms = terms,
            )
        }

        return if (terms.compoundEveryMonths != null) {
            quoteCompound(principalPaise, start, end, elapsedDays, monthSplit, terms)
        } else {
            val interest = simpleInterest(principalPaise, start, end, terms)
            InterestQuote(
                principalPaise = principalPaise,
                interestPaise = interest,
                totalPayablePaise = safeAdd(principalPaise, interest),
                originalPrincipalPaise = principalPaise,
                elapsedDays = elapsedDays,
                completedMonths = monthSplit.first,
                remainingDays = monthSplit.second,
                chargedMonths = chargedMonths(monthSplit.first, monthSplit.second, elapsedDays, terms.periodRule),
                compoundPeriodsApplied = 0,
                terms = terms,
            )
        }
    }

    private fun quoteCompound(
        originalPrincipal: Long,
        start: Calendar,
        end: Calendar,
        elapsedDays: Int,
        overallSplit: Pair<Int, Int>,
        terms: InterestTerms,
    ): InterestQuote {
        val interval = requireNotNull(terms.compoundEveryMonths)
        var runningPrincipal = originalPrincipal
        var cursor = start.clone() as Calendar
        var periods = 0

        while (true) {
            val next = cursor.clone() as Calendar
            next.add(Calendar.MONTH, interval)
            if (next.after(end)) break

            val periodInterest = percentForMonths(
                principalPaise = runningPrincipal,
                monthlyRateBasisPoints = terms.monthlyRateBasisPoints,
                months = interval,
            )
            runningPrincipal = safeAdd(runningPrincipal, periodInterest)
            cursor = next
            periods++
        }

        val tailTerms = terms.copy(compoundEveryMonths = null)
        val tailInterest = simpleInterest(runningPrincipal, cursor, end, tailTerms)
        val finalTotal = safeAdd(runningPrincipal, tailInterest)
        val totalInterest = finalTotal - originalPrincipal
        val tailSplit = splitCalendarMonths(cursor, end)

        return InterestQuote(
            principalPaise = originalPrincipal,
            interestPaise = totalInterest,
            totalPayablePaise = finalTotal,
            originalPrincipalPaise = originalPrincipal,
            elapsedDays = elapsedDays,
            completedMonths = overallSplit.first,
            remainingDays = overallSplit.second,
            chargedMonths = periods * interval + chargedMonths(
                tailSplit.first,
                tailSplit.second,
                daysBetween(cursor, end),
                terms.periodRule,
            ),
            compoundPeriodsApplied = periods,
            terms = terms,
        )
    }

    private fun simpleInterest(
        principalPaise: Long,
        start: Calendar,
        end: Calendar,
        terms: InterestTerms,
    ): Long {
        val elapsedDays = daysBetween(start, end)
        if (elapsedDays == 0) return 0
        val (fullMonths, remainingDays) = splitCalendarMonths(start, end)
        val monthlyCharge = monthlyChargePaise(principalPaise, terms)

        return when (terms.periodRule) {
            InterestPeriodRule.EXACT_DAYS -> forDays(monthlyCharge, elapsedDays)
            InterestPeriodRule.FULL_MONTH_STARTED -> {
                val months = fullMonths + if (remainingDays > 0) 1 else 0
                safeMultiply(monthlyCharge, months)
            }
            InterestPeriodRule.COMPLETED_MONTHS_PLUS_DAYS -> safeAdd(
                safeMultiply(monthlyCharge, fullMonths),
                forDays(monthlyCharge, remainingDays),
            )
        }
    }

    fun monthlyChargePaise(principalPaise: Long, terms: InterestTerms): Long = when (terms.mode) {
        InterestMode.PERCENT_PER_MONTH -> percentForMonths(principalPaise, terms.monthlyRateBasisPoints, 1)
        InterestMode.FLAT_PER_MONTH -> terms.flatMonthlyChargePaise
    }

    private fun percentForMonths(principalPaise: Long, monthlyRateBasisPoints: Int, months: Int): Long {
        if (principalPaise == 0L || monthlyRateBasisPoints == 0 || months == 0) return 0
        return BigDecimal.valueOf(principalPaise)
            .multiply(BigDecimal.valueOf(monthlyRateBasisPoints.toLong()))
            .multiply(BigDecimal.valueOf(months.toLong()))
            .divide(TEN_THOUSAND, 0, RoundingMode.HALF_UP)
            .longValueExact()
    }

    private fun forDays(monthlyChargePaise: Long, days: Int): Long {
        if (monthlyChargePaise == 0L || days == 0) return 0
        return BigDecimal.valueOf(monthlyChargePaise)
            .multiply(BigDecimal.valueOf(days.toLong()))
            .divide(THIRTY, 0, RoundingMode.HALF_UP)
            .longValueExact()
    }

    private fun chargedMonths(fullMonths: Int, remainingDays: Int, elapsedDays: Int, rule: InterestPeriodRule): Int = when (rule) {
        InterestPeriodRule.EXACT_DAYS -> if (elapsedDays == 0) 0 else fullMonths
        InterestPeriodRule.FULL_MONTH_STARTED -> fullMonths + if (remainingDays > 0) 1 else 0
        InterestPeriodRule.COMPLETED_MONTHS_PLUS_DAYS -> fullMonths
    }

    private fun splitCalendarMonths(start: Calendar, end: Calendar): Pair<Int, Int> {
        var cursor = start.clone() as Calendar
        var months = 0
        while (true) {
            val next = cursor.clone() as Calendar
            next.add(Calendar.MONTH, 1)
            if (next.after(end)) break
            cursor = next
            months++
        }
        return months to daysBetween(cursor, end)
    }

    private fun daysBetween(start: Calendar, end: Calendar): Int {
        if (!end.after(start)) return 0
        var cursor = start.clone() as Calendar
        var days = 0
        while (cursor.before(end)) {
            cursor.add(Calendar.DAY_OF_MONTH, 1)
            days++
        }
        return days
    }

    private fun day(millis: Long): Calendar = Calendar.getInstance().apply {
        timeInMillis = millis
        set(Calendar.HOUR_OF_DAY, 12)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    private fun safeAdd(a: Long, b: Long): Long = Math.addExact(a, b)

    private fun safeMultiply(value: Long, multiplier: Int): Long = Math.multiplyExact(value, multiplier.toLong())
}
