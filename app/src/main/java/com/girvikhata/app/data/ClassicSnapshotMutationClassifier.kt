package com.girvikhata.app.data

/**
 * Temporary migration bridge for the classic Compose UI.
 *
 * The legacy UI still produces a complete next snapshot. This classifier accepts only one
 * precisely recognised business change and converts it to a typed verified mutation.
 * Unknown, mixed or destructive snapshot changes fail closed.
 */
object ClassicSnapshotMutationClassifier {
    data class Classified(
        val mutation: VerifiedBusinessMutation,
        val title: String,
    )

    fun classify(before: AppSnapshot, next: AppSnapshot): Classified {
        require(before.schemaVersion == next.schemaVersion) { "Classic write cannot change schema" }

        classifyNewGirvi(before, next)?.let { return it }
        classifyGirviChange(before, next)?.let { return it }
        classifyCategoryChange(before, next)?.let { return it }

        error("Unsupported or mixed classic snapshot change; refresh and retry")
    }

    private fun classifyNewGirvi(before: AppSnapshot, next: AppSnapshot): Classified? {
        if (next.girvis.size != before.girvis.size + 1) return null
        require(next.categories == before.categories) { "Girvi create cannot change categories" }
        require(next.girvis.containsAll(before.girvis)) { "Girvi create cannot alter existing girvi" }

        val girvi = next.girvis.single { candidate -> before.girvis.none { it.id == candidate.id } }
        val customer = next.customers.firstOrNull { it.id == girvi.customerId }
            ?: error("New girvi customer missing")
        val existingCustomer = before.customers.firstOrNull { it.id == customer.id }
        val expectedCustomers = if (existingCustomer == null) before.customers + customer else before.customers
        require(next.customers.toSet() == expectedCustomers.toSet()) {
            "Girvi create contains unrelated customer changes"
        }
        require(existingCustomer == null || existingCustomer == customer) {
            "Existing customer changed during girvi create"
        }
        return Classified(
            mutation = VerifiedBusinessMutation.CreateGirviWithCustomer(customer, girvi),
            title = "Classic girvi ${girvi.girviNumber} created",
        )
    }

    private fun classifyGirviChange(before: AppSnapshot, next: AppSnapshot): Classified? {
        if (next.customers != before.customers || next.categories != before.categories) return null
        if (next.girvis.size != before.girvis.size) return null

        val changed = before.girvis.mapNotNull { old ->
            val updated = next.girvis.firstOrNull { it.id == old.id }
                ?: error("Classic flow cannot delete girvi")
            if (updated == old) null else old to updated
        }
        require(next.girvis.all { updated -> before.girvis.any { it.id == updated.id } }) {
            "Classic flow cannot replace girvi identity"
        }
        if (changed.isEmpty()) return null
        require(changed.size == 1) { "Classic flow must change exactly one girvi" }
        val (old, updated) = changed.single()

        if (updated.status == "RELEASED" && old.status == "ACTIVE") {
            return Classified(
                mutation = VerifiedBusinessMutation.ReleaseGirvi(updated),
                title = "Classic girvi ${old.girviNumber} released",
            )
        }

        require(updated.copy(payments = old.payments) == old) {
            "Unsupported classic girvi edit"
        }
        require(updated.payments.size == old.payments.size + 1) {
            "Classic ledger must append exactly one entry"
        }
        require(updated.payments.take(old.payments.size) == old.payments) {
            "Classic ledger history is immutable"
        }
        val payment = updated.payments.last()
        return if (payment.isReversal) {
            val originalPaymentId = payment.reversedPaymentId
                ?: error("Classic reversal original payment identity missing")
            Classified(
                mutation = VerifiedBusinessMutation.ReversePayment(old.id, originalPaymentId, payment),
                title = "Classic payment ${payment.receiptNumber} reversed",
            )
        } else {
            Classified(
                mutation = VerifiedBusinessMutation.AppendPayment(old.id, payment),
                title = "Classic payment ${payment.receiptNumber} received",
            )
        }
    }

    private fun classifyCategoryChange(before: AppSnapshot, next: AppSnapshot): Classified? {
        require(next.customers == before.customers && next.girvis == before.girvis) {
            "Category change cannot alter business records"
        }

        if (next.categories.size == before.categories.size + 1) {
            require(next.categories.containsAll(before.categories)) { "Category add altered existing categories" }
            val category = next.categories.single { candidate -> before.categories.none { it.id == candidate.id } }
            return Classified(
                mutation = VerifiedBusinessMutation.AddCategory(category),
                title = "Classic category ${category.name} added",
            )
        }

        if (next.categories.size == before.categories.size) {
            val changed = before.categories.mapNotNull { old ->
                val updated = next.categories.firstOrNull { it.id == old.id }
                    ?: error("Classic category deletion is blocked")
                if (updated == old) null else old to updated
            }
            if (changed.isEmpty()) return null
            require(changed.size == 1) { "Classic flow must change exactly one category" }
            val (old, updated) = changed.single()
            require(updated.name == old.name) { "Classic category rename requires Owner Settings" }
            require(updated.active != old.active) { "Unsupported classic category change" }
            return Classified(
                mutation = VerifiedBusinessMutation.SetCategoryActive(old.id, updated.active),
                title = "Classic category ${old.name} ${if (updated.active) "activated" else "deactivated"}",
            )
        }

        return null
    }
}
