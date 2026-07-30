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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
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
import androidx.fragment.app.FragmentActivity
import com.girvikhata.app.data.AppSnapshot
import com.girvikhata.app.data.CustomerRecord
import com.girvikhata.app.data.EncryptedMasterCatalogStore
import com.girvikhata.app.data.EncryptedRecordStore
import com.girvikhata.app.data.GirviItemRecord
import com.girvikhata.app.data.GirviRecord
import com.girvikhata.app.data.RelationalShadowFingerprint
import com.girvikhata.app.data.VerifiedBusinessMutation
import com.girvikhata.app.data.VerifiedBusinessWriteCoordinator
import com.girvikhata.app.data.VerifiedBusinessWriteRequest
import com.girvikhata.app.domain.CustomerCandidate
import com.girvikhata.app.domain.CustomerMatcher
import com.girvikhata.app.domain.GirviSequence
import com.girvikhata.app.domain.GirviSettlementUseCase
import com.girvikhata.app.domain.MasterCatalog
import com.girvikhata.app.domain.MasterCatalogOperations
import com.girvikhata.app.domain.MasterEntry
import com.girvikhata.app.domain.MasterKind
import com.girvikhata.app.domain.MoneyInput
import com.girvikhata.app.domain.PaymentAllocationMode
import com.girvikhata.app.security.PinVerificationResult
import com.girvikhata.app.security.SecurityPreferences
import java.util.Locale
import kotlin.math.roundToLong

class MasterWorkflowActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val records = EncryptedRecordStore(applicationContext)
        val masters = EncryptedMasterCatalogStore(applicationContext)
        val security = SecurityPreferences(applicationContext)
        val coordinator = VerifiedBusinessWriteCoordinator(applicationContext, records = records)
        setContent {
            MaterialTheme {
                MasterWorkflowRoot(
                    verifyPin = { security.verify(it.toCharArray()) },
                    loadSnapshot = records::load,
                    loadCatalog = masters::load,
                    commitGirvi = { screenSnapshot, customer, girvi ->
                        val result = coordinator.execute(
                            VerifiedBusinessWriteRequest(
                                expectedFingerprint = RelationalShadowFingerprint.sha256(screenSnapshot),
                                mutation = VerifiedBusinessMutation.CreateGirviWithCustomer(customer, girvi),
                                title = "Girvi ${girvi.girviNumber} created atomically",
                            ),
                        )
                        "Saved: ${girvi.girviNumber} • TX ${result.transactionId.take(8)}"
                    },
                    commitPayment = { screenSnapshot, updated ->
                        val original = screenSnapshot.girvis.firstOrNull { it.id == updated.id }
                            ?: error("Girvi refresh required")
                        require(updated.payments.size == original.payments.size + 1) {
                            "Payment transaction shape invalid; refresh and retry"
                        }
                        val payment = updated.payments.last()
                        val result = coordinator.execute(
                            VerifiedBusinessWriteRequest(
                                expectedFingerprint = RelationalShadowFingerprint.sha256(screenSnapshot),
                                mutation = VerifiedBusinessMutation.AppendPayment(updated.id, payment),
                                title = "Payment ${payment.receiptNumber} received",
                            ),
                        )
                        "Saved payment: ${payment.receiptNumber} • TX ${result.transactionId.take(8)}"
                    },
                    close = ::finish,
                )
            }
        }
    }
}

private enum class WorkflowTab { NEW_GIRVI, RECEIVE_PAYMENT }

