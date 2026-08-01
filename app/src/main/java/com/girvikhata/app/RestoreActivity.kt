package com.girvikhata.app

import android.net.Uri
import android.os.Bundle
import android.os.StatFs
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricPrompt
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
import androidx.compose.material.icons.filled.Fingerprint
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
import com.girvikhata.app.backup.MediaBackupSupport
import com.girvikhata.app.backup.PortableAppBundleCodec
import com.girvikhata.app.backup.PortableBackupCrypto
import com.girvikhata.app.backup.PortableMediaSupport
import com.girvikhata.app.backup.RecoveryKeyStore
import com.girvikhata.app.backup.SnapshotInspection
import com.girvikhata.app.backup.SnapshotPortableCodec
import com.girvikhata.app.data.AppSnapshot
import com.girvikhata.app.data.BlueprintRestoreCoordinator
import com.girvikhata.app.data.EncryptedMasterCatalogStore
import com.girvikhata.app.data.EncryptedRecordStore
import com.girvikhata.app.data.RecordStoreLoadState
import com.girvikhata.app.domain.MasterCatalog
import com.girvikhata.app.security.BiometricAvailability
import com.girvikhata.app.security.BiometricCapability
import com.girvikhata.app.security.PinVerificationResult
import com.girvikhata.app.security.SecurityPreferences
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max

class RestoreActivity : FragmentActivity() {
    private lateinit var security: SecurityPreferences
    private lateinit var biometricCapability: BiometricCapability

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        security = SecurityPreferences(applicationContext)
        biometricCapability = BiometricCapability(applicationContext)
        val store = EncryptedRecordStore(applicationContext)
        val masterStore = EncryptedMasterCatalogStore(applicationContext)
        val freshDevice = !security.hasPin() && runCatching {
            val snapshot = store.load()
            snapshot.customers.isEmpty() && snapshot.girvis.isEmpty()
        }.getOrDefault(false)

