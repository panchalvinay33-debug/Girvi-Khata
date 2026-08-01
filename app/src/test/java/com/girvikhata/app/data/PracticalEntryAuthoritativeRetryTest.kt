package com.girvikhata.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Rebuild regression coverage: Practical Entry no longer owns stale-fingerprint retry logic.
 * It classifies once, then applies the typed mutation to the latest authoritative snapshot.
 */
class PracticalEntryAuthoritativeRetryTest {
    @Test
    fun practicalCreate_appliesMutationToLatestAuthoritativeSnapshotWithoutFingerprintRetry() {
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
        val screenBefore = AppSnapshot()
        val proposed = screenBefore.copy(customers = listOf(customer), girvis = listOf(girvi))
        var authoritative = screenBefore.copy(categories = listOf(CategoryRecord(name = "Gold")))
        var executions = 0

        val gateway = ClassicVerifiedWriteGateway(
            reloadAuthoritative = { authoritative },
            executeMutation = { mutation ->
                executions++
                authoritative = mutation.apply(authoritative)
                authoritative
            },
        )

        val saved = gateway.persist(screenBefore, proposed)

        assertEquals(1, executions)
        assertTrue(saved.girvis.any { it.id == girvi.id })
        assertTrue(saved.customers.any { it.id == customer.id })
        assertEquals(1, saved.categories.size)
    }

    @Test
    fun writerFailure_isNotHiddenByLegacyRetryLoop() {
        val before = AppSnapshot(categories = listOf(CategoryRecord(name = "Gold")))
        val next = before.copy(categories = before.categories + CategoryRecord(name = "Silver"))
        var executions = 0
        val gateway = ClassicVerifiedWriteGateway(
            reloadAuthoritative = { before },
            executeMutation = {
                executions++
                error("authoritative encrypted save failed")
            },
        )

        val failure = runCatching { gateway.persist(before, next) }.exceptionOrNull()

        assertEquals(1, executions)
        assertTrue(failure?.message?.contains("authoritative encrypted save failed") == true)
    }
}
