package com.girvikhata.app.domain

import com.girvikhata.app.data.GirviRecord
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DateFormat
import java.util.Date

object ManualInterestAdjustment {
    fun apply(girvi: GirviRecord, signedRupees: String, reason: String): AdjustmentResult {
        require(girvi.status == "ACTIVE") { "Sirf active girvi par adjustment ho sakta hai" }
        val cleanReason = reason.trim().replace(Regex("\\s+"), " ")
        require(cleanReason.length >= 5) { "Adjustment reason kam se kam 5 characters ka ho" }
        val amount = signedRupees.trim().toBigDecimalOrNull() ?: error("Adjustment amount invalid hai")
        require(amount.compareTo(BigDecimal.ZERO) != 0) { "Adjustment zero nahi ho sakta" }
        val deltaPaise = amount.multiply(BigDecimal.valueOf(100L))
            .setScale(0, RoundingMode.HALF_UP)
            .longValueExact()
        require(deltaPaise in -100_000_000L..100_000_000L) { "Adjustment amount allowed limit se bahar hai" }
        val updatedTotal = Math.addExact(girvi.manualInterestAdjustmentPaise, deltaPaise)
        return AdjustmentResult(
            updatedGirvi = girvi.copy(manualInterestAdjustmentPaise = updatedTotal),
            deltaPaise = deltaPaise,
            cumulativePaise = updatedTotal,
            reason = cleanReason,
        )
    }
}

data class AdjustmentResult(
    val updatedGirvi: GirviRecord,
    val deltaPaise: Long,
    val cumulativePaise: Long,
    val reason: String,
)

object SettlementReceiptText {
    fun create(girvi: GirviRecord, months: Int): String {
        require(months in 0..120) { "Settlement months invalid" }
        val settlement = GirviSettlementUseCase.settlementView(girvi, months)
        val effectivePayments = girvi.payments.filterNot { payment ->
            payment.isReversal || girvi.payments.any { it.isReversal && it.reversedPaymentId == payment.id }
        }
        val paid = effectivePayments.sumOf { it.amountPaise }
        val itemLines = girvi.effectiveItems.joinToString("\n") {
            "- ${it.itemName} × ${it.quantity} | ${it.categoryName}" +
                it.grossWeightGrams.takeIf(String::isNotBlank)?.let { weight -> " | Gross ${weight}g" }.orEmpty()
        }
        return buildString {
            appendLine("GIRVI KHATA - FINAL SETTLEMENT / RELEASE RECEIPT")
            appendLine("Girvi No: ${girvi.girviNumber}")
            appendLine("Customer: ${girvi.customerName}")
            appendLine("Created: ${DateFormat.getDateInstance().format(Date(girvi.createdAt))}")
            appendLine("Status: ${girvi.status}")
            if (girvi.releasedAt != null) appendLine("Released: ${DateFormat.getDateTimeInstance().format(Date(girvi.releasedAt))}")
            appendLine()
            appendLine("Items:")
            appendLine(itemLines)
            appendLine()
            appendLine("Principal: ${money(girvi.principalPaise)}")
            appendLine("Calculated interest ($months months): ${money(settlement.calculatedInterestPaise)}")
            appendLine("Manual interest adjustment: ${signedMoney(girvi.manualInterestAdjustmentPaise)}")
            appendLine("Effective payments received: ${money(paid)}")
            appendLine("Principal due: ${money(settlement.principalDuePaise)}")
            appendLine("Interest due: ${money(settlement.interestDuePaise)}")
            appendLine("Total due at receipt: ${money(settlement.totalDuePaise)}")
            if (girvi.releaseNote.isNotBlank()) appendLine("Release note: ${girvi.releaseNote}")
            appendLine()
            appendLine("Receipt generated from encrypted local Girvi Khata records.")
        }.trim()
    }

    private fun money(paise: Long): String = "₹" + BigDecimal.valueOf(paise, 2).setScale(2).toPlainString()
    private fun signedMoney(paise: Long): String = (if (paise >= 0) "+" else "-") + money(kotlin.math.abs(paise))
}
