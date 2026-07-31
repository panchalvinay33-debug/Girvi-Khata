package com.girvikhata.app.data

/**
 * Narrow bridge used while the classic Compose UI still emits a complete next snapshot.
 * Every accepted change is first classified into a typed mutation and then executed by the
 * verified coordinator. The authoritative snapshot is always reloaded after success.
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
        val classified = ClassicSnapshotMutationClassifier.classify(screenSnapshot, nextSnapshot)
        executeVerified(
            VerifiedBusinessWriteRequest(
                expectedFingerprint = RelationalShadowFingerprint.sha256(screenSnapshot),
                mutation = classified.mutation,
                title = classified.title,
            ),
        )
        return reloadAuthoritative()
    }
}
