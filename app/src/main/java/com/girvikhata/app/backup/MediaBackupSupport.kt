package com.girvikhata.app.backup

import java.io.File

/**
 * Collects/restores already encrypted .gkm blobs for portable backup.
 * The media bytes stay encrypted end-to-end; this helper never decrypts photos.
 */
object MediaBackupSupport {
    private const val MEDIA_DIR = "secure_media_v1"
    private const val STAGING_DIR = "secure_media_restore_staging"
    private const val SAFETY_DIR = "secure_media_restore_safety"
    private const val MAX_FILE_BYTES = 24L * 1024L * 1024L
    private const val MAX_TOTAL_BYTES = 96L * 1024L * 1024L
    private val NAME = Regex("[A-Za-z0-9._-]{1,140}\\.gkm")

    fun collect(filesDir: File): Map<String, ByteArray> {
        val root = File(filesDir, MEDIA_DIR)
        if (!root.exists()) return emptyMap()
        var total = 0L
        return root.listFiles()
            ?.filter { it.isFile && NAME.matches(it.name) }
            ?.sortedBy { it.name }
            ?.associate { file ->
                require(file.length() in 1..MAX_FILE_BYTES) { "Encrypted media size invalid: ${file.name}" }
                total = Math.addExact(total, file.length())
                require(total <= MAX_TOTAL_BYTES) { "Encrypted media backup too large" }
                file.name to file.readBytes()
            }
            .orEmpty()
    }

    /**
     * Atomically replaces the current encrypted media directory with the verified backup set.
     * Existing media is retained as a short-lived safety generation until the new set verifies.
     */
    fun restore(filesDir: File, media: Map<String, ByteArray>) {
        validate(media)
        val root = File(filesDir, MEDIA_DIR)
        val staging = File(filesDir, STAGING_DIR)
        if (staging.exists()) check(staging.deleteRecursively()) { "Old media staging cleanup failed" }
        check(staging.mkdirs()) { "Media restore staging create failed" }

        media.toSortedMap().forEach { (name, bytes) ->
            val target = File(staging, name)
            target.writeBytes(bytes)
            require(target.readBytes().contentEquals(bytes)) { "Media staging verification failed: $name" }
        }

        val safetyParent = File(filesDir, SAFETY_DIR).apply { mkdirs() }
        val safety = File(safetyParent, "media-${System.currentTimeMillis()}")
        var movedOld = false
        try {
            if (root.exists()) {
                check(root.renameTo(safety)) { "Current encrypted media safety move failed" }
                movedOld = true
            }
            check(staging.renameTo(root)) { "Encrypted media activation failed" }
            val verified = collect(filesDir)
            require(sameBytes(verified, media)) { "Restored encrypted media verification failed" }
            if (movedOld) safety.deleteRecursively()
            pruneSafety(safetyParent)
        } catch (failure: Throwable) {
            if (root.exists()) root.deleteRecursively()
            if (movedOld && safety.exists()) safety.renameTo(root)
            if (staging.exists()) staging.deleteRecursively()
            throw failure
        }
    }

    fun validate(media: Map<String, ByteArray>) {
        var total = 0L
        media.forEach { (name, bytes) ->
            require(NAME.matches(name)) { "Backup media name invalid" }
            require(bytes.isNotEmpty() && bytes.size.toLong() <= MAX_FILE_BYTES) { "Backup media size invalid: $name" }
            total = Math.addExact(total, bytes.size.toLong())
            require(total <= MAX_TOTAL_BYTES) { "Backup media total too large" }
        }
    }

    private fun sameBytes(a: Map<String, ByteArray>, b: Map<String, ByteArray>): Boolean {
        if (a.keys != b.keys) return false
        return a.all { (name, bytes) -> b[name]?.contentEquals(bytes) == true }
    }

    private fun pruneSafety(parent: File) {
        parent.listFiles()
            ?.filter(File::isDirectory)
            ?.sortedByDescending(File::lastModified)
            ?.drop(2)
            ?.forEach(File::deleteRecursively)
    }
}
