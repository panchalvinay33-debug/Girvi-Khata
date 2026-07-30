package com.girvikhata.app.domain

import com.girvikhata.app.data.AppSnapshot
import com.girvikhata.app.data.CategoryRecord
import com.girvikhata.app.data.CustomerRecord
import com.girvikhata.app.data.GirviRecord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class CustomerAccountOperationsTest {
    private val customer = CustomerRecord(id = "c1", name = "Old Name", mobile = "9999999999", address = "Pitol")
    private val girvi = GirviRecord(
        id = "g1",
        girviNumber = "GK-20260729-0001",
        customerId = "c1",
        customerName = "Old Name",
        categoryName = "Gold",
        itemName = "Ring",
        weightGrams = "10",
        principalPaise = 100_000,
        monthlyRateBasisPoints = 200,
    )

    private fun snapshot(girvis: List<GirviRecord> = listOf(girvi), customers: List<CustomerRecord> = listOf(customer)) =
        AppSnapshot(schemaVersion = 3, customers = customers, categories = listOf(CategoryRecord(name = "Gold")), girvis = girvis)

    @Test
    fun updatePropagatesDisplayNameToGirviHistory() {
        val updated = CustomerAccountOperations.updateCustomer(snapshot(), "c1", "  Vinay   Panchal ", "98765 43210", "  New village ")
        assertEquals("Vinay Panchal", updated.customers.single().name)
        assertEquals("9876543210", updated.customers.single().mobile)
        assertEquals("Vinay Panchal", updated.girvis.single().customerName)
    }

    @Test
    fun duplicateMobileIsRejected() {
        val other = CustomerRecord(id = "c2", name = "Other", mobile = "8888888888")
        assertThrows(IllegalArgumentException::class.java) {
            CustomerAccountOperations.updateCustomer(snapshot(customers = listOf(customer, other)), "c1", "Name", "8888888888", "")
        }
    }

    @Test
    fun customerWithHistoryCannotBeDeleted() {
        assertFalse(CustomerAccountOperations.canDelete(snapshot(), "c1"))
        assertThrows(IllegalArgumentException::class.java) {
            CustomerAccountOperations.deleteUnusedCustomer(snapshot(), "c1")
        }
    }

    @Test
    fun unusedCustomerCanBeDeleted() {
        val empty = snapshot(girvis = emptyList())
        assertTrue(CustomerAccountOperations.canDelete(empty, "c1"))
        assertTrue(CustomerAccountOperations.deleteUnusedCustomer(empty, "c1").customers.isEmpty())
    }

    @Test
    fun profileIncludesOutstandingAndCounts() {
        val profile = CustomerAccountOperations.profile(snapshot(), "c1", settlementMonths = 1)
        assertEquals(1, profile.activeCount)
        assertEquals(0, profile.releasedCount)
        assertTrue(profile.totalOutstandingPaise > girvi.principalPaise)
    }
}
