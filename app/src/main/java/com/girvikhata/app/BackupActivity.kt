package com.girvikhata.app

import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
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
import com.girvikhata.app.backup.ExternalBackupVerification
import com.girvikhata.app.backup.PortableBackupCrypto
import com.girvikhata.app.backup.SnapshotPortableCodec
import com.girvikhata.app.data.DataSafetyJournal
import com.girvikhata.app.data.EncryptedRecordStore
import com.girvikhata.app.security.PinVerificationResult
import com.girvikhata.app.security.SecurityPreferences
import java.io.ByteArrayOutputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class BackupActivity : FragmentActivity() {
    private lateinit var security: SecurityPreferences
    private lateinit var store: EncryptedRecordStore
    private lateinit var journal: DataSafetyJournal

    private var pendingBackup: PendingExternalBackup? = null
    private var message by mutableStateOf("12+ characters, letters aur digits zaroori")
    private var busy by mutableStateOf(false)

    private val createDocument = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream"),
    ) { uri -> handleDocumentResult(uri) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        security = SecurityPreferences(applicationContext)
        store = EncryptedRecordStore(applicationContext)
        journal = DataSafetyJournal(applicationContext)

        setContent {
            MaterialTheme {
                BackupRoot(
                    verifyPin = { security.verify(it.toCharArray()) },
                    message = message,
                    busy = busy,
                    requestExternalBackup = ::prepareAndChooseDestination,
                    close = ::finish,
                )
            }
        }
    }

    override fun onDestroy() {
        pendingBackup?.clearSecret()
        pendingBackup = null
        super.onDestroy()
    }

    private fun prepareAndChooseDestination(passphrase: String) {
        if (busy) return
        busy = true
        runCatching {
            val secret = passphrase.toCharArray()
            val snapshot = store.load()
            val payload = SnapshotPortableCodec.encode(snapshot)
            val encrypted = PortableBackupCrypto.encrypt(payload, secret, snapshot.schemaVersion)
            ExternalBackupVerification.verify(
                expectedPackage = encrypted,
                writtenPackage = encrypted,
                expectedPayload = payload,
                expectedSchemaVersion = snapshot.schemaVersion,
                passphrase = secret,
            )

            val stamp = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())
            pendingBackup?.clearSecret()
            pendingBackup = PendingExternalBackup(
                fileName = "girvi-khata-backup-$stamp.gkb",
                encryptedBytes = encrypted,
                originalPayload = payload,
                schemaVersion = snapshot.schemaVersion,
                passphrase = secret,
                customerCount = snapshot.customers.size,
                girviCount = snapshot.girvis.size,
                ledgerCount = snapshot.girvis.sumOf { it.payments.size },
            )
            message = "Files/Drive location choose karein. Save ke baad app wahi file wapas read karke verify karegi."
            createDocument.launch(pendingBackup!!.fileName)
        }.onFailure {
            pendingBackup?.clearSecret()
            pendingBackup = null
            message = it.message ?: "Backup prepare nahi hua"
        }
        busy = false
    }

    private fun handleDocumentResult(uri: Uri?) {
        val pending = pendingBackup
        if (uri == null || pending == null) {
            pending?.clearSecret()
            pendingBackup = null
            message = "Backup save cancel hua. Safety Status reset nahi hua."
            busy = false
            return
        }

        busy = true
        runCatching {
            writeDocument(uri, pending.encryptedBytes)
            val writtenBytes = readDocument(uri)
            val verification = ExternalBackupVerification.verify(
                expectedPackage = pending.encryptedBytes,
                writtenPackage = writtenBytes,
                expectedPayload = pending.originalPayload,
                expectedSchemaVersion = pending.schemaVersion,
                passphrase = pending.passphrase,
            )

            journal.markVerifiedBackup(
                verification.sha256,
                pending.customerCount,
                pending.girviCount,
                pending.ledgerCount,
            )
            BackupResult(
                pending.customerCount,
                pending.girviCount,
                pending.ledgerCount,
                verification.sha256,
            )
        }.onSuccess { result ->
            message = "External backup verified: ${result.customers} customers • ${result.girvis} girvi • ${result.ledgerEntries} ledger • SHA ${result.sha256.take(12)}…"
        }.onFailure {
            message = "File save/read-back verify nahi hua: ${it.message ?: "unknown error"}. Backup due status unchanged hai."
        }
        pending.clearSecret()
        pendingBackup = null
        busy = false
    }

    private fun writeDocument(uri: Uri, bytes: ByteArray) {
        val descriptor = runCatching { contentResolver.openFileDescriptor(uri, "rwt") }.getOrNull()
            ?: runCatching { contentResolver.openFileDescriptor(uri, "w") }.getOrNull()
            ?: error("Selected file open nahi hui")
        descriptor.use { pfd ->
            FileOutputStream(pfd.fileDescriptor).use { output ->
                output.write(bytes)
                output.flush()
                runCatching { pfd.fileDescriptor.sync() }
            }
        }
    }

    private fun readDocument(uri: Uri): ByteArray {
        val input = contentResolver.openInputStream(uri) ?: error("Saved file read nahi hui")
        return input.use { stream ->
            val output = ByteArrayOutputStream()
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            var total = 0
            while (true) {
                val count = stream.read(buffer)
                if (count < 0) break
                total += count
                require(total <= MAX_BACKUP_BYTES) { "Saved backup file size invalid" }
                output.write(buffer, 0, count)
            }
            output.toByteArray().also { require(it.isNotEmpty()) { "Saved backup empty hai" } }
        }
    }

    private data class PendingExternalBackup(
        val fileName: String,
        val encryptedBytes: ByteArray,
        val originalPayload: ByteArray,
        val schemaVersion: Int,
        val passphrase: CharArray,
        val customerCount: Int,
        val girviCount: Int,
        val ledgerCount: Int,
    ) {
        fun clearSecret() = passphrase.fill('\u0000')
    }

    private companion object {
        const val MAX_BACKUP_BYTES = 128 * 1024 * 1024
    }
}

