package com.girvikhata.app

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.girvikhata.app.backup.AutoBackupConfig
import com.girvikhata.app.backup.AutoBackupWorker
import com.girvikhata.app.backup.RecoveryKeyStore
import com.girvikhata.app.security.PinVerificationResult
import com.girvikhata.app.security.SecurityPreferences
import java.text.DateFormat
import java.util.Date

class RecoveryCenterActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val security = SecurityPreferences(applicationContext)
        val keyStore = RecoveryKeyStore(applicationContext)
        val config = AutoBackupConfig(applicationContext)

        setContent {
            MaterialTheme {
                var unlocked by rememberSaveable { mutableStateOf(false) }
                if (!unlocked) {
                    RecoveryPinGate(
                        verify = { security.verify(it.toCharArray()) },
                        onSuccess = { unlocked = true },
                        onClose = ::finish,
                    )
                } else {
                    RecoveryCenterScreen(
                        keyStore = keyStore,
                        config = config,
                        chooseFolder = { uri -> configureFolder(uri, config) },
                        copyKey = ::copyRecoveryKey,
                        backupNow = { AutoBackupWorker.enqueueNow(applicationContext) },
                        openManualBackup = { startActivity(Intent(this, BackupActivity::class.java)) },
                        openRestore = { startActivity(Intent(this, RestoreActivity::class.java)) },
                        close = ::finish,
                    )
                }
            }
        }
    }

    private fun configureFolder(uri: Uri, config: AutoBackupConfig) {
        val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        contentResolver.takePersistableUriPermission(uri, flags)
        RecoveryKeyStore(applicationContext).createIfMissing()
        config.configure(uri, enabled = true)
        AutoBackupWorker.scheduleDaily(applicationContext)
        AutoBackupWorker.enqueueNow(applicationContext)
    }

    private fun copyRecoveryKey(value: String) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("Girvi Khata Recovery Key", value))
    }
}

