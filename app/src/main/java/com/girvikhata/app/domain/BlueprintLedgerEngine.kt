package com.girvikhata.app.domain

import com.girvikhata.app.data.GirviRecord
import com.girvikhata.app.data.PaymentRecord
import kotlin.math.max

/** Pure ledger projection used by girvi detail, settlement and reports. */
object BlueprintLedgerEngine {
    data class AdvanceSlice(
        val id: String,
        val amountPaise: Long,
        val createdAt: Long,
        val terms: InterestTerms,
        val original: Boolean,
        val note: String = "",
    )

    data class LedgerLine(
        val id: String,
        val createdAt: Long,
        val side: Side,
        val type: String,
        val amountPaise: Long,
        val principalPaise: Long = 0,
        val interestPaise: Long = 0,
        val chargesPaise: Long = 0,
        val note: String = "",
    )

    enum class Side { SHOPKEEPER_GAVE, CUSTOMER_PAID }

    data class SettlementProjection(
        val settlementAt: Long,
        val advances: List<AdvanceSlice>,
        val lines: List<LedgerLine>,
        val totalAdvancedPaise: Long,
        val principalReturnedPaise: Long,
        val principalOutstandingPaise: Long,
        val grossInterestAccruedPaise: Long,
        val interestReceivedPaise: Long,
        val interestOutstandingPaise: Long,
        val chargesReceivedPaise: Long,
        val totalDuePaise: Long,
    )

    fun project(girvi: GirviRecord, settlementAt: Long): SettlementProjection {
        require(settlementAt >= girvi.createdAt) { "Settlement date cannot be before girvi date" }

        val originalTerms = GirviInterestMetadata.read(girvi.items.firstOrNull()?.description)
            ?: InterestTerms(
                mode = InterestMode.PERCENT_PER_MONTH,
                monthlyRateBasisPoints = girvi.monthlyRateBasisPoints,
                periodRule = InterestPeriodRule.COMPLETED_MONTHS_PLUS_DAYS,
            )
        val advances = buildList {
            add(
                AdvanceSlice(
                    id = "original-${girvi.id}",
                    amountPaise = girvi.principalPaise,
                    createdAt = girvi.createdAt,
                    terms = originalTerms,
                    original = true,
                    note = "Original advance",
                ),
            )
            GirviAdvanceMetadata.read(girvi.releaseNote).forEach { advance ->
                add(
                    AdvanceSlice(
                        id = advance.id,
                        amountPaise = advance.amountPaise,
                        createdAt = advance.createdAt,
                        terms = advance.terms,
                        original = false,
                        note = advance.note,
                    ),
                )
            }
        }.sortedWith(compareBy<AdvanceSlice> { it.createdAt }.thenBy { it.id })

        advances.forEach { require(it.createdAt <= settlementAt) { "Advance date after settlement date" } }

        val effectivePayments = effectivePayments(girvi.payments)
            .filter { it.createdAt <= settlementAt }

        val totalAdvanced = advances.fold(0L) { total, advance -> Math.addExact(total, advance.amountPaise) }
        val principalReturned = effectivePayments.fold(0L) { total, payment -> Math.addExact(total, payment.principalPaise) }
        val interestReceived = effectivePayments.fold(0L) { total, payment -> Math.addExact(total, payment.interestPaise) }
        val chargesReceived = effectivePayments.fold(0L) { total, payment -> Math.addExact(total, payment.chargesPaise) }

        // Alpha 25C bridge: each advance accrues independently from its own date. Principal payment
        // allocation across individual advance lots is intentionally not guessed here. Until an
        // explicit allocation event is stored, accrued interest remains reproducible and payments
        // are subtracted from the received bucket rather than silently rewriting old principal.
        val grossInterest = advances.fold(0L) { total, advance ->
            Math.addExact(
                total,
                InterestEngine.quote(
                    principalPaise = advance.amountPaise,
                    startAtMillis = advance.createdAt,
                    endAtMillis = settlementAt,
                    terms = advance.terms,
                ).interestPaise,
            )
        }

        val principalOutstanding = max(0L, totalAdvanced - principalReturned)
        val interestOutstanding = max(0L, grossInterest - interestReceived + girvi.manualInterestAdjustmentPaise)
        val totalDue = Math.addExact(principalOutstanding, interestOutstanding)

        val lines = buildList {
            advances.forEach { advance ->
                add(
                    LedgerLine(
                        id = advance.id,
                        createdAt = advance.createdAt,
                        side = Side.SHOPKEEPER_GAVE,
                        type = if (advance.original) "ORIGINAL_ADVANCE" else "ADDITIONAL_ADVANCE",
                        amountPaise = advance.amountPaise,
                        principalPaise = advance.amountPaise,
                        note = advance.note,
                    ),
                )
            }
            effectivePayments.forEach { payment ->
                add(
                    LedgerLine(
                        id = payment.id,
                        createdAt = payment.createdAt,
                        side = Side.CUSTOMER_PAID,
                        type = "PAYMENT",
                        amountPaise = payment.amountPaise,
                        principalPaise = payment.principalPaise,
                        interestPaise = payment.interestPaise,
                        chargesPaise = payment.chargesPaise,
                        note = payment.note,
                    ),
                )
            }
        }.sortedWith(compareBy<LedgerLine> { it.createdAt }.thenBy { it.id })

        return SettlementProjection(
            settlementAt = settlementAt,
            advances = advances,
            lines = lines,
            totalAdvancedPaise = totalAdvanced,
            principalReturnedPaise = principalReturned,
            principalOutstandingPaise = principalOutstanding,
            grossInterestAccruedPaise = grossInterest,
            interestReceivedPaise = interestReceived,
            interestOutstandingPaise = interestOutstanding,
            chargesReceivedPaise = chargesReceived,
            totalDuePaise = totalDue,
        )
    }

    /** Reversal entries neutralize the referenced original payment. */
    fun effectivePayments(payments: List<PaymentRecord>): List<PaymentRecord> {
        val reversedIds = payments.filter { it.isReversal }.mapNotNull { it.reversedPaymentId }.toSet()
        return payments.filter { payment -> !payment.isReversal && payment.id !in reversedIds }
    }
}
