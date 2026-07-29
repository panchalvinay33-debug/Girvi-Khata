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

    fun shareCsv(context: Context, fileName: String, csv: String) {
        val exportDir = File(context.cacheDir, "shared_exports").apply { mkdirs() }
        exportDir.listFiles()?.forEach { old ->
            if (System.currentTimeMillis() - old.lastModified() > MAX_CACHE_AGE_MILLIS) old.delete()
        }
        val safeName = fileName.replace(Regex("[^A-Za-z0-9._-]"), "_").ifBlank { "girvi-report.csv" }
        val file = File(exportDir, safeName).apply { writeText(csv, Charsets.UTF_8) }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_SUBJECT, "Girvi Khata Collection Report")
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Collection CSV share karein"))
    }

    private const val MAX_CACHE_AGE_MILLIS = 24L * 60L * 60L * 1000L
}
