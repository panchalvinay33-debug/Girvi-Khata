package com.girvikhata.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RelationalShadowDeltaTest {
    @Test
    fun unchangedSnapshotProducesEmptyDelta() {
        val snapshot = sampleSnapshot()
        assertTrue(RelationalShadowDeltaPlanner.plan(snapshot, snapshot).isEmpty)
    }

    @Test
    fun paymentChangeReplacesOnlyAffectedGirviSubtree() {
        val before = sampleSnapshot()
        val changed = before.girvis.first().copy(
            payments = before.girvis.first().payments + PaymentRecord(
                id = "p2",
                receiptNumber = "R-2",
                amountPaise = 1000,
                principalPaise = 1000,
                interestPaise = 0,
            ),
        )
        val delta = RelationalShadowDeltaPlanner.plan(before, before.copy(girvis = listOf(changed)))
        assertEquals(listOf("g1"), delta.upsertGirvis.map { it.id })
        assertTrue(delta.upsertCustomers.isEmpty())
        assertTrue(delta.upsertCategories.isEmpty())
    }

    @Test
    fun deletedCustomerAndGirviArePlannedSeparately() {
        val before = sampleSnapshot()
        val delta = RelationalShadowDeltaPlanner.plan(before, before.copy(customers = emptyList(), girvis = emptyList()))
        assertEquals(setOf("g1"), delta.deleteGirviIds)
        assertEquals(setOf("c1"), delta.deleteCustomerIds)
    }

    @Test
    fun cutoverRequiresAllEvidence() {
        val incomplete = RelationalCutoverEvidence(
            consecutiveHealthySyncs = 24,
            lastHealthyAt = 100,
            lastFailureAt = null,
            stressDatasetVerified = false,
            rollbackSimulationVerified = false,
            ownerPhysicalTestApproved = false,
        )
        assertFalse(RelationalCutoverPolicy.eligible(incomplete))
        assertTrue(RelationalCutoverPolicy.blockers(incomplete).size >= 4)

        val complete = incomplete.copy(
            consecutiveHealthySyncs = 25,
            stressDatasetVerified = true,
            rollbackSimulationVerified = true,
            ownerPhysicalTestApproved = true,
        )
        assertTrue(RelationalCutoverPolicy.eligible(complete))
    }

    private fun sampleSnapshot() = AppSnapshot(
        customers = listOf(CustomerRecord(id = "c1", name = "A", createdAt = 1)),
        categories = listOf(CategoryRecord(id = "k1", name = "Gold")),
        girvis = listOf(
            GirviRecord(
                id = "g1",
                girviNumber = "GK-1",
                customerId = "c1",
                customerName = "A",
                categoryName = "Gold",
                itemName = "Ring",
                weightGrams = "5",
                principalPaise = 10000,
                monthlyRateBasisPoints = 200,
                createdAt = 1,
                items = listOf(GirviItemRecord(id = "i1", categoryName = "Gold", itemName = "Ring", grossWeightGrams = "5")),
                payments = listOf(PaymentRecord(id = "p1", receiptNumber = "R-1", amountPaise = 500, principalPaise = 0, interestPaise = 500)),
            ),
        ),
    )
}