private data class BackupResult(
    val customers: Int,
    val girvis: Int,
    val ledgerEntries: Int,
    val sha256: String,
)

@Composable
private fun BackupRoot(
    verifyPin: (String) -> PinVerificationResult,
    message: String,
    busy: Boolean,
    requestExternalBackup: (String) -> Unit,
    close: () -> Unit,
) {
    var unlocked by rememberSaveable { mutableStateOf(false) }
    if (!unlocked) {
        BackupPinScreen(verifyPin, { unlocked = true }, close)
    } else {
        BackupCreateScreen(message, busy, requestExternalBackup, close)
    }
}

@Composable
private fun BackupPinScreen(
    verifyPin: (String) -> PinVerificationResult,
    success: () -> Unit,
    close: () -> Unit,
) {
    var pin by rememberSaveable { mutableStateOf("") }
    var pinMessage by rememberSaveable { mutableStateOf("Backup ke liye PIN verify karein") }
    BackupPanel("Encrypted Backup", pinMessage) {
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
                    PinVerificationResult.NotConfigured -> pinMessage = "Main app mein pehle PIN setup karein"
                    is PinVerificationResult.Locked -> pinMessage = "Security lock active hai"
                    is PinVerificationResult.Failure -> pinMessage = "Galat PIN. Attempts: ${result.attempts}"
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
private fun BackupCreateScreen(
    message: String,
    busy: Boolean,
    requestExternalBackup: (String) -> Unit,
    close: () -> Unit,
) {
    var passphrase by rememberSaveable { mutableStateOf("") }
    var confirm by rememberSaveable { mutableStateOf("") }
    var localError by rememberSaveable { mutableStateOf<String?>(null) }

    BackupPanel("Portable Encrypted Backup", localError ?: message) {
        Text(
            "App direct Files/Drive location par .gkb save karegi, wahi bytes wapas read karegi aur decrypt/schema verify hone ke baad hi backup successful maanegi.",
            color = Color.Gray,
        )
        OutlinedTextField(
            passphrase,
            { passphrase = it },
            label = { Text("Recovery passphrase") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            confirm,
            { confirm = it },
            label = { Text("Passphrase dobara") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
        )
        Button(
            onClick = {
                when {
                    passphrase != confirm -> localError = "Dono passphrase match nahi kar rahe"
                    else -> {
                        localError = null
                        requestExternalBackup(passphrase)
                        passphrase = ""
                        confirm = ""
                    }
                }
            },
            enabled = !busy,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF171752)),
        ) { Text(if (busy) "Backup verify ho raha hai..." else "Location Chune Aur Verified Backup Save Karein") }
        OutlinedButton(onClick = close, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text("Close") }
    }
}

@Composable
private fun BackupPanel(
    title: String,
    subtitle: String,
    content: @Composable () -> Unit,
) {
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
        Card(
            Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
        ) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) { content() }
        }
    }
}
