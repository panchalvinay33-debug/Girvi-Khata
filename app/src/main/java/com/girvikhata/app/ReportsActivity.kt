package com.girvikhata.app

import android.app.DatePickerDialog
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.girvikhata.app.custody.CustodyPlacementStore
import com.girvikhata.app.custody.CustodyReportEngine
import com.girvikhata.app.data.AppSnapshot
import com.girvikhata.app.data.CustomerRecord
import com.girvikhata.app.data.EncryptedRecordStore
import com.girvikhata.app.domain.CustomerAccountOperations
import com.girvikhata.app.domain.DateRange
import com.girvikhata.app.domain.GirviStatusFilter
import com.girvikhata.app.domain.ReportingEngine
import com.girvikhata.app.export.CsvExportBuilder
import com.girvikhata.app.export.ReceiptTextBuilder
import com.girvikhata.app.export.SecureShare
import com.girvikhata.app.security.BiometricAvailability
import com.girvikhata.app.security.BiometricCapability
import com.girvikhata.app.security.PinVerificationResult
import com.girvikhata.app.security.SecurityPreferences
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private val ReportsNavy = Color(0xFF171752)
private val ReportsPurple = Color(0xFF5146B8)
private val ReportsGreen = Color(0xFF138A4A)
private val ReportsRed = Color(0xFF9B1C1C)
private val ReportsBackground = Color(0xFFF6F7FB)

