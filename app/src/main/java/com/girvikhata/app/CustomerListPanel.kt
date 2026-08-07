package com.girvikhata.app

import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.girvikhata.app.data.AppSnapshot
import com.girvikhata.app.data.BlueprintKhataRepository
import com.girvikhata.app.data.CustomerRecord
import com.girvikhata.app.data.DataSafetyJournal
import com.girvikhata.app.security.BiometricAvailability
import com.girvikhata.app.security.BiometricCapability
import com.girvikhata.app.security.PinVerificationResult
import com.girvikhata.app.security.SecurityPreferences

@Composable
internal fun CustomerListPanel(
    snapshot: AppSnapshot,
    repository: BlueprintKhataRepository,
    updateSnapshot: (AppSnapshot) -> Unit,
    openGirvi: (String) -> Unit,
) = BlueprintCustomerPage("Customers") {
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    val security = remember { SecurityPreferences(context.applicationContext) }
    val journal = remember { DataSafetyJournal(context.applicationContext) }
    val capability = remember { BiometricCapability(context.applicationContext) }

    var query by rememberSaveable { mutableStateOf("") }
    var menuCustomerId by rememberSaveable { mutableStateOf<String?>(null) }
    var confirmDelete by remember { mutableStateOf<CustomerRecord?>(null) }
    var pinDelete by remember { mutableStateOf<CustomerRecord?>(null) }
    var message by rememberSaveable { mutableStateOf<String?>(null) }

    OutlinedTextField(
        value = query,
        onValueChange = { query = it },
        label = { Text("नाम / Mobile / Item / Girvi search") },
        leadingIcon = { Icon(Icons.Default.Search, null) },
        modifier = Modifier.fillMaxWidth(),
    )

    message?.let {
        Text(it, color = if (it.startsWith("Delete")) Color(0xFF138A4A) else MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
    }

    val q = query.trim().lowercase()
    val customerRows = snapshot.customers.filter { customer ->
        q.isBlank() || customer.name.lowercase().contains(q) || customer.mobile.contains(q) || customer.address.lowercase().contains(q) ||
            snapshot.girvis.filter { it.customerId == customer.id }.any { girvi ->
                girvi.girviNumber.lowercase().contains(q) || girvi.effectiveItems.any { it.itemName.lowercase().contains(q) }
            }
    }.sortedBy { it.name.lowercase() }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(customerRows, key = { it.id }) { customer ->
            val girvis = snapshot.girvis.filter { it.customerId == customer.id }
            Card(
                Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(start = 14.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(
                        Modifier.weight(1f).clickable {
                            girvis.maxByOrNull { it.createdAt }?.let { openGirvi(it.id) }
                        }.padding(vertical = 6.dp),
                    ) {
                        Text(customer.name, fontWeight = FontWeight.Bold)
                        Text(
                            "${customer.mobile.ifBlank { "No mobile" }} • Active ${girvis.count { it.status == "ACTIVE" }} • Total ${girvis.size}",
                            color = Color.Gray,
                        )
                    }
                    Column {
                        IconButton(onClick = { menuCustomerId = customer.id }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Customer menu")
                        }
                        DropdownMenu(
                            expanded = menuCustomerId == customer.id,
                            onDismissRequest = { menuCustomerId = null },
                        ) {
                            DropdownMenuItem(
                                text = { Text("Delete Customer", color = MaterialTheme.colorScheme.error) },
                                onClick = {
                                    menuCustomerId = null
                                    if (girvis.isNotEmpty()) {
                                        message = "${customer.name} ke ${girvis.size} Girvi record hain. Financial history wale customer ko delete nahi kiya ja sakta."
                                    } else {
                                        message = null
                                        confirmDelete = customer
                                    }
                                },
                            )
                        }
                    }
                }
            }
        }
    }

    confirmDelete?.let { customer ->
        AlertDialog(
            onDismissRequest = { confirmDelete = null },
            title = { Text("Customer delete karein?") },
            text = {
                Text("${customer.name} ko permanently delete kiya jayega. Delete se pehle owner verification zaruri hai.")
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = null
                    val biometricEnabled = security.sessionSettings().biometricUnlockEnabled &&
                        capability.availability() == BiometricAvailability.AVAILABLE && activity != null
                    if (biometricEnabled) {
                        requestCustomerDeleteBiometric(
                            activity = activity!!,
                            onSuccess = {
                                runDeleteCustomer(customer, repository, updateSnapshot, journal) { message = it }
                            },
                            onPinFallback = { pinDelete = customer },
                            onError = { message = it },
                        )
                    } else {
                        pinDelete = customer
                    }
                }) { Text("Verify & Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = null }) { Text("Cancel") } },
        )
    }

    pinDelete?.let { customer ->
        CustomerDeletePinDialog(
            customer = customer,
            security = security,
            dismiss = { pinDelete = null },
            success = {
                pinDelete = null
                runDeleteCustomer(customer, repository, updateSnapshot, journal) { message = it }
            },
        )
    }
}

@Composable
private fun BlueprintCustomerPage(title: String, content: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        content()
    }
}

private fun requestCustomerDeleteBiometric(
    activity: FragmentActivity,
    onSuccess: () -> Unit,
    onPinFallback: () -> Unit,
    onError: (String) -> Unit,
) {
    val prompt = BiometricPrompt(
        activity,
        ContextCompat.getMainExecutor(activity),
        object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) = onSuccess()
            override fun onAuthenticationFailed() = onError("Fingerprint match nahi hua")
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                if (errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON) onPinFallback()
                else onError(errString.toString())
            }
        },
    )
    prompt.authenticate(
        BiometricPrompt.PromptInfo.Builder()
            .setTitle("Customer Delete")
            .setSubtitle("Owner fingerprint verify karein")
            .setNegativeButtonText("Use PIN")
            .build(),
    )
}

@Composable
private fun CustomerDeletePinDialog(
    customer: CustomerRecord,
    security: SecurityPreferences,
    dismiss: () -> Unit,
    success: () -> Unit,
) {
    var pin by rememberSaveable(customer.id) { mutableStateOf("") }
    var message by rememberSaveable(customer.id) { mutableStateOf("6-digit owner PIN daalein") }
    AlertDialog(
        onDismissRequest = dismiss,
        title = { Text("Delete ${customer.name}") },
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
            ) { Text("Delete", color = MaterialTheme.colorScheme.error) }
        },
        dismissButton = { TextButton(onClick = dismiss) { Text("Cancel") } },
    )
}

private fun runDeleteCustomer(
    customer: CustomerRecord,
    repository: BlueprintKhataRepository,
    updateSnapshot: (AppSnapshot) -> Unit,
    journal: DataSafetyJournal,
    message: (String) -> Unit,
) {
    runCatching { repository.deleteCustomer(customer.id) }
        .onSuccess { next ->
            updateSnapshot(next)
            runCatching { journal.recordNamedEvent("CUSTOMER_DELETED", "Customer deleted", customer.name.take(80)) }
            message("Delete complete: ${customer.name}")
        }
        .onFailure { message(it.message ?: "Customer delete nahi hua") }
}
