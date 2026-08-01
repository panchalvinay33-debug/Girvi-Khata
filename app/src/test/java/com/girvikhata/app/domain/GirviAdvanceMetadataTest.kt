package com.girvikhata.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GirviAdvanceMetadataTest {
    private val terms = InterestTerms(
        mode = InterestMode.PERCENT_PER_MONTH,
        monthlyRateBasisPoints = 200,
        periodRule = InterestPeriodRule.COMPLETED_MONTHS_PLUS_DAYS,
    )

    @Test
    fun `advance metadata round trips without destroying user release note`() {
        val advances = listOf(
            GirviAdvanceMetadata.Advance("a1", 50_000L, 1000L, terms, "festival cash"),
            GirviAdvanceMetadata.Advance("a2", 25_000L, 2000L, terms.copy(monthlyRateBasisPoints = 300), "second"),
        )

        val encoded = GirviAdvanceMetadata.attach("customer note", advances)

        assertEquals("customer note", GirviAdvanceMetadata.strip(encoded))
        assertEquals(advances, GirviAdvanceMetadata.read(encoded))
    }

    @Test
    fun `append preserves existing advances and rejects duplicate identity`() {
        val first = GirviAdvanceMetadata.Advance("a1", 10_000L, 1000L, terms)
        val second = GirviAdvanceMetadata.Advance("a2", 20_000L, 2000L, terms)
        val once = GirviAdvanceMetadata.append("note", first)
        val twice = GirviAdvanceMetadata.append(once, second)

        assertEquals(listOf(first, second), GirviAdvanceMetadata.read(twice))
        assertTrue(runCatching { GirviAdvanceMetadata.append(twice, second) }.isFailure)
    }

    @Test
    fun `unknown version fails closed as no advances`() {
        assertEquals(emptyList<GirviAdvanceMetadata.Advance>(), GirviAdvanceMetadata.read("[[GIRVI_ADVANCES:OTHER|abc]]"))
    }
}
