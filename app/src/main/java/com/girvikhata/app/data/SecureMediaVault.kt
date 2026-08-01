package com.girvikhata.app.data

import android.content.Context
import com.girvikhata.app.security.DeviceKeyManager
import com.girvikhata.app.security.EncryptedPayload
import java.io.BufferedOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

/** AES-GCM encrypted app-private media vault. */
class SecureMediaVault(
    context: Context,
    private val keyManager: DeviceKeyManager = DeviceKeyManager(),
) {
    private val root = File(context.filesDir, "secure_media_v1").apply { mkdirs() }
    private val temp = File(context.filesDir, "private_media/camera_tmp").apply { mkdirs() }

    fun newCameraTempFile(prefix: String): File = File(
        temp,
        "${prefix.take(20)}-${System.currentTimeMillis()}-${UUID.randomUUID()}.jpg",
    )

    @Synchronized
    fun importPhoto(source: File, mediaId: String): File {
        require(source.exists() && source.isFile) { "Photo file missing" }
        require(source.length() in 1L..MAX_PHOTO_BYTES) { "Photo size invalid" }
        val plaintext = source.readBytes()
        return try {
            importPhotoBytes(mediaId, plaintext)
        } finally {
            plaintext.fill(0)
            source.delete()
        }
    }

    /** Re-encrypt portable photo bytes under this device's Android Keystore key. */
    @Synchronized
    fun importPhotoBytes(mediaId: String, plaintext: ByteArray): File {
        require(plaintext.size in 1..MAX_PHOTO_BYTES.toInt()) { "Photo size invalid" }
        val encrypted = keyManager.encrypt(plaintext, aad(mediaId))
        val target = fileFor(mediaId)
        val temporary = File(root, ".$mediaId.tmp")
        runCatching {
            writeEnvelope(temporary, encrypted)
            val verified = readEnvelope(temporary, mediaId)
            check(verified.contentEquals(plaintext)) { "Encrypted photo verification failed" }
            verified.fill(0)
            if (target.exists() && !target.delete()) error("Old encrypted photo replace failed")
            if (!temporary.renameTo(target)) {
                temporary.copyTo(target, overwrite = true)
                temporary.delete()
            }
            val finalRead = readEnvelope(target, mediaId)
            check(finalRead.contentEquals(plaintext)) { "Stored photo verification failed" }
            finalRead.fill(0)
        }.onFailure {
            temporary.delete()
            throw it
        }
        return target
    }

    fun exists(mediaId: String): Boolean = fileFor(mediaId).exists()

    fun encryptedFile(mediaId: String): File = fileFor(mediaId)

    fun readPhoto(mediaId: String): ByteArray = readEnvelope(fileFor(mediaId), mediaId)

    fun delete(mediaId: String): Boolean = !fileFor(mediaId).exists() || fileFor(mediaId).delete()

    fun allEncryptedFiles(): List<File> = root.listFiles()?.filter { it.isFile && it.extension == EXTENSION }.orEmpty()

    fun allMediaIds(): List<String> = allEncryptedFiles().map { it.nameWithoutExtension }.sorted()

    fun cleanupTemps() {
        temp.listFiles()?.filter(File::isFile)?.forEach { file ->
            if (System.currentTimeMillis() - file.lastModified() > TEMP_MAX_AGE_MS) file.delete()
        }
    }

    private fun fileFor(mediaId: String): File {
        require(mediaId.matches(Regex("[A-Za-z0-9._-]{1,100}"))) { "Invalid media id" }
        return File(root, "$mediaId.$EXTENSION")
    }

    private fun writeEnvelope(target: File, payload: EncryptedPayload) {
        FileOutputStream(target).use { stream ->
            val out = DataOutputStream(BufferedOutputStream(stream))
            out.writeInt(MAGIC)
            out.writeInt(payload.iv.size)
            out.write(payload.iv)
            out.writeInt(payload.ciphertext.size)
            out.write(payload.ciphertext)
            out.flush()
            runCatching { stream.fd.sync() }
        }
    }

    private fun readEnvelope(source: File, mediaId: String): ByteArray = DataInputStream(source.inputStream().buffered()).use { input ->
        require(input.readInt() == MAGIC) { "Invalid encrypted media" }
        val ivSize = input.readInt()
        require(ivSize == 12) { "Invalid media IV" }
        val iv = ByteArray(ivSize).also(input::readFully)
        val cipherSize = input.readInt()
        require(cipherSize in 16..MAX_ENCRYPTED_BYTES) { "Invalid encrypted media size" }
        val ciphertext = ByteArray(cipherSize).also(input::readFully)
        require(input.read() == -1) { "Encrypted media trailing bytes" }
        keyManager.decrypt(EncryptedPayload(ciphertext, iv), aad(mediaId))
    }

    private fun aad(mediaId: String) = "girvi-khata-media-v1:$mediaId".toByteArray(Charsets.UTF_8)

    companion object {
        private const val MAGIC = 0x474B4D31
        private const val EXTENSION = "gkm"
        private const val MAX_PHOTO_BYTES = 20L * 1024L * 1024L
        private const val MAX_ENCRYPTED_BYTES = 21 * 1024 * 1024
        private const val TEMP_MAX_AGE_MS = 24L * 60L * 60L * 1000L
    }
}
