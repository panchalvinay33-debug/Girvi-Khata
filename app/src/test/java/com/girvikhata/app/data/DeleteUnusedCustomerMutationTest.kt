package com.girvikhata.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test

class DeleteUnusedCustomerMutationTest {
    @Test
    fun `unused customer can be deleted`() {
        val customer = CustomerRecord(id = "c1", name = "Test", mobile = "", address = "")
        val snapshot = AppSnapshot(customers = listOf(customer))

        val next = DeleteUnusedCustomerMutation("c1").apply(snapshot)

        assertEquals(emptyList<CustomerRecord>(), next.customers)
    }

    @Test
    fun `customer with girvi history cannot be deleted`() {
        val customer = CustomerRecord(id = "c1", name = "Test", mobile = "", address = "")
        val girvi = GirviRecord(
            id = "g1",
            girviNumber = "GK-1",
            customerId = "c1",
            customerName = "Test",
            categoryName = "Gold",
            itemName = "Ring",
            weightGrams = "10",
            principalPaise = 10000,
            monthlyRateBasisPoints = 100,
            createdAt = 1L,
        )
        val snapshot = AppSnapshot(customers = listOf(customer), girvis = listOf(girvi))

        try {
            DeleteUnusedCustomerMutation("c1").apply(snapshot)
            fail("Expected customer delete to be blocked")
        } catch (_: IllegalArgumentException) {
            // Expected: customer financial history is preserved.
        }
    }
}
