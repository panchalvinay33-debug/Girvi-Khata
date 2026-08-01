package com.girvikhata.app.backup

import android.content.Context
import android.os.FileObserver
import java.io.File

/** Debounces secure-media commits into the same unique automatic backup work. */
class MediaCommitObserver(context: Context) {
    private val appContext = context.applicationContext
    private val directory = File(appContext.filesDir, "secure_media_v1").apply { mkdirs() }
    private var lastQueuedAt = 0L

    @Suppress("DEPRECATION")
    private val observer = object : FileObserver(
        directory.absolutePath,
        CLOSE_WRITE or MOVED_TO or CREATE or DELETE,
    ) {
        override fun onEvent(event: Int, path: String?) {
            if (path.isNullOrBlank() || !path.endsWith(".gkm")) return
            val config = AutoBackupConfig(appContext).status()
            if (!config.enabled) return
            val now = System.currentTimeMillis()
            synchronized(this@MediaCommitObserver) {
                if (now - lastQueuedAt < 300L) return
                lastQueuedAt = now
            }
            runCatching { AutoBackupWorker.enqueueNow(appContext) }
        }
    }

    fun start() = observer.startWatching()
    fun stop() = observer.stopWatching()
}
