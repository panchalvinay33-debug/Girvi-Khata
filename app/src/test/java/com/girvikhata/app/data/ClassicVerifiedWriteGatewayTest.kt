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
    fun `gateway executes typed request with screen fingerprint and returns authoritative reload`() {
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
        val authoritative = proposed.copy(schemaVersion = proposed.schemaVersion)
        var captured: VerifiedBusinessWriteRequest? = null
        var reloads = 0
        val gateway = ClassicVerifiedWriteGateway(
            reloadAuthoritative = { reloads += 1; authoritative },
            executeVerified = { captured = it },
        )

        val result = gateway.persist(before, proposed)

        assertEquals(authoritative, result)
        assertEquals(1, reloads)
        assertEquals(RelationalShadowFingerprint.sha256(before), captured?.expectedFingerprint)
        assertTrue(captured?.mutation is VerifiedBusinessMutation.AppendPayment)
        assertTrue(captured?.title?.contains(payment.receiptNumber) == true)
    }

    @Test
    fun `gateway does not execute or reload when classifier rejects mixed change`() {
        val category = CategoryRecord(id = "cat1", name = "Gold")
        val before = AppSnapshot(customers = listOf(customer), categories = listOf(category))
        val mixed = before.copy(
            customers = listOf(customer.copy(name = "Changed")),
            categories = listOf(category.copy(active = false)),
        )
        var executions = 0
        var reloads = 0
        val gateway = ClassicVerifiedWriteGateway(
            reloadAuthoritative = { reloads += 1; before },
            executeVerified = { executions += 1 },
        )

        val failure = runCatching { gateway.persist(before, mixed) }.exceptionOrNull()

        assertNotEquals(null, failure)
        assertEquals(0, executions)
        assertEquals(0, reloads)
    }
}
