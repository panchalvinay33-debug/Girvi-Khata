package com.girvikhata.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ClassicSnapshotMutationClassifierTest {
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
    fun `new girvi is classified atomically with customer`() {
        val before = AppSnapshot(customers = emptyList(), categories = emptyList(), girvis = emptyList())
        val next = before.copy(customers = listOf(customer), girvis = listOf(girvi))

        val classified = ClassicSnapshotMutationClassifier.classify(before, next)

        assertTrue(classified.mutation is VerifiedBusinessMutation.CreateGirviWithCustomer)
    }

    @Test
    fun `normal payment append is classified`() {
        val before = AppSnapshot(customers = listOf(customer), girvis = listOf(girvi))
        val payment = PaymentRecord(
            id = "p1",
            receiptNumber = "R-001",
            amountPaise = 1_000L,
            principalPaise = 500L,
            interestPaise = 500L,
            createdAt = 3L,
        )
        val next = before.copy(girvis = listOf(girvi.copy(payments = listOf(payment))))

        val classified = ClassicSnapshotMutationClassifier.classify(before, next)

        assertTrue(classified.mutation is VerifiedBusinessMutation.AppendPayment)
    }

    @Test
    fun `reversal append is classified`() {
        val original = PaymentRecord(
            id = "p1",
            receiptNumber = "R-001",
            amountPaise = 1_000L,
            principalPaise = 500L,
            interestPaise = 500L,
            createdAt = 3L,
        )
        val reversal = PaymentRecord(
            id = "p2",
            receiptNumber = "R-002",
            amountPaise = 1_000L,
            principalPaise = 500L,
            interestPaise = 500L,
            createdAt = 4L,
            isReversal = true,
            reversedPaymentId = original.id,
        )
        val current = girvi.copy(payments = listOf(original))
        val before = AppSnapshot(customers = listOf(customer), girvis = listOf(current))
        val next = before.copy(girvis = listOf(current.copy(payments = listOf(original, reversal))))

        val classified = ClassicSnapshotMutationClassifier.classify(before, next)

        assertTrue(classified.mutation is VerifiedBusinessMutation.ReversePayment)
    }

    @Test
    fun `release is classified without ledger mutation`() {
        val before = AppSnapshot(customers = listOf(customer), girvis = listOf(girvi))
        val released = girvi.copy(status = "RELEASED", releasedAt = 5L, releaseNote = "Returned")
        val next = before.copy(girvis = listOf(released))

        val classified = ClassicSnapshotMutationClassifier.classify(before, next)

        assertTrue(classified.mutation is VerifiedBusinessMutation.ReleaseGirvi)
    }

    @Test
    fun `category add is classified`() {
        val before = AppSnapshot(categories = emptyList())
        val category = CategoryRecord(id = "cat1", name = "Silver")
        val next = before.copy(categories = listOf(category))

        val classified = ClassicSnapshotMutationClassifier.classify(before, next)

        assertTrue(classified.mutation is VerifiedBusinessMutation.AddCategory)
    }

    @Test
    fun `mixed customer and category change is rejected`() {
        val before = AppSnapshot(customers = listOf(customer), categories = listOf(CategoryRecord(id = "cat1", name = "Gold")))
        val next = before.copy(
            customers = listOf(customer.copy(name = "Changed")),
            categories = listOf(CategoryRecord(id = "cat1", name = "Gold", active = false)),
        )

        val failure = runCatching { ClassicSnapshotMutationClassifier.classify(before, next) }.exceptionOrNull()

        assertTrue(failure is IllegalArgumentException || failure is IllegalStateException)
    }

    @Test
    fun `existing payment history rewrite is rejected`() {
        val original = PaymentRecord(
            id = "p1",
            receiptNumber = "R-001",
            amountPaise = 1_000L,
            principalPaise = 500L,
            interestPaise = 500L,
            createdAt = 3L,
        )
        val beforeGirvi = girvi.copy(payments = listOf(original))
        val before = AppSnapshot(customers = listOf(customer), girvis = listOf(beforeGirvi))
        val rewritten = original.copy(note = "rewritten")
        val appended = PaymentRecord(
            id = "p2",
            receiptNumber = "R-002",
            amountPaise = 100L,
            principalPaise = 100L,
            interestPaise = 0L,
            createdAt = 4L,
        )
        val next = before.copy(girvis = listOf(beforeGirvi.copy(payments = listOf(rewritten, appended))))

        val failure = runCatching { ClassicSnapshotMutationClassifier.classify(before, next) }.exceptionOrNull()

        assertEquals("Classic ledger history is immutable", failure?.message)
    }
}
