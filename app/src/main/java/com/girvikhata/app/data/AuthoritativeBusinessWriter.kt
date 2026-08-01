package com.girvikhata.app.data

/**
 * Blueprint rebuild write path.
 *
 * The encrypted snapshot is the authoritative business store. A successful business write is
 * defined by: load -> typed mutation -> encrypted atomic save -> encrypted read-back equality.
 * The relational database remains a verification/search mirror and is never allowed to reject
 * an otherwise verified authoritative write.
 *
 * This deliberately removes the old screen-fingerprint/PENDING-intent feedback loop from normal
 * shop-floor writes. Restore remains handled by its dedicated fail-closed restore workflow.
 */
class AuthoritativeBusinessWriter(
    private val records: EncryptedRecordStore,
    private val shadowFactory: (() -> EncryptedRelationalShadowStore)? = null,
) {
    @Synchronized
    fun execute(mutation: VerifiedBusinessMutation): AppSnapshot {
        val before = records.load()
        val target = mutation.apply(before)
        require(target != before) { "Business operation produced no change" }

        records.save(target)
        val persisted = records.load()
        check(persisted == target) { "Authoritative encrypted read-back mismatch" }

        // Shadow health is useful evidence, not authority. A damaged/stale shadow must never make
        // the shopkeeper lose a valid encrypted write. It can be rebuilt from the snapshot later.
        runCatching {
            shadowFactory?.invoke()?.use { shadow ->
                shadow.syncIncremental(persisted)
                val dual = shadow.dualReadComparison(persisted)
                if (!dual.matches) shadow.replaceAll(persisted)
            }
        }

        return persisted
    }
}
