package com.girvikhata.app.data

/**
 * Compatibility bridge while legacy Compose screens still emit a complete next snapshot.
 *
 * The rebuild keeps classification here, but normal business writes no longer depend on a stale
 * UI fingerprint or a PENDING transaction journal. The typed mutation is applied to the latest
 * encrypted snapshot by [AuthoritativeBusinessWriter], then read back and verified.
 */
class ClassicVerifiedWriteGateway internal constructor(
    private val reloadAuthoritative: () -> AppSnapshot,
    private val executeMutation: (VerifiedBusinessMutation) -> AppSnapshot,
) {
    constructor(
        records: EncryptedRecordStore,
        @Suppress("UNUSED_PARAMETER") coordinator: VerifiedBusinessWriteCoordinator,
    ) : this(
        reloadAuthoritative = records::load,
        executeMutation = AuthoritativeBusinessWriter(
            records = records,
            shadowFactory = null,
        )::execute,
    )

    fun persist(screenSnapshot: AppSnapshot, nextSnapshot: AppSnapshot): AppSnapshot {
        val classified = classifySafely(screenSnapshot, nextSnapshot)
        return executeMutation(classified.mutation)
    }

    private fun classifySafely(before: AppSnapshot, next: AppSnapshot): ClassicSnapshotMutationClassifier.Classified {
        if (next.girvis.size == before.girvis.size + 1) {
            require(before.schemaVersion == next.schemaVersion) { "Girvi create cannot change schema" }
            require(next.categories == before.categories) { "Girvi create cannot change categories" }
            require(next.girvis.containsAll(before.girvis)) { "Girvi create cannot alter existing girvi" }

            val girvi = next.girvis.single { candidate -> before.girvis.none { it.id == candidate.id } }
            val customer = next.customers.firstOrNull { it.id == girvi.customerId }
                ?: error("New girvi customer missing")

            val unrelatedBefore = before.customers.filterNot { it.id == customer.id }.toSet()
            val unrelatedNext = next.customers.filterNot { it.id == customer.id }.toSet()
            require(unrelatedNext == unrelatedBefore) { "Girvi create contains unrelated customer changes" }
            require(next.customers.count { it.id == customer.id } == 1) { "Girvi create customer identity duplicated" }

            return ClassicSnapshotMutationClassifier.Classified(
                mutation = CreateGirviWithCustomerUpsertMutation(customer, girvi),
                title = "Practical girvi ${girvi.girviNumber} created",
            )
        }

        return ClassicSnapshotMutationClassifier.classify(before, next)
    }
}