        setContent {
            MaterialTheme {
                val availability = if (!freshDevice && security.sessionSettings().biometricUnlockEnabled) {
                    biometricCapability.availability()
                } else {
                    BiometricAvailability.UNSUPPORTED
                }
                RestoreRoot(
                    freshDevice = freshDevice,
                    verifyPin = { security.verify(it.toCharArray()) },
                    biometricAvailability = availability,
                    requestBiometric = ::requestBiometric,
                    readPackage = ::readPackage,
                    decryptPreview = { bytes, phrase ->
                        val decrypted = PortableBackupCrypto.decrypt(bytes, phrase.toCharArray())
                        val bundle = PortableAppBundleCodec.decode(decrypted.payload)
                        require(bundle.snapshot.schemaVersion == decrypted.schemaVersion) { "Backup schema mismatch" }
                        MediaBackupSupport.validate(bundle.encryptedMedia)
                        PortableMediaSupport.validate(bundle.portableMedia)
                        RestorePreview(
                            snapshot = bundle.snapshot,
                            masterCatalog = bundle.masterCatalog,
                            containsPortableMasters = bundle.containsPortableMasters,
                            encryptedMedia = bundle.encryptedMedia,
                            portableMedia = bundle.portableMedia,
                            inspection = SnapshotPortableCodec.inspect(SnapshotPortableCodec.encode(bundle.snapshot)),
                            sha256 = decrypted.payloadSha256,
                            createdAt = decrypted.createdAt,
                        )
                    },
                    commitRestore = { preview, phrase ->
                        ensureRestoreStorage(preview)
                        when (val current = store.loadState()) {
                            is RecordStoreLoadState.Ready -> createSafetyBackup(
                                current.snapshot,
                                masterStore.load(),
                                phrase,
                            )
                            is RecordStoreLoadState.Corrupt -> quarantineDamagedPrimary()
                        }

                        if (freshDevice && preview.encryptedMedia.isNotEmpty() && preview.portableMedia.isEmpty()) {
                            require(preview.encryptedMedia.isEmpty()) {
                                "Ye purana v2 backup hai: business data portable hai lekin photos old phone key se bandhi hain. Original phone par v3 backup banayein, ya photo-less legacy restore ke liye support flow use karein."
                            }
                        }

                        val result = BlueprintRestoreCoordinator(applicationContext).restore(
                            targetBusiness = preview.snapshot,
                            importedMasters = preview.masterCatalog,
                            containsPortableMasters = preview.containsPortableMasters,
                            targetMedia = preview.encryptedMedia,
                            targetPortableMedia = preview.portableMedia,
                        )
                        val reloaded = store.load()
                        require(reloaded.customers.size == preview.inspection.customerCount) { "Restore customer verification failed" }
                        require(reloaded.girvis.size == preview.inspection.girviCount) { "Restore girvi verification failed" }
                        require(reloaded.girvis.sumOf { it.payments.size } == preview.inspection.paymentEntryCount) { "Restore ledger verification failed" }
                        if (preview.portableMedia.isNotEmpty()) {
                            require(PortableMediaSupport.collect(applicationContext).also(PortableMediaSupport::clear).size == preview.portableMedia.size) {
                                "Portable photo restore count verification failed"
                            }
                        } else {
                            require(MediaBackupSupport.collect(filesDir).size == preview.encryptedMedia.size) { "Restore media count verification failed" }
                        }
                        if (preview.containsPortableMasters) require(masterStore.load() == preview.masterCatalog) { "Restore masters verification failed" }

                        if (RecoveryKeyStore.isValid(phrase)) {
                            RecoveryKeyStore(applicationContext).importAndStore(phrase)
                        }
                        result
                    },
                    saveNewPin = { pin -> security.savePin(pin.toCharArray()) },
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
                .setTitle("Encrypted Restore")
                .setSubtitle("Fingerprint se owner verify karein")
                .setNegativeButtonText("Use PIN")
                .build(),
        )
    }

    private fun readPackage(uri: Uri): ByteArray {
        val bytes = contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: error("Selected backup read nahi hua")
        require(bytes.isNotEmpty()) { "Backup file empty hai" }
        require(bytes.size <= MAX_BACKUP_BYTES) { "Backup file bahut badi hai" }
        return bytes
    }

    private fun ensureRestoreStorage(preview: RestorePreview) {
        val estimatedPayload = if (preview.portableMedia.isNotEmpty()) {
            PortableAppBundleCodec.encodePortable(preview.snapshot, preview.masterCatalog, preview.portableMedia).size.toLong()
        } else {
            PortableAppBundleCodec.encode(preview.snapshot, preview.masterCatalog, preview.encryptedMedia).size.toLong()
        }
        val requiredBytes = max(64L * 1024L * 1024L, estimatedPayload * 3L)
        val availableBytes = StatFs(filesDir.absolutePath).availableBytes
        require(availableBytes >= requiredBytes) {
            "Restore ke liye storage kam hai. Required ${requiredBytes / (1024 * 1024)} MB, available ${availableBytes / (1024 * 1024)} MB"
        }
    }

    private fun createSafetyBackup(current: AppSnapshot, masters: MasterCatalog, phrase: String) {
        val media = PortableMediaSupport.collect(applicationContext)
        val payload = try { PortableAppBundleCodec.encodePortable(current, masters, media) }
        finally { PortableMediaSupport.clear(media) }
        val bytes = PortableBackupCrypto.encrypt(payload, phrase.toCharArray(), current.schemaVersion)
        val dir = File(filesDir, "restore_safety").apply { mkdirs() }
        val stamp = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())
        val target = File(dir, "pre-restore-$stamp.gkb")
        target.writeBytes(bytes)
        require(target.readBytes().contentEquals(bytes)) { "Pre-restore safety backup verification failed" }
        val decrypted = PortableBackupCrypto.decrypt(target.readBytes(), phrase.toCharArray())
        val decoded = PortableAppBundleCodec.decode(decrypted.payload)
        require(SnapshotPortableCodec.encode(decoded.snapshot).contentEquals(SnapshotPortableCodec.encode(current))) {
            "Pre-restore business verification failed"
        }
        require(decoded.masterCatalog == masters) { "Pre-restore master verification failed" }
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

    private companion object { const val MAX_BACKUP_BYTES = 180 * 1024 * 1024 }
}

private data class RestorePreview(
    val snapshot: AppSnapshot,
    val masterCatalog: MasterCatalog,
    val containsPortableMasters: Boolean,
    val encryptedMedia: Map<String, ByteArray>,
    val portableMedia: Map<String, ByteArray>,
    val inspection: SnapshotInspection,
    val sha256: String,
    val createdAt: Long,
) {
    val mediaCount: Int get() = if (portableMedia.isNotEmpty()) portableMedia.size else encryptedMedia.size
}

@Composable
private fun RestoreRoot(
    freshDevice: Boolean,
    verifyPin: (String) -> PinVerificationResult,
    biometricAvailability: BiometricAvailability,
    requestBiometric: (() -> Unit, (String) -> Unit) -> Unit,
    readPackage: (Uri) -> ByteArray,
    decryptPreview: (ByteArray, String) -> RestorePreview,
    commitRestore: (RestorePreview, String) -> BlueprintRestoreCoordinator.Result,
    saveNewPin: (String) -> Unit,
    close: () -> Unit,
) {
    var unlocked by rememberSaveable { mutableStateOf(freshDevice) }
    if (!unlocked) {
        RestoreAuthScreen(
            biometricAvailability,
            verifyPin,
            requestBiometric,
            { unlocked = true },
            close,
        )
    } else {
        RestoreWizard(freshDevice, readPackage, decryptPreview, commitRestore, saveNewPin, close)
    }
}

@Composable
private fun RestoreAuthScreen(
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
        mutableStateOf(if (biometricFirst) "Fingerprint se owner verify karein" else "Restore ke liye PIN verify karein")
    }
    RestorePanel("Encrypted Restore", message, Icons.Default.Security) {
        if (!usePin && biometricFirst) {
            Button(
                onClick = { requestBiometric(success) { message = it } },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Default.Fingerprint, null)
                Text("  Fingerprint se Continue")
            }
            TextButton(onClick = { usePin = true; message = "6-digit PIN daalein" }, modifier = Modifier.fillMaxWidth()) {
                Text("Use PIN instead")
            }
        } else {
            OutlinedTextField(
                pin,
                { pin = it.filter(Char::isDigit).take(6) },
                label = { Text("6-digit PIN") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                modifier = Modifier.fillMaxWidth(),
            )
            Button(
                onClick = {
                    when (val result = verifyPin(pin)) {
                        PinVerificationResult.Success -> success()
                        PinVerificationResult.NotConfigured -> message = "New device recovery mode use karein"
                        is PinVerificationResult.Locked -> message = "Security lock active hai"
                        is PinVerificationResult.Failure -> message = "Galat PIN. Attempts: ${result.attempts}"
                    }
                    pin = ""
                },
                enabled = pin.length == 6,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("PIN Verify") }
            if (biometricFirst) TextButton(onClick = { usePin = false; pin = "" }, modifier = Modifier.fillMaxWidth()) {
                Text("Use Fingerprint")
            }
        }
        OutlinedButton(onClick = close, modifier = Modifier.fillMaxWidth()) { Text("Close") }
    }
}