@Composable
private fun MasterWorkflowRoot(
    verifyPin: (String) -> PinVerificationResult,
    loadSnapshot: () -> AppSnapshot,
    loadCatalog: () -> MasterCatalog,
    commitGirvi: (AppSnapshot, CustomerRecord, GirviRecord) -> String,
    commitPayment: (AppSnapshot, GirviRecord) -> String,
    close: () -> Unit,
) {
    var unlocked by rememberSaveable { mutableStateOf(false) }
    if (!unlocked) {
        WorkflowPinScreen(verifyPin, { unlocked = true }, close)
        return
    }
    var snapshot by remember { mutableStateOf(loadSnapshot()) }
    val catalog = remember { loadCatalog() }
    var tab by rememberSaveable { mutableStateOf(WorkflowTab.NEW_GIRVI) }

    Column(
        Modifier.fillMaxSize().background(Color(0xFFF6F7FB)).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text("Master-Assisted Entry", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Color(0xFF171752))
                Text("Verified snapshot + relational write", color = Color.Gray)
            }
            Row {
                TextButton(onClick = { snapshot = loadSnapshot() }) { Text("Refresh Data") }
                TextButton(onClick = close) { Text("Close") }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = { tab = WorkflowTab.NEW_GIRVI },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = if (tab == WorkflowTab.NEW_GIRVI) Color(0xFF5146B8) else Color.Gray),
            ) { Text("Naya Girvi") }
            Button(
                onClick = { tab = WorkflowTab.RECEIVE_PAYMENT },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = if (tab == WorkflowTab.RECEIVE_PAYMENT) Color(0xFF138A4A) else Color.Gray),
            ) { Text("Payment") }
        }
        when (tab) {
            WorkflowTab.NEW_GIRVI -> MasterNewGirvi(snapshot, catalog) { customer, girvi ->
                val result = commitGirvi(snapshot, customer, girvi)
                snapshot = loadSnapshot()
                result
            }
            WorkflowTab.RECEIVE_PAYMENT -> MasterPayment(snapshot, catalog) { updated ->
                val result = commitPayment(snapshot, updated)
                snapshot = loadSnapshot()
                result
            }
        }
    }
}

@Composable
private fun WorkflowPinScreen(verifyPin: (String) -> PinVerificationResult, success: () -> Unit, close: () -> Unit) {
    var pin by rememberSaveable { mutableStateOf("") }
    var message by rememberSaveable { mutableStateOf("Master-assisted entry ke liye PIN verify karein") }
    Column(
        Modifier.fillMaxSize().background(Color(0xFFF6F7FB)).padding(22.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Secure Business Entry", fontSize = 27.sp, fontWeight = FontWeight.Bold, color = Color(0xFF171752))
        Text(message, color = Color.Gray)
        Card(Modifier.fillMaxWidth().padding(top = 18.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(pin, { pin = it.filter(Char::isDigit).take(6) }, label = { Text("6-digit PIN") }, visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword), modifier = Modifier.fillMaxWidth())
                Button(onClick = {
                    when (val result = verifyPin(pin)) {
                        PinVerificationResult.Success -> success()
                        PinVerificationResult.NotConfigured -> message = "PIN configured nahi hai"
                        is PinVerificationResult.Locked -> message = "Security lock active hai"
                        is PinVerificationResult.Failure -> message = "Galat PIN. Attempts: ${result.attempts}"
                    }
                    pin = ""
                }, enabled = pin.length == 6, modifier = Modifier.fillMaxWidth()) { Text("PIN Verify") }
                OutlinedButton(onClick = close, modifier = Modifier.fillMaxWidth()) { Text("Close") }
            }
        }
    }
}

