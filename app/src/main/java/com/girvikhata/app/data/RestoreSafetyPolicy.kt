package com.girvikhata.app.data

/** Pure restore resource policy shared by UI and regression tests. */
object RestoreSafetyPolicy {
    const val MAX_PACKAGE_BYTES: Long = 128L * 1024L * 1024L
    const val MIN_FREE_BYTES: Long = 64L * 1024L * 1024L
    const val WORKSPACE_MULTIPLIER: Long = 3L

    data class Decision(
        val allowed: Boolean,
        val requiredBytes: Long,
        val freeBytes: Long,
        val reason: String? = null,
    )

    fun requiredBytes(packageBytes: Long): Long {
        require(packageBytes > 0L) { "Backup package empty hai" }
        require(packageBytes <= MAX_PACKAGE_BYTES) { "Backup package 128 MB limit se badi hai" }
        val multiplied = runCatching { Math.multiplyExact(packageBytes, WORKSPACE_MULTIPLIER) }
            .getOrElse { Long.MAX_VALUE }
        return maxOf(MIN_FREE_BYTES, multiplied)
    }

    fun evaluate(packageBytes: Long, freeBytes: Long): Decision {
        require(freeBytes >= 0L) { "Free storage invalid" }
        val required = requiredBytes(packageBytes)
        return if (freeBytes >= required) {
            Decision(true, required, freeBytes)
        } else {
            Decision(
                allowed = false,
                requiredBytes = required,
                freeBytes = freeBytes,
                reason = "Restore ke liye ${required / (1024L * 1024L)} MB free space chahiye; available ${freeBytes / (1024L * 1024L)} MB",
            )
        }
    }
}
