package com.girvikhata.app.data

import org.junit.Assert.assertEquals
import org.junit.Test

class UpdateCustomerProfileMutationTest {
    @Test
    fun `customer edit updates linked girvi display names only`() {
        val c1 = CustomerRecord(id = "c1", name = "Old Name", mobile = "9999999999", address = "Old")
        val c2 = CustomerRecord(id = "c2", name = "Other", mobile = "8888888888", address = "")
        val g1 = girvi("g1", "GK-1", "c1", "Old Name")
        val g2 = girvi("g2", "GK-2", "c2", "Other")
        val before = AppSnapshot(customers = listOf(c1, c2), girvis = listOf(g1, g2))

        val after = UpdateCustomerProfileMutation(
            c1.copy(name = "  New   Name  ", mobile = "7777777777", address = "New Address"),
        ).apply(before)

        assertEquals("New Name", after.customers.first { it.id == "c1" }.name)
        assertEquals("7777777777", after.customers.first { it.id == "c1" }.mobile)
        assertEquals("New Address", after.customers.first { it.id == "c1" }.address)
        assertEquals("New Name", after.girvis.first { it.id == "g1" }.customerName)
        assertEquals("Other", after.girvis.first { it.id == "g2" }.customerName)
    }

    private fun girvi(id: String, number: String, customerId: String, customerName: String) = GirviRecord(
        id = id,
        girviNumber = number,
        customerId = customerId,
        customerName = customerName,
        principalPaise = 10000L,
        monthlyRateBasisPoints = 100,
        createdAt = 1L,
    )
}