@Composable
private fun MasterNewGirvi(snapshot: AppSnapshot, catalog: MasterCatalog, save: (CustomerRecord, GirviRecord) -> String) {
    val categories = snapshot.categories.filter { it.active }.map { it.name }
    val items = MasterCatalogOperations.active(catalog, MasterKind.ITEM)
    val units = MasterCatalogOperations.active(catalog, MasterKind.UNIT)
    val plans = MasterCatalogOperations.active(catalog, MasterKind.INTEREST_PLAN)
    val lockers = MasterCatalogOperations.active(catalog, MasterKind.LOCKER)

    var customerName by rememberSaveable { mutableStateOf("") }
    var customerId by rememberSaveable { mutableStateOf<String?>(null) }
    var mobile by rememberSaveable { mutableStateOf("") }
    var address by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf(categories.firstOrNull().orEmpty()) }
    var selectedItemId by rememberSaveable { mutableStateOf<String?>(null) }
    var manualItem by rememberSaveable { mutableStateOf("") }
    var selectedUnitId by rememberSaveable { mutableStateOf(units.firstOrNull()?.id) }
    var quantity by rememberSaveable { mutableStateOf("1") }
    var gross by rememberSaveable { mutableStateOf("") }
    var deduction by rememberSaveable { mutableStateOf("") }
    var selectedPlanId by rememberSaveable { mutableStateOf(plans.firstOrNull()?.id) }
    var manualRate by rememberSaveable { mutableStateOf("2") }
    var selectedLockerId by rememberSaveable { mutableStateOf(lockers.firstOrNull()?.id) }
    var principal by rememberSaveable { mutableStateOf("") }
    var note by rememberSaveable { mutableStateOf("") }
    var message by rememberSaveable { mutableStateOf<String?>(null) }

    val selectedItem = items.firstOrNull { it.id == selectedItemId }
    val selectedUnit = units.firstOrNull { it.id == selectedUnitId }
    val selectedPlan = plans.firstOrNull { it.id == selectedPlanId }
    val selectedLocker = lockers.firstOrNull { it.id == selectedLockerId }
    val relevantItems = items.filter { it.categoryName.isBlank() || it.categoryName.equals(category, true) }

    LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item { OutlinedTextField(customerName, { customerName = it; customerId = null }, label = { Text("Customer name / search") }, modifier = Modifier.fillMaxWidth()) }
        if (customerName.isNotBlank() && customerId == null) {
            val matches = CustomerMatcher.search(snapshot.customers.map { CustomerCandidate(it.id, it.name, it.mobile, it.address) }, customerName).take(4)
            items(matches.size) { index ->
                val match = matches[index]
                TextButton(onClick = { customerName = match.name; customerId = match.id; mobile = match.mobile; address = match.address }) { Text("${match.name} • ${match.mobile} • ${match.address}") }
            }
        }
        item { OutlinedTextField(mobile, { mobile = it.filter(Char::isDigit).take(10) }, label = { Text("Mobile") }, modifier = Modifier.fillMaxWidth()) }
        item { OutlinedTextField(address, { address = it }, label = { Text("Address") }, modifier = Modifier.fillMaxWidth()) }
        item { Text("Category", fontWeight = FontWeight.Bold); ChoiceRow(categories, category) { category = it; selectedItemId = null } }
        item { Text("Saved Item", fontWeight = FontWeight.Bold); MasterChoiceRow(relevantItems, selectedItemId) { selectedItemId = it } }
        item { OutlinedTextField(manualItem, { manualItem = it }, label = { Text("Manual item fallback") }, modifier = Modifier.fillMaxWidth()) }
        item { Text("Unit", fontWeight = FontWeight.Bold); MasterChoiceRow(units, selectedUnitId) { selectedUnitId = it } }
        item { OutlinedTextField(quantity, { quantity = it.filter(Char::isDigit).take(4) }, label = { Text("Quantity") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth()) }
        item { OutlinedTextField(gross, { gross = decimalInput(it) }, label = { Text("Gross weight (${selectedUnit?.name ?: "unit"})") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth()) }
        item { OutlinedTextField(deduction, { deduction = decimalInput(it) }, label = { Text("Deduction") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth()) }
        item { Text("Interest Plan", fontWeight = FontWeight.Bold); MasterChoiceRow(plans, selectedPlanId) { selectedPlanId = it } }
        if (selectedPlan == null) item { OutlinedTextField(manualRate, { manualRate = decimalInput(it) }, label = { Text("Manual monthly rate %") }, modifier = Modifier.fillMaxWidth()) }
        item { Text("Locker / Storage", fontWeight = FontWeight.Bold); MasterChoiceRow(lockers, selectedLockerId) { selectedLockerId = it } }
        item { OutlinedTextField(principal, { principal = decimalInput(it) }, label = { Text("Principal ₹") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth()) }
        item { OutlinedTextField(note, { note = it }, label = { Text("Description / note") }, modifier = Modifier.fillMaxWidth()) }
        item { message?.let { Text(it, color = if (it.startsWith("Saved")) Color(0xFF138A4A) else MaterialTheme.colorScheme.error) } }
        item {
            Button(onClick = {
                runCatching {
                    val cleanName = customerName.trim()
                    require(cleanName.length >= 2) { "Customer required" }
                    require(category.isNotBlank()) { "Active category required" }
                    val itemName = selectedItem?.name ?: manualItem.trim()
                    require(itemName.isNotBlank()) { "Saved ya manual item required" }
                    val qty = quantity.toIntOrNull() ?: error("Valid quantity required")
                    require(qty > 0) { "Quantity positive hona chahiye" }
                    val grossValue = gross.toDoubleOrNull() ?: 0.0
                    val deductionValue = deduction.toDoubleOrNull() ?: 0.0
                    require(grossValue >= 0 && deductionValue >= 0 && deductionValue <= grossValue) { "Weight/deduction invalid" }
                    val amount = principal.toDoubleOrNull() ?: error("Valid principal required")
                    require(amount > 0) { "Principal positive hona chahiye" }
                    val rateBp = selectedPlan?.rateBasisPoints ?: ((manualRate.toDoubleOrNull() ?: error("Valid rate required")) * 100).roundToLong().toInt()
                    require(rateBp in 0..100_000) { "Interest rate invalid" }
                    val existing = customerId?.let { id -> snapshot.customers.firstOrNull { it.id == id } }
                        ?: CustomerMatcher.findBestMatch(snapshot.customers.map { CustomerCandidate(it.id, it.name, it.mobile, it.address) }, cleanName, mobile)?.let { match -> snapshot.customers.first { it.id == match.id } }
                    val customer = existing ?: CustomerRecord(name = cleanName, mobile = mobile, address = address.trim())
                    val metadata = buildList {
                        add("Unit: ${selectedUnit?.name ?: "manual"}")
                        add("Locker: ${selectedLocker?.name ?: "not selected"}")
                        add("Plan: ${selectedPlan?.name ?: "manual"}")
                        if (note.isNotBlank()) add(note.trim())
                    }.joinToString(" • ")
                    val item = GirviItemRecord(categoryName = category, itemName = itemName, quantity = qty, grossWeightGrams = gross, deductionWeightGrams = deduction, description = metadata)
                    val girvi = GirviRecord(girviNumber = GirviSequence.nextNumber(snapshot.girvis.map { it.girviNumber }), customerId = customer.id, customerName = customer.name, categoryName = category, itemName = itemName, weightGrams = gross, principalPaise = (amount * 100).roundToLong(), monthlyRateBasisPoints = rateBp, items = listOf(item))
                    val successMessage = save(customer, girvi)
                    customerName = ""; customerId = null; mobile = ""; address = ""; manualItem = ""; principal = ""; gross = ""; deduction = ""; note = ""
                    message = successMessage
                }.onFailure { message = it.message ?: "Girvi save failed" }
            }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5146B8))) { Text("Master Choices Ke Saath Girvi Save Karein") }
        }
    }
}