class ReportsActivity : FragmentActivity() {
    private lateinit var security: SecurityPreferences
    private lateinit var biometricCapability: BiometricCapability

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        security = SecurityPreferences(applicationContext)
        biometricCapability = BiometricCapability(applicationContext)
        val store = EncryptedRecordStore(applicationContext)
        setContent {
            MaterialTheme {
                val availability = if (security.sessionSettings().biometricUnlockEnabled) {
                    biometricCapability.availability()
                } else {
                    BiometricAvailability.UNSUPPORTED
                }
                SecureReportsRoot(
                    verifyPin = { security.verify(it.toCharArray()) },
                    biometricAvailability = availability,
                    requestBiometric = ::requestBiometric,
                    loadSnapshot = store::load,
                    saveSnapshot = store::save,
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
                .setTitle("Girvi Khata Reports")
                .setSubtitle("Fingerprint se owner verify karein")
                .setNegativeButtonText("Use PIN")
                .build(),
        )
    }
}

private enum class ReportsPage { OVERVIEW, KHATA, GIRVI, COLLECTIONS, CUSTODY }
private enum class CollectionPreset { TODAY, SEVEN_DAYS, THIRTY_DAYS, ALL, CUSTOM }

@Composable
private fun SecureReportsRoot(
    verifyPin: (String) -> PinVerificationResult,
    biometricAvailability: BiometricAvailability,
    requestBiometric: (() -> Unit, (String) -> Unit) -> Unit,
    loadSnapshot: () -> AppSnapshot,
    saveSnapshot: (AppSnapshot) -> Unit,
    close: () -> Unit,
) {
    var unlocked by rememberSaveable { mutableStateOf(false) }
    var snapshot by remember { mutableStateOf<AppSnapshot?>(null) }
    if (!unlocked) {
        ReportsAuthScreen(
            biometricAvailability,
            verifyPin,
            requestBiometric,
            {
                snapshot = loadSnapshot()
                unlocked = true
            },
            close,
        )
    } else {
        ReportsHome(
            snapshot = snapshot ?: AppSnapshot.defaults(),
            onSnapshotChanged = { updated ->
                saveSnapshot(updated)
                snapshot = updated
            },
            close = close,
        )
    }
}

@Composable
private fun ReportsAuthScreen(
    biometricAvailability: BiometricAvailability,
    verifyPin: (String) -> PinVerificationResult,
    requestBiometric: (() -> Unit, (String) -> Unit) -> Unit,
    unlocked: () -> Unit,
    close: () -> Unit,
) {
    val biometricFirst = biometricAvailability == BiometricAvailability.AVAILABLE
    var usePin by rememberSaveable { mutableStateOf(!biometricFirst) }
    var pin by rememberSaveable { mutableStateOf("") }
    var message by rememberSaveable {
        mutableStateOf(if (biometricFirst) "Fingerprint se reports kholein" else "Reports dekhne ke liye PIN daalein")
    }
    Column(
        Modifier.fillMaxSize().background(ReportsNavy).padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(if (!usePin && biometricFirst) Icons.Default.Fingerprint else Icons.Default.Lock, null, tint = Color(0xFFFFC54D))
        Spacer(Modifier.height(12.dp))
        Text("Girvi Khata Reports", color = Color.White, fontSize = 27.sp, fontWeight = FontWeight.Bold)
        Text(message, color = Color.White.copy(alpha = .8f))
        Spacer(Modifier.height(22.dp))
        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (!usePin && biometricFirst) {
                    Button(
                        onClick = { requestBiometric(unlocked) { message = it } },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = ReportsPurple),
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
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Button(
                        onClick = {
                            when (val result = verifyPin(pin)) {
                                PinVerificationResult.Success -> unlocked()
                                PinVerificationResult.NotConfigured -> message = "Main app mein pehle PIN setup karein"
                                is PinVerificationResult.Locked -> message = "Security lock active hai"
                                is PinVerificationResult.Failure -> message = "Galat PIN. Attempts: ${result.attempts}"
                            }
                            pin = ""
                        },
                        enabled = pin.length == 6,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = ReportsPurple),
                    ) { Text("Reports Unlock Karein") }
                    if (biometricFirst) TextButton(onClick = { usePin = false; pin = "" }, modifier = Modifier.fillMaxWidth()) {
                        Text("Use Fingerprint")
                    }
                }
                TextButton(onClick = close, modifier = Modifier.fillMaxWidth()) { Text("Close") }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReportsHome(snapshot: AppSnapshot, onSnapshotChanged: (AppSnapshot) -> Unit, close: () -> Unit) {
    var page by rememberSaveable { mutableStateOf(ReportsPage.OVERVIEW) }
    Scaffold(
        containerColor = ReportsBackground,
        topBar = {
            TopAppBar(
                title = { Text("Girvi Khata Reports", color = Color.White, fontWeight = FontWeight.Bold) },
                actions = { TextButton(onClick = close) { Text("Close", color = Color.White) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = ReportsNavy),
            )
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                ReportsPage.entries.forEach { item ->
                    TextButton(onClick = { page = item }, modifier = Modifier.weight(1f)) {
                        Text(if (page == item) "✓ ${item.label()}" else item.label(), fontSize = 10.sp)
                    }
                }
            }
            when (page) {
                ReportsPage.OVERVIEW -> OverviewReport(snapshot)
                ReportsPage.KHATA -> CustomerLedgerReport(snapshot, onSnapshotChanged)
                ReportsPage.GIRVI -> GirviFilterReport(snapshot)
                ReportsPage.COLLECTIONS -> CollectionReport(snapshot)
                ReportsPage.CUSTODY -> CustodyReport(snapshot)
            }
        }
    }
}

@Composable
private fun OverviewReport(snapshot: AppSnapshot) {
    var months by rememberSaveable { mutableStateOf("1") }
    val monthCount = months.toIntOrNull()?.coerceIn(0, 120) ?: 0
    val summary = remember(snapshot, monthCount) { ReportingEngine.portfolio(snapshot, monthCount) }
    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { OutlinedTextField(months, { months = it.filter(Char::isDigit).take(3) }, label = { Text("Settlement months") }, modifier = Modifier.fillMaxWidth()) }
        item { SummaryRow("Customers", summary.totalCustomers.toString(), "Total Girvi", summary.totalGirvi.toString()) }
        item { SummaryRow("Active", summary.activeGirvi.toString(), "Released", summary.releasedGirvi.toString()) }
        item { SummaryRow("Original Principal", reportsMoney(summary.originalPrincipalPaise), "Received", reportsMoney(summary.effectiveReceivedPaise)) }
        item { SummaryRow("Principal Due", reportsMoney(summary.outstandingPrincipalPaise), "Interest Due", reportsMoney(summary.outstandingInterestPaise)) }
        item { ReportsBanner("Total outstanding: ${reportsMoney(summary.totalOutstandingPaise)}") }
        item { Text("Sabse Zyada Baki Customers", fontWeight = FontWeight.Bold, fontSize = 18.sp) }
        items(ReportingEngine.outstandingCustomers(snapshot, monthCount).take(10), key = { it.customerId }) {
            ReportsCard(it.customerName, "Active ${it.activeGirvi} • Outstanding ${reportsMoney(it.totalOutstandingPaise)}")
        }
    }
}

@Composable
private fun CustomerLedgerReport(snapshot: AppSnapshot, onSnapshotChanged: (AppSnapshot) -> Unit) {
    val context = LocalContext.current
    var query by rememberSaveable { mutableStateOf("") }
    var months by rememberSaveable { mutableStateOf("1") }
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
    var editing by remember { mutableStateOf<CustomerRecord?>(null) }
    var deleteCandidate by remember { mutableStateOf<CustomerRecord?>(null) }
    var message by rememberSaveable { mutableStateOf("") }
    val monthCount = months.toIntOrNull()?.coerceIn(0, 120) ?: 0
    val customers = snapshot.customers.filter {
        query.isBlank() || it.name.contains(query, true) || it.mobile.contains(query) || it.address.contains(query, true)
    }

    editing?.let { customer ->
        CustomerEditDialog(
            customer = customer,
            onDismiss = { editing = null },
            onSave = { name, mobile, address ->
                runCatching { CustomerAccountOperations.updateCustomer(snapshot, customer.id, name, mobile, address) }
                    .onSuccess {
                        onSnapshotChanged(it)
                        message = "Customer profile save ho gaya"
                        editing = null
                    }
                    .onFailure { message = it.message ?: "Customer save nahi hua" }
            },
        )
    }

    deleteCandidate?.let { customer ->
        AlertDialog(
            onDismissRequest = { deleteCandidate = null },
            title = { Text("Unused customer delete karein?") },
            text = { Text("${customer.name} ka koi girvi history nahi hai. Delete undo nahi hoga.") },
            confirmButton = {
                TextButton(onClick = {
                    runCatching { CustomerAccountOperations.deleteUnusedCustomer(snapshot, customer.id) }
                        .onSuccess {
                            onSnapshotChanged(it)
                            selectedId = null
                            message = "Unused customer delete ho gaya"
                            deleteCandidate = null
                        }
                        .onFailure { message = it.message ?: "Customer delete nahi hua" }
                }) { Text("Delete", color = ReportsRed) }
            },
            dismissButton = { TextButton(onClick = { deleteCandidate = null }) { Text("Cancel") } },
        )
    }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            OutlinedTextField(query, { query = it }, label = { Text("Customer search") }, leadingIcon = { Icon(Icons.Default.Search, null) }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(months, { months = it.filter(Char::isDigit).take(3) }, label = { Text("Settlement months") }, modifier = Modifier.fillMaxWidth())
            if (message.isNotBlank()) Text(message, color = ReportsGreen, fontSize = 12.sp)
        }
        if (selectedId == null) {
            items(customers, key = { it.id }) { customer ->
                val ledger = ReportingEngine.customerLedger(snapshot, customer.id, monthCount)
                Card(Modifier.fillMaxWidth().clickable { selectedId = customer.id }, colors = CardDefaults.cardColors(containerColor = Color.White)) {
                    Column(Modifier.padding(14.dp)) {
                        Text(customer.name, fontWeight = FontWeight.Bold)
                        Text("${customer.mobile.ifBlank { "No mobile" }} • ${customer.address.ifBlank { "No address" }}", color = Color.Gray)
                        Text("Outstanding ${reportsMoney(ledger.totalOutstandingPaise)}", color = ReportsGreen)
                    }
                }
            }
        } else {
            val customer = snapshot.customers.firstOrNull { it.id == selectedId }
            if (customer == null) {
                item { TextButton(onClick = { selectedId = null }) { Text("Customer list par wapas") } }
            } else {
                val girvis = snapshot.girvis.filter { it.customerId == customer.id }
                val ledger = ReportingEngine.customerLedger(snapshot, customer.id, monthCount)
                item {
                    ReportsBanner("${ledger.customerName}: ${reportsMoney(ledger.totalOutstandingPaise)} outstanding")
                    SummaryRow("Total Girvi", ledger.totalGirvi.toString(), "Active", ledger.activeGirvi.toString())
                    SummaryRow("Received", reportsMoney(ledger.effectiveReceivedPaise), "Principal Due", reportsMoney(ledger.outstandingPrincipalPaise))
                    ReportsCard("Mobile", customer.mobile.ifBlank { "Not saved" })
                    ReportsCard("Address", customer.address.ifBlank { "Not saved" })
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { editing = customer }, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.Edit, null); Text(" Edit")
                        }
                        OutlinedButton(
                            onClick = { deleteCandidate = customer },
                            enabled = CustomerAccountOperations.canDelete(snapshot, customer.id),
                            modifier = Modifier.weight(1f),
                        ) { Icon(Icons.Default.Delete, null); Text(" Delete") }
                    }
                    if (!CustomerAccountOperations.canDelete(snapshot, customer.id)) {
                        Text("Girvi history wale customer ko delete nahi kiya ja sakta.", color = Color.Gray, fontSize = 12.sp)
                    }
                    OutlinedButton(
                        onClick = { SecureShare.shareText(context, "${ledger.customerName} Statement", ReceiptTextBuilder.customerStatement(ledger.customerName, girvis)) },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Icon(Icons.Default.Share, null); Text(" Customer Statement Share") }
                    TextButton(onClick = { selectedId = null }, modifier = Modifier.fillMaxWidth()) { Text("Customer list par wapas") }
                }
                items(girvis.sortedByDescending { it.createdAt }, key = { it.id }) {
                    ReportsCard(it.girviNumber, "${it.status} • Principal ${reportsMoney(it.principalPaise)}")
                }
            }
        }
    }
}

