package com.girvikhata.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ClassicVerifiedWriteGatewayTest {
    private val customer = CustomerRecord(id = "c1", name = "Ravi", createdAt = 1L)
    private val girvi = GirviRecord(
        id = "g1",
        girviNumber = "G-001",
        customerId = customer.id,
        customerName = customer.name,
        categoryName = "Jewellery",
        itemName = "Ring",
        weightGrams = "5",
        principalPaise = 10_000L,
        monthlyRateBasisPoints = 200,
        createdAt = 2L,
    )

    @Test
    fun `gateway executes typed mutation and returns authoritative result`() {
        val before = AppSnapshot(customers = listOf(customer), girvis = listOf(girvi))
        val payment = PaymentRecord(
            id = "p1",
            receiptNumber = "R-001",
            amountPaise = 1_000L,
            principalPaise = 500L,
            interestPaise = 500L,
            createdAt = 3L,
        )
        val proposed = before.copy(girvis = listOf(girvi.copy(payments = listOf(payment))))
        var captured: VerifiedBusinessMutation? = null
        val gateway = ClassicVerifiedWriteGateway(
            reloadAuthoritative = { before },
            executeMutation = { mutation ->
                captured = mutation
                mutation.apply(before)
            },
        )

        val result = gateway.persist(before, proposed)

        assertTrue(captured is VerifiedBusinessMutation.AppendPayment)
        assertEquals(listOf(payment), result.girvis.single().payments)
    }

    @Test
    fun `gateway practical create mutation can apply to fresher authoritative snapshot`() {
        val screen = AppSnapshot(customers = listOf(customer))
        val extra = CustomerRecord(id = "c2", name = "Sita", createdAt = 2L)
        val fresh = screen.copy(customers = listOf(customer, extra))
        val newGirvi = girvi.copy(id = "g2", girviNumber = "G-002")
        val proposed = screen.copy(girvis = listOf(newGirvi))
        val gateway = ClassicVerifiedWriteGateway(
            reloadAuthoritative = { fresh },
            executeMutation = { mutation -> mutation.apply(fresh) },
        )

        val result = gateway.persist(screen, proposed)

        assertTrue(result.customers.any { it.id == extra.id })
        assertTrue(result.girvis.any { it.id == newGirvi.id })
    }

    @Test
    fun `gateway does not execute when classifier rejects mixed change`() {
        val category = CategoryRecord(id = "cat1", name = "Gold")
        val before = AppSnapshot(customers = listOf(customer), categories = listOf(category))
        val mixed = before.copy(
            customers = listOf(customer.copy(name = "Changed")),
            categories = listOf(category.copy(active = false)),
        )
        var executions = 0
        val gateway = ClassicVerifiedWriteGateway(
            reloadAuthoritative = { before },
            executeMutation = { mutation -> executions += 1; mutation.apply(before) },
        )

        val failure = runCatching { gateway.persist(before, mixed) }.exceptionOrNull()

        assertNotEquals(null, failure)
        assertEquals(0, executions)
    }
}
