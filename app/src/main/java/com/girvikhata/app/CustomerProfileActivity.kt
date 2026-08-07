package com.girvikhata.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import com.girvikhata.app.custody.CustodyPlacementStore
import com.girvikhata.app.data.AppSnapshot
import com.girvikhata.app.data.CustomerPurgeCoordinator
import com.girvikhata.app.data.DataSafetyJournal
import com.girvikhata.app.data.EncryptedRecordStore
import com.girvikhata.app.data.SecureMediaVault
import com.girvikhata.app.domain.BlueprintLedgerEngine
import com.girvikhata.app.security.BiometricAvailability
import com.girvikhata.app.security.BiometricCapability
import com.girvikhata.app.security.PinVerificationResult
import com.girvikhata.app.security.SecurityPreferences
import java.text.DateFormat
import java.util.Date

class CustomerProfileActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val customerId = intent.getStringExtra(EXTRA_CUSTOMER_ID).orEmpty()
        if (customerId.isBlank()) {
            finish()
            return
        }
        val records = EncryptedRecordStore(applicationContext)
        val custody = CustodyPlacementStore(applicationContext)
        val media = SecureMediaVault(applicationContext)
        val purgeCoordinator = CustomerPurgeCoordinator(records, custody, media)
        val security = SecurityPreferences(applicationContext)
        val capability = BiometricCapability(applicationContext)
        val journal = DataSafetyJournal(applicationContext)

        setContent {
            MaterialTheme {
                var snapshot by remember { mutableStateOf(records.load()) }
                val customer = snapshot.customers.firstOrNull { it.id == customerId }
                if (customer == null) {
                    MissingCustomerScreen { finish() }
                } else {
                    CustomerKhataScreen(
                        customerId = customerId,
                        snapshot = snapshot,
                        custodyStore = custody,
                        security = security,
                        biometricAvailable = capability.availability() == BiometricAvailability.AVAILABLE && security.sessionSettings().biometricUnlockEnabled,
                        requestBiometric = { success, pinFallback, error -> requestOwnerBiometric(success, pinFallback, error) },
                        back = { finish() },
                        newGirvi = { startActivity(Intent(this, PracticalEntryActivity::class.java)) },
                        purge = {
                            runCatching { purgeCoordinator.purge(customerId) }
                                .onSuccess { result ->
                                    runCatching {
                                        journal.recordNamedEvent(
                                            "CUSTOMER_ACCOUNT_PURGED",
                                            "Customer ka pura khata delete",
                                            "${result.customerName} • Girvi ${result.girviCount} • Items ${result.itemCount} • Payments ${result.paymentCount} • Media ${result.mediaDeleted}",
                                        )
                                    }
                                    snapshot = records.load()
                                    setResult(RESULT_OK)
                                    finish()
                                }
                                .exceptionOrNull()?.message
                        },
                    )
                }
            }
        }
    }

    private fun requestOwnerBiometric(
        success: () -> Unit,
        pinFallback: () -> Unit,
        error: (String) -> Unit,
    ) {
        val prompt = BiometricPrompt(
            this,
            ContextCompat.getMainExecutor(this),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) = success()
                override fun onAuthenticationFailed() = error("Fingerprint match nahi hua")
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    if (errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON) pinFallback() else error(errString.toString())
                }
            },
        )
        prompt.authenticate(
            BiometricPrompt.PromptInfo.Builder()
                .setTitle("Pura Khata Delete")
                .setSubtitle("Owner fingerprint verify karein")
                .setNegativeButtonText("Use PIN")
                .build(),
        )
    }

    companion object {
        const val EXTRA_CUSTOMER_ID = "customer_id"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CustomerKhataScreen(
    customerId: String,
    snapshot: AppSnapshot,
    custodyStore: CustodyPlacementStore,
    security: SecurityPreferences,
    biometricAvailable: Boolean,
    requestBiometric: (() -> Unit, () -> Unit, (String) -> Unit) -> Unit,
    back: () -> Unit,
    newGirvi: () -> Unit,
    purge: () -> String?,
) {
    val customer = snapshot.customers.first { it.id == customerId }
    val girvis = snapshot.girvis.filter { it.customerId == customerId }.sortedByDescending { it.createdAt }
    val active = girvis.filter { it.status == "ACTIVE" }
    val closed = girvis.size - active.size
    val totalDue = active.sumOf { girvi ->
        runCatching { BlueprintLedgerEngine.project(girvi, System.currentTimeMillis()).totalDuePaise }
            .getOrDefault(girvi.principalPaise)
    }
    val custody = remember(snapshot) { custodyStore.load() }
    val externalLinks = remember(custody, girvis) {
        val ids = girvis.map { it.id }.toSet()
        custody.lots.sumOf { lot -> lot.items.count { it.girviId in ids && it.removedAt == null } }
    }
    val paymentCount = girvis.sumOf { it.payments.size }
    val itemCount = girvis.sumOf { it.effectiveItems.size }

    var menu by rememberSaveable { mutableStateOf(false) }
    var warning by rememberSaveable { mutableStateOf(false) }
    var pinDialog by rememberSaveable { mutableStateOf(false) }
    var finalConfirm by rememberSaveable { mutableStateOf(false) }
    var message by rememberSaveable { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Customer Khata", fontWeight = FontWeight.Bold, color = Color.White) },
                navigationIcon = { TextButton(onClick = back) { Text("← वापस", color = Color.White) } },
                actions = {
                    IconButton(onClick = { menu = true }) {
                        Icon(Icons.Default.MoreVert, "Customer menu", tint = Color.White)
                    }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        DropdownMenuItem(
                            text = { Text("Pura Khata Delete", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold) },
                            onClick = { menu = false; warning = true; message = null },
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF171752)),
            )
        },
    ) { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize().padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text(customer.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text(customer.mobile.ifBlank { "Mobile nahi diya" })
                    if (customer.address.isNotBlank()) Text(customer.address, color = Color.Gray)
                    Spacer(Modifier.size(4.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Active: ${active.size}", fontWeight = FontWeight.Bold)
                        Text("Closed: $closed")
                    }
                    Text(
                        "Total Due: ${profileMoney(totalDue)}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF5146B8),
                    )
                }
            }

            Button(onClick = newGirvi, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Add, null)
                Text("  Naya Girvi")
            }

            message?.let { Text(it, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold) }
            Text("Girvi History (${girvis.size})", fontWeight = FontWeight.Bold)
            if (girvis.isEmpty()) {
                Card(Modifier.fillMaxWidth()) {
                    Text("Abhi is customer ka koi Girvi record nahi hai.", Modifier.padding(16.dp))
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(girvis, key = { it.id }) { girvi ->
                        Card(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(13.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(girvi.girviNumber, fontWeight = FontWeight.Bold)
                                    Text(
                                        girvi.status,
                                        color = if (girvi.status == "ACTIVE") Color(0xFF138A4A) else Color.Gray,
                                    )
                                }
                                Text(girvi.effectiveItems.joinToString { it.itemName.ifBlank { it.categoryName } })
                                Text(
                                    "Principal: ${profileMoney(girvi.principalPaise)} • ${DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(girvi.createdAt))}",
                                    color = Color.Gray,
                                )
                                if (girvi.status == "ACTIVE") {
                                    val due = runCatching { BlueprintLedgerEngine.project(girvi, System.currentTimeMillis()).totalDuePaise }
                                        .getOrDefault(girvi.principalPaise)
                                    Text("Aaj ka Due: ${profileMoney(due)}", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (warning) {
        AlertDialog(
            onDismissRequest = { warning = false },
            title = { Text("Pura khata permanently delete?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("${customer.name} ka current app data delete hoga:")
                    Text("• ${girvis.size} Girvi\n• $itemCount items\n• $paymentCount payment/reversal entries\n• $externalLinks active external item links")
                    Text(
                        "Customer aur uski Girvi/payment/custody history current phone se hat jayegi.",
                        fontWeight = FontWeight.Bold,
                    )
                    Text("Purane .gkb backup files automatically delete nahi honge.", color = MaterialTheme.colorScheme.error)
                    if (externalLinks > 0) {
                        Text("Shared external finance ledger safe rahega; sirf is customer ke item links hatenge.")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    warning = false
                    if (biometricAvailable) {
                        requestBiometric(
                            { finalConfirm = true },
                            { pinDialog = true },
                            { message = it },
                        )
                    } else {
                        pinDialog = true
                    }
                }) { Text("Owner Verify Karein", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { warning = false }) { Text("Cancel") } },
        )
    }

    if (pinDialog) {
        OwnerPurgePinDialog(
            security = security,
            dismiss = { pinDialog = false },
            success = { pinDialog = false; finalConfirm = true },
        )
    }

    if (finalConfirm) {
        AlertDialog(
            onDismissRequest = { finalConfirm = false },
            title = { Text("Final Confirmation") },
            text = { Text("Ye action current app me undo nahi hoga. '${customer.name}' ka pura khata permanently delete karein?") },
            confirmButton = {
                TextButton(onClick = {
                    finalConfirm = false
                    val error = purge()
                    if (error != null) message = "Delete nahi hua: $error"
                }) {
                    Text("PERMANENTLY DELETE", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = { TextButton(onClick = { finalConfirm = false }) { Text("Nahi") } },
        )
    }
}

@Composable
private fun OwnerPurgePinDialog(
    security: SecurityPreferences,
    dismiss: () -> Unit,
    success: () -> Unit,
) {
    var pin by rememberSaveable { mutableStateOf("") }
    var message by rememberSaveable { mutableStateOf("6-digit owner PIN daalein") }
    AlertDialog(
        onDismissRequest = dismiss,
        title = { Text("Owner Verification") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(message)
                OutlinedTextField(
                    value = pin,
                    onValueChange = { pin = it.filter(Char::isDigit).take(6) },
                    label = { Text("Owner PIN") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = pin.length == 6,
                onClick = {
                    when (val result = security.verify(pin.toCharArray())) {
                        PinVerificationResult.Success -> success()
                        PinVerificationResult.NotConfigured -> message = "PIN configured nahi hai"
                        is PinVerificationResult.Locked -> message = "Security lock active hai"
                        is PinVerificationResult.Failure -> message = "Galat PIN. Attempts: ${result.attempts}"
                    }
                    pin = ""
                },
            ) { Text("Verify") }
        },
        dismissButton = { TextButton(onClick = dismiss) { Text("Cancel") } },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MissingCustomerScreen(back: () -> Unit) {
    Scaffold(topBar = { TopAppBar(title = { Text("Customer nahi mila") }) }) { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize().padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text("Ye customer record ab available nahi hai.")
            OutlinedButton(onClick = back) { Text("Wapas") }
        }
    }
}

private fun profileMoney(paise: Long): String =
    "₹" + java.math.BigDecimal(paise).movePointLeft(2).setScale(2).toPlainString()
