package com.girvikhata.app.backup

import android.content.Context
import com.girvikhata.app.data.SecureMediaVault

/**
 * Converts device-bound media encryption into portable plaintext bytes only inside the already
 * recovery-key encrypted .gkb payload. On restore these bytes are re-encrypted with the new device key.
 */
object PortableMediaSupport {
    private const val MAX_MEDIA_COUNT = 5_000
    private const val MAX_MEDIA_BYTES = 20 * 1024 * 1024
    private const val MAX_TOTAL_BYTES = 120 * 1024 * 1024

    fun collect(context: Context): Map<String, ByteArray> {
        val vault = SecureMediaVault(context.applicationContext)
        val ids = vault.allMediaIds()
        require(ids.size <= MAX_MEDIA_COUNT) { "Too many photos for portable backup" }
        var total = 0L
        val result = linkedMapOf<String, ByteArray>()
        ids.forEach { id ->
            require(id.matches(Regex("[A-Za-z0-9._-]{1,100}"))) { "Invalid portable media id" }
            val bytes = vault.readPhoto(id)
            require(bytes.size in 1..MAX_MEDIA_BYTES) { "Portable photo size invalid" }
            total += bytes.size
            require(total <= MAX_TOTAL_BYTES) { "Portable photo backup too large" }
            result[id] = bytes
        }
        return result
    }

    fun validate(media: Map<String, ByteArray>) {
        require(media.size <= MAX_MEDIA_COUNT) { "Too many portable photos" }
        var total = 0L
        media.forEach { (id, bytes) ->
            require(id.matches(Regex("[A-Za-z0-9._-]{1,100}"))) { "Portable media id invalid" }
            require(bytes.size in 1..MAX_MEDIA_BYTES) { "Portable media size invalid" }
            total += bytes.size
            require(total <= MAX_TOTAL_BYTES) { "Portable media total too large" }
        }
    }

    fun restore(context: Context, media: Map<String, ByteArray>) {
        validate(media)
        val vault = SecureMediaVault(context.applicationContext)
        val previousIds = vault.allMediaIds().toSet()
        val targetIds = media.keys
        media.toSortedMap().forEach { (id, bytes) -> vault.importPhotoBytes(id, bytes) }
        (previousIds - targetIds).forEach(vault::delete)
        require(vault.allMediaIds().toSet() == targetIds) { "Portable media restore count mismatch" }
        media.forEach { (id, expected) ->
            val actual = vault.readPhoto(id)
            try { require(actual.contentEquals(expected)) { "Portable media restore verification failed" } }
            finally { actual.fill(0) }
        }
    }

    fun clear(media: Map<String, ByteArray>) = media.values.forEach { it.fill(0) }
}