@Composable
private fun RestoreWizard(
    freshDevice: Boolean,
    readPackage: (Uri) -> ByteArray,
    decryptPreview: (ByteArray, String) -> RestorePreview,
    commitRestore: (RestorePreview, String) -> BlueprintRestoreCoordinator.Result,
    saveNewPin: (String) -> Unit,
    close: () -> Unit,
) {
    var packageBytes by remember { mutableStateOf<ByteArray?>(null) }
    var selectedName by rememberSaveable { mutableStateOf("Koi backup select nahi") }
    var phrase by rememberSaveable { mutableStateOf("") }
    var preview by remember { mutableStateOf<RestorePreview?>(null) }
    var message by rememberSaveable { mutableStateOf(if (freshDevice) "New phone: .gkb backup choose karein" else "Pehle .gkb backup choose karein") }
    var restored by rememberSaveable { mutableStateOf(false) }
    var newPin by rememberSaveable { mutableStateOf("") }
    var confirmPin by rememberSaveable { mutableStateOf("") }
    var pinSaved by rememberSaveable { mutableStateOf(!freshDevice) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        preview = null
        restored = false
        if (uri != null) {
            runCatching { readPackage(uri) }
                .onSuccess {
                    packageBytes = it
                    selectedName = uri.lastPathSegment ?: "Selected .gkb file"
                    message = "File selected. Recovery Key daalein."
                }
                .onFailure { message = it.message ?: "Backup file read nahi hui" }
        }
    }

    RestorePanel(if (freshDevice) "New Device Recovery" else "Verified Backup Restore", message, Icons.Default.Restore) {
        Text("Business + masters + photos verify hone ke baad hi restore hoga.", color = Color.Gray)
        OutlinedButton(onClick = { picker.launch(arrayOf("application/octet-stream", "*/*")) }, modifier = Modifier.fillMaxWidth()) {
            Text("Backup File Choose Karein")
        }
        Text(selectedName, color = Color.Gray, fontSize = 12.sp)
        OutlinedTextField(
            phrase,
            { phrase = RecoveryKeyStore.normalize(it) },
            label = { Text("Recovery Key / legacy passphrase") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
        )
        Button(
            onClick = {
                val bytes = packageBytes ?: return@Button
                runCatching { decryptPreview(bytes, phrase) }
                    .onSuccess {
                        preview = it
                        message = "Backup verified. Counts check karke confirm karein."
                    }
                    .onFailure {
                        preview = null
                        message = it.message ?: "Backup verify nahi hua"
                    }
            },
            enabled = packageBytes != null && phrase.length >= 12 && !restored,
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Decrypt & Verify Preview") }

        preview?.let { data ->
            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFFEAF7EF))) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Verified Backup", fontWeight = FontWeight.Bold, color = Color(0xFF138A4A))
                    Text("Backup date: ${java.text.DateFormat.getDateTimeInstance().format(Date(data.createdAt))}")
                    Text("Customers: ${data.inspection.customerCount}")
                    Text("Categories: ${data.inspection.categoryCount}")
                    Text("Girvi: ${data.inspection.girviCount}")
                    Text("Ledger entries: ${data.inspection.paymentEntryCount}")
                    Text("Photos: ${data.mediaCount}${if (data.portableMedia.isNotEmpty()) " • new-device portable ✅" else if (data.encryptedMedia.isNotEmpty()) " • legacy device-bound" else ""}")
                    Text(if (data.containsPortableMasters) "Masters: ${data.masterCatalog.entries.size}" else "Legacy backup: current masters preserve honge")
                    Text("SHA-256: ${data.sha256.take(16)}…", fontSize = 12.sp, color = Color.Gray)
                }
            }
            Button(
                onClick = {
                    runCatching { commitRestore(data, phrase) }
                        .onSuccess { result ->
                            restored = true
                            message = "Restore successful • ${result.girviCount} girvi • ${result.mediaCount} photos verified."
                        }
                        .onFailure { message = it.message ?: "Restore commit failed" }
                },
                enabled = !restored,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB3261E)),
            ) { Text("CONFIRM: Current Data Replace Karein") }
        }

        if (restored && freshDevice && !pinSaved) {
            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF4E5))) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Naye phone ke liye naya PIN set karein", fontWeight = FontWeight.Bold)
                    OutlinedTextField(newPin, { newPin = it.filter(Char::isDigit).take(6) }, label = { Text("New 6-digit PIN") }, visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword))
                    OutlinedTextField(confirmPin, { confirmPin = it.filter(Char::isDigit).take(6) }, label = { Text("PIN dobara") }, visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword))
                    Button(
                        enabled = newPin.length == 6 && confirmPin.length == 6,
                        onClick = {
                            if (newPin != confirmPin) message = "Dono PIN match nahi kar rahe"
                            else {
                                saveNewPin(newPin)
                                newPin = ""
                                confirmPin = ""
                                pinSaved = true
                                phrase = ""
                                message = "Recovery complete. Naya PIN set ho gaya."
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Save New PIN") }
                }
            }
        }

        if (restored && pinSaved) Text("✓ Verified recovery complete", color = Color(0xFF138A4A), fontWeight = FontWeight.Bold)
        OutlinedButton(onClick = close, enabled = !freshDevice || pinSaved, modifier = Modifier.fillMaxWidth()) { Text("Close") }
    }
}

@Composable
private fun RestorePanel(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable () -> Unit,
) {
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