package com.girvikhata.app.backup

import com.girvikhata.app.data.AppSnapshot
import com.girvikhata.app.domain.MasterCatalog
import org.json.JSONObject
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Base64

class PortableAppBundleCodecV2Test {
    @Test
    fun `v2 round trip preserves encrypted media bytes`() {
        val snapshot = AppSnapshot.defaults()
        val masters = MasterCatalog()
        val media = linkedMapOf(
            "customer-c1.gkm" to byteArrayOf(1, 2, 3),
            "item-i1.gkm" to byteArrayOf(9, 8, 7, 6),
        )

        val decoded = PortableAppBundleCodec.decode(PortableAppBundleCodec.encode(snapshot, masters, media))

        assertEquals(snapshot, decoded.snapshot)
        assertEquals(masters, decoded.masterCatalog)
        assertTrue(decoded.containsPortableMasters)
        assertEquals(media.keys, decoded.encryptedMedia.keys)
        media.forEach { (name, bytes) -> assertArrayEquals(bytes, decoded.encryptedMedia.getValue(name)) }
    }

    @Test
    fun `bundle v1 remains readable with no media`() {
        val snapshot = AppSnapshot.defaults()
        val masters = MasterCatalog()
        val payload = JSONObject().apply {
            put("bundleVersion", 1)
            put("snapshot", Base64.getEncoder().encodeToString(SnapshotPortableCodec.encode(snapshot)))
            put("masterCatalog", Base64.getEncoder().encodeToString(MasterCatalogPortableCodec.encode(masters)))
        }.toString().toByteArray()

        val decoded = PortableAppBundleCodec.decode(payload)

        assertEquals(snapshot, decoded.snapshot)
        assertEquals(masters, decoded.masterCatalog)
        assertTrue(decoded.encryptedMedia.isEmpty())
    }

    @Test
    fun `invalid media filename fails closed`() {
        val snapshot = AppSnapshot.defaults()
        val masters = MasterCatalog()
        val failure = runCatching {
            PortableAppBundleCodec.encode(snapshot, masters, mapOf("../photo.gkm" to byteArrayOf(1)))
        }.exceptionOrNull()

        assertTrue(failure is IllegalArgumentException)
    }
}
