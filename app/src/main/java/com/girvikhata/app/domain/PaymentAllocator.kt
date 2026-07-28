package com.girvikhata.app.domain

import java.math.BigDecimal

enum class AllocationPriority { INTEREST_FIRST, PRINCIPAL_FIRST, CUSTOM }

data class OutstandingBalance(
    val principal: Money,
    val interest: Money,
    val charges: Money = BigDecimal.ZERO,
)

data class PaymentAllocation(
    val received: Money,
    val principalPaid: Money,
    val interestPaid: Money,
    val chargesPaid: Money,
    val unallocated: Money,
    val remaining: OutstandingBalance,
)

class PaymentAllocator {
    fun allocate(
        received: Money,
        outstanding: OutstandingBalance,
        priority: AllocationPriority,
        customPrincipal: Money = BigDecimal.ZERO,
        customInterest: Money = BigDecimal.ZERO,
        customCharges: Money = BigDecimal.ZERO,
    ): PaymentAllocation {
        require(received >= BigDecimal.ZERO)
        require(outstanding.principal >= BigDecimal.ZERO)
        require(outstanding.interest >= BigDecimal.ZERO)
        require(outstanding.charges >= BigDecimal.ZERO)

        var remainingPayment = received
        var principalPaid = BigDecimal.ZERO
        var interestPaid = BigDecimal.ZERO
        var chargesPaid = BigDecimal.ZERO

        fun take(maximum: Money): Money {
            val amount = remainingPayment.min(maximum)
            remainingPayment = remainingPayment.subtract(amount)
            return amount
        }

        when (priority) {
            AllocationPriority.INTEREST_FIRST -> {
                chargesPaid = take(outstanding.charges)
                interestPaid = take(outstanding.interest)
                principalPaid = take(outstanding.principal)
            }
            AllocationPriority.PRINCIPAL_FIRST -> {
                principalPaid = take(outstanding.principal)
                interestPaid = take(outstanding.interest)
                chargesPaid = take(outstanding.charges)
            }
            AllocationPriority.CUSTOM -> {
                require(customPrincipal >= BigDecimal.ZERO && customInterest >= BigDecimal.ZERO && customCharges >= BigDecimal.ZERO)
                require(customPrincipal <= outstanding.principal)
                require(customInterest <= outstanding.interest)
                require(customCharges <= outstanding.charges)
                require(customPrincipal.add(customInterest).add(customCharges) == received) {
                    "Custom allocation must equal received amount"
                }
                principalPaid = customPrincipal
                interestPaid = customInterest
                chargesPaid = customCharges
                remainingPayment = BigDecimal.ZERO
            }
        }

        return PaymentAllocation(
            received = received,
            principalPaid = principalPaid,
            interestPaid = interestPaid,
            chargesPaid = chargesPaid,
            unallocated = remainingPayment,
            remaining = OutstandingBalance(
                principal = outstanding.principal.subtract(principalPaid),
                interest = outstanding.interest.subtract(interestPaid),
                charges = outstanding.charges.subtract(chargesPaid),
            ),
        )
    }
}
