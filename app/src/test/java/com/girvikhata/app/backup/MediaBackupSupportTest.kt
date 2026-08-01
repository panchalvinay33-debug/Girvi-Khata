package com.girvikhata.app.backup

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files

class MediaBackupSupportTest {
    @Test
    fun `collect and restore preserve encrypted media bytes exactly`() {
        val root = Files.createTempDirectory("gk-media-test").toFile()
        try {
            val mediaDir = root.resolve("secure_media_v1").apply { mkdirs() }
            mediaDir.resolve("customer-c1.gkm").writeBytes(byteArrayOf(1, 2, 3, 4))
            mediaDir.resolve("item-i1.gkm").writeBytes(byteArrayOf(9, 8, 7))

            val collected = MediaBackupSupport.collect(root)
            assertEquals(setOf("customer-c1.gkm", "item-i1.gkm"), collected.keys)

            mediaDir.deleteRecursively()
            MediaBackupSupport.restore(root, collected)
            val restored = MediaBackupSupport.collect(root)

            assertEquals(collected.keys, restored.keys)
            collected.forEach { (name, bytes) -> assertArrayEquals(bytes, restored.getValue(name)) }
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun `restore rolls exact set and removes media absent from backup`() {
        val root = Files.createTempDirectory("gk-media-test").toFile()
        try {
            val mediaDir = root.resolve("secure_media_v1").apply { mkdirs() }
            mediaDir.resolve("old.gkm").writeBytes(byteArrayOf(5))

            MediaBackupSupport.restore(root, mapOf("new.gkm" to byteArrayOf(6, 7)))
            val restored = MediaBackupSupport.collect(root)

            assertEquals(setOf("new.gkm"), restored.keys)
            assertTrue(!root.resolve("secure_media_v1/old.gkm").exists())
        } finally {
            root.deleteRecursively()
        }
    }
}
