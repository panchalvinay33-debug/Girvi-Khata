package com.girvikhata.app

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
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
import androidx.fragment.app.FragmentActivity
import com.girvikhata.app.data.AppSnapshot
import com.girvikhata.app.data.CategoryRecord
import com.girvikhata.app.data.EncryptedRecordStore
import com.girvikhata.app.domain.CategorySettingsOperations
import com.girvikhata.app.security.PinVerificationResult
import com.girvikhata.app.security.SecurityPreferences
import com.girvikhata.app.security.SessionSecuritySettings

class OwnerSettingsActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val security = SecurityPreferences(applicationContext)
        val store = EncryptedRecordStore(applicationContext)
        setContent {
            MaterialTheme {
                var snapshot by remember { mutableStateOf(store.load()) }
                OwnerSettingsRoot(
                    verifyPin = { security.verify(it.toCharArray()) },
                    verifierStatus = security.verifierStatus().name,
                    loadSecurity = security::sessionSettings,
                    saveSecurity = security::saveSessionSettings,
                    snapshot = snapshot,
                    renameCategory = { id, name ->
                        val updated = CategorySettingsOperations.rename(snapshot, id, name)
                        store.save(updated)
                        snapshot = updated
                    },
                    moveCategory = { id, direction ->
                        val updated = CategorySettingsOperations.move(snapshot, id, direction)
                        if (updated != snapshot) store.save(updated)
                        snapshot = updated
                    },
                    close = ::finish,
                )
            }
        }
    }
}

@Composable
private fun OwnerSettingsRoot(
    verifyPin: (String) -> PinVerificationResult,
    verifierStatus: String,
    loadSecurity: () -> SessionSecuritySettings,
    saveSecurity: (SessionSecuritySettings) -> Unit,
    snapshot: AppSnapshot,
    renameCategory: (String, String) -> Unit,
    moveCategory: (String, Int) -> Unit,
    close: () -> Unit,
) {
    var unlocked by rememberSaveable { mutableStateOf(false) }
    if (!unlocked) SettingsPinScreen(verifyPin, { unlocked = true }, close)
    else OwnerSettingsScreen(verifierStatus, loadSecurity, saveSecurity, snapshot.categories, renameCategory, moveCategory, close)
}

@Composable
private fun SettingsPinScreen(verifyPin: (String) -> PinVerificationResult, success: () -> Unit, close: () -> Unit) {
    var pin by rememberSaveable { mutableStateOf("") }
    var message by rememberSaveable { mutableStateOf("Owner settings ke liye PIN verify karein") }
    Column(
        Modifier.fillMaxSize().background(Color(0xFFF6F7FB)).padding(20.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Owner Settings", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(message, color = Color.Gray)
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
        OutlinedButton(onClick = close, modifier = Modifier.fillMaxWidth()) { Text("Close") }
    }
}

@Composable
private fun OwnerSettingsScreen(
    verifierStatus: String,
    loadSecurity: () -> SessionSecuritySettings,
    saveSecurity: (SessionSecuritySettings) -> Unit,
    categories: List<CategoryRecord>,
    renameCategory: (String, String) -> Unit,
    moveCategory: (String, Int) -> Unit,
    close: () -> Unit,
) {
    var settings by remember { mutableStateOf(loadSecurity()) }
    var message by remember { mutableStateOf("Settings encrypted business records ko delete nahi karti.") }
    var editing by remember { mutableStateOf<CategoryRecord?>(null) }

    Column(Modifier.fillMaxSize().background(Color(0xFFF6F7FB)).padding(16.dp)) {
        Text("Owner Settings", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(message, color = Color.Gray)
        LazyColumn(
            Modifier.weight(1f).padding(top = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                SettingsCard("Security diagnostics") {
                    Text("PIN verifier: $verifierStatus", fontWeight = FontWeight.Bold)
                    Text("Biometric availability device par check hoti hai; toggle sirf app unlock button control karta hai.", color = Color.Gray)
                }
            }
            item {
                SettingsCard("Biometric unlock") {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(Modifier.weight(1f)) {
                            Text(if (settings.biometricUnlockEnabled) "Enabled" else "Disabled", fontWeight = FontWeight.Bold)
                            Text("PIN unlock hamesha available rahega.", color = Color.Gray)
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
