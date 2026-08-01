package com.girvikhata.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PracticalEntrySaveRegressionTest {
    @Test
    fun practicalEntry_newCustomerAndGirvi_persistsAsOneTypedMutation() {
        val before = AppSnapshot()
        val customer = CustomerRecord(name = "Test Customer", mobile = "9999999999")
        val girvi = GirviRecord(
            girviNumber = "GK-0001",
            customerId = customer.id,
            customerName = customer.name,
            categoryName = "Gold",
            itemName = "Ring",
            weightGrams = "10",
            principalPaise = 10_000_00,
            monthlyRateBasisPoints = 200,
            items = listOf(GirviItemRecord(categoryName = "Gold", itemName = "Ring", grossWeightGrams = "10")),
        )
        val next = before.copy(customers = listOf(customer), girvis = listOf(girvi))
        var authoritative = before
        var executed: VerifiedBusinessMutation? = null
        val gateway = ClassicVerifiedWriteGateway(
            reloadAuthoritative = { authoritative },
            executeMutation = { mutation ->
                executed = mutation
                authoritative = mutation.apply(authoritative)
                authoritative
            },
        )

        val saved = gateway.persist(before, next)

        assertEquals(1, saved.customers.size)
        assertEquals(1, saved.girvis.size)
        assertEquals(girvi.id, saved.girvis.single().id)
        assertEquals("CUSTOMER_UPSERT_GIRVI_CREATE", executed?.auditLabel)
    }

    @Test
    fun practicalEntry_existingMatchedCustomerEditAndGirvi_areAtomic() {
        val oldCustomer = CustomerRecord(name = "Old Name", mobile = "9999999999", address = "Old")
        val before = AppSnapshot(customers = listOf(oldCustomer))
        val editedCustomer = oldCustomer.copy(name = "Contact Name", address = "New Address")
        val girvi = GirviRecord(
            girviNumber = "GK-0002",
            customerId = editedCustomer.id,
            customerName = editedCustomer.name,
            categoryName = "Silver",
            itemName = "Payal",
            weightGrams = "25",
            principalPaise = 5_000_00,
            monthlyRateBasisPoints = 250,
            items = listOf(GirviItemRecord(categoryName = "Silver", itemName = "Payal", grossWeightGrams = "25")),
        )
        val next = before.copy(customers = listOf(editedCustomer), girvis = listOf(girvi))
        var authoritative = before
        val gateway = ClassicVerifiedWriteGateway(
            reloadAuthoritative = { authoritative },
            executeMutation = { mutation ->
                authoritative = mutation.apply(authoritative)
                authoritative
            },
        )

        val saved = gateway.persist(before, next)

        assertEquals("Contact Name", saved.customers.single().name)
        assertEquals("New Address", saved.customers.single().address)
        assertTrue(saved.girvis.any { it.id == girvi.id })
    }
}
