package com.girvikhata.app.data

/** Pure validation helpers used by tests and future database migration tooling. */
object RecordStoreSafetyPolicy {
    const val maxSafetyCopies: Int = 5
    const val maxQuarantineCopies: Int = 2
    const val minIvBytes: Int = 12
    const val maxIvBytes: Int = 32
    const val minCiphertextBytes: Int = 16
    const val maxCiphertextBytes: Int = 128 * 1024 * 1024

    fun validateEnvelopeLengths(ivLength: Int, ciphertextLength: Int) {
        require(ivLength in minIvBytes..maxIvBytes) { "Encrypted store IV length invalid" }
        require(ciphertextLength in minCiphertextBytes..maxCiphertextBytes) { "Encrypted store payload length invalid" }
    }

    fun validateSnapshot(snapshot: AppSnapshot) {
        require(snapshot.schemaVersion == 3) { "Unsupported local schema" }
        require(snapshot.customers.map { it.id }.distinct().size == snapshot.customers.size) { "Duplicate customer ID" }
        require(snapshot.girvis.map { it.id }.distinct().size == snapshot.girvis.size) { "Duplicate girvi ID" }
        require(snapshot.girvis.map { it.girviNumber }.distinct().size == snapshot.girvis.size) { "Duplicate girvi number" }
        val customerIds = snapshot.customers.map { it.id }.toSet()
        require(snapshot.girvis.all { it.customerId in customerIds }) { "Girvi customer link missing" }
        require(snapshot.girvis.all { it.principalPaise > 0L && it.createdAt > 0L }) { "Girvi amount/timestamp invalid" }
        require(snapshot.girvis.all { it.status in setOf("ACTIVE", "RELEASED") }) { "Girvi status invalid" }
    }

    fun <T> newestFirstRetained(items: List<T>, modifiedAt: (T) -> Long, keep: Int): List<T> {
        require(keep >= 0)
        return items.sortedByDescending(modifiedAt).take(keep)
    }
}