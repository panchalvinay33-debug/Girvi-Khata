package com.girvikhata.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class LegacyChildIdentityTest {
    @Test
    fun `legacy child ids are deterministic across repeated loads`() {
        val firstItem = legacyChildId("item", "girvi-42", 0)
        val secondItem = legacyChildId("item", "girvi-42", 0)
        val firstPayment = legacyChildId("payment", "girvi-42", 1)
        val secondPayment = legacyChildId("payment", "girvi-42", 1)

        assertEquals(firstItem, secondItem)
        assertEquals(firstPayment, secondPayment)
        assertNotEquals(firstItem, firstPayment)
        assertNotEquals(legacyChildId("item", "girvi-42", 0), legacyChildId("item", "girvi-42", 1))
    }
}