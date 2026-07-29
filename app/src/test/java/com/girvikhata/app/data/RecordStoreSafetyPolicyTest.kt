package com.girvikhata.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class RecordStoreSafetyPolicyTest {
    @Test
    fun envelopeLengthsRejectInvalidValues() {
        RecordStoreSafetyPolicy.validateEnvelopeLengths(12, 16)
        assertThrows(IllegalArgumentException::class.java) {
            RecordStoreSafetyPolicy.validateEnvelopeLengths(0, 16)
        }
        assertThrows(IllegalArgumentException::class.java) {
            RecordStoreSafetyPolicy.validateEnvelopeLengths(12, 0)
        }
    }

    @Test
    fun retentionKeepsNewestEntriesOnly() {
        val retained = RecordStoreSafetyPolicy.newestFirstRetained(
            items = listOf(1L, 5L, 3L, 2L, 4L, 6L),
            modifiedAt = { it },
            keep = 5,
        )
        assertEquals(listOf(6L, 5L, 4L, 3L, 2L), retained)
    }

    @Test
    fun duplicateGirviNumberIsRejected() {
        val customer = CustomerRecord(id = "c1", name = "Vinay")
        val first = GirviRecord(
            id = "g1", girviNumber = "GK-1", customerId = customer.id, customerName = customer.name,
            categoryName = "Gold", itemName = "Ring", weightGrams = "5", principalPaise = 100_00,
            monthlyRateBasisPoints = 200,
        )
        val second = first.copy(id = "g2")
        assertThrows(IllegalArgumentException::class.java) {
            RecordStoreSafetyPolicy.validateSnapshot(AppSnapshot(customers = listOf(customer), girvis = listOf(first, second)))
        }
    }

    @Test
    fun missingCustomerRelationshipIsRejected() {
        val girvi = GirviRecord(
            girviNumber = "GK-1", customerId = "missing", customerName = "Missing",
            categoryName = "Gold", itemName = "Ring", weightGrams = "5", principalPaise = 100_00,
            monthlyRateBasisPoints = 200,
        )
        assertThrows(IllegalArgumentException::class.java) {
            RecordStoreSafetyPolicy.validateSnapshot(AppSnapshot(girvis = listOf(girvi)))
        }
    }
}