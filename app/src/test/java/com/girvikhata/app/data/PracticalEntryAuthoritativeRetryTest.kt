package com.girvikhata.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PracticalEntryAuthoritativeRetryTest {
    @Test
    fun practicalCreate_retriesOnceAgainstLatestAuthoritativeSnapshot() {
        val customer = CustomerRecord(name = "Retry Customer", mobile = "9999999999")
        val girvi = GirviRecord(
            girviNumber = "GK-0009",
            customerId = customer.id,
            customerName = customer.name,
            categoryName = "Gold",
            itemName = "Chain",
            weightGrams = "2",
            principalPaise = 50_000_00,
            monthlyRateBasisPoints = 200,
            items = listOf(GirviItemRecord(categoryName = "Gold", itemName = "Chain")),
        )
        val before = AppSnapshot()
        val next = before.copy(customers = listOf(customer), girvis = listOf(girvi))
        var authoritative = before.copy(categories = listOf(CategoryRecord(name = "Gold")))
        var attempts = 0

        val gateway = ClassicVerifiedWriteGateway(
            reloadAuthoritative = { authoritative },
            executeVerified = { request ->
                attempts++
                if (attempts == 1) {
                    throw IllegalArgumentException("Business data changed before transaction test; refresh and retry")
                }
                authoritative = request.mutation.apply(authoritative)
            },
        )

        val saved = gateway.persist(before, next)

        assertEquals(2, attempts)
        assertTrue(saved.girvis.any { it.id == girvi.id })
        assertTrue(saved.customers.any { it.id == customer.id })
    }

    @Test
    fun nonPracticalMutation_doesNotReceiveAutomaticRetry() {
        val before = AppSnapshot(categories = listOf(CategoryRecord(name = "Gold")))
        val next = before.copy(categories = before.categories + CategoryRecord(name = "Silver"))
        var attempts = 0
        val gateway = ClassicVerifiedWriteGateway(
            reloadAuthoritative = { before },
            executeVerified = {
                attempts++
                throw IllegalArgumentException("Business data changed before transaction test; refresh and retry")
            },
        )

        runCatching { gateway.persist(before, next) }

        assertEquals(1, attempts)
    }
}
