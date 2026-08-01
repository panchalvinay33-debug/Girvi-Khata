package com.girvikhata.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FreshInstallFirstGirviRegressionTest {
    @Test
    fun firstGirviAcceptsFreshDefaultSnapshotsWithDifferentGeneratedCategoryIds() {
        val screen = AppSnapshot.defaults()
        val customer = CustomerRecord(id = "customer-1", name = "पहला ग्राहक", mobile = "9999999999")
        val girvi = GirviRecord(
            id = "girvi-1",
            girviNumber = "GK-0001",
            customerId = customer.id,
            customerName = customer.name,
            categoryName = screen.categories.first().name,
            itemName = "Kada",
            weightGrams = "5",
            principalPaise = 10_000_00,
            monthlyRateBasisPoints = 200,
            items = listOf(
                GirviItemRecord(
                    id = "item-1",
                    categoryName = screen.categories.first().name,
                    itemName = "Kada",
                    grossWeightGrams = "5",
                ),
            ),
        )
        val next = screen.copy(customers = listOf(customer), girvis = listOf(girvi))

        var persisted: AppSnapshot? = null
        val gateway = ClassicVerifiedWriteGateway(
            reloadAuthoritative = {
                persisted ?: AppSnapshot.defaults() // intentionally new default UUIDs on every pre-save load
            },
            executeVerified = { request ->
                val authoritative = persisted ?: AppSnapshot.defaults()
                assertEquals(
                    RelationalShadowFingerprint.sha256(authoritative),
                    request.expectedFingerprint,
                )
                persisted = request.mutation.apply(authoritative)
            },
        )

        val saved = gateway.persist(screen, next)

        assertTrue(saved.girvis.any { it.id == "girvi-1" })
        assertTrue(saved.customers.any { it.id == "customer-1" })
        assertEquals("GK-0001", saved.girvis.single().girviNumber)
    }
}
