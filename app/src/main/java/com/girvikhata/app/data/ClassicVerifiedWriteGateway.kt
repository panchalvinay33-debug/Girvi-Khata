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
        executeVerified = { request -> coordinator.execute(request) },
    )

    fun persist(screenSnapshot: AppSnapshot, nextSnapshot: AppSnapshot): AppSnapshot {
        val classified = classifySafely(screenSnapshot, nextSnapshot)
        val practicalCreate = classified.mutation is CreateGirviWithCustomerUpsertMutation

        // Practical Entry can stay open while startup recovery, contact import, or another
        // activity refreshes the encrypted snapshot. Do not use that old screen fingerprint.
        // Build the transaction against the authoritative snapshot immediately before write.
        val authoritativeBase = if (practicalCreate) reloadAuthoritative() else screenSnapshot
        val initialRequest = VerifiedBusinessWriteRequest(
            expectedFingerprint = RelationalShadowFingerprint.sha256(authoritativeBase),
            mutation = classified.mutation,
            title = classified.title,
        )

        try {
            executeVerified(initialRequest)
        } catch (failure: Throwable) {
            val staleSnapshot = failure.message?.contains(
                "Business data changed before transaction",
                ignoreCase = true,
            ) == true
            if (!practicalCreate || !staleSnapshot) throw failure

            // One bounded retry handles a refresh that lands in the tiny interval between the
            // authoritative read above and coordinator execution. The coordinator validates and
            // repairs any interrupted practical-write intent before accepting this retry.
            val latest = reloadAuthoritative()
            executeVerified(
                VerifiedBusinessWriteRequest(
                    expectedFingerprint = RelationalShadowFingerprint.sha256(latest),
                    mutation = classified.mutation,
                    title = "${classified.title} • authoritative retry",
                ),
            )
        }
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
