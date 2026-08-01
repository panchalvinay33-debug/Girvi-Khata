package com.girvikhata.app.backup

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.girvikhata.app.data.DataSafetyJournal
import com.girvikhata.app.data.EncryptedMasterCatalogStore
import com.girvikhata.app.data.EncryptedRecordStore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class AutoBackupWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val config = AutoBackupConfig(applicationContext)
        val status = config.status()
        if (!status.enabled || status.folderUri.isNullOrBlank()) return Result.success()
        config.recordAttempt()

        return runCatching {
            val key = RecoveryKeyStore(applicationContext).reveal()
            val secret = key.toCharArray()
            val media = PortableMediaSupport.collect(applicationContext)
            try {
                val snapshot = EncryptedRecordStore(applicationContext).load()
                val masters = EncryptedMasterCatalogStore(applicationContext).load()
                val payload = PortableAppBundleCodec.encodePortable(snapshot, masters, media)
                val encrypted = PortableBackupCrypto.encrypt(payload, secret, snapshot.schemaVersion)
                val folderUri = Uri.parse(status.folderUri)
                val folder = DocumentFile.fromTreeUri(applicationContext, folderUri)
                    ?: error("Backup folder unavailable")
                require(folder.exists() && folder.canWrite()) { "Backup folder write permission lost" }

                val stamp = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())
                val fileName = "girvi-khata-auto-$stamp.gkb"
                val target = folder.createFile("application/octet-stream", fileName)
                    ?: error("Automatic backup file create nahi hui")
                val resolver = applicationContext.contentResolver
                resolver.openOutputStream(target.uri, "wt")?.use { out ->
                    out.write(encrypted)
                    out.flush()
                } ?: error("Automatic backup write nahi hui")

                val written = resolver.openInputStream(target.uri)?.use { it.readBytes() }
                    ?: error("Automatic backup verify read nahi hui")
                require(written.contentEquals(encrypted)) { "Automatic backup byte verification failed" }
                val decrypted = PortableBackupCrypto.decrypt(written, secret)
                val decoded = PortableAppBundleCodec.decode(decrypted.payload)
                require(decoded.snapshot.customers.size == snapshot.customers.size) { "Automatic backup customer verification failed" }
                require(decoded.snapshot.girvis.size == snapshot.girvis.size) { "Automatic backup girvi verification failed" }
                require(decoded.masterCatalog == masters) { "Automatic backup master verification failed" }
                require(decoded.portableMedia.size == media.size) { "Automatic backup photo verification failed" }

                val generations = pruneGenerations(folder)
                val sha = PortableBackupCrypto.sha256(written)
                config.recordSuccess(sha, generations)
                runCatching {
                    DataSafetyJournal(applicationContext).markVerifiedBackup(
                        sha,
                        snapshot.customers.size,
                        snapshot.girvis.size,
                        snapshot.girvis.sumOf { it.payments.size },
                    )
                }
                secret.fill('\u0000')
                Result.success()
            } finally {
                PortableMediaSupport.clear(media)
                secret.fill('\u0000')
            }
        }.getOrElse { failure ->
            config.recordFailure(failure.message ?: failure::class.java.simpleName)
            Result.retry()
        }
    }

    private fun pruneGenerations(folder: DocumentFile): Int {
        val backups = folder.listFiles()
            .filter { it.isFile && it.name?.startsWith("girvi-khata-auto-") == true && it.name?.endsWith(".gkb") == true }
            .sortedByDescending { it.lastModified() }
        backups.drop(MAX_GENERATIONS).forEach { runCatching { it.delete() } }
        return backups.take(MAX_GENERATIONS).size
    }

    companion object {
        private const val UNIQUE_IMMEDIATE = "girvi-auto-backup-immediate"
        private const val UNIQUE_PERIODIC = "girvi-auto-backup-periodic"
        private const val MAX_GENERATIONS = 12

        private val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresStorageNotLow(true)
            .build()

        fun enqueueNow(context: Context) {
            val request = OneTimeWorkRequestBuilder<AutoBackupWorker>()
                .setConstraints(constraints)
                .build()
            WorkManager.getInstance(context.applicationContext)
                .enqueueUniqueWork(UNIQUE_IMMEDIATE, ExistingWorkPolicy.REPLACE, request)
        }

        fun scheduleDaily(context: Context) {
            val request = PeriodicWorkRequestBuilder<AutoBackupWorker>(24, TimeUnit.HOURS)
                .setConstraints(constraints)
                .build()
            WorkManager.getInstance(context.applicationContext)
                .enqueueUniquePeriodicWork(UNIQUE_PERIODIC, ExistingPeriodicWorkPolicy.UPDATE, request)
        }

        fun cancel(context: Context) {
            val work = WorkManager.getInstance(context.applicationContext)
            work.cancelUniqueWork(UNIQUE_IMMEDIATE)
            work.cancelUniqueWork(UNIQUE_PERIODIC)
        }
    }
}
