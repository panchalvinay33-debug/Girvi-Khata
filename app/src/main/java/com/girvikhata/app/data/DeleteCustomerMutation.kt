package com.girvikhata.app.data

/**
 * Hard-delete is intentionally limited to unused customer rows.
 * Financial history must never be cascaded or silently removed.
 */
data class DeleteUnusedCustomerMutation(
    val customerId: String,
) : VerifiedBusinessMutation {
    override val auditLabel: String = "CUSTOMER_DELETE"

    override fun apply(snapshot: AppSnapshot): AppSnapshot {
        require(snapshot.customers.any { it.id == customerId }) { "Customer nahi mila" }
        val linked = snapshot.girvis.count { it.customerId == customerId }
        require(linked == 0) {
            "Is customer ke $linked Girvi record hain. Financial history wale customer ko delete nahi kiya ja sakta."
        }
        return snapshot.copy(customers = snapshot.customers.filterNot { it.id == customerId })
    }
}