@Composable
private fun CustomerEditDialog(customer: CustomerRecord, onDismiss: () -> Unit, onSave: (String, String, String) -> Unit) {
    var name by remember(customer.id) { mutableStateOf(customer.name) }
    var mobile by remember(customer.id) { mutableStateOf(customer.mobile) }
    var address by remember(customer.id) { mutableStateOf(customer.address) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Customer Profile Edit") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(
                    mobile,
                    { mobile = it.filter(Char::isDigit).take(15) },
                    label = { Text("Mobile") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(address, { address = it }, label = { Text("Address / Village") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = { TextButton(onClick = { onSave(name, mobile, address) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun GirviFilterReport(snapshot: AppSnapshot) {
    var query by rememberSaveable { mutableStateOf("") }
    var status by rememberSaveable { mutableStateOf(GirviStatusFilter.ALL) }
    val rows = ReportingEngine.filterGirvi(snapshot, status, query)
    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            OutlinedTextField(query, { query = it }, label = { Text("Girvi, customer, item ya category") }, leadingIcon = { Icon(Icons.Default.Search, null) }, modifier = Modifier.fillMaxWidth())
            Row {
                GirviStatusFilter.entries.forEach { filter ->
                    TextButton(onClick = { status = filter }, modifier = Modifier.weight(1f)) { Text(if (status == filter) "✓ ${filter.name}" else filter.name) }
                }
            }
            ReportsBanner("${rows.size} matching records")
        }
        items(rows, key = { it.id }) {
            ReportsCard(it.girviNumber, "${it.customerName} • ${it.status} • ${it.effectiveItems.joinToString { item -> item.itemName }} • ${reportsMoney(it.principalPaise)}")
        }
    }
}

@Composable
private fun CustodyReport(snapshot: AppSnapshot) {
    val context = LocalContext.current
    val custody = remember { CustodyPlacementStore(context.applicationContext).load() }
    val allItemIds = remember(snapshot) { snapshot.girvis.flatMap { it.effectiveItems }.map { it.id }.toSet() }
    val report = remember(snapshot, custody) {
        CustodyReportEngine.build(custody, allItemIds, System.currentTimeMillis(), movementLimit = 50)
    }
    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            ReportsBanner("Custody: ${allItemIds.size - report.unassignedItemIds.size} assigned • ${report.unassignedItemIds.size} unassigned")
            Text("Locker / Storage", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }
        if (report.storage.isEmpty()) item { ReportsCard("No locker items", "Storage location assign karne par yahan summary dikhegi") }
        items(report.storage, key = { "storage-${it.locationId}" }) { row ->
            ReportsCard(row.locationName, "${row.itemCount} current item(s)")
        }
        item { Text("External Placement • Owner Finance", fontWeight = FontWeight.Bold, fontSize = 18.sp) }
        if (report.externalParties.isEmpty()) item { ReportsCard("No active external placement", "Active lots/funding yahan dikhenge") }
        items(report.externalParties, key = { "party-${it.partyId}" }) { row ->
            ReportsCard(
                row.partyName,
                "${row.activeLotCount} active lot • ${row.activeItemCount} items • Principal ${reportsMoney(row.principalOutstandingPaise)} • Interest ${reportsMoney(row.interestOutstandingPaise)} • Total due ${reportsMoney(row.totalDuePaise)}",
            )
        }
        if (report.unassignedItemIds.isNotEmpty()) {
            item { Text("Unassigned Items", fontWeight = FontWeight.Bold, fontSize = 18.sp) }
            items(report.unassignedItemIds.toList().take(50), key = { "unassigned-$it" }) { itemId ->
                val girvi = snapshot.girvis.firstOrNull { g -> g.effectiveItems.any { it.id == itemId } }
                val item = girvi?.effectiveItems?.firstOrNull { it.id == itemId }
                ReportsCard(girvi?.girviNumber ?: "Unknown Girvi", "${girvi?.customerName.orEmpty()} • ${item?.itemName ?: itemId} • Location not assigned")
            }
        }
        item { Text("Recent Movement", fontWeight = FontWeight.Bold, fontSize = 18.sp) }
        items(report.recentMovements, key = { "move-${it.movementId}" }) { row ->
            val girvi = snapshot.girvis.firstOrNull { it.id == row.girviId }
            val item = girvi?.effectiveItems?.firstOrNull { it.id == row.itemId }
            ReportsCard(
                "${girvi?.girviNumber ?: row.girviId} • ${girvi?.customerName.orEmpty()}",
                "${item?.itemName ?: row.itemId} → ${row.destinationLabel} • ${DateFormat.getDateInstance().format(Date(row.movedAt))}${if (row.note.isBlank()) "" else " • ${row.note}"}",
            )
        }
    }
}

@Composable
private fun CollectionReport(snapshot: AppSnapshot) {
    val context = LocalContext.current
    var preset by rememberSaveable { mutableStateOf(CollectionPreset.TODAY) }
    var customFrom by rememberSaveable { mutableStateOf(startOfToday()) }
    var customTo by rememberSaveable { mutableStateOf(endOfToday()) }
    var message by rememberSaveable { mutableStateOf("") }
    val now = System.currentTimeMillis()
    val range = remember(preset, customFrom, customTo, now) {
        when (preset) {
            CollectionPreset.TODAY -> DateRange(startOfToday(), now)
            CollectionPreset.SEVEN_DAYS -> DateRange(startOfDaysAgo(6), now)
            CollectionPreset.THIRTY_DAYS -> DateRange(startOfDaysAgo(29), now)
            CollectionPreset.ALL -> DateRange(0L, now)
            CollectionPreset.CUSTOM -> DateRange(customFrom, customTo)
        }
    }
    val rows = remember(snapshot, range) {
        runCatching { ReportingEngine.collections(snapshot, range) }
            .onFailure { message = it.message ?: "Date range invalid hai" }
            .getOrDefault(emptyList())
    }
    val formatter = remember { SimpleDateFormat("dd MMM yyyy", Locale("en", "IN")) }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Row {
                listOf(
                    CollectionPreset.TODAY to "Today",
                    CollectionPreset.SEVEN_DAYS to "7 Days",
                    CollectionPreset.THIRTY_DAYS to "30 Days",
                    CollectionPreset.ALL to "All",
                ).forEach { (value, label) ->
                    TextButton(onClick = { preset = value }, modifier = Modifier.weight(1f)) {
                        Text(if (preset == value) "✓ $label" else label, fontSize = 11.sp)
                    }
                }
            }
            OutlinedButton(onClick = { preset = CollectionPreset.CUSTOM }, modifier = Modifier.fillMaxWidth()) {
                Text(if (preset == CollectionPreset.CUSTOM) "✓ Custom Date Range" else "Custom Date Range")
            }
            if (preset == CollectionPreset.CUSTOM) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            showDatePicker(context, customFrom) { selected ->
                                customFrom = startOfDay(selected)
                                if (customFrom > customTo) message = "From date, To date se baad nahi ho sakti"
                            }
                        },
                        modifier = Modifier.weight(1f),
                    ) { Text("From\n${formatter.format(Date(customFrom))}", fontSize = 11.sp) }
                    OutlinedButton(
                        onClick = {
                            showDatePicker(context, customTo) { selected ->
                                customTo = endOfDay(selected)
                                if (customFrom > customTo) message = "From date, To date se baad nahi ho sakti"
                            }
                        },
                        modifier = Modifier.weight(1f),
                    ) { Text("To\n${formatter.format(Date(customTo))}", fontSize = 11.sp) }
                }
            }
            if (message.isNotBlank()) Text(message, color = ReportsRed, fontSize = 12.sp)
            ReportsBanner("Collections: ${reportsMoney(rows.sumOf { it.amountPaise })} • ${rows.size} receipts")
            Button(
                onClick = { SecureShare.shareCsv(context, "girvi-collections-${System.currentTimeMillis()}.csv", CsvExportBuilder.collections(rows)) },
                enabled = rows.isNotEmpty(),
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = ReportsPurple),
            ) { Icon(Icons.Default.Share, null); Text(" Collection CSV Share") }
        }
        items(rows, key = { it.receiptNumber }) { row ->
            val girvi = snapshot.girvis.firstOrNull { it.girviNumber == row.girviNumber }
            val payment = girvi?.payments?.firstOrNull { it.receiptNumber == row.receiptNumber }
            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(row.receiptNumber, fontWeight = FontWeight.Bold)
                    Text("${row.customerName} • ${row.girviNumber}")
                    Text("${reportsMoney(row.amountPaise)} • ${row.mode} • ${DateFormat.getDateTimeInstance().format(Date(row.createdAt))}", color = ReportsGreen)
                    if (girvi != null && payment != null) {
                        OutlinedButton(
                            onClick = { SecureShare.shareText(context, row.receiptNumber, ReceiptTextBuilder.paymentReceipt(girvi, payment)) },
                            modifier = Modifier.fillMaxWidth(),
                        ) { Icon(Icons.Default.Share, null); Text(" Receipt Share") }
                    }
                }
            }
        }
    }
}

