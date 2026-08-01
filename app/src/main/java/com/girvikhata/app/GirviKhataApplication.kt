package com.girvikhata.app

import android.app.Activity
import android.app.Application
import android.os.Bundle
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

        // Finish an unambiguous staged cross-store restore before any ordinary business write recovery.
        // Failure remains fail-closed because the pending restore intent blocks later coordinated writes.
        runCatching { RestoreGenerationCoordinator(this).reconcileOnStartup() }

        // Reconcile normal interrupted writes, then repair only a stale business-write block from the
        // verified encrypted authoritative snapshot. Active restore recovery is never bypassed.
        runCatching { InterruptedWriteRecoveryCoordinator(this).reconcileOnStartup() }
        runCatching { VerifiedWriteRecoveryRepair(this).repairIfBlocked() }

        commitObserver = BusinessCommitObserver(this).also { it.start() }
    }

    override fun onTerminate() {
        if (::commitObserver.isInitialized) commitObserver.stop()
        super.onTerminate()
    }

    // FLAG_SECURE is intentionally not applied in Alpha25B testing builds so owners can capture
    // diagnostic screenshots while save/recovery behavior is being validated on real devices.
    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit

    override fun onActivityStarted(activity: Activity) = Unit
    override fun onActivityResumed(activity: Activity) = Unit
    override fun onActivityPaused(activity: Activity) = Unit
    override fun onActivityStopped(activity: Activity) = Unit
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
    override fun onActivityDestroyed(activity: Activity) = Unit
}
