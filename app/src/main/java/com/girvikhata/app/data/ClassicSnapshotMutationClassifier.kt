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

        val unrelatedBefore = before.customers.filterNot { it.id == customer.id }.toSet()
        val unrelatedNext = next.customers.filterNot { it.id == customer.id }.toSet()
        require(unrelatedNext == unrelatedBefore) {
            "Girvi create contains unrelated customer changes"
        }
        require(next.customers.count { it.id == customer.id } == 1) {
            "Girvi create customer identity duplicated"
        }
        require(next.customers.size == before.customers.size + if (existingCustomer == null) 1 else 0) {
            "Girvi create customer count invalid"
        }

        val mutation: VerifiedBusinessMutation = if (existingCustomer == null || existingCustomer == customer) {
            VerifiedBusinessMutation.CreateGirviWithCustomer(customer, girvi)
        } else {
            CreateGirviWithCustomerUpsertMutation(customer, girvi)
        }
        return Classified(
            mutation = mutation,
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
        if (next.customers != before.customers) return null

        val candidates = mutableListOf<Classified>()

        if (next.categories.size == before.categories.size + 1 && next.girvis == before.girvis) {
            val added = next.categories.filter { candidate -> before.categories.none { it.id == candidate.id } }
            if (added.size == 1) {
                val category = added.single()
                candidates += Classified(
                    mutation = VerifiedBusinessMutation.AddCategory(category),
                    title = "Classic category ${category.name} added",
                )
            }
        }

        if (next.categories.size == before.categories.size) {
            before.categories.forEach { category ->
                val updated = next.categories.firstOrNull { it.id == category.id }
                if (updated != null) {
                    if (updated.name != category.name) {
                        candidates += Classified(
                            mutation = RenameCategoryMutation(category.id, updated.name),
                            title = "Owner category ${category.name} renamed to ${updated.name}",
                        )
                    }
                    if (updated.active != category.active) {
                        candidates += Classified(
                            mutation = VerifiedBusinessMutation.SetCategoryActive(category.id, updated.active),
                            title = "Classic category ${category.name} ${if (updated.active) "activated" else "deactivated"}",
                        )
                    }
                }
                candidates += Classified(
                    mutation = ReorderCategoryMutation(category.id, -1),
                    title = "Owner category ${category.name} moved up",
                )
                candidates += Classified(
                    mutation = ReorderCategoryMutation(category.id, 1),
                    title = "Owner category ${category.name} moved down",
                )
            }
        }

        val matches = candidates.filter { candidate ->
            runCatching { candidate.mutation.apply(before) == next }.getOrDefault(false)
        }
        if (matches.isEmpty()) return null
        val auditLabels = matches.map { it.mutation.auditLabel }.distinct()
        require(auditLabels.size == 1) { "Ambiguous category snapshot change" }
        return matches.first()
    }
}
