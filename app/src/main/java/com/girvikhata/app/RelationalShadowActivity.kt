package com.girvikhata.app

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.girvikhata.app.data.EncryptedMasterCatalogStore
import com.girvikhata.app.data.EncryptedRecordStore
import com.girvikhata.app.data.EncryptedRelationalShadowStore
import com.girvikhata.app.data.MigrationDiagnosticReport
import com.girvikhata.app.data.RelationalCutoverPolicy
import com.girvikhata.app.data.RelationalDualReadReport
import com.girvikhata.app.data.RelationalMasterInstallResult
import com.girvikhata.app.data.RelationalMasterSchemaInstaller
import com.girvikhata.app.data.RelationalMigrationDiagnostics
import com.girvikhata.app.data.RelationalShadowStatus
import com.girvikhata.app.data.VerifiedWriteCutoverPolicy
import com.girvikhata.app.data.VerifiedWriteObservation
import com.girvikhata.app.data.VerifiedWriteObservationStore
import com.girvikhata.app.security.PinVerificationResult
import com.girvikhata.app.security.SecurityPreferences
import java.text.DateFormat
import java.util.Date

data class MigrationDashboardState(
    val status: RelationalShadowStatus,
    val dualRead: RelationalDualReadReport,
    val blockers: List<String>,
    val diagnostic: MigrationDiagnosticReport? = null,
    val masters: RelationalMasterInstallResult = RelationalMasterInstallResult(false, reason = "Master schema unavailable"),
    val verifiedWrites: VerifiedWriteObservation = VerifiedWriteObservation(0),
)

class RelationalShadowActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val records = EncryptedRecordStore(applicationContext)
        val masterCatalog = EncryptedMasterCatalogStore(applicationContext)
        val masterInstaller = RelationalMasterSchemaInstaller(applicationContext)
        val security = SecurityPreferences(applicationContext)
        val diagnostics = RelationalMigrationDiagnostics(applicationContext)
        val writeObservations = VerifiedWriteObservationStore(applicationContext)

        fun dashboard(): MigrationDashboardState {
            val snapshot = records.load()
            return EncryptedRelationalShadowStore(applicationContext).use { shadow ->
                val status = shadow.statusAgainst(snapshot)
                val dual = shadow.dualReadComparison(snapshot)
                val blockers = RelationalCutoverPolicy.blockers(shadow.cutoverEvidence(ownerApproved = false)).toMutableList()
                val diagnostic = diagnostics.latest()
                val masters = masterInstaller.status()
                val observations = writeObservations.load()
                if (diagnostic?.rollbackVerified != true) blockers += "Device rollback simulation pending"
                if (diagnostic?.benchmarkVerified != true) blockers += "Device benchmark pending"
                if (!masters.installed) blockers += "Relational master schema/link verification pending"
                blockers += VerifiedWriteCutoverPolicy.blockers(observations)
                MigrationDashboardState(status, dual, blockers.distinct(), diagnostic, masters, observations)
            }
        }

        fun syncMasters() {
            masterInstaller.synchronize(records.load(), masterCatalog.load())
        }

        setContent {
            MaterialTheme {
                RelationalShadowRoot(
                    verifyPin = { security.verify(it.toCharArray()) },
                    loadDashboard = ::dashboard,
                    incrementalSync = {
                        val snapshot = records.load()
                        EncryptedRelationalShadowStore(applicationContext).use { it.syncIncremental(snapshot) }
                        syncMasters()
                        dashboard()
                    },
                    rebuild = {
                        val snapshot = records.load()
                        EncryptedRelationalShadowStore(applicationContext).use { it.replaceAll(snapshot) }
                        syncMasters()
                        dashboard()
                    },
                    syncMasterLinks = { syncMasters(); dashboard() },
                    runDiagnostics = { diagnostics.run(records.load()); dashboard() },
                    close = ::finish,
                )
            }
        }
    }
}

