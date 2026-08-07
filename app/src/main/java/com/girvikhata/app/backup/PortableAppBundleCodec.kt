package com.girvikhata.app.backup

import com.girvikhata.app.custody.CustodyPlacementSnapshot
import com.girvikhata.app.data.AppSnapshot
import com.girvikhata.app.domain.MasterCatalog
import org.json.JSONObject
import java.util.Base64

/**
 * Portable bundle v4:
 * - business snapshot
 * - master catalog
 * - portable photo bytes
 * - storage/custody/external placement snapshot
 *
 * v3 portable-media, v2 device-encrypted media and v1/legacy snapshot payloads remain readable.
 */
object PortableAppBundleCodec {
    private const val CURRENT_BUNDLE_VERSION = 4

    data class DecodedBundle(
        val snapshot: AppSnapshot,
        val masterCatalog: MasterCatalog,
        val containsPortableMasters: Boolean,
        val encryptedMedia: Map<String, ByteArray> = emptyMap(),
        val portableMedia: Map<String, ByteArray> = emptyMap(),
        val custodySnapshot: CustodyPlacementSnapshot = CustodyPlacementSnapshot(),
        val containsPortableCustody: Boolean = false,
    ) {
        val mediaCount: Int get() = if (portableMedia.isNotEmpty()) portableMedia.size else encryptedMedia.size
        val hasPortableMedia: Boolean get() = portableMedia.isNotEmpty()
    }

    fun encode(snapshot: AppSnapshot, masterCatalog: MasterCatalog): ByteArray =
        encodePortable(snapshot, masterCatalog, emptyMap(), CustodyPlacementSnapshot())

    /** Legacy v2 encoder retained for old tests/import tooling. New backup code should use encodePortable. */
    fun encode(
        snapshot: AppSnapshot,
        masterCatalog: MasterCatalog,
        encryptedMedia: Map<String, ByteArray>,
    ): ByteArray = JSONObject().apply {
        put("bundleVersion", 2)
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

    fun encodePortable(
        snapshot: AppSnapshot,
        masterCatalog: MasterCatalog,
        portableMedia: Map<String, ByteArray>,
        custodySnapshot: CustodyPlacementSnapshot = CustodyPlacementSnapshot(),
    ): ByteArray {
        PortableMediaSupport.validate(portableMedia)
        val custodyBytes = CustodyPortableCodec.encode(custodySnapshot)
        return JSONObject().apply {
            put("bundleVersion", CURRENT_BUNDLE_VERSION)
            put("snapshot", Base64.getEncoder().encodeToString(SnapshotPortableCodec.encode(snapshot)))
            put("masterCatalog", Base64.getEncoder().encodeToString(MasterCatalogPortableCodec.encode(masterCatalog)))
            put("custody", Base64.getEncoder().encodeToString(custodyBytes))
            put("portableMedia", JSONObject().apply {
                portableMedia.toSortedMap().forEach { (id, bytes) ->
                    put(id, Base64.getEncoder().encodeToString(bytes))
                }
            })
        }.toString().toByteArray(Charsets.UTF_8)
    }

    fun decode(payload: ByteArray): DecodedBundle {
        require(payload.isNotEmpty()) { "Backup payload empty" }
        val root = runCatching { JSONObject(String(payload, Charsets.UTF_8)) }.getOrNull()
        if (root?.has("bundleVersion") != true) {
            return DecodedBundle(
                snapshot = SnapshotPortableCodec.decode(payload),
                masterCatalog = MasterCatalog(),
                containsPortableMasters = false,
            )
        }

        val version = root.optInt("bundleVersion", 0)
        require(version in 1..CURRENT_BUNDLE_VERSION) { "Backup bundle version unsupported" }
        val snapshotBytes = decodeBase64(root.optString("snapshot"), "Business snapshot missing")
        val masterBytes = decodeBase64(root.optString("masterCatalog"), "Master catalog missing")
        val encrypted = if (version == 2) decodeLegacyEncryptedMedia(root.optJSONObject("media")) else emptyMap()
        val portable = if (version >= 3) decodePortableMedia(root.optJSONObject("portableMedia")) else emptyMap()
        val hasCustody = version >= 4 && root.optString("custody").isNotBlank()
        val custody = if (hasCustody) {
            CustodyPortableCodec.decode(decodeBase64(root.optString("custody"), "Custody placement missing"))
        } else {
            CustodyPlacementSnapshot()
        }
        return DecodedBundle(
            snapshot = SnapshotPortableCodec.decode(snapshotBytes),
            masterCatalog = MasterCatalogPortableCodec.decode(masterBytes),
            containsPortableMasters = true,
            encryptedMedia = encrypted,
            portableMedia = portable,
            custodySnapshot = custody,
            containsPortableCustody = hasCustody,
        )
    }

    private fun decodeLegacyEncryptedMedia(root: JSONObject?): Map<String, ByteArray> {
        if (root == null) return emptyMap()
        val result = linkedMapOf<String, ByteArray>()
        root.keys().asSequence().toList().sorted().forEach { name ->
            require(name.matches(Regex("[A-Za-z0-9._-]{1,140}\\.gkm"))) { "Backup media name invalid" }
            val bytes = decodeBase64(root.optString(name), "Backup media missing")
            require(bytes.size <= 24 * 1024 * 1024) { "Backup media too large" }
            result[name] = bytes
        }
        return result
    }

    private fun decodePortableMedia(root: JSONObject?): Map<String, ByteArray> {
        if (root == null) return emptyMap()
        val result = linkedMapOf<String, ByteArray>()
        root.keys().asSequence().toList().sorted().forEach { id ->
            require(id.matches(Regex("[A-Za-z0-9._-]{1,100}"))) { "Portable media id invalid" }
            result[id] = decodeBase64(root.optString(id), "Portable media missing")
        }
        PortableMediaSupport.validate(result)
        return result
    }

    private fun decodeBase64(value: String, error: String): ByteArray {
        require(value.isNotBlank()) { error }
        return runCatching { Base64.getDecoder().decode(value) }
            .getOrElse { throw IllegalArgumentException("Backup bundle base64 damaged") }
            .also { require(it.isNotEmpty()) { error } }
    }
}