@Composable
private fun MasterPayment(snapshot: AppSnapshot, catalog: MasterCatalog, save: (GirviRecord) -> String) {
    val activeGirvis = snapshot.girvis.filter { it.status == "ACTIVE" }
    val modes = MasterCatalogOperations.active(catalog, MasterKind.PAYMENT_MODE)
    var selectedGirviId by rememberSaveable { mutableStateOf(activeGirvis.firstOrNull()?.id) }
    var selectedModeId by rememberSaveable { mutableStateOf(modes.firstOrNull()?.id) }
    var amount by rememberSaveable { mutableStateOf("") }
    var months by rememberSaveable { mutableStateOf("1") }
    var interestFirst by rememberSaveable { mutableStateOf(true) }
    var note by rememberSaveable { mutableStateOf("") }
    var message by rememberSaveable { mutableStateOf<String?>(null) }
    val selectedGirvi = activeGirvis.firstOrNull { it.id == selectedGirviId }
    val selectedMode = modes.firstOrNull { it.id == selectedModeId }

    LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(9.dp)) {
        item { Text("Active Girvi", fontWeight = FontWeight.Bold) }
        items(activeGirvis.size) { index ->
            val girvi = activeGirvis[index]
            TextButton(onClick = { selectedGirviId = girvi.id }) { Text(if (selectedGirviId == girvi.id) "✓ ${girvi.girviNumber} • ${girvi.customerName}" else "${girvi.girviNumber} • ${girvi.customerName}") }
        }
        item { OutlinedTextField(months, { months = it.filter(Char::isDigit).take(3) }, label = { Text("Settlement months") }, modifier = Modifier.fillMaxWidth()) }
        item { selectedGirvi?.let { girvi -> runCatching { GirviSettlementUseCase.settlementView(girvi, months.toIntOrNull() ?: 0) }.getOrNull()?.let { Text("Due: Principal ${moneyText(it.principalDuePaise)} • Interest ${moneyText(it.interestDuePaise)} • Total ${moneyText(it.totalDuePaise)}") } } }
        item { Text("Saved Payment Mode", fontWeight = FontWeight.Bold); MasterChoiceRow(modes, selectedModeId) { selectedModeId = it } }
        item { OutlinedTextField(amount, { amount = decimalInput(it) }, label = { Text("Payment amount ₹") }, modifier = Modifier.fillMaxWidth()) }
        item { Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(interestFirst, { interestFirst = it }); Text(if (interestFirst) "Interest first allocation" else "Principal first allocation") } }
        item { OutlinedTextField(note, { note = it }, label = { Text("Payment note") }, modifier = Modifier.fillMaxWidth()) }
        item { message?.let { Text(it, color = if (it.startsWith("Saved")) Color(0xFF138A4A) else MaterialTheme.colorScheme.error) } }
        item {
            Button(onClick = {
                runCatching {
                    val girvi = requireNotNull(selectedGirvi) { "Active girvi select karein" }
                    val mode = requireNotNull(selectedMode) { "Active payment mode select karein" }
                    val monthCount = months.toIntOrNull() ?: error("Valid months required")
                    require(monthCount in 0..1_200) { "Settlement months invalid" }
                    GirviSettlementUseCase.postPayment(girvi, monthCount, MoneyInput.rupeesToPaise(amount), if (interestFirst) PaymentAllocationMode.INTEREST_FIRST else PaymentAllocationMode.PRINCIPAL_FIRST, mode.name, note, snapshot.girvis.flatMap { it.payments }.map { it.receiptNumber })
                }.onSuccess { updated ->
                    message = save(updated)
                    amount = ""; note = ""
                }.onFailure { message = it.message ?: "Payment save failed" }
            }, enabled = activeGirvis.isNotEmpty() && modes.isNotEmpty(), modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF138A4A))) { Text("Saved Mode Ke Saath Payment Receive Karein") }
        }
    }
}

@Composable
private fun MasterChoiceRow(entries: List<MasterEntry>, selectedId: String?, select: (String) -> Unit) {
    if (entries.isEmpty()) { Text("Koi active saved option nahi", color = Color.Gray); return }
    entries.forEach { entry ->
        TextButton(onClick = { select(entry.id) }) {
            val detail = if (entry.kind == MasterKind.INTEREST_PLAN) " (${entry.rateBasisPoints / 100.0}%)" else ""
            Text(if (selectedId == entry.id) "✓ ${entry.name}$detail" else "${entry.name}$detail")
        }
    }
}

@Composable
private fun ChoiceRow(values: List<String>, selected: String, select: (String) -> Unit) {
    values.forEach { value -> TextButton(onClick = { select(value) }) { Text(if (selected == value) "✓ $value" else value) } }
}

private fun decimalInput(value: String): String {
    val clean = value.filter { it.isDigit() || it == '.' }
    val firstDot = clean.indexOf('.')
    return if (firstDot < 0) clean.take(12) else clean.substring(0, firstDot + 1) + clean.substring(firstDot + 1).replace(".", "").take(2)
}

private fun moneyText(paise: Long): String = String.format(Locale.US, "₹%.2f", paise / 100.0)
