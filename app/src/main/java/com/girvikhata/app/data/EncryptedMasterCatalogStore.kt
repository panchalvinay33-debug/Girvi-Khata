package com.girvikhata.app.data

import android.content.Context
import com.girvikhata.app.domain.MasterCatalog
import com.girvikhata.app.domain.MasterEntry
import com.girvikhata.app.domain.MasterKind
import com.girvikhata.app.security.DeviceKeyManager
import com.girvikhata.app.security.EncryptedPayload
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileOutputStream

class EncryptedMasterCatalogStore(
    context: Context,
    private val keyManager: DeviceKeyManager = DeviceKeyManager(),
) {
    private val file = File(context.filesDir, FILE_NAME)

    @Synchronized
    fun load(): MasterCatalog {
        if (!file.exists()) {
            val defaults = MasterCatalog()
            save(defaults)
            return defaults
        }
        return read(file)
    }

    @Synchronized
    fun save(catalog: MasterCatalog) {
        validate(catalog)
        val temp = File(file.parentFile, "$FILE_NAME.tmp")
        runCatching {
            write(temp, catalog)
            check(read(temp) == catalog) { "Master catalog read-back failed" }
            if (file.exists() && !file.delete()) error("Old master catalog replace nahi hua")
            if (!temp.renameTo(file)) {
                temp.copyTo(file, overwrite = true)
                check(temp.delete()) { "Master catalog temp cleanup failed" }
            }
            check(read(file) == catalog) { "Master catalog final verification failed" }
        }.onFailure {
            temp.delete()
            throw it
        }
    }

    private fun write(target: File, catalog: MasterCatalog) {
        val plaintext = encode(catalog).toByteArray(Charsets.UTF_8)
        val encrypted = keyManager.encrypt(plaintext, AAD)
        FileOutputStream(target).use { stream ->
            val output = DataOutputStream(BufferedOutputStream(stream))
            output.writeInt(MAGIC)
            output.writeInt(VERSION)
            output.writeInt(encrypted.iv.size)
            output.write(encrypted.iv)
            output.writeInt(encrypted.ciphertext.size)
            output.write(encrypted.ciphertext)
            output.flush()
            runCatching { stream.fd.sync() }
        }
    }

    private fun read(source: File): MasterCatalog = DataInputStream(source.inputStream().buffered()).use { input ->
        require(input.readInt() == MAGIC) { "Master catalog format invalid" }
        require(input.readInt() == VERSION) { "Master catalog version unsupported" }
        val ivSize = input.readInt()
        require(ivSize in 12..32) { "Master catalog IV invalid" }
        val iv = ByteArray(ivSize).also(input::readFully)
        val payloadSize = input.readInt()
        require(payloadSize in 16..MAX_BYTES) { "Master catalog payload invalid" }
        val payload = ByteArray(payloadSize).also(input::readFully)
        require(input.read() == -1) { "Master catalog trailing bytes" }
        val plaintext = keyManager.decrypt(EncryptedPayload(payload, iv), AAD)
        decode(String(plaintext, Charsets.UTF_8)).also(::validate)
    }

    private fun encode(catalog: MasterCatalog): String = JSONObject().apply {
        put("version", VERSION)
        put("entries", JSONArray().apply {
            catalog.entries.forEach { entry ->
                put(JSONObject().apply {
                    put("id", entry.id)
                    put("kind", entry.kind.name)
                    put("name", entry.name)
                    put("active", entry.active)
                    put("categoryName", entry.categoryName)
                    put("rateBasisPoints", entry.rateBasisPoints)
                })
            }
        })
    }.toString()

    private fun decode(value: String): MasterCatalog {
        val root = JSONObject(value)
        require(root.optInt("version", 0) == VERSION) { "Master catalog JSON version unsupported" }
        val array = root.optJSONArray("entries") ?: JSONArray()
        return MasterCatalog(
            entries = List(array.length()) { index ->
                array.getJSONObject(index).run {
                    MasterEntry(
                        id = getString("id"),
                        kind = MasterKind.valueOf(getString("kind")),
                        name = getString("name"),
                        active = optBoolean("active", true),
                        categoryName = optString("categoryName"),
                        rateBasisPoints = optInt("rateBasisPoints", 0),
                    )
                }
            },
        )
    }

    private fun validate(catalog: MasterCatalog) {
        require(catalog.entries.size <= 2_000) { "Too many master entries" }
        require(catalog.entries.map { it.id }.distinct().size == catalog.entries.size) { "Duplicate master ID" }
        require(catalog.entries.all { it.name.isNotBlank() && it.name.length <= 60 }) { "Master name invalid" }
        require(catalog.entries.all { it.rateBasisPoints in 0..100_000 }) { "Master rate invalid" }
    }

    companion object {
        private const val FILE_NAME = "master_catalog_v1.bin"
        private const val MAGIC = 0x474B4D31
        private const val VERSION = 1
        private const val MAX_BYTES = 4 * 1024 * 1024
        private val AAD = "girvi-khata-master-catalog-v1".toByteArray(Charsets.UTF_8)
    }
}
