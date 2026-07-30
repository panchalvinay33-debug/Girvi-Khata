package com.girvikhata.app.domain

import com.girvikhata.app.data.GirviItemRecord

data class MasterItemDraft(
    val categoryName: String,
    val itemName: String,
    val quantity: Int,
    val grossWeightGrams: String,
    val deductionWeightGrams: String,
    val description: String,
)

data class CustomPaymentDraft(
    val principalPaise: Long,
    val interestPaise: Long,
    val chargesPaise: Long,
) {
    val totalPaise: Long get() = Math.addExact(Math.addExact(principalPaise, interestPaise), chargesPaise)
}

object MasterWorkflowTransactions {
    fun validateItems(drafts: List<MasterItemDraft>): List<GirviItemRecord> {
        require(drafts.isNotEmpty()) { "Kam se kam ek item required" }
        require(drafts.size <= 50) { "Ek girvi mein maximum 50 items allowed" }

        val normalizedKeys = mutableSetOf<String>()
        return drafts.map { draft ->
            val category = draft.categoryName.trim().replace(Regex("\\s+"), " ")
            val item = draft.itemName.trim().replace(Regex("\\s+"), " ")
            require(category.isNotBlank()) { "Item category required" }
            require(item.isNotBlank()) { "Item name required" }
            require(draft.quantity in 1..10_000) { "Item quantity invalid" }

            val gross = parseWeight(draft.grossWeightGrams, "Gross weight")
            val deduction = parseWeight(draft.deductionWeightGrams, "Deduction")
            require(deduction <= gross) { "Deduction gross weight se zyada nahi ho sakti" }

            val key = "${category.lowercase()}|${item.lowercase()}"
            require(normalizedKeys.add(key)) { "Same category aur item duplicate hai" }

            GirviItemRecord(
                categoryName = category,
                itemName = item,
                quantity = draft.quantity,
                grossWeightGrams = canonicalWeight(gross),
                deductionWeightGrams = canonicalWeight(deduction),
                description = draft.description.trim(),
            )
        }
    }

    fun validateCustomPayment(
        draft: CustomPaymentDraft,
        balance: AccountBalance,
        enteredAmountPaise: Long,
    ): PaymentSplit {
        require(enteredAmountPaise > 0) { "Payment amount positive hona chahiye" }
        require(draft.principalPaise >= 0 && draft.interestPaise >= 0 && draft.chargesPaise >= 0) {
            "Custom split negative nahi ho sakta"
        }
        require(draft.totalPaise == enteredAmountPaise) { "Custom split total payment amount se match nahi karta" }
        require(draft.principalPaise <= balance.principalDuePaise) { "Principal split due se zyada hai" }
        require(draft.interestPaise <= balance.interestDuePaise) { "Interest split due se zyada hai" }
        require(draft.chargesPaise <= balance.chargesDuePaise) { "Charges split due se zyada hai" }
        return PaymentSplit(
            principalPaise = draft.principalPaise,
            interestPaise = draft.interestPaise,
            chargesPaise = draft.chargesPaise,
        )
    }

    private fun parseWeight(raw: String, label: String): Double {
        val value = raw.trim().ifBlank { "0" }.toDoubleOrNull() ?: error("$label invalid")
        require(value.isFinite() && value >= 0.0 && value <= 1_000_000.0) { "$label invalid" }
        return value
    }

    private fun canonicalWeight(value: Double): String =
        if (value == 0.0) "" else java.math.BigDecimal.valueOf(value).stripTrailingZeros().toPlainString()
}
