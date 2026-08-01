package com.girvikhata.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GirviInterestMetadataTest {
    @Test
    fun attachReadAndStrip_preserveUserDescription() {
        val terms = InterestTerms(
            monthlyRateBasisPoints = 250,
            periodRule = InterestPeriodRule.EXACT_DAYS,
            compoundEveryMonths = 3,
        )
        val attached = GirviInterestMetadata.attach("सोने की चैन / Gold chain", terms)
        assertEquals(terms, GirviInterestMetadata.read(attached))
        assertEquals("सोने की चैन / Gold chain", GirviInterestMetadata.strip(attached))
    }

    @Test
    fun malformedMetadata_doesNotInventTerms() {
        assertNull(GirviInterestMetadata.read("note [[GIRVI_INTEREST:bad]]"))
        assertEquals("plain note", GirviInterestMetadata.strip("plain note"))
    }
}
