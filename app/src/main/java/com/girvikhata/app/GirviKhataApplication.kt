package com.girvikhata.app

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.view.WindowManager
import com.girvikhata.app.data.BusinessCommitObserver
import com.girvikhata.app.data.InterruptedWriteRecoveryCoordinator
import com.girvikhata.app.data.RestoreGenerationCoordinator

/** Central privacy policy plus process-lifetime verified business-commit observer. */
class GirviKhataApplication : Application(), Application.ActivityLifecycleCallbacks {
    private lateinit var commitObserver: BusinessCommitObserver

    override fun onCreate() {
        super.onCreate()
        registerActivityLifecycleCallbacks(this)

        // Finish an unambiguous staged cross-store restore before any ordinary business write recovery.
        // Failure remains fail-closed because the pending restore intent blocks later coordinated writes.
        runCatching { RestoreGenerationCoordinator(this).reconcileOnStartup() }

        // Non-destructive reconciliation only: it never replaces the authoritative snapshot.
        // Unknown fingerprint states remain blocked and are journaled for explicit recovery.
        runCatching { InterruptedWriteRecoveryCoordinator(this).reconcileOnStartup() }

        commitObserver = BusinessCommitObserver(this).also { it.start() }
    }

    override fun onTerminate() {
        if (::commitObserver.isInitialized) commitObserver.stop()
        super.onTerminate()
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        activity.window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
    }

    override fun onActivityStarted(activity: Activity) = Unit
    override fun onActivityResumed(activity: Activity) = Unit
    override fun onActivityPaused(activity: Activity) = Unit
    override fun onActivityStopped(activity: Activity) = Unit
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
    override fun onActivityDestroyed(activity: Activity) = Unit
}
