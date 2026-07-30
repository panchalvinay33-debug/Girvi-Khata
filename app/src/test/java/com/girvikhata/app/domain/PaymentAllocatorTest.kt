package com.girvikhata.app.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal

class PaymentAllocatorTest {
    private val allocator = PaymentAllocator()

    @Test
    fun interestFirstAllocation() {
        val result = allocator.allocate(
            received = BigDecimal("5000"),
            outstanding = OutstandingBalance(
                principal = BigDecimal("50000"),
                interest = BigDecimal("2000"),
            ),
            priority = AllocationPriority.INTEREST_FIRST,
        )
        assertEquals(BigDecimal("2000"), result.interestPaid)
        assertEquals(BigDecimal("3000"), result.principalPaid)
        assertEquals(BigDecimal("47000"), result.remaining.principal)
    }

    @Test
    fun principalFirstAllocation() {
        val result = allocator.allocate(
            received = BigDecimal("5000"),
            outstanding = OutstandingBalance(
                principal = BigDecimal("50000"),
                interest = BigDecimal("2000"),
            ),
            priority = AllocationPriority.PRINCIPAL_FIRST,
        )
        assertEquals(BigDecimal("5000"), result.principalPaid)
        assertEquals(BigDecimal.ZERO, result.interestPaid)
    }

    @Test(expected = IllegalArgumentException::class)
    fun customAllocationMustEqualReceived() {
        allocator.allocate(
            received = BigDecimal("5000"),
            outstanding = OutstandingBalance(BigDecimal("50000"), BigDecimal("2000")),
            priority = AllocationPriority.CUSTOM,
            customPrincipal = BigDecimal("1000"),
            customInterest = BigDecimal("1000"),
        )
    }
}
