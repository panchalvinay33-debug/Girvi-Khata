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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
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
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.girvikhata.app.data.AppSnapshot
import com.girvikhata.app.data.CategoryRecord
import com.girvikhata.app.data.EncryptedRecordStore
import com.girvikhata.app.data.RelationalShadowFingerprint
import com.girvikhata.app.data.RenameCategoryMutation
import com.girvikhata.app.data.ReorderCategoryMutation
import com.girvikhata.app.data.VerifiedBusinessMutation
import com.girvikhata.app.data.VerifiedBusinessWriteCoordinator
import com.girvikhata.app.data.VerifiedBusinessWriteRequest
import com.girvikhata.app.security.BiometricAvailability
import com.girvikhata.app.security.BiometricCapability
import com.girvikhata.app.security.PinVerificationResult
import com.girvikhata.app.security.SecurityPreferences
import com.girvikhata.app.security.SessionSecuritySettings

class OwnerSettingsActivity : FragmentActivity() {
    private lateinit var security: SecurityPreferences
    private lateinit var biometricCapability: BiometricCapability

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        security = SecurityPreferences(applicationContext)
        biometricCapability = BiometricCapability(applicationContext)
        val profileStore = OwnerBusinessProfileStore(applicationContext)
        val store = EncryptedRecordStore(applicationContext)
        val coordinator = VerifiedBusinessWriteCoordinator(applicationContext, records = store)
        setContent {
            MaterialTheme {
                var snapshot by remember { mutableStateOf(store.load()) }
                var profile by remember { mutableStateOf(profileStore.load()) }

                fun commitCategoryMutation(mutation: VerifiedBusinessMutation, title: String) {
                    coordinator.execute(
                        VerifiedBusinessWriteRequest(
                            expectedFingerprint = RelationalShadowFingerprint.sha256(snapshot),
                            mutation = mutation,
                            title = title,
                        ),
                    )
                    snapshot = store.load()
                }

                val availability = if (security.sessionSettings().biometricUnlockEnabled) {
                    biometricCapability.availability()
                } else {
                    BiometricAvailability.UNSUPPORTED
                }

                OwnerSettingsRoot(
                    verifyPin = { security.verify(it.toCharArray()) },
                    biometricAvailability = availability,
                    requestBiometric = ::requestBiometric,
                    verifierStatus = security.verifierStatus().name,
                    loadSecurity = security::sessionSettings,
                    saveSecurity = security::saveSessionSettings,
                    profile = profile,
                    saveProfile = {
                        profileStore.save(it)
                        profile = profileStore.load()
                    },
                    snapshot = snapshot,
                    renameCategory = { id, name ->
                        commitCategoryMutation(RenameCategoryMutation(id, name), "Owner category rename")
                    },
                    moveCategory = { id, direction ->
                        commitCategoryMutation(ReorderCategoryMutation(id, direction), "Owner category reorder")
                    },
                    openRecoveryCenter = { startActivity(Intent(this, RecoveryCenterActivity::class.java)) },
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
                .setTitle("Owner Settings")
                .setSubtitle("Fingerprint se verify karein")
                .setNegativeButtonText("Use PIN")
                .build(),
        )
    }
}

@Composable
private fun OwnerSettingsRoot(
    verifyPin: (String) -> PinVerificationResult,
    biometricAvailability: BiometricAvailability,
    requestBiometric: (() -> Unit, (String) -> Unit) -> Unit,
    verifierStatus: String,
    loadSecurity: () -> SessionSecuritySettings,
    saveSecurity: (SessionSecuritySettings) -> Unit,
    profile: OwnerBusinessProfile,
    saveProfile: (OwnerBusinessProfile) -> Unit,
    snapshot: AppSnapshot,
    renameCategory: (String, String) -> Unit,
    moveCategory: (String, Int) -> Unit,
    openRecoveryCenter: () -> Unit,
    close: () -> Unit,
) {
    var unlocked by rememberSaveable { mutableStateOf(false) }
    if (!unlocked) {
        SettingsUnlockScreen(
            biometricAvailability,
            verifyPin,
            requestBiometric,
            { unlocked = true },
            close,
        )
    } else {
        OwnerSettingsScreen(
            verifierStatus,
            loadSecurity,
            saveSecurity,
            profile,
            saveProfile,
            snapshot.categories,
            renameCategory,
            moveCategory,
            openRecoveryCenter,
            close,
        )
    }
}

@Composable
private fun SettingsUnlockScreen(
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
        mutableStateOf(if (biometricFirst) "Fingerprint se owner verify karein" else "Owner PIN verify karein")
    }
    Column(
        Modifier.fillMaxSize().background(Color(0xFFF6F7FB)).padding(20.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Owner Settings", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(message, color = Color.Gray)
        if (!usePin && biometricFirst) {
            Button(
                onClick = { requestBiometric(success) { message = it } },
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
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
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            )
            Button(
                onClick = {
                    when (val result = verifyPin(pin)) {
                        PinVerificationResult.Success -> success()
                        PinVerificationResult.NotConfigured -> message = "PIN configured nahi hai"
                        is PinVerificationResult.Locked -> message = "Security lock active hai"
                        is PinVerificationResult.Failure -> message = "Galat PIN. Attempts: ${result.attempts}"
                    }
                    pin = ""
                },
                enabled = pin.length == 6,
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
            ) { Text("PIN Verify") }
            if (biometricFirst) TextButton(onClick = { usePin = false; pin = "" }, modifier = Modifier.fillMaxWidth()) {
                Text("Use Fingerprint")
            }
        }
        OutlinedButton(onClick = close, modifier = Modifier.fillMaxWidth()) { Text("Close") }
    }
}

@Composable
private fun OwnerSettingsScreen(
    verifierStatus: String,
    loadSecurity: () -> SessionSecuritySettings,
    saveSecurity: (SessionSecuritySettings) -> Unit,
    profile: OwnerBusinessProfile,
    saveProfile: (OwnerBusinessProfile) -> Unit,
    categories: List<CategoryRecord>,
    renameCategory: (String, String) -> Unit,
    moveCategory: (String, Int) -> Unit,
    openRecoveryCenter: () -> Unit,
    close: () -> Unit,
) {
    var settings by remember { mutableStateOf(loadSecurity()) }
    var message by remember { mutableStateOf("Settings encrypted business records ko delete nahi karti.") }
    var editing by remember { mutableStateOf<CategoryRecord?>(null) }
    var editProfile by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().background(Color(0xFFF6F7FB)).padding(16.dp)) {
        Text("Owner Settings", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(message, color = Color.Gray)
        LazyColumn(
            Modifier.weight(1f).padding(top = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                SettingsCard("Dukaan / Owner Profile") {
                    Text(profile.businessName.ifBlank { "Business name not set" }, fontWeight = FontWeight.Bold)
                    Text(profile.ownerName.ifBlank { "Owner name not set" }, color = Color.Gray)
                    if (profile.mobile.isNotBlank()) Text(profile.mobile, color = Color.Gray)
                    if (profile.address.isNotBlank()) Text(profile.address, color = Color.Gray)
                    Button(onClick = { editProfile = true }, modifier = Modifier.fillMaxWidth()) { Text("Edit Profile") }
                }
            }
            item {
                SettingsCard("Recovery & Backup") {
                    Text("Mobile lost/reset hone par Recovery Key + encrypted off-device backup se khata wapas aayega.", color = Color.Gray)
                    Button(onClick = openRecoveryCenter, modifier = Modifier.fillMaxWidth()) {
                        Text("Open Recovery Center")
                    }
                }
            }
            item {
                SettingsCard("Security diagnostics") {
                    Text("PIN verifier: $verifierStatus", fontWeight = FontWeight.Bold)
                    Text("Fingerprint available ho to PIN se pehle fingerprint dikhaya jayega.", color = Color.Gray)
                }
            }
            item {
                SettingsCard("Biometric unlock") {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(Modifier.weight(1f)) {
                            Text(if (settings.biometricUnlockEnabled) "Enabled" else "Disabled", fontWeight = FontWeight.Bold)
                            Text("PIN fallback hamesha available rahega.", color = Color.Gray)
                        }
                        Switch(
                            checked = settings.biometricUnlockEnabled,
                            onCheckedChange = { enabled ->
                                val next = settings.copy(biometricUnlockEnabled = enabled)
                                runCatching { saveSecurity(next) }
                                    .onSuccess { settings = next; message = "Biometric preference saved" }
                                    .onFailure { message = it.message ?: "Setting save nahi hui" }
                            },
                        )
                    }
                }
            }
            item {
                SettingsCard("Auto-lock timeout") {
                    lockOptions.forEach { (millis, label) ->
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = settings.autoLockTimeoutMillis == millis,
                                onClick = {
                                    val next = settings.copy(autoLockTimeoutMillis = millis)
                                    runCatching { saveSecurity(next) }
                                        .onSuccess { settings = next; message = "Auto-lock: $label" }
                                        .onFailure { message = it.message ?: "Setting save nahi hui" }
                                },
                            )
                            Text(label)
                        }
                    }
                }
            }
            item { Text("Category order & rename", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
            itemsIndexed(categories, key = { _, item -> item.id }) { index, category ->
                Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(category.name, fontWeight = FontWeight.Bold)
                        Text(if (category.active) "Active" else "Inactive", color = Color.Gray)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { moveCategory(category.id, -1); message = "Category order saved" }, enabled = index > 0) { Text("↑ Up") }
                            OutlinedButton(onClick = { moveCategory(category.id, 1); message = "Category order saved" }, enabled = index < categories.lastIndex) { Text("↓ Down") }
                            Button(onClick = { editing = category }) { Text("Rename") }
                        }
                    }
                }
            }
            item { OutlinedButton(onClick = close, modifier = Modifier.fillMaxWidth()) { Text("Close") } }
        }
    }

    if (editProfile) {
        var businessName by remember(profile) { mutableStateOf(profile.businessName) }
        var ownerName by remember(profile) { mutableStateOf(profile.ownerName) }
        var mobile by remember(profile) { mutableStateOf(profile.mobile) }
        var address by remember(profile) { mutableStateOf(profile.address) }
        AlertDialog(
            onDismissRequest = { editProfile = false },
            title = { Text("Dukaan / Owner Profile") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(businessName, { businessName = it.take(60) }, label = { Text("Dukaan / Business name") })
                    OutlinedTextField(ownerName, { ownerName = it.take(60) }, label = { Text("Owner / User name") })
                    OutlinedTextField(
                        mobile,
                        { mobile = it.filter(Char::isDigit).take(10) },
                        label = { Text("Mobile") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    )
                    OutlinedTextField(address, { address = it.take(180) }, label = { Text("Address") })
                }
            },
            confirmButton = {
                Button(onClick = {
                    runCatching { saveProfile(OwnerBusinessProfile(businessName, ownerName, mobile, address)) }
                        .onSuccess { editProfile = false; message = "Dukaan / owner profile saved" }
                        .onFailure { message = it.message ?: "Profile save nahi hua" }
                }) { Text("Save") }
            },
            dismissButton = { OutlinedButton(onClick = { editProfile = false }) { Text("Cancel") } },
        )
    }

    editing?.let { category ->
        var name by remember(category.id) { mutableStateOf(category.name) }
        AlertDialog(
            onDismissRequest = { editing = null },
            title = { Text("Rename category") },
            text = { OutlinedTextField(name, { name = it }, label = { Text("Category name") }, singleLine = true) },
            confirmButton = {
                Button(onClick = {
                    runCatching { renameCategory(category.id, name) }
                        .onSuccess { message = "Category renamed and linked girvi updated"; editing = null }
                        .onFailure { message = it.message ?: "Rename failed" }
                }) { Text("Save") }
            },
            dismissButton = { OutlinedButton(onClick = { editing = null }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun SettingsCard(title: String, content: @Composable () -> Unit) {
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            content()
        }
    }
}

private val lockOptions = listOf(
    0L to "Immediately",
    30_000L to "30 seconds",
    60_000L to "1 minute",
    300_000L to "5 minutes",
)