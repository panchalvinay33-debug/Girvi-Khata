package com.girvikhata.app

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
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
import com.girvikhata.app.backup.PortableBackupCrypto
import com.girvikhata.app.backup.SnapshotPortableCodec
import com.girvikhata.app.data.DataSafetyJournal
import com.girvikhata.app.data.EncryptedRecordStore
import com.girvikhata.app.export.SecureShare
import com.girvikhata.app.security.PinVerificationResult
import com.girvikhata.app.security.SecurityPreferences
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class BackupActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val security = SecurityPreferences(applicationContext)
        val store = EncryptedRecordStore(applicationContext)
        val journal = DataSafetyJournal(applicationContext)
        setContent {
            MaterialTheme {
                BackupRoot(
                    verifyPin = { security.verify(it.toCharArray()) },
                    createBackup = { passphrase ->
                        val snapshot = store.load()
                        val payload = SnapshotPortableCodec.encode(snapshot)
                        val encrypted = PortableBackupCrypto.encrypt(payload, passphrase.toCharArray(), snapshot.schemaVersion)
                        val verified = PortableBackupCrypto.decrypt(encrypted, passphrase.toCharArray())
                        check(verified.payload.contentEquals(payload)) { "Backup package read-back verification failed" }
                        check(verified.schemaVersion == snapshot.schemaVersion) { "Backup schema verification failed" }
                        val packageSha = DataSafetyJournal.sha256(encrypted)
                        val customerCount = snapshot.customers.size
                        val girviCount = snapshot.girvis.size
                        val ledgerCount = snapshot.girvis.sumOf { it.payments.size }
                        journal.markVerifiedBackup(packageSha, customerCount, girviCount, ledgerCount)
                        val stamp = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())
                        SecureShare.shareBinary(
                            this,
                            "girvi-khata-backup-$stamp.gkb",
                            "application/octet-stream",
                            encrypted,
                            "Girvi Khata encrypted backup",
                        )
                        BackupResult(customerCount, girviCount, ledgerCount, packageSha)
                    },
                    close = ::finish,
                )
            }
        }
    }
}

private data class BackupResult(val customers: Int, val girvis: Int, val ledgerEntries: Int, val sha256: String)

@Composable
private fun BackupRoot(
    verifyPin: (String) -> PinVerificationResult,
    createBackup: (String) -> BackupResult,
    close: () -> Unit,
) {
    var unlocked by rememberSaveable { mutableStateOf(false) }
    if (!unlocked) BackupPinScreen(verifyPin, { unlocked = true }, close)
    else BackupCreateScreen(createBackup, close)
}

@Composable
private fun BackupPinScreen(verifyPin: (String) -> PinVerificationResult, success: () -> Unit, close: () -> Unit) {
    var pin by rememberSaveable { mutableStateOf("") }
    var message by rememberSaveable { mutableStateOf("Backup ke liye PIN verify karein") }
    BackupPanel("Encrypted Backup", message) {
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
                    PinVerificationResult.NotConfigured -> message = "Main app mein pehle PIN setup karein"
                    is PinVerificationResult.Locked -> message = "Security lock active hai"
                    is PinVerificationResult.Failure -> message = "Galat PIN. Attempts: ${result.attempts}"
                }
                pin = ""
            },
            enabled = pin.length == 6,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5146B8)),
        ) { Text("PIN Verify") }
        OutlinedButton(onClick = close, modifier = Modifier.fillMaxWidth()) { Text("Close") }
    }
}

@Composable
private fun BackupCreateScreen(createBackup: (String) -> BackupResult, close: () -> Unit) {
    var passphrase by rememberSaveable { mutableStateOf("") }
    var confirm by rememberSaveable { mutableStateOf("") }
    var message by rememberSaveable { mutableStateOf("12+ characters, letters aur digits zaroori") }
    var busy by remember { mutableStateOf(false) }
    BackupPanel("Portable Encrypted Backup", message) {
        Text("Package encrypt hone ke baad app usko decrypt/read-back karke verify karegi. Share sheet mein Files/Drive choose karke external copy zaroor save karein.", color = Color.Gray)
        OutlinedTextField(passphrase, { passphrase = it }, label = { Text("Recovery passphrase") }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
        OutlinedTextField(confirm, { confirm = it }, label = { Text("Passphrase dobara") }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
        Button(
            onClick = {
                when {
                    passphrase != confirm -> message = "Dono passphrase match nahi kar rahe"
                    else -> {
                        busy = true
                        runCatching { createBackup(passphrase) }
                            .onSuccess { result ->
                                message = "Verified package ready: ${result.customers} customers • ${result.girvis} girvi • ${result.ledgerEntries} ledger • SHA ${result.sha256.take(12)}…"
                                passphrase = ""; confirm = ""
                            }
                            .onFailure { message = it.message ?: "Backup create nahi hua" }
                        busy = false
                    }
                }
            },
            enabled = !busy,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF171752)),
        ) { Text(if (busy) "Encrypt & verify ho raha hai..." else "Backup Verify Karke Share") }
        OutlinedButton(onClick = close, modifier = Modifier.fillMaxWidth()) { Text("Close") }
    }
}

@Composable
private fun BackupPanel(title: String, subtitle: String, content: @Composable () -> Unit) {
    Column(
        Modifier.fillMaxSize().background(Color(0xFFF6F7FB)).padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(if (title.contains("Portable")) Icons.Default.Backup else Icons.Default.Lock, null, tint = Color(0xFF171752))
        Spacer(Modifier.height(10.dp))
        Text(title, fontSize = 27.sp, fontWeight = FontWeight.Bold, color = Color(0xFF171752))
        Text(subtitle, color = Color.Gray)
        Spacer(Modifier.height(18.dp))
        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) { content() }
        }
    }
}