@Composable
private fun RelationalShadowRoot(
    verifyPin: (String) -> PinVerificationResult,
    loadDashboard: () -> MigrationDashboardState,
    incrementalSync: () -> MigrationDashboardState,
    rebuild: () -> MigrationDashboardState,
    syncMasterLinks: () -> MigrationDashboardState,
    runDiagnostics: () -> MigrationDashboardState,
    close: () -> Unit,
) {
    var unlocked by rememberSaveable { mutableStateOf(false) }
    if (!unlocked) {
        var pin by rememberSaveable { mutableStateOf("") }
        var message by rememberSaveable { mutableStateOf("Database migration status ke liye PIN verify karein") }
        Column(
            Modifier.fillMaxSize().background(Color(0xFFF6F7FB)).padding(22.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("Secure Migration Status", fontSize = 27.sp, fontWeight = FontWeight.Bold)
            Text(message, color = Color.Gray)
            OutlinedTextField(
                pin,
                { pin = it.filter(Char::isDigit).take(6) },
                label = { Text("6-digit PIN") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            )
            Button(
                onClick = {
                    when (val result = verifyPin(pin)) {
                        PinVerificationResult.Success -> unlocked = true
                        PinVerificationResult.NotConfigured -> message = "PIN configured nahi hai"
                        is PinVerificationResult.Locked -> message = "Security lock active hai"
                        is PinVerificationResult.Failure -> message = "Galat PIN. Attempts: ${result.attempts}"
                    }
                    pin = ""
                },
                enabled = pin.length == 6,
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
            ) { Text("PIN Verify") }
            OutlinedButton(onClick = close, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) { Text("Close") }
        }
        return
    }

    val empty = MigrationDashboardState(
        RelationalShadowStatus(false, reason = "Status unavailable"),
        RelationalDualReadReport(false, reason = "Comparison unavailable"),
        listOf("Verification unavailable"),
    )
    var dashboard by remember {
        mutableStateOf(runCatching(loadDashboard).getOrElse { empty.copy(status = RelationalShadowStatus(false, reason = it.message)) })
    }
    var message by remember { mutableStateOf<String?>(null) }
    val status = dashboard.status
    val good = status.healthy && dashboard.dualRead.matches

    Column(
        Modifier.fillMaxSize().background(Color(0xFFF6F7FB)).verticalScroll(rememberScrollState()).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("Database Migration Status", fontSize = 27.sp, fontWeight = FontWeight.Bold, color = Color(0xFF171752))
        Text("Encrypted snapshot source-of-truth • incremental relational shadow", color = Color.Gray)
        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(if (good) "DUAL READ VERIFIED ✅" else "CUTOVER BLOCKED ⚠️", fontWeight = FontWeight.Bold, color = if (good) Color(0xFF138A4A) else MaterialTheme.colorScheme.error)
                Text("Sync mode: ${status.syncMode ?: "—"}")
                Text("Last changed rows: ${status.changedRows ?: 0}")
                Text("Consecutive healthy syncs: ${status.consecutiveHealthySyncs}")
                Text("Expected: ${status.expectedCounts ?: "not available"}")
                Text("Database: ${status.actualCounts ?: "not available"}")
                Text("Snapshot fingerprint: ${dashboard.dualRead.snapshotFingerprint?.take(24) ?: "—"}")
                Text("Relational fingerprint: ${dashboard.dualRead.relationalFingerprint?.take(24) ?: "—"}")
                Text("Last mirror: ${status.mirroredAt?.let(::formatTime) ?: "Never"}")
                status.lastFailureAt?.let { Text("Last failure: ${formatTime(it)}", color = MaterialTheme.colorScheme.error) }
                status.reason?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                dashboard.dualRead.reason?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        }

        val writes = dashboard.verifiedWrites
        val writeReady = VerifiedWriteCutoverPolicy.blockers(writes).isEmpty()
        Card(
            Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = if (writeReady) Color(0xFFE8F5E9) else Color(0xFFFFF8E1)),
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Verified Business Writes", fontWeight = FontWeight.Bold)
                Text("Observation progress: ${writes.successfulWrites}/${VerifiedWriteCutoverPolicy.MINIMUM_COORDINATED_WRITES}")
                Text("Latest fingerprint proof: ${if (writes.fingerprintsMatch) "MATCH ✅" else "MISSING / MISMATCH ⚠️"}")
                Text("Last TX: ${writes.lastTransactionId?.take(12) ?: "None"}")
                Text("Last committed: ${writes.lastCommittedAt?.let(::formatTime) ?: "Never"}")
                writes.lastSnapshotFingerprint?.let { Text("Snapshot: ${it.take(24)}", fontSize = 12.sp) }
                writes.lastRelationalFingerprint?.let { Text("Relational: ${it.take(24)}", fontSize = 12.sp) }
                writes.lastFailureAt?.let { Text("Last write failure: ${formatTime(it)}", color = MaterialTheme.colorScheme.error) }
                writes.lastFailureReason?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp) }
                Text(if (writeReady) "WRITE OBSERVATION GATE VERIFIED ✅" else "Relational write cutover remains blocked", fontWeight = FontWeight.Bold)
            }
        }

        val master = dashboard.masters
        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = if (master.installed) Color(0xFFE8F5E9) else Color(0xFFFFEBEE))) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Relational Master IDs", fontWeight = FontWeight.Bold)
                Text(if (master.installed) "SCHEMA + LINKS VERIFIED ✅" else "PENDING / FAILED ⚠️")
                Text("Encrypted master rows: ${master.masterRows}")
                master.coverage?.let { coverage ->
                    Text("Item master: ${coverage.itemMasterLinked}/${coverage.totalItems}")
                    Text("Unit: ${coverage.unitLinked}/${coverage.totalItems} • Locker: ${coverage.lockerLinked}/${coverage.totalItems}")
                    Text("Interest plan: ${coverage.interestPlanLinked}/${coverage.totalGirvis}")
                    Text("Payment mode: ${coverage.paymentModeLinked}/${coverage.totalPayments}")
                    Text("Complete automatic coverage: ${if (coverage.complete) "YES" else "NO — manual/unresolved values preserved"}", fontSize = 12.sp)
                }
                Text("Last verified: ${master.verifiedAt?.let(::formatTime) ?: "Never"}", fontSize = 12.sp)
                master.reason?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        }

        dashboard.diagnostic?.let { diagnostic ->
            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = if (diagnostic.rollbackVerified && diagnostic.benchmarkVerified) Color(0xFFE8F5E9) else Color(0xFFFFEBEE))) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Device Diagnostic Proof", fontWeight = FontWeight.Bold)
                    Text("Rollback simulation: ${if (diagnostic.rollbackVerified) "VERIFIED" else "PENDING/FAILED"}")
                    Text("Benchmark: ${if (diagnostic.benchmarkVerified) "VERIFIED" else "PENDING/FAILED"}")
                    Text("Storage headroom: ${if (diagnostic.lowSpaceSafe) "SAFE" else "BLOCKED"} • ${diagnostic.freeBytes / (1024 * 1024)} MB free")
                    Text("Full rebuild: ${diagnostic.rebuildMillis?.let { "$it ms" } ?: "—"} • No-change verify: ${diagnostic.noChangeMillis?.let { "$it ms" } ?: "—"}")
                    Text("Completed: ${formatTime(diagnostic.completedAt)}", fontSize = 12.sp)
                    diagnostic.reason?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                }
            }
        }

        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E1))) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Cutover blockers (${dashboard.blockers.size})", fontWeight = FontWeight.Bold)
                dashboard.blockers.forEach { Text("• $it", fontSize = 13.sp) }
                Text("Screen se database cutover enable nahi kiya ja sakta.", color = Color.Gray, fontSize = 12.sp)
            }
        }

        message?.let {
            Text(it, color = if (it.startsWith("Verified") || it.startsWith("Synced") || it.startsWith("Status") || it.startsWith("Diagnostic") || it.startsWith("Master")) Color(0xFF138A4A) else MaterialTheme.colorScheme.error)
        }
        Button(
            onClick = { runCatching(syncMasterLinks).onSuccess { dashboard = it; message = "Master schema and links verified" }.onFailure { message = it.message ?: "Master-link sync failed" } },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Install / Verify Master-ID Links") }
        Button(
            onClick = { runCatching(runDiagnostics).onSuccess { dashboard = it; message = "Diagnostic rollback and benchmark completed" }.onFailure { message = it.message ?: "Diagnostic failed" } },
            modifier = Modifier.fillMaxWidth(),
            colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = Color(0xFF8B1E1E)),
        ) { Text("Run Device Rollback & Benchmark Proof") }
        Button(
            onClick = { runCatching(incrementalSync).onSuccess { dashboard = it; message = "Synced incremental delta and verified dual read" }.onFailure { message = it.message ?: "Incremental sync failed" } },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Incremental Sync & Dual-Read Verify") }
        OutlinedButton(
            onClick = { runCatching(rebuild).onSuccess { dashboard = it; message = "Verified full relational rebuild" }.onFailure { message = it.message ?: "Relational rebuild failed" } },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Full Rebuild & Verify") }
        OutlinedButton(
            onClick = { runCatching(loadDashboard).onSuccess { dashboard = it; message = "Status refreshed" }.onFailure { message = it.message ?: "Status refresh failed" } },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Refresh Status") }
        OutlinedButton(onClick = close, modifier = Modifier.fillMaxWidth()) { Text("Close") }
    }
}

private fun formatTime(value: Long): String = DateFormat.getDateTimeInstance().format(Date(value))
