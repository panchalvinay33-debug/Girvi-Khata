package com.girvikhata.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CategorySettingsMutationsTest {
    private val gold = CategoryRecord(id = "gold", name = "Gold")
    private val silver = CategoryRecord(id = "silver", name = "Silver")
    private val customer = CustomerRecord(id = "c1", name = "Ravi", createdAt = 1L)

    @Test
    fun `rename updates category plus legacy and item references`() {
        val girvi = GirviRecord(
            id = "g1",
            girviNumber = "G-001",
            customerId = customer.id,
            customerName = customer.name,
            categoryName = "Gold",
            itemName = "Ring",
            weightGrams = "5",
            principalPaise = 10_000L,
            monthlyRateBasisPoints = 200,
            createdAt = 2L,
            items = listOf(GirviItemRecord(id = "i1", categoryName = "Gold", itemName = "Ring")),
        )
        val before = AppSnapshot(customers = listOf(customer), categories = listOf(gold, silver), girvis = listOf(girvi))

        val next = RenameCategoryMutation(gold.id, " Jewellery ").apply(before)

        assertEquals("Jewellery", next.categories.first().name)
        assertEquals("Jewellery", next.girvis.single().categoryName)
        assertEquals("Jewellery", next.girvis.single().items.single().categoryName)
        assertEquals("CATEGORY_RENAME", RenameCategoryMutation(gold.id, "Jewellery").auditLabel)
    }

    @Test
    fun `rename rejects case insensitive duplicate`() {
        val before = AppSnapshot(categories = listOf(gold, silver))

        val failure = runCatching { RenameCategoryMutation(gold.id, " silver ").apply(before) }.exceptionOrNull()

        assertEquals("Duplicate category name", failure?.message)
    }

    @Test
    fun `reorder moves exactly one adjacent category`() {
        val before = AppSnapshot(categories = listOf(gold, silver))

        val next = ReorderCategoryMutation(silver.id, -1).apply(before)

        assertEquals(listOf("silver", "gold"), next.categories.map { it.id })
        assertEquals("CATEGORY_REORDER", ReorderCategoryMutation(silver.id, -1).auditLabel)
    }

    @Test
    fun `reorder rejects boundary move`() {
        val before = AppSnapshot(categories = listOf(gold, silver))

        val failure = runCatching { ReorderCategoryMutation(gold.id, -1).apply(before) }.exceptionOrNull()

        assertTrue(failure is IllegalArgumentException)
        assertEquals("Category already at boundary", failure?.message)
    }
}
