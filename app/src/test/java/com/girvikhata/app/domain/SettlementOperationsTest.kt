package com.girvikhata.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SettlementOperationsTest {
    private val balance = AccountBalance(
        principalDuePaise = 100_000,
        interestDuePaise = 20_000,
        chargesDuePaise = 5_000,
    )

    @Test
    fun `interest first pays charges then interest then principal`() {
        val result = SettlementEngine.postPayment(
            balance = balance,
            amountPaise = 35_000,
            mode = PaymentAllocationMode.INTEREST_FIRST,
        )

        assertEquals(PaymentSplit(10_000, 20_000, 5_000), result.split)
        assertEquals(AccountBalance(90_000, 0, 0), result.balanceAfter)
    }

    @Test
    fun `principal first pays principal before other dues`() {
        val result = SettlementEngine.postPayment(
            balance = balance,
            amountPaise = 35_000,
            mode = PaymentAllocationMode.PRINCIPAL_FIRST,
        )

        assertEquals(PaymentSplit(35_000, 0, 0), result.split)
        assertEquals(AccountBalance(65_000, 20_000, 5_000), result.balanceAfter)
    }

    @Test
    fun `overpayment is returned as excess`() {
        val result = SettlementEngine.postPayment(
            balance = balance,
            amountPaise = 150_000,
            mode = PaymentAllocationMode.INTEREST_FIRST,
        )

        assertEquals(25_000, result.excessPaise)
        assertEquals(0, result.balanceAfter.totalDuePaise)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `custom allocation must exactly reconcile`() {
        SettlementEngine.postPayment(
            balance = balance,
            amountPaise = 10_000,
            mode = PaymentAllocationMode.CUSTOM,
            customSplit = PaymentSplit(5_000, 2_000, 0),
        )
    }

    @Test
    fun `reversal removes original payment from effective ledger`() {
        val original = LedgerPayment(
            id = "p1",
            amountPaise = 30_000,
            principalPaise = 20_000,
            interestPaise = 10_000,
            chargesPaise = 0,
        )
        val reversal = LedgerPayment(
            id = "r1",
            amountPaise = 30_000,
            principalPaise = 20_000,
            interestPaise = 10_000,
            chargesPaise = 0,
            reversedPaymentId = "p1",
            isReversal = true,
        )

        val calculated = LedgerBalanceCalculator.calculate(
            originalPrincipalPaise = 100_000,
            snapshot = SettlementSnapshot(
                calculatedInterestPaise = 10_000,
                manualAdjustmentPaise = 0,
                payments = listOf(original, reversal),
            ),
        )

        assertEquals(AccountBalance(100_000, 10_000, 0), calculated)
    }

    @Test
    fun `negative manual adjustment cannot create negative interest due`() {
        val calculated = LedgerBalanceCalculator.calculate(
            originalPrincipalPaise = 50_000,
            snapshot = SettlementSnapshot(
                calculatedInterestPaise = 5_000,
                manualAdjustmentPaise = -10_000,
                payments = emptyList(),
            ),
        )

        assertEquals(0, calculated.interestDuePaise)
    }

    @Test
    fun `release is blocked while amount remains`() {
        assertFalse(ReleasePolicy.evaluate(balance, explicitOwnerOverride = false).allowed)
        assertTrue(ReleasePolicy.evaluate(balance, explicitOwnerOverride = true).allowed)
        assertTrue(ReleasePolicy.evaluate(AccountBalance(0, 0, 0), false).allowed)
    }

    @Test
    fun `money input rounds rupees to paise`() {
        assertEquals(10_051, MoneyInput.rupeesToPaise("100.505"))
    }
}
