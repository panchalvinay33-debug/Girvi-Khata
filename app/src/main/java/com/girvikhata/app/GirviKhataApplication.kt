package com.girvikhata.app

import android.app.Activity
import android.app.Application
import android.os.Bundle
import com.girvikhata.app.backup.AutoBackupConfig
import com.girvikhata.app.backup.AutoBackupWorker
import com.girvikhata.app.data.BusinessCommitObserver
import com.girvikhata.app.data.InterruptedWriteRecoveryCoordinator
import com.girvikhata.app.data.RestoreGenerationCoordinator
import com.girvikhata.app.data.VerifiedWriteRecoveryRepair

/** Process-lifetime verified business-commit observer. */
class GirviKhataApplication : Application(), Application.ActivityLifecycleCallbacks {
    private lateinit var commitObserver: BusinessCommitObserver

    override fun onCreate() {
        super.onCreate()
        registerActivityLifecycleCallbacks(this)

        runCatching { RestoreGenerationCoordinator(this).reconcileOnStartup() }
        runCatching { InterruptedWriteRecoveryCoordinator(this).reconcileOnStartup() }
        runCatching { VerifiedWriteRecoveryRepair(this).repairIfBlocked() }

        commitObserver = BusinessCommitObserver(this).also { it.start() }

        // WorkManager survives process restarts. The daily job is only scheduled after the owner has
        // configured an off-device folder and recovery key in Recovery Center.
        if (AutoBackupConfig(this).status().enabled) {
            runCatching { AutoBackupWorker.scheduleDaily(this) }
        }
    }

    override fun onTerminate() {
        if (::commitObserver.isInitialized) commitObserver.stop()
        super.onTerminate()
    }

    // FLAG_SECURE intentionally remains disabled while the owner validates recovery on real devices.
    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
    override fun onActivityStarted(activity: Activity) = Unit
    override fun onActivityResumed(activity: Activity) = Unit
    override fun onActivityPaused(activity: Activity) = Unit
    override fun onActivityStopped(activity: Activity) = Unit
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
    override fun onActivityDestroyed(activity: Activity) = Unit
}
