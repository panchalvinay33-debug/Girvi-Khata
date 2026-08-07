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
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.girvikhata.app.data.EncryptedMasterCatalogStore
import com.girvikhata.app.domain.MasterCatalog
import com.girvikhata.app.domain.MasterCatalogOperations
import com.girvikhata.app.domain.MasterEntry
import com.girvikhata.app.domain.MasterKind
import com.girvikhata.app.security.BiometricAvailability
import com.girvikhata.app.security.BiometricCapability
import com.girvikhata.app.security.PinVerificationResult
import com.girvikhata.app.security.SecurityPreferences

class MasterCatalogActivity : FragmentActivity() {
    private lateinit var security: SecurityPreferences
    private lateinit var biometricCapability: BiometricCapability

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        security = SecurityPreferences(applicationContext)
        biometricCapability = BiometricCapability(applicationContext)
        val store = EncryptedMasterCatalogStore(applicationContext)
        setContent {
            MaterialTheme {
                var catalog by remember { mutableStateOf(store.load()) }
                var unlocked by rememberSaveable { mutableStateOf(false) }
                val availability = if (security.sessionSettings().biometricUnlockEnabled) {
                    biometricCapability.availability()
                } else {
                    BiometricAvailability.UNSUPPORTED
                }
                if (!unlocked) {
                    MasterAuthScreen(
                        biometricAvailability = availability,
                        verify = { security.verify(it.toCharArray()) },
                        requestBiometric = ::requestBiometric,
                        success = { unlocked = true },
                        close = ::finish,
                    )
                } else {
                    MasterCatalogScreen(
                        catalog = catalog,
                        save = { next -> store.save(next); catalog = next },
                        openCustody = { startActivity(Intent(this, CustodyPlacementActivity::class.java)) },
                        close = ::finish,
                    )
                }
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
                .setTitle("Business Masters")
                .setSubtitle("Fingerprint se owner verify karein")
                .setNegativeButtonText("Use PIN")
                .build(),
        )
    }
}

