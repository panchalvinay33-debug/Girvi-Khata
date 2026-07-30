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
import com.girvikhata.app.data.EncryptedRecordStore
import com.girvikhata.app.data.EncryptedRelationalShadowStore
import com.girvikhata.app.data.RelationalCutoverPolicy
import com.girvikhata.app.data.RelationalDualReadReport
import com.girvikhata.app.data.RelationalShadowStatus
import com.girvikhata.app.security.PinVerificationResult
import com.girvikhata.app.security.SecurityPreferences
import java.text.DateFormat
import java.util.Date

data class MigrationDashboardState(
    val status: RelationalShadowStatus,
    val dualRead: RelationalDualReadReport,
    val blockers: List<String>,
)

class RelationalShadowActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val records = EncryptedRecordStore(applicationContext)
        val security = SecurityPreferences(applicationContext)
        fun dashboard(): MigrationDashboardState {
            val snapshot = records.load()
            return EncryptedRelationalShadowStore(applicationContext).use { shadow ->
                val status = shadow.statusAgainst(snapshot)
                val dual = shadow.dualReadComparison(snapshot)
                val blockers = RelationalCutoverPolicy.blockers(shadow.cutoverEvidence(ownerApproved = false))
                MigrationDashboardState(status, dual, blockers)
            }
        }
        setContent {
            MaterialTheme {
                RelationalShadowRoot(
                    verifyPin = { security.verify(it.toCharArray()) },
                    loadDashboard = ::dashboard,
                    incrementalSync = {
                        val snapshot = records.load()
                        EncryptedRelationalShadowStore(applicationContext).use { it.syncIncremental(snapshot) }
                        dashboard()
                    },
                    rebuild = {
                        val snapshot = records.load()
                        EncryptedRelationalShadowStore(applicationContext).use { it.replaceAll(snapshot) }
                        dashboard()
                    },
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
    close: () -> Unit,
) {
    var unlocked by rememberSaveable { mutableStateOf(false) }
    if (!unlocked) {
        var pin by rememberSaveable { mutableStateOf("") }
        var message by rememberSaveable { mutableStateOf("Database migration status ke liye PIN verify karein") }
        Column(Modifier.fillMaxSize().background(Color(0xFFF6F7FB)).padding(22.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Secure Migration Status", fontSize = 27.sp, fontWeight = FontWeight.Bold)
            Text(message, color = Color.Gray)
            OutlinedTextField(pin, { pin = it.filter(Char::isDigit).take(6) }, label = { Text("6-digit PIN") }, visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword), modifier = Modifier.fillMaxWidth().padding(top = 16.dp))
            Button(onClick = {
                when (val result = verifyPin(pin)) {
                    PinVerificationResult.Success -> unlocked = true
                    PinVerificationResult.NotConfigured -> message = "PIN configured nahi hai"
                    is PinVerificationResult.Locked -> message = "Security lock active hai"
                    is PinVerificationResult.Failure -> message = "Galat PIN. Attempts: ${result.attempts}"
                }
                pin = ""
            }, enabled = pin.length == 6, modifier = Modifier.fillMaxWidth().padding(top = 10.dp)) { Text("PIN Verify") }
            OutlinedButton(onClick = close, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) { Text("Close") }
        }
        return
    }

    val empty = MigrationDashboardState(RelationalShadowStatus(false, reason = "Status unavailable"), RelationalDualReadReport(false, reason = "Comparison unavailable"), listOf("Verification unavailable"))
    var dashboard by remember { mutableStateOf(runCatching(loadDashboard).getOrElse { empty.copy(status = RelationalShadowStatus(false, reason = it.message)) }) }
    var message by remember { mutableStateOf<String?>(null) }
    val status = dashboard.status
    val good = status.healthy && dashboard.dualRead.matches

    Column(Modifier.fillMaxSize().background(Color(0xFFF6F7FB)).verticalScroll(rememberScrollState()).padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
                Text("Last mirror: ${status.mirroredAt?.let { DateFormat.getDateTimeInstance().format(Date(it)) } ?: "Never"}")
                status.lastFailureAt?.let { Text("Last failure: ${DateFormat.getDateTimeInstance().format(Date(it))}", color = MaterialTheme.colorScheme.error) }
                status.reason?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                dashboard.dualRead.reason?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        }

        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E1))) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Cutover blockers (${dashboard.blockers.size})", fontWeight = FontWeight.Bold)
                dashboard.blockers.forEach { Text("• $it", fontSize = 13.sp) }
                Text("Screen se database cutover enable nahi kiya ja sakta.", color = Color.Gray, fontSize = 12.sp)
            }
        }

        message?.let { Text(it, color = if (it.startsWith("Verified") || it.startsWith("Synced") || it.startsWith("Status")) Color(0xFF138A4A) else MaterialTheme.colorScheme.error) }
        Button(onClick = {
            runCatching(incrementalSync).onSuccess { dashboard = it; message = "Synced incremental delta and verified dual read" }.onFailure { message = it.message ?: "Incremental sync failed" }
        }, modifier = Modifier.fillMaxWidth()) { Text("Incremental Sync & Dual-Read Verify") }
        OutlinedButton(onClick = {
            runCatching(rebuild).onSuccess { dashboard = it; message = "Verified full relational rebuild" }.onFailure { message = it.message ?: "Relational rebuild failed" }
        }, modifier = Modifier.fillMaxWidth()) { Text("Full Rebuild & Verify") }
        OutlinedButton(onClick = {
            runCatching(loadDashboard).onSuccess { dashboard = it; message = "Status refreshed" }.onFailure { message = it.message ?: "Status refresh failed" }
        }, modifier = Modifier.fillMaxWidth()) { Text("Refresh Status") }
        OutlinedButton(onClick = close, modifier = Modifier.fillMaxWidth()) { Text("Close") }
    }
}