private fun showDatePicker(context: android.content.Context, currentMillis: Long, onSelected: (Long) -> Unit) {
    val current = Calendar.getInstance().apply { timeInMillis = currentMillis }
    DatePickerDialog(
        context,
        { _, year, month, day ->
            onSelected(Calendar.getInstance().apply {
                set(Calendar.YEAR, year)
                set(Calendar.MONTH, month)
                set(Calendar.DAY_OF_MONTH, day)
            }.timeInMillis)
        },
        current.get(Calendar.YEAR),
        current.get(Calendar.MONTH),
        current.get(Calendar.DAY_OF_MONTH),
    ).show()
}

private fun startOfToday(): Long = startOfDay(System.currentTimeMillis())
private fun endOfToday(): Long = endOfDay(System.currentTimeMillis())
private fun startOfDaysAgo(days: Int): Long = Calendar.getInstance().apply {
    add(Calendar.DAY_OF_YEAR, -days)
}.timeInMillis.let(::startOfDay)

private fun startOfDay(millis: Long): Long = Calendar.getInstance().apply {
    timeInMillis = millis
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
}.timeInMillis

private fun endOfDay(millis: Long): Long = Calendar.getInstance().apply {
    timeInMillis = millis
    set(Calendar.HOUR_OF_DAY, 23)
    set(Calendar.MINUTE, 59)
    set(Calendar.SECOND, 59)
    set(Calendar.MILLISECOND, 999)
}.timeInMillis