@Composable
private fun MasterAuthScreen(
    biometricAvailability: BiometricAvailability,
    verify: (String) -> PinVerificationResult,
    requestBiometric: (() -> Unit, (String) -> Unit) -> Unit,
    success: () -> Unit,
    close: () -> Unit,
) {
    val biometricFirst = biometricAvailability == BiometricAvailability.AVAILABLE
    var usePin by rememberSaveable { mutableStateOf(!biometricFirst) }
    var pin by rememberSaveable { mutableStateOf("") }
    var message by rememberSaveable {
        mutableStateOf(if (biometricFirst) "Fingerprint se owner verify karein" else "Master settings ke liye PIN verify karein")
    }
    Column(
        Modifier.fillMaxSize().background(Color(0xFFF6F7FB)).padding(20.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Business Masters", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
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
                value = pin,
                onValueChange = { pin = it.filter(Char::isDigit).take(6) },
                label = { Text("6-digit PIN") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            )
            Button(
                onClick = {
                    when (val result = verify(pin)) {
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
private fun MasterCatalogScreen(catalog: MasterCatalog, save: (MasterCatalog) -> Unit, openCustody: () -> Unit, close: () -> Unit) {
    var addKind by remember { mutableStateOf<MasterKind?>(null) }
    var editing by remember { mutableStateOf<MasterEntry?>(null) }
    var message by remember { mutableStateOf("Encrypted master catalog ready") }

    Column(Modifier.fillMaxSize().background(Color(0xFFF6F7FB)).padding(14.dp)) {
        Text("Business Master Catalog", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(message, color = Color.Gray)
        Button(onClick = openCustody, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
            Text("🔐 Storage / Locker & External Placement")
        }
        LazyColumn(
            modifier = Modifier.weight(1f).padding(top = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            MasterKind.entries.forEach { kind ->
                item { Text(kind.label(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
                item {
                    Button(onClick = { addKind = kind }, modifier = Modifier.fillMaxWidth()) { Text("Add ${kind.label()}") }
                }
                val entries = catalog.entries.filter { it.kind == kind }
                if (entries.isEmpty()) item { Text("Abhi entry nahi hai", color = Color.Gray) }
                itemsIndexed(entries, key = { _, entry -> entry.id }) { index, entry ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(entry.name, fontWeight = FontWeight.Bold)
                            val detail = buildList {
                                add(if (entry.active) "Active" else "Inactive")
                                if (entry.categoryName.isNotBlank()) add("Category: ${entry.categoryName}")
                                if (entry.kind == MasterKind.INTEREST_PLAN) add("Rate: ${entry.rateBasisPoints / 100.0}%")
                            }.joinToString(" • ")
                            Text(detail, color = Color.Gray)
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                OutlinedButton(
                                    onClick = { runCatching { save(MasterCatalogOperations.move(catalog, entry.id, -1)) }.onFailure { message = it.message.orEmpty() } },
                                    enabled = index > 0,
                                ) { Text("↑") }
                                OutlinedButton(
                                    onClick = { runCatching { save(MasterCatalogOperations.move(catalog, entry.id, 1)) }.onFailure { message = it.message.orEmpty() } },
                                    enabled = index < entries.lastIndex,
                                ) { Text("↓") }
                                OutlinedButton(onClick = { editing = entry }) { Text("Rename") }
                                Button(onClick = {
                                    runCatching { save(MasterCatalogOperations.toggle(catalog, entry.id)) }
                                        .onSuccess { message = "${entry.name} status updated" }
                                        .onFailure { message = it.message.orEmpty() }
                                }) { Text(if (entry.active) "Disable" else "Enable") }
                            }
                        }
                    }
                }
            }
            item { OutlinedButton(onClick = close, modifier = Modifier.fillMaxWidth()) { Text("Close") } }
        }
    }

    addKind?.let { kind ->
        MasterEditDialog(
            title = "Add ${kind.label()}",
            initialName = "",
            initialCategory = "",
            initialRate = if (kind == MasterKind.INTEREST_PLAN) "2" else "",
            showCategory = kind == MasterKind.ITEM,
            showRate = kind == MasterKind.INTEREST_PLAN,
            dismiss = { addKind = null },
            save = { name, category, rate ->
                runCatching {
                    val basisPoints = if (kind == MasterKind.INTEREST_PLAN) ((rate.toDoubleOrNull() ?: error("Rate required")) * 100).toInt() else 0
                    MasterCatalogOperations.add(catalog, kind, name, category, basisPoints)
                }.onSuccess { save(it); message = "${kind.label()} added"; addKind = null }
                    .onFailure { message = it.message ?: "Save failed" }
            },
        )
    }

    editing?.let { entry ->
        MasterEditDialog(
            title = "Rename ${entry.kind.label()}",
            initialName = entry.name,
            initialCategory = entry.categoryName,
            initialRate = (entry.rateBasisPoints / 100.0).toString(),
            showCategory = false,
            showRate = false,
            dismiss = { editing = null },
            save = { name, _, _ ->
                runCatching { MasterCatalogOperations.rename(catalog, entry.id, name) }
                    .onSuccess { save(it); message = "Entry renamed"; editing = null }
                    .onFailure { message = it.message ?: "Rename failed" }
            },
        )
    }
}

@Composable
private fun MasterEditDialog(
    title: String,
    initialName: String,
    initialCategory: String,
    initialRate: String,
    showCategory: Boolean,
    showRate: Boolean,
    dismiss: () -> Unit,
    save: (String, String, String) -> Unit,
) {
    var name by rememberSaveable(title) { mutableStateOf(initialName) }
    var category by rememberSaveable(title) { mutableStateOf(initialCategory) }
    var rate by rememberSaveable(title) { mutableStateOf(initialRate) }
    AlertDialog(
        onDismissRequest = dismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Name") }, singleLine = true)
                if (showCategory) OutlinedTextField(category, { category = it }, label = { Text("Category name (optional)") }, singleLine = true)
                if (showRate) OutlinedTextField(rate, { rate = it.filter { ch -> ch.isDigit() || ch == '.' }.take(7) }, label = { Text("Monthly rate %") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
            }
        },
        confirmButton = { Button(onClick = { save(name, category, rate) }) { Text("Save") } },
        dismissButton = { OutlinedButton(onClick = dismiss) { Text("Cancel") } },
    )
}

private fun MasterKind.label(): String = when (this) {
    MasterKind.ITEM -> "Item Master"
    MasterKind.UNIT -> "Unit Master"
    MasterKind.INTEREST_PLAN -> "Interest Plans"
    MasterKind.PAYMENT_MODE -> "Payment Modes"
    MasterKind.LOCKER -> "Locker / Storage"
}
