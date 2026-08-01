package com.girvikhata.app.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class GirviInterestResolverTest {
    @Test
    fun legacyRecordWithoutMetadata_keepsMonthlyPercentage() {
        val terms = GirviInterestResolver.resolve(
            firstItemDescription = "पुराना सामान / legacy item",
            legacyMonthlyRateBasisPoints = 225,
        )
        assertEquals(InterestMode.PERCENT_PER_MONTH, terms.mode)
        assertEquals(225, terms.monthlyRateBasisPoints)
        assertEquals(InterestPeriodRule.COMPLETED_MONTHS_PLUS_DAYS, terms.periodRule)
        assertEquals(null, terms.compoundEveryMonths)
    }

    @Test
    fun newMetadata_winsOverLegacyField() {
        val metadataTerms = InterestTerms(
            mode = InterestMode.FLAT_PER_MONTH,
            flatMonthlyChargePaise = 25_000,
            periodRule = InterestPeriodRule.FULL_MONTH_STARTED,
        )
        val description = GirviInterestMetadata.attach("item", metadataTerms)
        val resolved = GirviInterestResolver.resolve(description, legacyMonthlyRateBasisPoints = 999)
        assertEquals(metadataTerms, resolved)
    }

    @Test
    fun negativeLegacyRate_isClampedToZeroRatherThanCreatingInvalidTerms() {
        val terms = GirviInterestResolver.resolve(null, -20)
        assertEquals(0, terms.monthlyRateBasisPoints)
    }
}
