package com.girvikhata.app.domain

import com.girvikhata.app.data.AppSnapshot
import com.girvikhata.app.data.CategoryRecord
import com.girvikhata.app.data.CustomerRecord
import com.girvikhata.app.data.GirviItemRecord
import com.girvikhata.app.data.GirviRecord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class CategorySettingsOperationsTest {
    private val customer = CustomerRecord(id = "c1", name = "A")
    private val gold = CategoryRecord(id = "gold", name = "Gold")
    private val silver = CategoryRecord(id = "silver", name = "Silver")

    private fun snapshot() = AppSnapshot(
        customers = listOf(customer),
        categories = listOf(gold, silver),
        girvis = listOf(
            GirviRecord(
                id = "g1",
                girviNumber = "G1",
                customerId = customer.id,
                customerName = customer.name,
                categoryName = "Gold",
                itemName = "Ring",
                weightGrams = "10",
                principalPaise = 10000,
                monthlyRateBasisPoints = 200,
                createdAt = 1,
                items = listOf(GirviItemRecord(id = "i1", categoryName = "Gold", itemName = "Ring")),
            ),
        ),
    )

    @Test fun renamePropagatesToLegacyAndItems() {
        val result = CategorySettingsOperations.rename(snapshot(), "gold", " Gold Jewellery ")
        assertEquals("Gold Jewellery", result.categories.first().name)
        assertEquals("Gold Jewellery", result.girvis.first().categoryName)
        assertEquals("Gold Jewellery", result.girvis.first().items.first().categoryName)
    }

    @Test(expected = IllegalArgumentException::class)
    fun duplicateNameRejectedIgnoringCase() {
        CategorySettingsOperations.rename(snapshot(), "gold", "silver")
    }

    @Test fun moveReordersAndBoundaryIsNoOp() {
        val original = snapshot()
        val moved = CategorySettingsOperations.move(original, "silver", -1)
        assertEquals(listOf("Silver", "Gold"), moved.categories.map { it.name })
        assertSame(original, CategorySettingsOperations.move(original, "gold", -1))
    }
}
