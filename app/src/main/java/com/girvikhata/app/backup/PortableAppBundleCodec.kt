package com.girvikhata.app.backup

import android.util.Base64
import com.girvikhata.app.data.AppSnapshot
import com.girvikhata.app.domain.MasterCatalog
import org.json.JSONObject

/** New backups wrap business records and master catalog; legacy snapshot-only payloads remain readable. */
object PortableAppBundleCodec {
    private const val BUNDLE_VERSION = 1

    data class DecodedBundle(
        val snapshot: AppSnapshot,
        val masterCatalog: MasterCatalog,
        val containsPortableMasters: Boolean,
    )

    fun encode(snapshot: AppSnapshot, masterCatalog: MasterCatalog): ByteArray = JSONObject().apply {
        put("bundleVersion", BUNDLE_VERSION)
        put("snapshot", Base64.encodeToString(SnapshotPortableCodec.encode(snapshot), Base64.NO_WRAP))
        put("masterCatalog", Base64.encodeToString(MasterCatalogPortableCodec.encode(masterCatalog), Base64.NO_WRAP))
    }.toString().toByteArray(Charsets.UTF_8)

    fun decode(payload: ByteArray): DecodedBundle {
        require(payload.isNotEmpty()) { "Backup payload empty" }
        val text = String(payload, Charsets.UTF_8)
        val root = runCatching { JSONObject(text) }.getOrNull()
        if (root?.has("bundleVersion") != true) {
            return DecodedBundle(
                snapshot = SnapshotPortableCodec.decode(payload),
                masterCatalog = MasterCatalog(),
                containsPortableMasters = false,
            )
        }
        require(root.optInt("bundleVersion", 0) == BUNDLE_VERSION) { "Backup bundle version unsupported" }
        val snapshotBytes = decodeBase64(root.optString("snapshot"), "Business snapshot missing")
        val masterBytes = decodeBase64(root.optString("masterCatalog"), "Master catalog missing")
        return DecodedBundle(
            snapshot = SnapshotPortableCodec.decode(snapshotBytes),
            masterCatalog = MasterCatalogPortableCodec.decode(masterBytes),
            containsPortableMasters = true,
        )
    }

    private fun decodeBase64(value: String, error: String): ByteArray {
        require(value.isNotBlank()) { error }
        return runCatching { Base64.decode(value, Base64.DEFAULT) }
            .getOrElse { throw IllegalArgumentException("Backup bundle base64 damaged") }
            .also { require(it.isNotEmpty()) { error } }
    }
}
