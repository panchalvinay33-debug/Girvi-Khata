package com.girvikhata.app

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import android.view.WindowManager
import com.girvikhata.app.backup.AutoBackupConfig
import com.girvikhata.app.backup.AutoBackupWorker
import com.girvikhata.app.backup.MediaCommitObserver
import com.girvikhata.app.data.BusinessCommitObserver
import com.girvikhata.app.data.DataSafetyJournal
import com.girvikhata.app.data.InterruptedWriteRecoveryCoordinator
import com.girvikhata.app.data.RestoreGenerationCoordinator
import com.girvikhata.app.data.VerifiedWriteRecoveryRepair

/** Process-lifetime verified business/media commit observers plus app-wide privacy hardening. */
class GirviKhataApplication : Application(), Application.ActivityLifecycleCallbacks {
    private lateinit var commitObserver: BusinessCommitObserver
    private lateinit var mediaObserver: MediaCommitObserver

    override fun onCreate() {
        super.onCreate()
        instance = this
        registerActivityLifecycleCallbacks(this)

        reconcile("RESTORE_RECONCILE_FAILED", "Restore startup reconciliation failed") {
            RestoreGenerationCoordinator(this).reconcileOnStartup()
        }
        reconcile("WRITE_RECONCILE_FAILED", "Interrupted write reconciliation failed") {
            InterruptedWriteRecoveryCoordinator(this).reconcileOnStartup()
        }
        reconcile("WRITE_REPAIR_FAILED", "Verified write recovery repair failed") {
            VerifiedWriteRecoveryRepair(this).repairIfBlocked()
        }

        commitObserver = BusinessCommitObserver(this).also { it.start() }
        mediaObserver = MediaCommitObserver(this).also { it.start() }

        if (AutoBackupConfig(this).status().enabled) {
            runCatching { AutoBackupWorker.scheduleDaily(this) }
                .onFailure { recordStartupFailure("AUTO_BACKUP_SCHEDULE_FAILED", "Automatic backup schedule failed", it) }
        }
    }

    private inline fun reconcile(type: String, title: String, block: () -> Unit) {
        runCatching(block).onFailure { recordStartupFailure(type, title, it) }
    }

    private fun recordStartupFailure(type: String, title: String, failure: Throwable) {
        runCatching {
            val detail = (failure.message ?: failure::class.java.simpleName).take(420).ifBlank { "Unknown startup error" }
            DataSafetyJournal(this).recordNamedEvent(type, title, detail)
        }
    }

    override fun onTerminate() {
        if (::commitObserver.isInitialized) commitObserver.stop()
        if (::mediaObserver.isInitialized) mediaObserver.stop()
        instance = null
        super.onTerminate()
    }

    /**
     * Girvi Khata contains customer financial data, photos and recovery secrets.
     * Block screenshots/screen recording and hide content from the Recent Apps preview on every activity.
     */
    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        activity.window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
    }

    override fun onActivityStarted(activity: Activity) = Unit
    override fun onActivityResumed(activity: Activity) = Unit
    override fun onActivityPaused(activity: Activity) = Unit
    override fun onActivityStopped(activity: Activity) = Unit
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
    override fun onActivityDestroyed(activity: Activity) = Unit

    companion object {
        @Volatile
        private var instance: GirviKhataApplication? = null

        /** Available for cross-store integrity checks that must fail closed in the real app process. */
        fun appContextOrNull(): Context? = instance?.applicationContext
    }
}
