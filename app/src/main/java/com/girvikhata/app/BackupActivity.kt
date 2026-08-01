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
import androidx.compose.material.icons.filled.Key
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
import com.girvikhata.app.backup.PortableAppBundleCodec
import com.girvikhata.app.backup.PortableBackupCrypto
import com.girvikhata.app.backup.PortableMediaSupport
import com.girvikhata.app.backup.RecoveryKeyStore
import com.girvikhata.app.backup.SnapshotPortableCodec
import com.girvikhata.app.data.DataSafetyJournal
import com.girvikhata.app.data.EncryptedMasterCatalogStore
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
    private lateinit var masterStore: EncryptedMasterCatalogStore
    private lateinit var journal: DataSafetyJournal
    private lateinit var recoveryKeyStore: RecoveryKeyStore

    private var pendingBackup: PendingExternalBackup? = null
    private var message by mutableStateOf("Recovery Key se portable encrypted backup banega")
    private var busy by mutableStateOf(false)

    private val createDocument = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream"),
    ) { uri -> handleDocumentResult(uri) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        security = SecurityPreferences(applicationContext)
        store = EncryptedRecordStore(applicationContext)
        masterStore = EncryptedMasterCatalogStore(applicationContext)
        journal = DataSafetyJournal(applicationContext)
        recoveryKeyStore = RecoveryKeyStore(applicationContext)
        setContent {
            MaterialTheme {
                BackupRoot(
                    verifyPin = { security.verify(it.toCharArray()) },
                    message = message,
                    busy = busy,
                    keyFingerprint = recoveryKeyStore.fingerprint(),
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

    private fun prepareAndChooseDestination() {
        if (busy) return
        busy = true
        runCatching {
            val recoveryKey = recoveryKeyStore.createIfMissing()
            val secret = recoveryKey.toCharArray()
            val snapshot = store.load()
            val masters = masterStore.load()
            val media = PortableMediaSupport.collect(applicationContext)
            val payload = try {
                PortableAppBundleCodec.encodePortable(snapshot, masters, media)
            } finally {
                PortableMediaSupport.clear(media)
            }
            val encrypted = PortableBackupCrypto.encrypt(payload, secret, snapshot.schemaVersion)

            ExternalBackupVerification.verify(
                expectedPackage = encrypted,
                writtenPackage = encrypted,
                expectedPayload = payload,
                expectedSchemaVersion = snapshot.schemaVersion,
                passphrase = secret,
            )
            val decoded = PortableAppBundleCodec.decode(payload)
            require(
                SnapshotPortableCodec.encode(decoded.snapshot)
                    .contentEquals(SnapshotPortableCodec.encode(snapshot)),
            ) { "Business bundle self-check failed" }
            require(decoded.masterCatalog == masters) { "Master bundle self-check failed" }

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
                masterCount = masters.entries.size,
                mediaCount = decoded.portableMedia.size,
            )
            message = "Location choose karein. New phone recovery-ready .gkb verify hoga."
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
            message = "Backup save cancel hua."
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
            val decrypted = PortableBackupCrypto.decrypt(writtenBytes, pending.passphrase)
            val decoded = PortableAppBundleCodec.decode(decrypted.payload)
            require(decoded.snapshot.customers.size == pending.customerCount) { "Written customer count mismatch" }
            require(decoded.snapshot.girvis.size == pending.girviCount) { "Written girvi count mismatch" }
            require(decoded.masterCatalog.entries.size == pending.masterCount) { "Written master count mismatch" }
            require(decoded.mediaCount == pending.mediaCount) { "Written media count mismatch" }

            journal.markVerifiedBackup(verification.sha256, pending.customerCount, pending.girviCount, pending.ledgerCount)
            BackupResult(
                pending.customerCount,
                pending.girviCount,
                pending.ledgerCount,
                pending.masterCount,
                pending.mediaCount,
                verification.sha256,
            )
        }.onSuccess { result ->
            message = "Verified portable backup: ${result.customers} customers • ${result.girvis} girvi • ${result.ledgerEntries} ledger • ${result.masters} masters • ${result.media} photos • SHA ${result.sha256.take(12)}…"
        }.onFailure {
            message = "Backup verify nahi hua: ${it.message ?: "unknown error"}"
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
        val masterCount: Int,
        val mediaCount: Int,
    ) { fun clearSecret() = passphrase.fill('\u0000') }

    private companion object { const val MAX_BACKUP_BYTES = 180 * 1024 * 1024 }
}

private data class BackupResult(
    val customers: Int,
    val girvis: Int,
    val ledgerEntries: Int,
    val masters: Int,
    val media: Int,
    val sha256: String,
)

@Composable
private fun BackupRoot(
    verifyPin: (String) -> PinVerificationResult,
    message: String,
    busy: Boolean,
    keyFingerprint: String?,
    requestExternalBackup: () -> Unit,
    close: () -> Unit,
) {
    var unlocked by rememberSaveable { mutableStateOf(false) }
    if (!unlocked) BackupPinScreen(verifyPin, { unlocked = true }, close)
    else BackupCreateScreen(message, busy, keyFingerprint, requestExternalBackup, close)
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
                    PinVerificationResult.NotConfigured -> pinMessage = "Pehle PIN setup karein"
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
    keyFingerprint: String?,
    requestExternalBackup: () -> Unit,
    close: () -> Unit,
) {
    BackupPanel("Manual Emergency Backup", message) {
        Icon(Icons.Default.Key, null)
        Text("Recovery Key ID: ${keyFingerprint ?: "will be generated"}", fontWeight = FontWeight.Bold)
        Text("Business records, masters aur photos portable encrypted .gkb mein save honge. Naye phone par same Recovery Key se restore hoga.", color = Color.Gray)
        Button(
            onClick = requestExternalBackup,
            enabled = !busy,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF171752)),
        ) { Text(if (busy) "Backup verify ho raha hai..." else "Location Chune Aur Verified .gkb Save Karein") }
        OutlinedButton(onClick = close, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text("Close") }
    }
}

@Composable
private fun BackupPanel(title: String, subtitle: String, content: @Composable () -> Unit) {
    Column(
        Modifier.fillMaxSize().background(Color(0xFFF6F7FB)).padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(if (title.contains("Manual")) Icons.Default.Backup else Icons.Default.Lock, null, tint = Color(0xFF171752))
        Spacer(Modifier.height(10.dp))
        Text(title, fontSize = 27.sp, fontWeight = FontWeight.Bold, color = Color(0xFF171752))
        Text(subtitle, color = Color.Gray)
        Spacer(Modifier.height(18.dp))
        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) { content() }
        }
    }
}
