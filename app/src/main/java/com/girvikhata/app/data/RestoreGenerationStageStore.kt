package com.girvikhata.app.data

import android.content.Context
import com.girvikhata.app.backup.PortableAppBundleCodec
import com.girvikhata.app.domain.MasterCatalog
import com.girvikhata.app.security.DeviceKeyManager
import com.girvikhata.app.security.EncryptedPayload
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileOutputStream

/** App-private encrypted target bundle used to finish an interrupted restore generation. */
class RestoreGenerationStageStore(
    context: Context,
    private val keyManager: DeviceKeyManager = DeviceKeyManager(),
) {
    data class StagedTarget(
        val generationId: String,
        val snapshot: AppSnapshot,
        val masterCatalog: MasterCatalog,
    )

    private val file = File(context.applicationContext.filesDir, FILE_NAME)

    @Synchronized
    fun stage(target: StagedTarget) {
        require(target.generationId.isNotBlank() && target.generationId.length <= 80) {
            "Restore stage generation ID invalid"
        }
        val payload = PortableAppBundleCodec.encode(target.snapshot, target.masterCatalog)
        val encrypted = keyManager.encrypt(payload, aad(target.generationId))
        val temporary = File(file.parentFile, "$FILE_NAME.tmp")
        runCatching {
            write(temporary, target.generationId, encrypted)
            check(read(temporary) == target) { "Restore stage read-back failed" }
            replace(temporary)
            check(read(file) == target) { "Restore stage final verification failed" }
        }.onFailure {
            temporary.delete()
            throw it
        }
    }

    @Synchronized
    fun load(expectedGenerationId: String): StagedTarget {
        val target = read(file)
        require(target.generationId == expectedGenerationId) { "Restore stage generation mismatch" }
        return target
    }

    @Synchronized
    fun clear(expectedGenerationId: String) {
        if (!file.exists()) return
        load(expectedGenerationId)
        check(file.delete()) { "Restore stage cleanup failed" }
    }

    @Synchronized
    fun exists(): Boolean = file.exists()

    private fun write(target: File, generationId: String, encrypted: EncryptedPayload) {
        FileOutputStream(target).use { stream ->
            DataOutputStream(stream.buffered()).use { output ->
                val generation = generationId.toByteArray(Charsets.UTF_8)
                output.writeInt(MAGIC)
                output.writeInt(VERSION)
                output.writeInt(generation.size)
                output.write(generation)
                output.writeInt(encrypted.iv.size)
                output.write(encrypted.iv)
                output.writeInt(encrypted.ciphertext.size)
                output.write(encrypted.ciphertext)
                output.flush()
            }
            stream.fd.sync()
        }
    }

    private fun read(source: File): StagedTarget {
        require(source.exists() && source.isFile) { "Restore stage missing" }
        return DataInputStream(source.inputStream().buffered()).use { input ->
            require(input.readInt() == MAGIC) { "Restore stage format invalid" }
            require(input.readInt() == VERSION) { "Restore stage version unsupported" }
            val generationSize = input.readInt()
            require(generationSize in 1..80) { "Restore stage generation invalid" }
            val generationId = String(ByteArray(generationSize).also(input::readFully), Charsets.UTF_8)
            val ivSize = input.readInt()
            require(ivSize in 12..32) { "Restore stage IV invalid" }
            val iv = ByteArray(ivSize).also(input::readFully)
            val payloadSize = input.readInt()
            require(payloadSize in 16..MAX_BYTES) { "Restore stage payload invalid" }
            val ciphertext = ByteArray(payloadSize).also(input::readFully)
            require(input.read() == -1) { "Restore stage trailing bytes" }
            val plaintext = keyManager.decrypt(EncryptedPayload(ciphertext, iv), aad(generationId))
            val bundle = PortableAppBundleCodec.decode(plaintext)
            require(bundle.containsPortableMasters) { "Restore stage bundle incomplete" }
            StagedTarget(generationId, bundle.snapshot, bundle.masterCatalog)
        }
    }

    private fun replace(temporary: File) {
        if (file.exists() && !file.delete()) error("Old restore stage replace failed")
        if (!temporary.renameTo(file)) {
            temporary.copyTo(file, overwrite = true)
            check(temporary.delete()) { "Restore stage temp cleanup failed" }
        }
    }

    private fun aad(generationId: String): ByteArray =
        "girvi-khata-restore-stage-v1:$generationId".toByteArray(Charsets.UTF_8)

    private companion object {
        const val FILE_NAME = "restore_generation_stage_v1.bin"
        const val MAGIC = 0x474B5231
        const val VERSION = 1
        const val MAX_BYTES = 128 * 1024 * 1024
    }
}
