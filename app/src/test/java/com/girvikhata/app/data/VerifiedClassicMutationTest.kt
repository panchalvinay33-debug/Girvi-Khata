package com.girvikhata.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class VerifiedClassicMutationTest {
    private val customer = CustomerRecord(id = "customer-1", name = "Ramesh", createdAt = 1L)
    private val originalPayment = PaymentRecord(
        id = "payment-1",
        receiptNumber = "RCPT-1",
        amountPaise = 10_000L,
        principalPaise = 8_000L,
        interestPaise = 2_000L,
        createdAt = 2L,
    )
    private val girvi = GirviRecord(
        id = "girvi-1",
        girviNumber = "G-1",
        customerId = customer.id,
        customerName = customer.name,
        categoryName = "Jewellery",
        itemName = "Ring",
        weightGrams = "5",
        principalPaise = 50_000L,
        monthlyRateBasisPoints = 200,
        createdAt = 1L,
        payments = listOf(originalPayment),
    )
    private val snapshot = AppSnapshot(
        customers = listOf(customer),
        categories = listOf(CategoryRecord(id = "category-1", name = "Jewellery")),
        girvis = listOf(girvi),
    )

    @Test
    fun validPaymentReversalAppendsExactlyOneLedgerEntry() {
        val reversal = PaymentRecord(
            id = "reversal-1",
            receiptNumber = "REV-1",
            amountPaise = originalPayment.amountPaise,
            principalPaise = originalPayment.principalPaise,
            interestPaise = originalPayment.interestPaise,
            createdAt = 3L,
            isReversal = true,
            reversedPaymentId = originalPayment.id,
        )

        val result = VerifiedBusinessMutation.ReversePayment(
            girviId = girvi.id,
            originalPaymentId = originalPayment.id,
            reversal = reversal,
        ).apply(snapshot)

        assertEquals(2, result.girvis.single().payments.size)
        assertEquals(reversal, result.girvis.single().payments.last())
    }

    @Test
    fun paymentCannotBeReversedTwice() {
        val reversal = PaymentRecord(
            id = "reversal-1",
            receiptNumber = "REV-1",
            amountPaise = originalPayment.amountPaise,
            principalPaise = originalPayment.principalPaise,
            interestPaise = originalPayment.interestPaise,
            createdAt = 3L,
            isReversal = true,
            reversedPaymentId = originalPayment.id,
        )
        val alreadyReversed = snapshot.copy(
            girvis = listOf(girvi.copy(payments = girvi.payments + reversal)),
        )
        val second = reversal.copy(id = "reversal-2", receiptNumber = "REV-2", createdAt = 4L)

        assertThrows(IllegalArgumentException::class.java) {
            VerifiedBusinessMutation.ReversePayment(girvi.id, originalPayment.id, second).apply(alreadyReversed)
        }
    }

    @Test
    fun releasePreservesLedgerAndCoreIdentity() {
        val released = girvi.copy(status = "RELEASED", releasedAt = 5L, releaseNote = "Returned to owner")

        val result = VerifiedBusinessMutation.ReleaseGirvi(released).apply(snapshot)

        assertEquals("RELEASED", result.girvis.single().status)
        assertEquals(girvi.payments, result.girvis.single().payments)
        assertEquals(girvi.girviNumber, result.girvis.single().girviNumber)
    }

    @Test
    fun releaseCannotRewritePaymentLedger() {
        val changedLedger = girvi.copy(
            status = "RELEASED",
            releasedAt = 5L,
            payments = emptyList(),
        )

        assertThrows(IllegalArgumentException::class.java) {
            VerifiedBusinessMutation.ReleaseGirvi(changedLedger).apply(snapshot)
        }
    }

    @Test
    fun categoryAddRejectsCaseInsensitiveDuplicate() {
        assertThrows(IllegalArgumentException::class.java) {
            VerifiedBusinessMutation.AddCategory(
                CategoryRecord(id = "category-2", name = " jewellery "),
            ).apply(snapshot)
        }
    }

    @Test
    fun activeGirviCategoryCannotBeDeactivated() {
        assertThrows(IllegalArgumentException::class.java) {
            VerifiedBusinessMutation.SetCategoryActive("category-1", false).apply(snapshot)
        }
    }

    @Test
    fun unusedCategoryCanBeDeactivated() {
        val withUnused = snapshot.copy(
            categories = snapshot.categories + CategoryRecord(id = "category-2", name = "Documents"),
        )

        val result = VerifiedBusinessMutation.SetCategoryActive("category-2", false).apply(withUnused)

        assertFalse(result.categories.first { it.id == "category-2" }.active)
        assertTrue(result.categories.first { it.id == "category-1" }.active)
    }
}
