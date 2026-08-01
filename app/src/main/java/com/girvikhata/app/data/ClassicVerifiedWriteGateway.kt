package com.girvikhata.app.data

/**
 * Narrow bridge used while the classic Compose UI still emits a complete next snapshot.
 * Every accepted change is converted to a typed mutation and executed by the verified coordinator.
 * The authoritative snapshot is always reloaded after success.
 */
class ClassicVerifiedWriteGateway internal constructor(
    private val reloadAuthoritative: () -> AppSnapshot,
    private val executeVerified: (VerifiedBusinessWriteRequest) -> Unit,
) {
    constructor(
        records: EncryptedRecordStore,
        coordinator: VerifiedBusinessWriteCoordinator,
    ) : this(
        reloadAuthoritative = records::load,
        executeVerified = { request ->
            try {
                coordinator.execute(request)
            } catch (failure: Throwable) {
                val practicalCreate = request.mutation is CreateGirviWithCustomerUpsertMutation
                val staleSnapshot = failure.message?.contains(
                    "Business data changed before transaction",
                    ignoreCase = true,
                ) == true
                if (!practicalCreate || !staleSnapshot) throw failure

                // The first rejected attempt may have left a PENDING intent. The coordinator's
                // recovery path reconciles that metadata before this one-time retry. No business
                // rows are discarded; the encrypted authoritative snapshot remains source of truth.
                val latest = records.load()
                coordinator.execute(
                    VerifiedBusinessWriteRequest(
                        expectedFingerprint = RelationalShadowFingerprint.sha256(latest),
                        mutation = request.mutation,
                        title = "${request.title} • authoritative retry",
                    ),
                )
            }
        },
    )

    fun persist(screenSnapshot: AppSnapshot, nextSnapshot: AppSnapshot): AppSnapshot {
        val classified = classifySafely(screenSnapshot, nextSnapshot)
        executeVerified(
            VerifiedBusinessWriteRequest(
                expectedFingerprint = RelationalShadowFingerprint.sha256(screenSnapshot),
                mutation = classified.mutation,
                title = classified.title,
            ),
        )
        return reloadAuthoritative()
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
