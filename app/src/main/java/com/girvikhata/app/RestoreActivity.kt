package com.girvikhata.app

import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Security
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
import com.girvikhata.app.backup.PortableAppBundleCodec
import com.girvikhata.app.backup.PortableBackupCrypto
import com.girvikhata.app.backup.SnapshotInspection
import com.girvikhata.app.backup.SnapshotPortableCodec
import com.girvikhata.app.data.AppSnapshot
import com.girvikhata.app.data.EncryptedMasterCatalogStore
import com.girvikhata.app.data.EncryptedRecordStore
import com.girvikhata.app.data.RecordStoreLoadState
import com.girvikhata.app.domain.MasterCatalog
import com.girvikhata.app.security.PinVerificationResult
import com.girvikhata.app.security.SecurityPreferences
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class RestoreActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val security = SecurityPreferences(applicationContext)
        val store = EncryptedRecordStore(applicationContext)
        val masterStore = EncryptedMasterCatalogStore(applicationContext)
        setContent {
            MaterialTheme {
                RestoreRoot(
                    verifyPin = { security.verify(it.toCharArray()) },
                    readPackage = ::readPackage,
                    decryptPreview = { bytes, phrase ->
                        val decrypted = PortableBackupCrypto.decrypt(bytes, phrase.toCharArray())
                        val bundle = PortableAppBundleCodec.decode(decrypted.payload)
                        require(bundle.snapshot.schemaVersion == decrypted.schemaVersion) { "Backup schema mismatch" }
                        RestorePreview(
                            snapshot = bundle.snapshot,
                            masterCatalog = bundle.masterCatalog,
                            containsPortableMasters = bundle.containsPortableMasters,
                            inspection = SnapshotPortableCodec.inspect(SnapshotPortableCodec.encode(bundle.snapshot)),
                            sha256 = decrypted.payloadSha256,
                        )
                    },
                    commitRestore = { preview, phrase ->
                        when (val current = store.loadState()) {
                            is RecordStoreLoadState.Ready -> createSafetyBackup(current.snapshot, masterStore.load(), phrase)
                            is RecordStoreLoadState.Corrupt -> quarantineDamagedPrimary()
                        }
                        store.save(preview.snapshot)
                        if (preview.containsPortableMasters) masterStore.save(preview.masterCatalog)

                        store.load().also { reloaded ->
                            require(reloaded.customers.size == preview.inspection.customerCount) { "Restore customer verification failed" }
                            require(reloaded.girvis.size == preview.inspection.girviCount) { "Restore girvi verification failed" }
                            require(reloaded.girvis.sumOf { it.payments.size } == preview.inspection.paymentEntryCount) { "Restore ledger verification failed" }
                        }
                        if (preview.containsPortableMasters) {
                            require(masterStore.load() == preview.masterCatalog) { "Restore master catalog verification failed" }
                        }
                    },
                    close = ::finish,
                )
            }
        }
    }

    private fun readPackage(uri: Uri): ByteArray {
        val bytes = contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: error("Selected backup read nahi hua")
        require(bytes.isNotEmpty()) { "Backup file empty hai" }
        require(bytes.size <= 128 * 1024 * 1024) { "Backup file bahut badi hai" }
        return bytes
    }

    private fun createSafetyBackup(current: AppSnapshot, masters: MasterCatalog, phrase: String) {
        val payload = PortableAppBundleCodec.encode(current, masters)
        val bytes = PortableBackupCrypto.encrypt(payload, phrase.toCharArray(), current.schemaVersion)
        val dir = File(filesDir, "restore_safety").apply { mkdirs() }
        val stamp = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())
        val target = File(dir, "pre-restore-$stamp.gkb")
        target.writeBytes(bytes)
        require(target.readBytes().contentEquals(bytes)) { "Pre-restore safety backup verification failed" }
        val decrypted = PortableBackupCrypto.decrypt(target.readBytes(), phrase.toCharArray())
        val decoded = PortableAppBundleCodec.decode(decrypted.payload)
        require(decoded.snapshot == current && decoded.masterCatalog == masters) { "Pre-restore bundle verification failed" }
        dir.listFiles()?.sortedByDescending { it.lastModified() }?.drop(3)?.forEach(File::delete)
    }

    private fun quarantineDamagedPrimary() {
        val primary = File(filesDir, "business_records_v1.bin")
        if (!primary.exists()) return
        val dir = File(filesDir, "record_quarantine").apply { mkdirs() }
        val target = File(dir, "damaged-before-restore-${System.currentTimeMillis()}.bin")
        primary.copyTo(target, overwrite = false)
        require(target.length() == primary.length()) { "Damaged primary quarantine verification failed" }
        require(primary.delete()) { "Damaged primary ko quarantine nahi kiya ja saka" }
        dir.listFiles()?.sortedByDescending { it.lastModified() }?.drop(2)?.forEach(File::delete)
    }
}

private data class RestorePreview(
    val snapshot: AppSnapshot,
    val masterCatalog: MasterCatalog,
    val containsPortableMasters: Boolean,
    val inspection: SnapshotInspection,
    val sha256: String,
)

