package com.girvikhata.app.data

/**
 * Atomic mutation for the practical-entry flow when a matched existing customer is edited
 * (for example after importing a phone contact) at the same time as a new girvi is created.
 *
 * Only the girvi-linked customer may be inserted/updated; no other customer or existing girvi
 * may change through this mutation.
 */
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

        val customers = snapshot.customers.filterNot { it.id == customer.id } + customer
        return snapshot.copy(
            customers = customers.sortedBy { it.id },
            girvis = (snapshot.girvis + girvi).sortedBy { it.id },
        )
    }
}
