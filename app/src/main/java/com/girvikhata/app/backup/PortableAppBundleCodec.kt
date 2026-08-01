package com.girvikhata.app.backup

import com.girvikhata.app.data.AppSnapshot
import com.girvikhata.app.domain.MasterCatalog
import org.json.JSONObject
import java.util.Base64

/**
 * Portable bundle v2: business snapshot + master catalog + already device-encrypted media blobs.
 * Bundle v1 and legacy snapshot-only payloads remain readable.
 */
object PortableAppBundleCodec {
    private const val CURRENT_BUNDLE_VERSION = 2

    data class DecodedBundle(
        val snapshot: AppSnapshot,
        val masterCatalog: MasterCatalog,
        val containsPortableMasters: Boolean,
        val encryptedMedia: Map<String, ByteArray> = emptyMap(),
    )

    fun encode(snapshot: AppSnapshot, masterCatalog: MasterCatalog): ByteArray =
        encode(snapshot, masterCatalog, emptyMap())

    fun encode(
        snapshot: AppSnapshot,
        masterCatalog: MasterCatalog,
        encryptedMedia: Map<String, ByteArray>,
    ): ByteArray = JSONObject().apply {
        put("bundleVersion", CURRENT_BUNDLE_VERSION)
        put("snapshot", Base64.getEncoder().encodeToString(SnapshotPortableCodec.encode(snapshot)))
        put("masterCatalog", Base64.getEncoder().encodeToString(MasterCatalogPortableCodec.encode(masterCatalog)))
        put("media", JSONObject().apply {
            encryptedMedia.toSortedMap().forEach { (name, bytes) ->
                require(name.matches(Regex("[A-Za-z0-9._-]{1,140}\\.gkm"))) { "Invalid media backup name" }
                require(bytes.isNotEmpty()) { "Empty encrypted media" }
                put(name, Base64.getEncoder().encodeToString(bytes))
            }
        })
    }.toString().toByteArray(Charsets.UTF_8)

    fun decode(payload: ByteArray): DecodedBundle {
        require(payload.isNotEmpty()) { "Backup payload empty" }
        val root = runCatching { JSONObject(String(payload, Charsets.UTF_8)) }.getOrNull()
        if (root?.has("bundleVersion") != true) {
            return DecodedBundle(
                snapshot = SnapshotPortableCodec.decode(payload),
                masterCatalog = MasterCatalog(),
                containsPortableMasters = false,
                encryptedMedia = emptyMap(),
            )
        }

        val version = root.optInt("bundleVersion", 0)
        require(version in 1..CURRENT_BUNDLE_VERSION) { "Backup bundle version unsupported" }
        val snapshotBytes = decodeBase64(root.optString("snapshot"), "Business snapshot missing")
        val masterBytes = decodeBase64(root.optString("masterCatalog"), "Master catalog missing")
        val media = if (version >= 2) decodeMedia(root.optJSONObject("media")) else emptyMap()
        return DecodedBundle(
            snapshot = SnapshotPortableCodec.decode(snapshotBytes),
            masterCatalog = MasterCatalogPortableCodec.decode(masterBytes),
            containsPortableMasters = true,
            encryptedMedia = media,
        )
    }

    private fun decodeMedia(root: JSONObject?): Map<String, ByteArray> {
        if (root == null) return emptyMap()
        val result = linkedMapOf<String, ByteArray>()
        val keys = root.keys().asSequence().toList().sorted()
        keys.forEach { name ->
            require(name.matches(Regex("[A-Za-z0-9._-]{1,140}\\.gkm"))) { "Backup media name invalid" }
            val bytes = decodeBase64(root.optString(name), "Backup media missing")
            require(bytes.size <= 24 * 1024 * 1024) { "Backup media too large" }
            result[name] = bytes
        }
        return result
    }

    private fun decodeBase64(value: String, error: String): ByteArray {
        require(value.isNotBlank()) { error }
        return runCatching { Base64.getDecoder().decode(value) }
            .getOrElse { throw IllegalArgumentException("Backup bundle base64 damaged") }
            .also { require(it.isNotEmpty()) { error } }
    }
}
