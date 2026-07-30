package com.girvikhata.app.domain

import com.girvikhata.app.data.GirviRecord
import com.girvikhata.app.data.PaymentRecord
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

data class GirviSettlementView(
    val calculatedInterestPaise: Long,
    val principalDuePaise: Long,
    val interestDuePaise: Long,
    val chargesDuePaise: Long,
    val totalDuePaise: Long,
)

object ReceiptNumberGenerator {
    fun next(existing: Collection<String>, now: Date = Date()): String {
        val date = SimpleDateFormat("yyyyMMdd", Locale.US).format(now)
        val prefix = "PAY-$date-"
        val sequence = existing.asSequence()
            .filter { it.startsWith(prefix) }
            .mapNotNull { it.removePrefix(prefix).toIntOrNull() }
            .maxOrNull()
            ?.plus(1)
            ?: 1
        return prefix + sequence.toString().padStart(4, '0')
    }
}

object GirviSettlementUseCase {
    fun settlementView(girvi: GirviRecord, months: Int): GirviSettlementView {
        val interest = SimpleInterestPreview.calculate(
            principalPaise = girvi.principalPaise,
            monthlyRateBasisPoints = girvi.monthlyRateBasisPoints,
            months = months,
        ).totalInterestPaise

        val ledger = girvi.payments.map {
            LedgerPayment(
                id = it.id,
                amountPaise = it.amountPaise,
                principalPaise = it.principalPaise,
                interestPaise = it.interestPaise,
                chargesPaise = it.chargesPaise,
                reversedPaymentId = it.reversedPaymentId,
                isReversal = it.isReversal,
            )
        }
        val balance = LedgerBalanceCalculator.calculate(
            originalPrincipalPaise = girvi.principalPaise,
            snapshot = SettlementSnapshot(
                calculatedInterestPaise = interest,
                manualAdjustmentPaise = girvi.manualInterestAdjustmentPaise,
                payments = ledger,
            ),
        )
        return GirviSettlementView(
            calculatedInterestPaise = interest,
            principalDuePaise = balance.principalDuePaise,
            interestDuePaise = balance.interestDuePaise,
            chargesDuePaise = balance.chargesDuePaise,
            totalDuePaise = balance.totalDuePaise,
        )
    }

    fun postPayment(
        girvi: GirviRecord,
        months: Int,
        amountPaise: Long,
        allocationMode: PaymentAllocationMode,
        paymentMode: String,
        note: String,
        allReceiptNumbers: Collection<String>,
        customSplit: PaymentSplit? = null,
        now: Date = Date(),
    ): GirviRecord {
        require(girvi.status == "ACTIVE") { "Only active girvi can receive payment" }
        val view = settlementView(girvi, months)
        val posting = SettlementEngine.postPayment(
            balance = AccountBalance(
                principalDuePaise = view.principalDuePaise,
                interestDuePaise = view.interestDuePaise,
                chargesDuePaise = view.chargesDuePaise,
            ),
            amountPaise = amountPaise,
            mode = allocationMode,
            customSplit = customSplit,
        )
        require(posting.excessPaise == 0L) { "Payment cannot exceed total due" }
        val payment = PaymentRecord(
            id = UUID.randomUUID().toString(),
            receiptNumber = ReceiptNumberGenerator.next(allReceiptNumbers, now),
            amountPaise = posting.split.totalPaise,
            principalPaise = posting.split.principalPaise,
            interestPaise = posting.split.interestPaise,
            chargesPaise = posting.split.chargesPaise,
            mode = paymentMode.trim().ifBlank { "CASH" },
            note = note.trim(),
            createdAt = now.time,
        )
        return girvi.copy(payments = girvi.payments + payment)
    }

    fun reversePayment(
        girvi: GirviRecord,
        paymentId: String,
        reason: String,
        allReceiptNumbers: Collection<String>,
        now: Date = Date(),
    ): GirviRecord {
        require(reason.trim().length >= 3) { "Reversal reason required" }
        val original = girvi.payments.firstOrNull { it.id == paymentId && !it.isReversal }
            ?: error("Original payment not found")
        require(girvi.payments.none { it.isReversal && it.reversedPaymentId == paymentId }) {
            "Payment already reversed"
        }
        val reversal = PaymentRecord(
            receiptNumber = ReceiptNumberGenerator.next(allReceiptNumbers, now),
            amountPaise = original.amountPaise,
            principalPaise = original.principalPaise,
            interestPaise = original.interestPaise,
            chargesPaise = original.chargesPaise,
            mode = "REVERSAL",
            note = reason.trim(),
            createdAt = now.time,
            isReversal = true,
            reversedPaymentId = original.id,
        )
        return girvi.copy(payments = girvi.payments + reversal)
    }

    fun release(
        girvi: GirviRecord,
        months: Int,
        releaseNote: String,
        explicitOwnerOverride: Boolean,
        now: Date = Date(),
    ): GirviRecord {
        require(girvi.status == "ACTIVE") { "Girvi already closed" }
        val view = settlementView(girvi, months)
        val decision = ReleasePolicy.evaluate(
            AccountBalance(
                principalDuePaise = view.principalDuePaise,
                interestDuePaise = view.interestDuePaise,
                chargesDuePaise = view.chargesDuePaise,
            ),
            explicitOwnerOverride = explicitOwnerOverride,
        )
        require(decision.allowed) { decision.reason }
        require(releaseNote.trim().length >= 3) { "Release note required" }
        return girvi.copy(
            status = "RELEASED",
            releasedAt = now.time,
            releaseNote = releaseNote.trim(),
        )
    }
}
