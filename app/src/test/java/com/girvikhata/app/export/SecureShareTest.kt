package com.girvikhata.app.export

import org.junit.Assert.assertEquals
import org.junit.Test

class SecureShareTest {
    @Test
    fun brandedSubject_usesBusinessNameWhenConfigured() {
        assertEquals(
            "Shree Shyam Jewellers • Payment Receipt",
            SecureShare.brandedSubject(" Shree Shyam Jewellers ", "Payment Receipt"),
        )
    }

    @Test
    fun brandedSubject_fallsBackWhenBusinessNameMissing() {
        assertEquals("Payment Receipt", SecureShare.brandedSubject("", "Payment Receipt"))
    }
}
