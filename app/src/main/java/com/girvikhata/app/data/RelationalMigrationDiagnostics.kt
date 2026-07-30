package com.girvikhata.app.data

import android.content.Context
import android.os.StatFs
import kotlin.math.max

data class MigrationDiagnosticReport(
    val rollbackVerified: Boolean,
    val benchmarkVerified: Boolean,
    val lowSpaceSafe: Boolean,
    val freeBytes: Long,
    val rebuildMillis: Long?,
    val noChangeMillis: Long?,
    val fingerprint: String?,
    val completedAt: Long,
    val reason: String? = null,
)

object RelationalSpacePolicy {
    const val MIN_FREE_BYTES = 64L * 1024L * 1024L
    fun hasSafeHeadroom(freeBytes: Long, estimatedDatabaseBytes: Long): Boolean =
        freeBytes >= max(MIN_FREE_BYTES, estimatedDatabaseBytes * 3L)
}

class RelationalMigrationDiagnostics(private val context: Context) {
    private val prefs = context.getSharedPreferences("relational_migration_diagnostics_v1", Context.MODE_PRIVATE)

    fun run(snapshot: AppSnapshot): MigrationDiagnosticReport {
        val freeBytes = StatFs(context.filesDir.absolutePath).availableBytes
        val estimated = estimateBytes(snapshot)
        if (!RelationalSpacePolicy.hasSafeHeadroom(freeBytes, estimated)) {
            return save(MigrationDiagnosticReport(false, false, false, freeBytes, null, null, null, System.currentTimeMillis(), "Storage headroom kam hai"))
        }
        return runCatching {
            EncryptedRelationalShadowStore(context).use { store ->
                val rebuildStart = System.nanoTime()
                val baseline = store.replaceAll(snapshot)
                val rebuildMs = elapsedMillis(rebuildStart)
                check(baseline.healthy) { "Baseline rebuild verify nahi hua" }
                val beforeFingerprint = baseline.actualFingerprint

                val target = snapshot.copy(categories = snapshot.categories + CategoryRecord(name = "__rollback_probe__"))
                val injected = runCatching { store.syncIncremental(target, ShadowFailurePoint.BEFORE_COMMIT) }
                check(injected.isFailure) { "Injected failure trigger nahi hui" }
                val afterFailure = store.statusAgainst(snapshot)
                check(afterFailure.healthy && afterFailure.actualFingerprint == beforeFingerprint) { "Rollback ke baad relational state badal gayi" }

                val noChangeStart = System.nanoTime()
                val unchanged = store.syncIncremental(snapshot)
                val noChangeMs = elapsedMillis(noChangeStart)
                check(unchanged.healthy && unchanged.syncMode == "NO_CHANGE") { "No-change verification failed" }

                save(MigrationDiagnosticReport(true, true, true, freeBytes, rebuildMs, noChangeMs, unchanged.actualFingerprint, System.currentTimeMillis()))
            }
        }.getOrElse { error ->
            save(MigrationDiagnosticReport(false, false, true, freeBytes, null, null, null, System.currentTimeMillis(), error.message ?: "Diagnostic failed"))
        }
    }

    fun latest(): MigrationDiagnosticReport? {
        val at = prefs.getLong("at", 0L)
        if (at == 0L) return null
        return MigrationDiagnosticReport(
            rollbackVerified = prefs.getBoolean("rollback", false),
            benchmarkVerified = prefs.getBoolean("benchmark", false),
            lowSpaceSafe = prefs.getBoolean("space", false),
            freeBytes = prefs.getLong("free", 0L),
            rebuildMillis = prefs.getLong("rebuild", -1L).takeIf { it >= 0 },
            noChangeMillis = prefs.getLong("no_change", -1L).takeIf { it >= 0 },
            fingerprint = prefs.getString("fingerprint", null),
            completedAt = at,
            reason = prefs.getString("reason", null),
        )
    }

    private fun save(report: MigrationDiagnosticReport): MigrationDiagnosticReport {
        prefs.edit().putBoolean("rollback", report.rollbackVerified).putBoolean("benchmark", report.benchmarkVerified)
            .putBoolean("space", report.lowSpaceSafe).putLong("free", report.freeBytes)
            .putLong("rebuild", report.rebuildMillis ?: -1L).putLong("no_change", report.noChangeMillis ?: -1L)
            .putString("fingerprint", report.fingerprint).putLong("at", report.completedAt).putString("reason", report.reason).apply()
        return report
    }

    private fun estimateBytes(snapshot: AppSnapshot): Long =
        2L * 1024L * 1024L + snapshot.customers.size * 1024L + snapshot.categories.size * 512L +
            snapshot.girvis.size * 2048L + snapshot.girvis.sumOf { it.effectiveItems.size } * 1024L +
            snapshot.girvis.sumOf { it.payments.size } * 768L

    private fun elapsedMillis(start: Long): Long = (System.nanoTime() - start) / 1_000_000L
}
