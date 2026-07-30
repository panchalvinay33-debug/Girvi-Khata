package com.girvikhata.app.data

import android.content.Context
import android.os.FileObserver
import java.io.File
import java.util.concurrent.Executors

/**
 * Records only committed primary-store replacements. Temporary and safety-copy writes are ignored.
 * The raw business file remains encrypted; only its SHA-256 and aggregate counts enter the journal.
 * Every verified commit is also mirrored into a transactional encrypted relational shadow database.
 */
class BusinessCommitObserver(context: Context) {
    private val appContext = context.applicationContext
    private val executor = Executors.newSingleThreadExecutor()
    private var lastSignature: String? = null
    private var lastRecordedAt: Long = 0

    @Suppress("DEPRECATION")
    private val observer = object : FileObserver(
        appContext.filesDir.absolutePath,
        CLOSE_WRITE or MOVED_TO or CREATE,
    ) {
        override fun onEvent(event: Int, path: String?) {
            if (path != BUSINESS_FILE) return
            executor.execute(::recordCommittedState)
        }
    }

    fun start() = observer.startWatching()
    fun stop() { observer.stopWatching(); executor.shutdown() }

    private fun recordCommittedState() {
        Thread.sleep(120)
        val source = File(appContext.filesDir, BUSINESS_FILE)
        if (!source.exists() || source.length() == 0L) return
        val signature = runCatching { DataSafetyJournal.sha256(source.readBytes()) }.getOrNull() ?: return
        val now = System.currentTimeMillis()
        synchronized(this) {
            if (signature == lastSignature || now - lastRecordedAt < 250L) return
            lastSignature = signature
            lastRecordedAt = now
        }
        val snapshot = runCatching { EncryptedRecordStore(appContext).load() }.getOrNull() ?: return
        runCatching {
            DataSafetyJournal(appContext).recordSnapshotChange(
                before = null,
                after = snapshot,
                explicitReason = "BUSINESS_STORE_COMMITTED",
            )
        }
        runCatching { EncryptedRelationalShadowStore(appContext).use { it.replaceAll(snapshot) } }
            .onSuccess { status ->
                runCatching {
                    DataSafetyJournal(appContext).recordNamedEvent(
                        type = "RELATIONAL_SHADOW_VERIFIED",
                        title = "Relational shadow verified",
                        detail = "${status.actualCounts?.customers ?: 0} customers • ${status.actualCounts?.girvis ?: 0} girvi • fingerprint ${status.actualFingerprint?.take(16)}…",
                    )
                }
            }
            .onFailure { error ->
                runCatching {
                    DataSafetyJournal(appContext).recordNamedEvent(
                        type = "RELATIONAL_SHADOW_FAILED",
                        title = "Relational shadow verification failed",
                        detail = (error.message ?: "Unknown relational shadow error").take(450),
                    )
                }
            }
    }

    private companion object {
        const val BUSINESS_FILE = "business_records_v1.bin"
    }
}
