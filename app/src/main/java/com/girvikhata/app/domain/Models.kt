package com.girvikhata.app.domain

import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

typealias Money = BigDecimal

enum class RecordStatus { DRAFT, ACTIVE, PARTIALLY_PAID, DUE, OVERDUE, READY_FOR_RELEASE, CLOSED, CANCELLED }
enum class PaymentMode { CASH, UPI, BANK_TRANSFER, MIXED, OTHER }
enum class PaymentKind { INTEREST_ONLY, PRINCIPAL_ONLY, MIXED, PARTIAL, FULL_SETTLEMENT, REVERSAL }
enum class InterestKind { SIMPLE_MONTHLY_PERCENT, SIMPLE_DAILY_PERCENT, SIMPLE_YEARLY_PERCENT, FIXED_MONTHLY, FIXED_PERIOD, COMPOUND, MANUAL, NONE }
enum class PartialMonthRule { EXACT_DAYS_30, EXTRA_DAY_FULL_MONTH, HALF_MONTH_SLAB, GRACE_THEN_FULL_MONTH, MANUAL }
enum class RoundingRule { NONE, NEAREST_RUPEE, NEAREST_FIVE, NEAREST_TEN, ROUND_UP_RUPEE, ROUND_DOWN_RUPEE }

data class ShopProfile(
    val id: String = "owner-shop",
    val shopName: String,
    val ownerName: String,
    val mobile: String = "",
    val address: String = "",
    val receiptLanguage: String = "hi",
)

data class Category(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val isActive: Boolean = true,
    val requiresWeight: Boolean = true,
    val requiresQuantity: Boolean = true,
    val photoRequired: Boolean = false,
    val sortOrder: Int = 0,
)

data class ItemMaster(
    val id: String = UUID.randomUUID().toString(),
    val categoryId: String,
    val name: String,
    val defaultUnit: String = "gram",
    val isActive: Boolean = true,
    val customFieldKeys: List<String> = emptyList(),
)

data class Customer(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val mobile: String,
    val alternateMobile: String = "",
    val relativeName: String = "",
    val address: String = "",
    val villageCity: String = "",
    val photoVaultId: String? = null,
    val documentVaultIds: List<String> = emptyList(),
    val notes: String = "",
    val createdOn: LocalDate = LocalDate.now(),
)

data class Weight(
    val kilograms: BigDecimal = BigDecimal.ZERO,
    val grams: BigDecimal = BigDecimal.ZERO,
    val deductionGrams: BigDecimal = BigDecimal.ZERO,
) {
    init {
        require(kilograms >= BigDecimal.ZERO)
        require(grams >= BigDecimal.ZERO)
        require(deductionGrams >= BigDecimal.ZERO)
    }

    val grossGrams: BigDecimal get() = kilograms.multiply(BigDecimal(1000)).add(grams)
    val netGrams: BigDecimal get() = grossGrams.subtract(deductionGrams).max(BigDecimal.ZERO)
}

data class GirviItem(
    val id: String = UUID.randomUUID().toString(),
    val categoryId: String,
    val itemMasterId: String? = null,
    val manualName: String? = null,
    val quantity: BigDecimal = BigDecimal.ONE,
    val unit: String = "piece",
    val weight: Weight? = null,
    val description: String = "",
    val condition: String = "",
    val customFields: Map<String, String> = emptyMap(),
    val mediaVaultIds: List<String> = emptyList(),
) {
    init {
        require(itemMasterId != null || !manualName.isNullOrBlank()) { "List item or manual item is required" }
        require(quantity > BigDecimal.ZERO)
    }
}

data class InterestPlan(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val kind: InterestKind,
    val ratePercent: BigDecimal = BigDecimal.ZERO,
    val fixedAmount: Money = BigDecimal.ZERO,
    val compoundEveryMonths: Int = 0,
    val partialMonthRule: PartialMonthRule = PartialMonthRule.EXTRA_DAY_FULL_MONTH,
    val graceDays: Int = 0,
    val roundingRule: RoundingRule = RoundingRule.NEAREST_RUPEE,
    val isActive: Boolean = true,
) {
    init {
        require(ratePercent >= BigDecimal.ZERO)
        require(fixedAmount >= BigDecimal.ZERO)
        require(compoundEveryMonths >= 0)
        require(graceDays >= 0)
        if (kind == InterestKind.COMPOUND) require(compoundEveryMonths > 0)
    }
}

data class GirviAccount(
    val id: String = UUID.randomUUID().toString(),
    val girviNumber: String,
    val customerId: String,
    val items: List<GirviItem>,
    val originalPrincipal: Money,
    val currentPrincipal: Money = originalPrincipal,
    val startDate: LocalDate,
    val expectedDueDate: LocalDate? = null,
    val interestPlanSnapshot: InterestPlan,
    val paymentMode: PaymentMode,
    val transactionReference: String = "",
    val storageLocation: String = "",
    val status: RecordStatus = RecordStatus.ACTIVE,
    val notes: String = "",
) {
    init {
        require(items.isNotEmpty())
        require(originalPrincipal > BigDecimal.ZERO)
        require(currentPrincipal >= BigDecimal.ZERO)
    }
}

data class Payment(
    val id: String = UUID.randomUUID().toString(),
    val girviId: String,
    val date: LocalDate,
    val kind: PaymentKind,
    val totalAmount: Money,
    val principalAmount: Money = BigDecimal.ZERO,
    val interestAmount: Money = BigDecimal.ZERO,
    val chargeAmount: Money = BigDecimal.ZERO,
    val mode: PaymentMode,
    val reference: String = "",
    val reversesPaymentId: String? = null,
    val note: String = "",
) {
    init {
        require(totalAmount >= BigDecimal.ZERO)
        require(principalAmount >= BigDecimal.ZERO)
        require(interestAmount >= BigDecimal.ZERO)
        require(chargeAmount >= BigDecimal.ZERO)
        require(principalAmount.add(interestAmount).add(chargeAmount).compareTo(totalAmount) == 0)
        if (kind == PaymentKind.REVERSAL) require(!reversesPaymentId.isNullOrBlank())
    }
}
