package com.girvikhata.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RelationalShadowStressTest {
    @Test
    fun largeDatasetFingerprintIsDeterministic() {
        val snapshot = largeSnapshot()
        val first = RelationalShadowFingerprint.sha256(snapshot)
        val second = RelationalShadowFingerprint.sha256(snapshot.copy(
            customers = snapshot.customers.reversed(),
            categories = snapshot.categories.reversed(),
            girvis = snapshot.girvis.reversed(),
        ))
        assertEquals(first, second)
        assertEquals(2_000, RelationalShadowFingerprint.expectedCounts(snapshot).items)
        assertEquals(1_000, RelationalShadowFingerprint.expectedCounts(snapshot).payments)
    }

    @Test
    fun onePaymentChangePlansOneGirviSubtree() {
        val before = largeSnapshot()
        val targetId = "g-777"
        val after = before.copy(girvis = before.girvis.map { girvi ->
            if (girvi.id != targetId) girvi else girvi.copy(
                payments = girvi.payments + PaymentRecord(
                    id = "extra-payment",
                    receiptNumber = "EXTRA-777",
                    amountPaise = 100,
                    principalPaise = 100,
                    interestPaise = 0,
                ),
            )
        })
        val delta = RelationalShadowDeltaPlanner.plan(before, after)
        assertEquals(listOf(targetId), delta.upsertGirvis.map { it.id })
        assertTrue(delta.deleteGirviIds.isEmpty())
        assertTrue(delta.upsertCustomers.isEmpty())
        assertTrue(delta.upsertCategories.isEmpty())
        assertEquals(5, delta.changedRows)
    }

    private fun largeSnapshot(): AppSnapshot {
        val customers = (0 until 200).map { index ->
            CustomerRecord(id = "c-$index", name = "Customer $index", mobile = "900000${index.toString().padStart(4, '0')}", createdAt = index + 1L)
        }
        val categories = (0 until 20).map { index -> CategoryRecord(id = "k-$index", name = "Category $index") }
        val girvis = (0 until 1_000).map { index ->
            val customer = customers[index % customers.size]
            val category = categories[index % categories.size]
            GirviRecord(
                id = "g-$index",
                girviNumber = "GK-STRESS-${index.toString().padStart(5, '0')}",
                customerId = customer.id,
                customerName = customer.name,
                categoryName = category.name,
                itemName = "Item $index-A",
                weightGrams = "10",
                principalPaise = 100_000L + index,
                monthlyRateBasisPoints = 200,
                createdAt = 1_000L + index,
                items = listOf(
                    GirviItemRecord(id = "i-$index-a", categoryName = category.name, itemName = "Item $index-A", grossWeightGrams = "10"),
                    GirviItemRecord(id = "i-$index-b", categoryName = category.name, itemName = "Item $index-B", grossWeightGrams = "20"),
                ),
                payments = listOf(
                    PaymentRecord(id = "p-$index", receiptNumber = "R-$index", amountPaise = 500, principalPaise = 0, interestPaise = 500),
                ),
            )
        }
        return AppSnapshot(customers = customers, categories = categories, girvis = girvis)
    }
}
