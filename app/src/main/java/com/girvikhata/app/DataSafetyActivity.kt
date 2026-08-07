package com.girvikhata.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.girvikhata.app.data.DataSafetyJournal
import com.girvikhata.app.data.DataSafetyStatus
import com.girvikhata.app.data.EncryptedRecordStore
import com.girvikhata.app.data.RecordStoreLoadState
import com.girvikhata.app.security.BiometricAvailability
import com.girvikhata.app.security.BiometricCapability
import com.girvikhata.app.security.PinVerificationResult
import com.girvikhata.app.security.SecurityPreferences
import java.text.DateFormat
import java.util.Date

class DataSafetyActivity : FragmentActivity() {
    private lateinit var security: SecurityPreferences
    private lateinit var biometricCapability: BiometricCapability

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        security = SecurityPreferences(applicationContext)
        biometricCapability = BiometricCapability(applicationContext)
        val journal = DataSafetyJournal(applicationContext)
        val store = EncryptedRecordStore(applicationContext)
        setContent {
            MaterialTheme {
                val availability = if (security.sessionSettings().biometricUnlockEnabled) {
                    biometricCapability.availability()
                } else {
                    BiometricAvailability.UNSUPPORTED
                }
                DataSafetyRoot(
                    verifyPin = { security.verify(it.toCharArray()) },
                    biometricAvailability = availability,
                    requestBiometric = ::requestBiometric,
                    loadStatus = { journal.status() to store.loadState() },
                    openBackup = { startActivity(Intent(this, BackupActivity::class.java)) },
                    close = ::finish,
                )
            }
        }
    }

    private fun requestBiometric(onSuccess: () -> Unit, onError: (String) -> Unit) {
        if (!security.sessionSettings().biometricUnlockEnabled) {
            onError("Fingerprint unlock disabled hai")
            return
        }
        val prompt = BiometricPrompt(
            this,
            ContextCompat.getMainExecutor(this),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) = onSuccess()
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) = onError(errString.toString())
                override fun onAuthenticationFailed() = onError("Fingerprint match nahi hua")
            },
        )
        prompt.authenticate(
            BiometricPrompt.PromptInfo.Builder()
                .setTitle("Data Safety Status")
                .setSubtitle("Fingerprint se owner verify karein")
                .setNegativeButtonText("Use PIN")
                .build(),
        )
    }
}

@Composable
private fun DataSafetyRoot(
    verifyPin: (String) -> PinVerificationResult,
    biometricAvailability: BiometricAvailability,
    requestBiometric: (() -> Unit, (String) -> Unit) -> Unit,
    loadStatus: () -> Pair<DataSafetyStatus, RecordStoreLoadState>,
    openBackup: () -> Unit,
    close: () -> Unit,
) {
    var unlocked by rememberSaveable { mutableStateOf(false) }
    if (!unlocked) {
        SafetyUnlockScreen(
            biometricAvailability = biometricAvailability,
            verifyPin = verifyPin,
            requestBiometric = requestBiometric,
            success = { unlocked = true },
            close = close,
        )
    } else {
        SafetyDashboard(loadStatus, openBackup, close)
    }
}

