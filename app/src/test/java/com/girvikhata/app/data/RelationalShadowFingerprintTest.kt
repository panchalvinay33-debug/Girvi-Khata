package com.girvikhata.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class RelationalShadowFingerprintTest {
    @Test
    fun fingerprint_is_order_independent_for_relational_collections() {
        val customerA = CustomerRecord(id = "c-a", name = "A")
        val customerB = CustomerRecord(id = "c-b", name = "B")
        val girviA = sampleGirvi("g-a", "GK-2", customerA)
        val girviB = sampleGirvi("g-b", "GK-1", customerB)
        val one = AppSnapshot(customers = listOf(customerA, customerB), categories = listOf(CategoryRecord(id = "k", name = "Gold")), girvis = listOf(girviA, girviB))
        val two = one.copy(customers = one.customers.reversed(), girvis = one.girvis.reversed())
        assertEquals(RelationalShadowFingerprint.sha256(one), RelationalShadowFingerprint.sha256(two))
    }

    @Test
    fun fingerprint_changes_when_accounting_value_changes() {
        val customer = CustomerRecord(id = "c", name = "Owner")
        val original = AppSnapshot(customers = listOf(customer), girvis = listOf(sampleGirvi("g", "GK-1", customer)))
        val changed = original.copy(girvis = listOf(original.girvis.single().copy(principalPaise = 10_001)))
        assertNotEquals(RelationalShadowFingerprint.sha256(original), RelationalShadowFingerprint.sha256(changed))
    }

    @Test
    fun legacy_item_id_is_deterministic() {
        val customer = CustomerRecord(id = "c", name = "Owner")
        val legacy = sampleGirvi("legacy-g", "GK-1", customer).copy(items = emptyList(), categoryName = "Gold", itemName = "Ring", weightGrams = "12")
        assertEquals("legacy-item-legacy-g", RelationalShadowFingerprint.stableItems(legacy).single().id)
        val snapshot = AppSnapshot(customers = listOf(customer), girvis = listOf(legacy))
        assertEquals(RelationalShadowFingerprint.sha256(snapshot), RelationalShadowFingerprint.sha256(snapshot))
    }

    @Test
    fun decoded_legacy_single_item_random_ids_do_not_change_fingerprint() {
        val customer = CustomerRecord(id = "c", name = "Owner")
        val base = GirviRecord(
            id = "legacy-g",
            girviNumber = "GK-9",
            customerId = customer.id,
            customerName = customer.name,
            categoryName = "Gold",
            itemName = "Old Ring",
            weightGrams = "7.5",
            principalPaise = 25_000,
            monthlyRateBasisPoints = 200,
            createdAt = 100,
        )
        val firstRead = base.copy(items = listOf(GirviItemRecord(id = "random-read-a", categoryName = "Gold", itemName = "Old Ring", grossWeightGrams = "7.5")))
        val secondRead = base.copy(items = listOf(GirviItemRecord(id = "random-read-b", categoryName = "Gold", itemName = "Old Ring", grossWeightGrams = "7.5")))

        assertEquals("legacy-item-legacy-g", RelationalShadowFingerprint.stableItems(firstRead).single().id)
        assertEquals(
            RelationalShadowFingerprint.sha256(AppSnapshot(customers = listOf(customer), girvis = listOf(firstRead))),
            RelationalShadowFingerprint.sha256(AppSnapshot(customers = listOf(customer), girvis = listOf(secondRead))),
        )
    }

    @Test
    fun explicit_item_identity_still_changes_fingerprint() {
        val customer = CustomerRecord(id = "c", name = "Owner")
        val base = GirviRecord(
            id = "new-g",
            girviNumber = "GK-10",
            customerId = customer.id,
            customerName = customer.name,
            categoryName = "Gold",
            itemName = "Ring",
            weightGrams = "8",
            principalPaise = 25_000,
            monthlyRateBasisPoints = 200,
            createdAt = 100,
        )
        val first = base.copy(items = listOf(GirviItemRecord(id = "real-a", categoryName = "Gold", itemName = "Ring", grossWeightGrams = "8", description = "Hallmarked")))
        val second = base.copy(items = listOf(GirviItemRecord(id = "real-b", categoryName = "Gold", itemName = "Ring", grossWeightGrams = "8", description = "Hallmarked")))
        assertNotEquals(
            RelationalShadowFingerprint.sha256(AppSnapshot(customers = listOf(customer), girvis = listOf(first))),
            RelationalShadowFingerprint.sha256(AppSnapshot(customers = listOf(customer), girvis = listOf(second))),
        )
    }

    @Test
    fun expected_counts_include_items_and_payments() {
        val customer = CustomerRecord(id = "c", name = "Owner")
        val girvi = sampleGirvi("g", "GK-1", customer)
        assertEquals(RelationalShadowCounts(1, 0, 1, 2, 1), RelationalShadowFingerprint.expectedCounts(AppSnapshot(customers = listOf(customer), girvis = listOf(girvi))))
    }

    private fun sampleGirvi(id: String, number: String, customer: CustomerRecord) = GirviRecord(
        id = id,
        girviNumber = number,
        customerId = customer.id,
        customerName = customer.name,
        categoryName = "Gold",
        itemName = "Ring",
        weightGrams = "10",
        principalPaise = 10_000,
        monthlyRateBasisPoints = 200,
        createdAt = 100,
        items = listOf(
            GirviItemRecord(id = "$id-i1", categoryName = "Gold", itemName = "Ring", grossWeightGrams = "10"),
            GirviItemRecord(id = "$id-i2", categoryName = "Gold", itemName = "Chain", grossWeightGrams = "5"),
        ),
        payments = listOf(PaymentRecord(id = "$id-p", receiptNumber = "R-$id", amountPaise = 1000, principalPaise = 500, interestPaise = 500, createdAt = 200)),
    )
}
