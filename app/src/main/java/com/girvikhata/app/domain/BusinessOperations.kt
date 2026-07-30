package com.girvikhata.app.domain

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class DraftItem(
    val categoryName: String,
    val itemName: String,
    val quantity: Int = 1,
    val grossWeightGrams: BigDecimal? = null,
    val deductionWeightGrams: BigDecimal = BigDecimal.ZERO,
    val description: String = "",
) {
    val netWeightGrams: BigDecimal?
        get() = grossWeightGrams?.subtract(deductionWeightGrams)

    fun validate(): List<String> = buildList {
        if (categoryName.isBlank()) add("Category required")
        if (itemName.isBlank()) add("Item name required")
        if (quantity <= 0) add("Quantity must be positive")
        if (grossWeightGrams != null && grossWeightGrams < BigDecimal.ZERO) add("Gross weight cannot be negative")
        if (deductionWeightGrams < BigDecimal.ZERO) add("Deduction cannot be negative")
        if (netWeightGrams != null && netWeightGrams!! < BigDecimal.ZERO) add("Deduction cannot exceed gross weight")
    }
}

data class CustomerCandidate(
    val id: String,
    val name: String,
    val mobile: String,
    val address: String,
)

object CustomerMatcher {
    fun findBestMatch(
        customers: List<CustomerCandidate>,
        name: String,
        mobile: String,
    ): CustomerCandidate? {
        val normalizedMobile = mobile.filter(Char::isDigit)
        val normalizedName = normalizeName(name)
        return customers.firstOrNull {
            normalizedMobile.isNotBlank() && it.mobile.filter(Char::isDigit) == normalizedMobile
        } ?: customers.firstOrNull {
            normalizedName.isNotBlank() && normalizeName(it.name) == normalizedName
        }
    }

    fun search(customers: List<CustomerCandidate>, query: String): List<CustomerCandidate> {
        val clean = query.trim()
        if (clean.isBlank()) return customers
        return customers.filter {
            it.name.contains(clean, ignoreCase = true) ||
                it.mobile.contains(clean) ||
                it.address.contains(clean, ignoreCase = true)
        }
    }

    private fun normalizeName(value: String): String = value
        .trim()
        .lowercase(Locale.ROOT)
        .replace(Regex("\\s+"), " ")
}

object GirviSequence {
    fun nextNumber(existingNumbers: Collection<String>, now: Date = Date()): String {
        val date = SimpleDateFormat("yyyyMMdd", Locale.US).format(now)
        val prefix = "GK-$date-"
        val next = existingNumbers.asSequence()
            .filter { it.startsWith(prefix) }
            .mapNotNull { it.removePrefix(prefix).toIntOrNull() }
            .maxOrNull()
            ?.plus(1)
            ?: 1
        return prefix + next.toString().padStart(4, '0')
    }
}

data class MonthlyInterestRow(
    val monthNumber: Int,
    val openingPrincipalPaise: Long,
    val interestPaise: Long,
    val closingPayablePaise: Long,
)

data class SimpleInterestBreakdown(
    val principalPaise: Long,
    val monthlyRateBasisPoints: Int,
    val months: Int,
    val totalInterestPaise: Long,
    val totalPayablePaise: Long,
    val rows: List<MonthlyInterestRow>,
)

object SimpleInterestPreview {
    fun calculate(
        principalPaise: Long,
        monthlyRateBasisPoints: Int,
        months: Int,
    ): SimpleInterestBreakdown {
        require(principalPaise > 0) { "Principal must be positive" }
        require(monthlyRateBasisPoints >= 0) { "Rate cannot be negative" }
        require(months >= 0) { "Months cannot be negative" }

        val monthlyInterest = BigDecimal.valueOf(principalPaise)
            .multiply(BigDecimal.valueOf(monthlyRateBasisPoints.toLong()))
            .divide(BigDecimal.valueOf(10_000L), 0, RoundingMode.HALF_UP)
            .longValueExact()
        val rows = (1..months).map { month ->
            MonthlyInterestRow(
                monthNumber = month,
                openingPrincipalPaise = principalPaise,
                interestPaise = monthlyInterest,
                closingPayablePaise = principalPaise + monthlyInterest * month,
            )
        }
        val totalInterest = monthlyInterest * months
        return SimpleInterestBreakdown(
            principalPaise = principalPaise,
            monthlyRateBasisPoints = monthlyRateBasisPoints,
            months = months,
            totalInterestPaise = totalInterest,
            totalPayablePaise = principalPaise + totalInterest,
            rows = rows,
        )
    }
}

object CategoryRules {
    fun canDeactivate(categoryName: String, activeGirviCategoryNames: Collection<String>): Boolean =
        activeGirviCategoryNames.none { it.equals(categoryName, ignoreCase = true) }
}
