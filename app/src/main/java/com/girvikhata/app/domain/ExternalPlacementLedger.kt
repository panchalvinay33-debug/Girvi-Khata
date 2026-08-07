package com.girvikhata.app.domain

import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Calendar
import java.util.UUID

enum class ExternalInterestRule { EXACT_DAYS, STARTED_MONTH_FULL, COMPLETED_MONTHS_PLUS_DAYS }

data class ExternalFundingAdvance(
    val id: String = UUID.randomUUID().toString(),
    val amountPaise: Long,
    val monthlyRateBasisPoints: Int,
    val createdAt: Long,
    val interestRule: ExternalInterestRule = ExternalInterestRule.EXACT_DAYS,
    val note: String = "",
)

data class ExternalFundingPayment(
    val id: String = UUID.randomUUID().toString(),
    val amountPaise: Long,
    val createdAt: Long,
    val note: String = "",
    val isReversal: Boolean = false,
    val reversedPaymentId: String? = null,
)

data class ExternalFundingProjection(
    val totalAdvancedPaise: Long,
    val principalOutstandingPaise: Long,
    val grossInterestPaise: Long,
    val interestOutstandingPaise: Long,
    val totalPaidPaise: Long,
    val totalDuePaise: Long,
)

object ExternalPlacementLedger {
    fun project(
        advances: List<ExternalFundingAdvance>,
        payments: List<ExternalFundingPayment>,
        at: Long,
    ): ExternalFundingProjection {
        require(at > 0L) { "Projection date invalid" }
        val eligibleAdvances = advances.filter { it.createdAt <= at }.sortedBy { it.createdAt }
        require(eligibleAdvances.all { it.amountPaise > 0L && it.monthlyRateBasisPoints >= 0 }) { "External advance invalid" }

        val reversedIds = payments.filter { it.isReversal && it.createdAt <= at }.mapNotNull { it.reversedPaymentId }.toSet()
        val effectivePayments = payments.filter { payment ->
            payment.createdAt <= at && !payment.isReversal && payment.id !in reversedIds
        }.sortedBy { it.createdAt }
        require(effectivePayments.all { it.amountPaise > 0L }) { "External payment invalid" }

        val totalAdvanced = eligibleAdvances.sumOf { it.amountPaise }
        val grossInterest = eligibleAdvances.sumOf { advance -> interestFor(advance, at) }
        var remainingPayment = effectivePayments.sumOf { it.amountPaise }
        val interestPaid = minOf(remainingPayment, grossInterest)
        remainingPayment -= interestPaid
        val principalPaid = minOf(remainingPayment, totalAdvanced)
        val interestOutstanding = grossInterest - interestPaid
        val principalOutstanding = totalAdvanced - principalPaid
        return ExternalFundingProjection(
            totalAdvancedPaise = totalAdvanced,
            principalOutstandingPaise = principalOutstanding,
            grossInterestPaise = grossInterest,
            interestOutstandingPaise = interestOutstanding,
            totalPaidPaise = interestPaid + principalPaid,
            totalDuePaise = principalOutstanding + interestOutstanding,
        )
    }

    fun interestFor(advance: ExternalFundingAdvance, at: Long): Long {
        if (at <= advance.createdAt || advance.monthlyRateBasisPoints == 0) return 0L
        val monthUnits = when (advance.interestRule) {
            ExternalInterestRule.EXACT_DAYS -> BigDecimal(daysBetween(advance.createdAt, at)).divide(BigDecimal(30), 12, RoundingMode.HALF_UP)
            ExternalInterestRule.STARTED_MONTH_FULL -> BigDecimal(startedMonths(advance.createdAt, at))
            ExternalInterestRule.COMPLETED_MONTHS_PLUS_DAYS -> {
                val completed = completedMonths(advance.createdAt, at)
                val anchor = addMonths(advance.createdAt, completed)
                BigDecimal(completed).add(BigDecimal(daysBetween(anchor, at)).divide(BigDecimal(30), 12, RoundingMode.HALF_UP))
            }
        }
        return BigDecimal(advance.amountPaise)
            .multiply(BigDecimal(advance.monthlyRateBasisPoints))
            .divide(BigDecimal(10_000), 12, RoundingMode.HALF_UP)
            .multiply(monthUnits)
            .setScale(0, RoundingMode.HALF_UP)
            .longValueExact()
    }

    private fun daysBetween(from: Long, to: Long): Long {
        val fromDay = dayNumber(from)
        val toDay = dayNumber(to)
        return (toDay - fromDay).coerceAtLeast(0)
    }

    private fun dayNumber(value: Long): Long = value / DAY_MILLIS

    private fun startedMonths(from: Long, to: Long): Int {
        if (to <= from) return 0
        val completed = completedMonths(from, to)
        return if (addMonths(from, completed) == to) completed else completed + 1
    }

    private fun completedMonths(from: Long, to: Long): Int {
        if (to <= from) return 0
        val start = Calendar.getInstance().apply { timeInMillis = from }
        val end = Calendar.getInstance().apply { timeInMillis = to }
        var months = (end.get(Calendar.YEAR) - start.get(Calendar.YEAR)) * 12 + end.get(Calendar.MONTH) - start.get(Calendar.MONTH)
        if (months <= 0) return 0
        while (months > 0 && addMonths(from, months) > to) months--
        return months
    }

    private fun addMonths(value: Long, months: Int): Long = Calendar.getInstance().apply {
        timeInMillis = value
        add(Calendar.MONTH, months)
    }.timeInMillis

    private const val DAY_MILLIS = 24L * 60L * 60L * 1000L
}
