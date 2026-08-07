package com.girvikhata.app.data

/** Updates customer identity fields and keeps denormalized Girvi customer names in sync. */
data class UpdateCustomerProfileMutation(
    val customer: CustomerRecord,
) : VerifiedBusinessMutation {
    override val auditLabel: String = "CUSTOMER_EDIT"

    override fun apply(snapshot: AppSnapshot): AppSnapshot {
        require(snapshot.customers.any { it.id == customer.id }) { "Customer nahi mila" }
        val cleanName = customer.name.trim().replace(Regex("\\s+"), " ")
        require(cleanName.isNotBlank()) { "Customer name required" }
        val normalized = customer.copy(
            name = cleanName.take(80),
            mobile = customer.mobile.filter(Char::isDigit).take(10),
            address = customer.address.trim().take(180),
        )
        return snapshot.copy(
            customers = snapshot.customers.map { if (it.id == customer.id) normalized else it },
            girvis = snapshot.girvis.map {
                if (it.customerId == customer.id) it.copy(customerName = normalized.name) else it
            },
        )
    }
}
