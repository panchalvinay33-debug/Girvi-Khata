package com.girvikhata.app.domain

import java.math.BigDecimal
import java.math.RoundingMode

enum class PaymentAllocationMode {
    INTEREST_FIRST,
    PRINCIPAL_FIRST,
    CUSTOM,
}

data class AccountBalance(
    val principalDuePaise: Long,
    val interestDuePaise: Long,
    val chargesDuePaise: Long = 0,
) {
    init {
        require(principalDuePaise >= 0)
        require(interestDuePaise >= 0)
        require(chargesDuePaise >= 0)
    }

    val totalDuePaise: Long
        get() = principalDuePaise + interestDuePaise + chargesDuePaise
}

data class PaymentSplit(
    val principalPaise: Long,
    val interestPaise: Long,
    val chargesPaise: Long,
) {
    init {
        require(principalPaise >= 0)
        require(interestPaise >= 0)
        require(chargesPaise >= 0)
    }

    val totalPaise: Long
        get() = principalPaise + interestPaise + chargesPaise
}

data class PaymentPostingResult(
    val split: PaymentSplit,
    val balanceAfter: AccountBalance,
    val excessPaise: Long,
)

object SettlementEngine {
    fun postPayment(
        balance: AccountBalance,
        amountPaise: Long,
        mode: PaymentAllocationMode,
        customSplit: PaymentSplit? = null,
    ): PaymentPostingResult {
        require(amountPaise > 0) { "Payment amount must be positive" }

        val payableAmount = minOf(amountPaise, balance.totalDuePaise)
        val split = when (mode) {
            PaymentAllocationMode.INTEREST_FIRST -> allocateInterestFirst(balance, payableAmount)
            PaymentAllocationMode.PRINCIPAL_FIRST -> allocatePrincipalFirst(balance, payableAmount)
            PaymentAllocationMode.CUSTOM -> validateCustom(balance, payableAmount, customSplit)
        }

        return PaymentPostingResult(
            split = split,
            balanceAfter = AccountBalance(
                principalDuePaise = balance.principalDuePaise - split.principalPaise,
                interestDuePaise = balance.interestDuePaise - split.interestPaise,
                chargesDuePaise = balance.chargesDuePaise - split.chargesPaise,
            ),
            excessPaise = amountPaise - payableAmount,
        )
    }

    private fun allocateInterestFirst(balance: AccountBalance, amount: Long): PaymentSplit {
        var remaining = amount
        val charges = minOf(remaining, balance.chargesDuePaise)
        remaining -= charges
        val interest = minOf(remaining, balance.interestDuePaise)
        remaining -= interest
        val principal = minOf(remaining, balance.principalDuePaise)
        return PaymentSplit(principal, interest, charges)
    }

    private fun allocatePrincipalFirst(balance: AccountBalance, amount: Long): PaymentSplit {
        var remaining = amount
        val principal = minOf(remaining, balance.principalDuePaise)
        remaining -= principal
        val charges = minOf(remaining, balance.chargesDuePaise)
        remaining -= charges
        val interest = minOf(remaining, balance.interestDuePaise)
        return PaymentSplit(principal, interest, charges)
    }

    private fun validateCustom(
        balance: AccountBalance,
        amount: Long,
        customSplit: PaymentSplit?,
    ): PaymentSplit {
        val split = requireNotNull(customSplit) { "Custom split required" }
        require(split.totalPaise == amount) { "Custom split must equal applied payment" }
        require(split.principalPaise <= balance.principalDuePaise) { "Principal allocation exceeds due" }
        require(split.interestPaise <= balance.interestDuePaise) { "Interest allocation exceeds due" }
        require(split.chargesPaise <= balance.chargesDuePaise) { "Charges allocation exceeds due" }
        return split
    }
}

data class SettlementSnapshot(
    val calculatedInterestPaise: Long,
    val manualAdjustmentPaise: Long,
    val payments: List<LedgerPayment>,
) {
    init {
        require(calculatedInterestPaise >= 0)
    }
}

data class LedgerPayment(
    val id: String,
    val amountPaise: Long,
    val principalPaise: Long,
    val interestPaise: Long,
    val chargesPaise: Long,
    val reversedPaymentId: String? = null,
    val isReversal: Boolean = false,
) {
    init {
        require(amountPaise > 0)
        require(principalPaise >= 0)
        require(interestPaise >= 0)
        require(chargesPaise >= 0)
        require(principalPaise + interestPaise + chargesPaise == amountPaise)
        if (isReversal) require(!reversedPaymentId.isNullOrBlank())
    }
}

object LedgerBalanceCalculator {
    fun calculate(
        originalPrincipalPaise: Long,
        snapshot: SettlementSnapshot,
    ): AccountBalance {
        require(originalPrincipalPaise >= 0)

        val reversedIds = snapshot.payments
            .filter { it.isReversal }
            .mapNotNull { it.reversedPaymentId }
            .toSet()

        val effectivePayments = snapshot.payments.filter {
            !it.isReversal && it.id !in reversedIds
        }

        val paidPrincipal = effectivePayments.sumOf { it.principalPaise }
        val paidInterest = effectivePayments.sumOf { it.interestPaise }
        val paidCharges = effectivePayments.sumOf { it.chargesPaise }
        val adjustedInterest = (snapshot.calculatedInterestPaise + snapshot.manualAdjustmentPaise)
            .coerceAtLeast(0)

        return AccountBalance(
            principalDuePaise = (originalPrincipalPaise - paidPrincipal).coerceAtLeast(0),
            interestDuePaise = (adjustedInterest - paidInterest).coerceAtLeast(0),
            chargesDuePaise = (0L - paidCharges).coerceAtLeast(0),
        )
    }
}

data class ReleaseDecision(
    val allowed: Boolean,
    val reason: String,
)

object ReleasePolicy {
    fun evaluate(balance: AccountBalance, explicitOwnerOverride: Boolean): ReleaseDecision = when {
        balance.totalDuePaise == 0L -> ReleaseDecision(true, "Account fully settled")
        explicitOwnerOverride -> ReleaseDecision(true, "Owner override recorded; remaining amount must be adjusted in ledger")
        else -> ReleaseDecision(false, "Outstanding amount remains")
    }
}

object MoneyInput {
    fun rupeesToPaise(value: String): Long {
        val amount = value.trim().toBigDecimalOrNull() ?: error("Invalid amount")
        require(amount > BigDecimal.ZERO) { "Amount must be positive" }
        return amount.multiply(BigDecimal.valueOf(100L))
            .setScale(0, RoundingMode.HALF_UP)
            .longValueExact()
    }
}
