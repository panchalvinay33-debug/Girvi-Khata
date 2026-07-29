package com.girvikhata.app.export

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File

object SecureShare {
    fun shareText(context: Context, subject: String, text: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(Intent.createChooser(intent, subject))
    }

    fun shareCsv(context: Context, fileName: String, csv: String) =
        shareBinary(
            context = context,
            fileName = fileName,
            mimeType = "text/csv",
            bytes = csv.toByteArray(Charsets.UTF_8),
            subject = "Girvi Khata Collection Report",
        )

    fun shareBinary(context: Context, fileName: String, mimeType: String, bytes: ByteArray, subject: String) {
        require(bytes.isNotEmpty()) { "Share file is empty" }
        val exportDir = File(context.cacheDir, "shared_exports").apply { mkdirs() }
        exportDir.listFiles()?.forEach { old ->
            if (System.currentTimeMillis() - old.lastModified() > MAX_CACHE_AGE_MILLIS) old.delete()
        }
        val safeName = fileName.replace(Regex("[^A-Za-z0-9._-]"), "_").ifBlank { "girvi-export.bin" }
        val file = File(exportDir, safeName).apply { writeBytes(bytes) }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, subject))
    }

    private const val MAX_CACHE_AGE_MILLIS = 24L * 60L * 60L * 1000L
}
