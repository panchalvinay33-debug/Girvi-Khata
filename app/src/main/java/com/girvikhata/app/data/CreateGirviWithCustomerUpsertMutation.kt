package com.girvikhata.app.data

import com.girvikhata.app.domain.GirviInterestMetadata
import com.girvikhata.app.domain.InterestMode
import com.girvikhata.app.domain.InterestPeriodRule
import com.girvikhata.app.domain.InterestTerms

/** Atomic practical-entry mutation: customer upsert + new girvi create. */
data class CreateGirviWithCustomerUpsertMutation(
    val customer: CustomerRecord,
    val girvi: GirviRecord,
) : VerifiedBusinessMutation {
    override val auditLabel: String = "CUSTOMER_UPSERT_GIRVI_CREATE"

    override fun apply(snapshot: AppSnapshot): AppSnapshot {
        require(girvi.customerId == customer.id) { "Girvi customer identity mismatch" }
        require(customer.name.isNotBlank()) { "Customer name required" }
        require(snapshot.girvis.none { it.id == girvi.id }) { "Duplicate girvi ID" }
        require(snapshot.girvis.none { it.girviNumber == girvi.girviNumber }) { "Duplicate girvi number" }

        val storedGirvi = freezeInterestTerms(girvi)
        val customers = snapshot.customers.filterNot { it.id == customer.id } + customer
        return snapshot.copy(
            customers = customers.sortedBy { it.id },
            girvis = (snapshot.girvis + storedGirvi).sortedBy { it.id },
        )
    }

    private fun freezeInterestTerms(value: GirviRecord): GirviRecord {
        if (value.items.isEmpty()) return value
        val first = value.items.first()
        if (GirviInterestMetadata.read(first.description) != null) return value
        val legacyTerms = InterestTerms(
            mode = InterestMode.PERCENT_PER_MONTH,
            monthlyRateBasisPoints = value.monthlyRateBasisPoints,
            periodRule = InterestPeriodRule.COMPLETED_MONTHS_PLUS_DAYS,
        )
        val frozen = first.copy(
            description = GirviInterestMetadata.attach(first.description, legacyTerms),
        )
        return value.copy(items = listOf(frozen) + value.items.drop(1))
    }
}
