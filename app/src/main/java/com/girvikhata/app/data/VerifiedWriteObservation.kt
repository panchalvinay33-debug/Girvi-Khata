package com.girvikhata.app.data

/** Evidence policy for eventual relational write cutover. */
data class VerifiedWriteObservation(
    val successfulWrites: Int,
    val lastTransactionId: String? = null,
    val lastCommittedAt: Long? = null,
    val lastSnapshotFingerprint: String? = null,
    val lastRelationalFingerprint: String? = null,
    val lastFailureAt: Long? = null,
    val lastFailureReason: String? = null,
) {
    val fingerprintsMatch: Boolean
        get() = !lastSnapshotFingerprint.isNullOrBlank() &&
            lastSnapshotFingerprint == lastRelationalFingerprint
}

object VerifiedWriteCutoverPolicy {
    const val MINIMUM_COORDINATED_WRITES = 25

    fun blockers(observation: VerifiedWriteObservation): List<String> = buildList {
        if (observation.successfulWrites < MINIMUM_COORDINATED_WRITES) {
            add("Verified coordinated writes ${observation.successfulWrites}/$MINIMUM_COORDINATED_WRITES")
        }
        if (!observation.fingerprintsMatch) add("Latest coordinated write fingerprint proof missing")
        if (observation.lastFailureAt != null &&
            (observation.lastCommittedAt == null || observation.lastFailureAt > observation.lastCommittedAt)
        ) {
            add("Latest coordinated write attempt failed")
        }
    }
}