@Composable
private fun RestoreRoot(
    verifyPin: (String) -> PinVerificationResult,
    readPackage: (Uri) -> ByteArray,
    decryptPreview: (ByteArray, String) -> RestorePreview,
    commitRestore: (RestorePreview, String) -> Unit,
    close: () -> Unit,
) {
    var unlocked by rememberSaveable { mutableStateOf(false) }
    if (!unlocked) RestorePinScreen(verifyPin, { unlocked = true }, close)
    else RestoreWizard(readPackage, decryptPreview, commitRestore, close)
}

@Composable
private fun RestorePinScreen(verifyPin: (String) -> PinVerificationResult, success: () -> Unit, close: () -> Unit) {
    var pin by rememberSaveable { mutableStateOf("") }
    var message by rememberSaveable { mutableStateOf("Restore ke liye PIN verify karein") }
    RestorePanel("Encrypted Restore", message, Icons.Default.Security) {
        OutlinedTextField(
            pin, { pin = it.filter(Char::isDigit).take(6) }, label = { Text("6-digit PIN") },
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
            }, enabled = pin.length == 6, modifier = Modifier.fillMaxWidth(),
        ) { Text("PIN Verify") }
        OutlinedButton(onClick = close, modifier = Modifier.fillMaxWidth()) { Text("Close") }
    }
}

@Composable
private fun RestoreWizard(
    readPackage: (Uri) -> ByteArray,
    decryptPreview: (ByteArray, String) -> RestorePreview,
    commitRestore: (RestorePreview, String) -> Unit,
    close: () -> Unit,
) {
    var packageBytes by remember { mutableStateOf<ByteArray?>(null) }
    var selectedName by rememberSaveable { mutableStateOf("Koi backup select nahi") }
    var phrase by rememberSaveable { mutableStateOf("") }
    var preview by remember { mutableStateOf<RestorePreview?>(null) }
    var message by rememberSaveable { mutableStateOf("Pehle .gkb backup choose karein") }
    var restored by rememberSaveable { mutableStateOf(false) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        preview = null
        restored = false
        if (uri != null) runCatching { readPackage(uri) }
            .onSuccess { packageBytes = it; selectedName = uri.lastPathSegment ?: "Selected .gkb file"; message = "File selected. Recovery passphrase daalein." }
            .onFailure { message = it.message ?: "Backup file read nahi hui" }
    }

    RestorePanel("Verified Backup Restore", message, Icons.Default.Restore) {
        Text("New backup business + masters restore karegi. Legacy backup business restore karke current masters preserve karegi.", color = Color.Gray)
        OutlinedButton(onClick = { picker.launch(arrayOf("application/octet-stream", "*/*")) }, modifier = Modifier.fillMaxWidth()) { Text("Backup File Choose Karein") }
        Text(selectedName, color = Color.Gray, fontSize = 12.sp)
        OutlinedTextField(phrase, { phrase = it }, label = { Text("Recovery passphrase") }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
        Button(
            onClick = {
                val bytes = packageBytes ?: return@Button
                runCatching { decryptPreview(bytes, phrase) }
                    .onSuccess { preview = it; message = "Backup verified. Counts check karke confirm karein." }
                    .onFailure { preview = null; message = it.message ?: "Backup verify nahi hua" }
            }, enabled = packageBytes != null && phrase.length >= 12 && !restored, modifier = Modifier.fillMaxWidth(),
        ) { Text("Decrypt & Verify Preview") }

        preview?.let { data ->
            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFFEAF7EF))) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Verified Backup", fontWeight = FontWeight.Bold, color = Color(0xFF138A4A))
                    Text("Customers: ${data.inspection.customerCount}")
                    Text("Categories: ${data.inspection.categoryCount}")
                    Text("Girvi: ${data.inspection.girviCount}")
                    Text("Ledger entries: ${data.inspection.paymentEntryCount}")
                    Text(if (data.containsPortableMasters) "Masters: ${data.masterCatalog.entries.size} (restore honge)" else "Legacy backup: current masters preserve honge")
                    Text("SHA-256: ${data.sha256.take(16)}…", fontSize = 12.sp, color = Color.Gray)
                }
            }
            Button(
                onClick = {
                    runCatching { commitRestore(data, phrase) }
                        .onSuccess { restored = true; phrase = ""; message = "Restore successful. Main app mein Dobara Check Karein dabayein." }
                        .onFailure { message = it.message ?: "Restore commit failed" }
                }, enabled = !restored, modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB3261E)),
            ) { Text("CONFIRM: Current Data Replace Karein") }
        }
        if (restored) Text("✓ Restore complete", color = Color(0xFF138A4A), fontWeight = FontWeight.Bold)
        OutlinedButton(onClick = close, modifier = Modifier.fillMaxWidth()) { Text("Close") }
    }
}

@Composable
private fun RestorePanel(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector, content: @Composable () -> Unit) {
    Column(
        Modifier.fillMaxSize().background(Color(0xFFF6F7FB)).verticalScroll(rememberScrollState()).padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(icon, null, tint = Color(0xFF171752))
        Spacer(Modifier.height(10.dp))
        Text(title, fontSize = 27.sp, fontWeight = FontWeight.Bold, color = Color(0xFF171752))
        Text(subtitle, color = Color.Gray)
        Spacer(Modifier.height(18.dp))
        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) { content() }
        }
    }
}