@Composable
private fun RecoveryPinGate(
    verify: (String) -> PinVerificationResult,
    onSuccess: () -> Unit,
    onClose: () -> Unit,
) {
    var pin by rememberSaveable { mutableStateOf("") }
    var message by rememberSaveable { mutableStateOf("Recovery settings ke liye PIN verify karein") }
    RecoveryShell("Recovery Center", message) {
        OutlinedTextField(
            value = pin,
            onValueChange = { pin = it.filter(Char::isDigit).take(6) },
            label = { Text("6-digit PIN") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
        )
        Button(
            enabled = pin.length == 6,
            onClick = {
                when (val result = verify(pin)) {
                    PinVerificationResult.Success -> onSuccess()
                    PinVerificationResult.NotConfigured -> message = "Pehle PIN setup karein"
                    is PinVerificationResult.Locked -> message = "Security lock active hai"
                    is PinVerificationResult.Failure -> message = "Galat PIN. Attempts: ${result.attempts}"
                }
                pin = ""
            },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("PIN Verify") }
        OutlinedButton(onClick = onClose, modifier = Modifier.fillMaxWidth()) { Text("Close") }
    }
}

@Composable
private fun RecoveryCenterScreen(
    keyStore: RecoveryKeyStore,
    config: AutoBackupConfig,
    chooseFolder: (Uri) -> Unit,
    copyKey: (String) -> Unit,
    backupNow: () -> Unit,
    openManualBackup: () -> Unit,
    openRestore: () -> Unit,
    close: () -> Unit,
) {
    val context = LocalContext.current
    var refresh by remember { mutableIntStateOf(0) }
    var revealKey by rememberSaveable { mutableStateOf(false) }
    var acknowledgementChecked by rememberSaveable { mutableStateOf(false) }
    var message by rememberSaveable { mutableStateOf("Recovery key ko phone ke bahar likh kar rakhein") }
    val folderPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            runCatching { chooseFolder(uri) }
                .onSuccess { message = "Automatic backup configured. First backup queued."; refresh++ }
                .onFailure { message = it.message ?: "Backup folder configure nahi hua" }
        }
    }
    val status = remember(refresh) { config.status() }
    val key = remember(refresh, revealKey) {
        if (!revealKey) null else runCatching { keyStore.createIfMissing() }.getOrNull()
    }
    val fingerprint = remember(refresh) { keyStore.fingerprint() }

    RecoveryShell("Khata Recovery", message) {
        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFFEAF7EF))) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Key, null)
                    Text("  Recovery Key", fontWeight = FontWeight.Bold)
                }
                Text(if (keyStore.hasRecoveryKey()) "Configured • ID ${fingerprint ?: "—"}" else "Not configured")
                if (key != null) {
                    Text(key, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text("Is key ka screenshot/print ya paper copy phone se alag rakhein.", color = Color(0xFF8A4B00))
                    OutlinedButton(onClick = { copyKey(key); message = "Recovery key clipboard me copy hui" }) { Text("Copy Recovery Key") }
                    if (!status.recoveryKeyAcknowledged) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = acknowledgementChecked, onCheckedChange = { acknowledgementChecked = it })
                            Text("Maine Recovery Key phone ke bahar safe rakh li hai")
                        }
                        Button(
                            enabled = acknowledgementChecked,
                            onClick = {
                                config.acknowledgeRecoveryKey()
                                message = "Recovery Key confirmed. Ab cloud folder choose kar sakte hain."
                                refresh++
                            },
                        ) { Text("Confirm Recovery Key Saved") }
                    } else {
                        Text("✓ External Recovery Key confirmed", color = Color(0xFF138A4A), fontWeight = FontWeight.Bold)
                    }
                }
                Button(onClick = { revealKey = !revealKey; if (revealKey) refresh++ }) {
                    Text(if (revealKey) "Hide Recovery Key" else if (keyStore.hasRecoveryKey()) "Show Recovery Key" else "Generate Recovery Key")
                }
            }
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CloudDone, null)
                    Text("  Automatic Off-device Backup", fontWeight = FontWeight.Bold)
                }
                Text(if (status.enabled) "ON ✅" else "Not configured")
                Text("Folder: ${status.folderUri?.takeLast(46) ?: "Google Drive / cloud folder choose karein"}", fontSize = 12.sp)
                if (status.lastSuccessAt > 0) Text("Last safe backup: ${DateFormat.getDateTimeInstance().format(Date(status.lastSuccessAt))}")
                Text("Generations kept: ${status.generationCount} / 12")
                status.lastError?.let { Text("Last error: $it", color = MaterialTheme.colorScheme.error) }
                Button(
                    enabled = status.recoveryKeyAcknowledged,
                    onClick = { folderPicker.launch(null) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(if (!status.recoveryKeyAcknowledged) "Pehle Recovery Key confirm karein" else if (status.folderUri == null) "Choose Google Drive / Cloud Folder" else "Change Backup Folder")
                }
                OutlinedButton(
                    enabled = status.enabled && keyStore.hasRecoveryKey(),
                    onClick = { backupNow(); message = "Verified backup queued"; refresh++ },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Backup Now") }
                if (status.enabled) {
                    OutlinedButton(
                        onClick = {
                            config.setEnabled(false)
                            AutoBackupWorker.cancel(context)
                            message = "Automatic backup paused"
                            refresh++
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Pause Automatic Backup") }
                } else if (status.folderUri != null && status.recoveryKeyAcknowledged) {
                    OutlinedButton(
                        onClick = {
                            config.setEnabled(true)
                            AutoBackupWorker.scheduleDaily(context)
                            AutoBackupWorker.enqueueNow(context)
                            message = "Automatic backup resumed"
                            refresh++
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Resume Automatic Backup") }
                }
            }
        }

        Button(onClick = openManualBackup, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.Backup, null)
            Text("  Manual Emergency .gkb Backup")
        }
        OutlinedButton(onClick = openRestore, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.Restore, null)
            Text("  Recover / Restore on This Phone")
        }
        OutlinedButton(onClick = close, modifier = Modifier.fillMaxWidth()) { Text("Close") }
    }
}

@Composable
private fun RecoveryShell(title: String, subtitle: String, content: @Composable () -> Unit) {
    Column(
        Modifier.fillMaxSize().background(Color(0xFFF6F7FB)).verticalScroll(rememberScrollState()).padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top,
    ) {
        Spacer(Modifier.height(26.dp))
        Text(title, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color(0xFF171752))
        Text(subtitle, color = Color.Gray)
        Spacer(Modifier.height(16.dp))
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) { content() }
    }
}