@Composable
private fun SummaryRow(leftLabel: String, leftValue: String, rightLabel: String, rightValue: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        SummaryCard(leftLabel, leftValue, Modifier.weight(1f))
        SummaryCard(rightLabel, rightValue, Modifier.weight(1f))
    }
}

@Composable
private fun SummaryCard(label: String, value: String, modifier: Modifier) = Card(modifier, colors = CardDefaults.cardColors(containerColor = Color.White)) {
    Column(Modifier.padding(14.dp)) { Text(label, color = Color.Gray, fontSize = 12.sp); Text(value, fontWeight = FontWeight.Bold, fontSize = 17.sp) }
}

@Composable
private fun ReportsCard(title: String, subtitle: String) = Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White)) {
    Column(Modifier.padding(14.dp)) { Text(title, fontWeight = FontWeight.Bold); Text(subtitle, color = Color.Gray) }
}

@Composable
private fun ReportsBanner(text: String) = Box(Modifier.fillMaxWidth().background(Color(0xFFEAF7EF), RoundedCornerShape(12.dp)).padding(12.dp)) {
    Text(text, color = ReportsGreen, fontWeight = FontWeight.Medium)
}

private fun ReportsPage.label(): String = when (this) {
    ReportsPage.OVERVIEW -> "Overview"
    ReportsPage.KHATA -> "Khata"
    ReportsPage.GIRVI -> "Girvi"
    ReportsPage.COLLECTIONS -> "Collection"
    ReportsPage.CUSTODY -> "Custody"
}

private fun reportsMoney(paise: Long): String = "₹%,.2f".format(Locale("en", "IN"), paise / 100.0)
