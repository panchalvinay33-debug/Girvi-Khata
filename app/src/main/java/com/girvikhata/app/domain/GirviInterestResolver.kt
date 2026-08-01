package com.girvikhata.app.domain

object GirviInterestResolver {
    /**
     * New records prefer versioned metadata. Old records without metadata retain their historic
     * monthly percentage field so upgrading the app never silently changes their calculation mode.
     */
    fun resolve(
        firstItemDescription: String?,
        legacyMonthlyRateBasisPoints: Int,
    ): InterestTerms {
        GirviInterestMetadata.read(firstItemDescription)?.let { return it }
        return InterestTerms(
            mode = InterestMode.PERCENT_PER_MONTH,
            monthlyRateBasisPoints = legacyMonthlyRateBasisPoints.coerceAtLeast(0),
            periodRule = InterestPeriodRule.COMPLETED_MONTHS_PLUS_DAYS,
            compoundEveryMonths = null,
        )
    }
}