@Composable
private fun SafetyUnlockScreen(
    biometricAvailability: BiometricAvailability,
    verifyPin: (String) -> PinVerificationResult,
    requestBiometric: (() -> Unit, (String) -> Unit) -> Unit,
    success: () -> Unit,
    close: () -> Unit,
) {
    val biometricFirst = biometricAvailability == BiometricAvailability.AVAILABLE
    var usePin by rememberSaveable { mutableStateOf(!biometricFirst) }
    var pin by rememberSaveable { mutableStateOf("") }
    var message by rememberSaveable {
        mutableStateOf(if (biometricFirst) "Fingerprint se owner verify karein" else "Data Safety dekhne ke liye PIN verify karein")
    }
    Column(
        Modifier.fillMaxSize().background(Color(0xFFF6F7FB)).padding(20.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(Icons.Default.Security, null, tint = Color(0xFF171752))
        Text("Data Safety Status", fontSize = 27.sp, fontWeight = FontWeight.Bold, color = Color(0xFF171752))
        Text(message, color = Color.Gray)
        Card(Modifier.fillMaxWidth().padding(top = 18.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (!usePin && biometricFirst) {
                    Button(
                        onClick = { requestBiometric(success) { message = it } },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Default.Fingerprint, null)
                        Text("  Fingerprint se Continue")
                    }
                    TextButton(
                        onClick = { usePin = true; message = "6-digit PIN daalein" },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Use PIN instead") }
                } else {
                    OutlinedTextField(
                        pin,
                        { pin = it.filter(Char::isDigit).take(6) },
                        label = { Text("6-digit PIN") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Button(
                        onClick = {
                            when (val result = verifyPin(pin)) {
                                PinVerificationResult.Success -> success()
                                PinVerificationResult.NotConfigured -> message = "Main app mein PIN setup nahi mila"
                                is PinVerificationResult.Locked -> message = "Security lock active hai"
                                is PinVerificationResult.Failure -> message = "Galat PIN. Attempts: ${result.attempts}"
                            }
                            pin = ""
                        },
                        enabled = pin.length == 6,
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("PIN Verify") }
                    if (biometricFirst) {
                        TextButton(
                            onClick = { usePin = false; pin = ""; message = "Fingerprint se owner verify karein" },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Icon(Icons.Default.Fingerprint, null)
                            Text("  Use Fingerprint")
                        }
                    }
                }
                OutlinedButton(onClick = close, modifier = Modifier.fillMaxWidth()) { Text("Close") }
            }
        }
    }
}

@Composable
private fun SafetyDashboard(
    loadStatus: () -> Pair<DataSafetyStatus, RecordStoreLoadState>,
    openBackup: () -> Unit,
    close: () -> Unit,
) {
    var data by remember { mutableStateOf(loadStatus()) }
    val status = data.first
    val storeState = data.second
    val green = Color(0xFF138A4A)
    val red = Color(0xFFB3261E)
    val amber = Color(0xFF9A6700)

    Column(Modifier.fillMaxSize().background(Color(0xFFF6F7FB)).padding(16.dp)) {
        Text("Data Safety Status", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Color(0xFF171752))
        Text("Encrypted journal, local recovery aur backup readiness", color = Color.Gray)
        LazyColumn(
            modifier = Modifier.weight(1f).padding(top = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                StatusCard(
                    "Business records",
                    when (storeState) {
                        is RecordStoreLoadState.Ready -> if (storeState.recoveredFromSafetyCopy) "Recovered from local safety copy" else "Encrypted store verified"
                        is RecordStoreLoadState.Corrupt -> "Recovery required: ${storeState.reason}"
                    },
                    if (storeState is RecordStoreLoadState.Ready) green else red,
                )
                StatusCard(
                    "Audit journal",
                    if (status.journalValid) "Hash chain verified • ${status.events.size} entries" else status.journalError ?: "Journal verification failed",
                    if (status.journalValid) green else red,
                )
                StatusCard(
                    "Backup status",
                    if (status.backupDue) "BACKUP DUE • ${status.changesSinceBackup} changes since verified backup" else "Up to date • ${status.changesSinceBackup} changes",
                    if (status.backupDue) amber else green,
                )
                StatusCard(
                    "Last verified backup",
                    if (status.lastVerifiedBackupAt > 0) DateFormat.getDateTimeInstance().format(Date(status.lastVerifiedBackupAt)) else "Abhi verified backup record nahi hai",
                    if (status.lastVerifiedBackupAt > 0) green else amber,
                )
                if (status.lastVerifiedBackupSha256.isNotBlank()) {
                    Text("Backup SHA-256: ${status.lastVerifiedBackupSha256.take(24)}…", color = Color.Gray, fontSize = 12.sp)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = openBackup, modifier = Modifier.weight(1f)) { Text("Backup Banaye") }
                    OutlinedButton(onClick = { data = loadStatus() }, modifier = Modifier.weight(1f)) { Text("Refresh") }
                }
                Text("Recent Activity / Audit Trail", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text("Har entry hash-chain se tamper-check hoti hai.", color = Color.Gray, fontSize = 12.sp)
            }
            if (status.events.isEmpty()) item { Text("Abhi journal entry nahi hai. Agla committed save automatically record hoga.", color = Color.Gray) }
            items(status.events.asReversed().take(150), key = { it.id }) { event ->
                Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(event.title, fontWeight = FontWeight.Bold)
                        Text(event.detail, color = Color.Gray)
                        Text(DateFormat.getDateTimeInstance().format(Date(event.createdAt)), color = Color.Gray, fontSize = 11.sp)
                        Text("${event.type} • Hash ${event.hash.take(12)}…", color = Color.Gray, fontSize = 10.sp)
                    }
                }
            }
            item { OutlinedButton(onClick = close, modifier = Modifier.fillMaxWidth()) { Text("Close") } }
        }
    }
}

@Composable
private fun StatusCard(title: String, detail: String, color: Color) {
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(Modifier.padding(12.dp)) {
            Text(title, fontWeight = FontWeight.Bold)
            Text(detail, color = color)
        }
    }
}
