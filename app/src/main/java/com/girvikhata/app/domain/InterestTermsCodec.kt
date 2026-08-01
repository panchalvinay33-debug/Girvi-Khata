package com.girvikhata.app.domain

object InterestTermsCodec {
    private const val PREFIX = "GKINT1"

    fun encode(terms: InterestTerms): String = listOf(
        PREFIX,
        terms.mode.name,
        terms.monthlyRateBasisPoints.toString(),
        terms.flatMonthlyChargePaise.toString(),
        terms.periodRule.name,
        (terms.compoundEveryMonths ?: 0).toString(),
    ).joinToString("|")

    fun decode(value: String?): InterestTerms? {
        if (value.isNullOrBlank()) return null
        val parts = value.split('|')
        if (parts.size != 6 || parts[0] != PREFIX) return null
        return runCatching {
            InterestTerms(
                mode = InterestMode.valueOf(parts[1]),
                monthlyRateBasisPoints = parts[2].toInt(),
                flatMonthlyChargePaise = parts[3].toLong(),
                periodRule = InterestPeriodRule.valueOf(parts[4]),
                compoundEveryMonths = parts[5].toInt().takeIf { it > 0 },
            )
        }.getOrNull()
    }
}
