package com.girvikhata.app.domain

data class InterestComparison(
    val exactDays: InterestQuote,
    val fullMonthStarted: InterestQuote,
    val completedMonthsPlusDays: InterestQuote,
)

object InterestComparisonEngine {
    fun compare(
        principalPaise: Long,
        startAtMillis: Long,
        endAtMillis: Long,
        baseTerms: InterestTerms,
    ): InterestComparison {
        return InterestComparison(
            exactDays = InterestEngine.quote(
                principalPaise,
                startAtMillis,
                endAtMillis,
                baseTerms.copy(periodRule = InterestPeriodRule.EXACT_DAYS),
            ),
            fullMonthStarted = InterestEngine.quote(
                principalPaise,
                startAtMillis,
                endAtMillis,
                baseTerms.copy(periodRule = InterestPeriodRule.FULL_MONTH_STARTED),
            ),
            completedMonthsPlusDays = InterestEngine.quote(
                principalPaise,
                startAtMillis,
                endAtMillis,
                baseTerms.copy(periodRule = InterestPeriodRule.COMPLETED_MONTHS_PLUS_DAYS),
            ),
        )
    }
}
