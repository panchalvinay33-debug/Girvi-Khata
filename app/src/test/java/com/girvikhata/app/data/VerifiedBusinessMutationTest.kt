package com.girvikhata.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VerifiedBusinessMutationTest {
    private val customer = CustomerRecord(id = "c1", name = "Owner")
    private val girvi = GirviRecord(
        id = "g1",
        girviNumber = "G-1",
        customerId = customer.id,
        customerName = customer.name,
        categoryName = "Gold",
        itemName = "Ring",
        weightGrams = "10",
        principalPaise = 100_000,
        monthlyRateBasisPoints = 200,
    )

    @Test fun `customer upsert replaces same ID deterministically`() {
        val snapshot = AppSnapshot(customers = listOf(customer))
        val updated = customer.copy(name = "Updated")
        val result = VerifiedBusinessMutation.UpsertCustomer(updated).apply(snapshot)
        assertEquals(1, result.customers.size)
        assertEquals("Updated", result.customers.single().name)
    }

    @Test fun `girvi upsert requires existing customer`() {
        val snapshot = AppSnapshot(customers = listOf(customer), girvis = listOf(girvi))
        val updated = girvi.copy(principalPaise = 200_000)
        val result = VerifiedBusinessMutation.UpsertGirvi(updated).apply(snapshot)
        assertEquals(200_000, result.girvis.single().principalPaise)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `girvi upsert blocks missing customer`() {
        VerifiedBusinessMutation.UpsertGirvi(girvi.copy(customerId = "missing")).apply(AppSnapshot())
    }

    @Test fun `payment append changes only target girvi`() {
        val other = girvi.copy(id = "g2", girviNumber = "G-2")
        val payment = PaymentRecord(
            id = "p1",
            receiptNumber = "R-1",
            amountPaise = 10_000,
            principalPaise = 10_000,
            interestPaise = 0,
        )
        val before = AppSnapshot(customers = listOf(customer), girvis = listOf(girvi, other))
        val after = VerifiedBusinessMutation.AppendPayment(girvi.id, payment).apply(before)
        assertEquals(1, after.girvis.single { it.id == girvi.id }.payments.size)
        assertTrue(after.girvis.single { it.id == other.id }.payments.isEmpty())
        assertNotEquals(RelationalShadowFingerprint.sha256(before), RelationalShadowFingerprint.sha256(after))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `duplicate receipt is blocked across girvis`() {
        val existing = PaymentRecord(
            id = "p1",
            receiptNumber = "R-1",
            amountPaise = 10_000,
            principalPaise = 10_000,
            interestPaise = 0,
        )
        val first = girvi.copy(payments = listOf(existing))
        val second = girvi.copy(id = "g2", girviNumber = "G-2")
        val duplicate = existing.copy(id = "p2")
        VerifiedBusinessMutation.AppendPayment(second.id, duplicate)
            .apply(AppSnapshot(customers = listOf(customer), girvis = listOf(first, second)))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `released girvi payment is blocked`() {
        val released = girvi.copy(status = "RELEASED", releasedAt = 1L)
        val payment = PaymentRecord(
            id = "p1",
            receiptNumber = "R-1",
            amountPaise = 10_000,
            principalPaise = 10_000,
            interestPaise = 0,
        )
        VerifiedBusinessMutation.AppendPayment(released.id, payment)
            .apply(AppSnapshot(customers = listOf(customer), girvis = listOf(released)))
    }

    @Test fun `restore replacement returns exact target`() {
        val before = AppSnapshot(customers = listOf(customer))
        val target = AppSnapshot(customers = listOf(customer), girvis = listOf(girvi))
        assertEquals(target, VerifiedBusinessMutation.ReplaceSnapshotForRestore(target).apply(before))
    }
}
