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
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.runtime.mutableStateListOf
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
import com.girvikhata.app.data.DataSafetyJournal
import com.girvikhata.app.data.EncryptedMasterCatalogStore
import com.girvikhata.app.data.EncryptedRecordStore
import com.girvikhata.app.data.GirviRecord
import com.girvikhata.app.domain.AccountBalance
import com.girvikhata.app.domain.CustomPaymentDraft
import com.girvikhata.app.domain.GirviSequence
import com.girvikhata.app.domain.GirviSettlementUseCase
import com.girvikhata.app.domain.MasterCatalog
import com.girvikhata.app.domain.MasterCatalogOperations
import com.girvikhata.app.domain.MasterEntry
import com.girvikhata.app.domain.MasterItemDraft
import com.girvikhata.app.domain.MasterKind
import com.girvikhata.app.domain.MasterWorkflowTransactions
import com.girvikhata.app.domain.MoneyInput
import com.girvikhata.app.domain.PaymentAllocationMode
import com.girvikhata.app.security.PinVerificationResult
import com.girvikhata.app.security.SecurityPreferences
import java.util.Locale
import kotlin.math.roundToLong

class AdvancedWorkflowActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val records = EncryptedRecordStore(applicationContext)
        val masters = EncryptedMasterCatalogStore(applicationContext)
        val security = SecurityPreferences(applicationContext)
        val journal = DataSafetyJournal(applicationContext)
        setContent {
            MaterialTheme {
                AdvancedRoot(
                    verifyPin = { security.verify(it.toCharArray()) },
                    loadSnapshot = records::load,
                    loadCatalog = masters::load,
                    persist = records::save,
                    audit = journal::recordNamedEvent,
                    close = ::finish,
                )
            }
        }
    }
}

private enum class AdvancedTab { MULTI_ITEM, CUSTOM_PAYMENT }

@Composable
private fun AdvancedRoot(
    verifyPin: (String) -> PinVerificationResult,
    loadSnapshot: () -> AppSnapshot,
    loadCatalog: () -> MasterCatalog,
    persist: (AppSnapshot) -> Unit,
    audit: (String, String, String) -> Unit,
    close: () -> Unit,
) {
    var unlocked by rememberSaveable { mutableStateOf(false) }
    if (!unlocked) {
        SecureEntry(verifyPin, { unlocked = true }, close)
        return
    }
    var snapshot by remember { mutableStateOf(loadSnapshot()) }
    val catalog = remember { loadCatalog() }
    var tab by rememberSaveable { mutableStateOf(AdvancedTab.MULTI_ITEM) }

    Column(Modifier.fillMaxSize().background(Color(0xFFF6F7FB)).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text("Advanced Girvi Entry", fontSize = 25.sp, fontWeight = FontWeight.Bold, color = Color(0xFF171752))
                Text("Multiple items aur exact payment split", color = Color.Gray)
            }
            TextButton(onClick = close) { Text("Close") }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { tab = AdvancedTab.MULTI_ITEM }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = if (tab == AdvancedTab.MULTI_ITEM) Color(0xFF5146B8) else Color.Gray)) { Text("Multi Item") }
            Button(onClick = { tab = AdvancedTab.CUSTOM_PAYMENT }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = if (tab == AdvancedTab.CUSTOM_PAYMENT) Color(0xFF138A4A) else Color.Gray)) { Text("Custom Split") }
        }
        when (tab) {
            AdvancedTab.MULTI_ITEM -> MultiItemEntry(snapshot, catalog) { customer, girvi ->
                val customers = if (snapshot.customers.any { it.id == customer.id }) snapshot.customers else snapshot.customers + customer
                val next = snapshot.copy(customers = customers, girvis = snapshot.girvis + girvi)
                persist(next)
                snapshot = next
                audit("GIRVI_MULTI_ITEM_CREATED", "Multi-item girvi created", "${girvi.girviNumber} • ${girvi.items.size} items • ${girvi.customerName}")
            }
            AdvancedTab.CUSTOM_PAYMENT -> CustomSplitPayment(snapshot, catalog) { updated ->
                val next = snapshot.copy(girvis = snapshot.girvis.map { if (it.id == updated.id) updated else it })
                persist(next)
                snapshot = next
                val p = updated.payments.last()
                audit("PAYMENT_CUSTOM_SPLIT", "Custom split payment received", "${updated.girviNumber} • ${p.receiptNumber} • principal ${p.principalPaise} • interest ${p.interestPaise} • charges ${p.chargesPaise}")
            }
        }
    }
}

@Composable
private fun SecureEntry(verifyPin: (String) -> PinVerificationResult, success: () -> Unit, close: () -> Unit) {
    var pin by rememberSaveable { mutableStateOf("") }
    var message by rememberSaveable { mutableStateOf("Advanced entry ke liye PIN verify karein") }
    Column(Modifier.fillMaxSize().background(Color(0xFFF6F7FB)).padding(22.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Secure Advanced Entry", fontSize = 27.sp, fontWeight = FontWeight.Bold)
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
private fun MultiItemEntry(snapshot: AppSnapshot, catalog: MasterCatalog, save: (CustomerRecord, GirviRecord) -> Unit) {
    val categories = snapshot.categories.filter { it.active }.map { it.name }
    val savedItems = MasterCatalogOperations.active(catalog, MasterKind.ITEM)
    val units = MasterCatalogOperations.active(catalog, MasterKind.UNIT)
    val plans = MasterCatalogOperations.active(catalog, MasterKind.INTEREST_PLAN)
    val lockers = MasterCatalogOperations.active(catalog, MasterKind.LOCKER)
    val drafts = remember { mutableStateListOf<MasterItemDraft>() }

    var customerName by rememberSaveable { mutableStateOf("") }
    var mobile by rememberSaveable { mutableStateOf("") }
    var address by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf(categories.firstOrNull().orEmpty()) }
    var itemId by rememberSaveable { mutableStateOf<String?>(null) }
    var manualItem by rememberSaveable { mutableStateOf("") }
    var unitId by rememberSaveable { mutableStateOf(units.firstOrNull()?.id) }
    var quantity by rememberSaveable { mutableStateOf("1") }
    var gross by rememberSaveable { mutableStateOf("") }
    var deduction by rememberSaveable { mutableStateOf("") }
    var itemNote by rememberSaveable { mutableStateOf("") }
    var planId by rememberSaveable { mutableStateOf(plans.firstOrNull()?.id) }
    var manualRate by rememberSaveable { mutableStateOf("2") }
    var lockerId by rememberSaveable { mutableStateOf(lockers.firstOrNull()?.id) }
    var principal by rememberSaveable { mutableStateOf("") }
    var message by rememberSaveable { mutableStateOf<String?>(null) }

    val selectedItem = savedItems.firstOrNull { it.id == itemId }
    val selectedUnit = units.firstOrNull { it.id == unitId }
    val selectedPlan = plans.firstOrNull { it.id == planId }
    val selectedLocker = lockers.firstOrNull { it.id == lockerId }
    val relevantItems = savedItems.filter { it.categoryName.isBlank() || it.categoryName.equals(category, true) }

    LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item { OutlinedTextField(customerName, { customerName = it }, label = { Text("Customer name") }, modifier = Modifier.fillMaxWidth()) }
        item { OutlinedTextField(mobile, { mobile = it.filter(Char::isDigit).take(10) }, label = { Text("Mobile") }, modifier = Modifier.fillMaxWidth()) }
        item { OutlinedTextField(address, { address = it }, label = { Text("Address") }, modifier = Modifier.fillMaxWidth()) }
        item { Text("Category", fontWeight = FontWeight.Bold); TextChoices(categories, category) { category = it; itemId = null } }
        item { Text("Saved Item", fontWeight = FontWeight.Bold); MasterChoices(relevantItems, itemId) { itemId = it } }
        item { OutlinedTextField(manualItem, { manualItem = it }, label = { Text("Manual item fallback") }, modifier = Modifier.fillMaxWidth()) }
        item { Text("Unit", fontWeight = FontWeight.Bold); MasterChoices(units, unitId) { unitId = it } }
        item { OutlinedTextField(quantity, { quantity = it.filter(Char::isDigit).take(4) }, label = { Text("Quantity") }, modifier = Modifier.fillMaxWidth()) }
        item { OutlinedTextField(gross, { gross = decimal(it) }, label = { Text("Gross weight") }, modifier = Modifier.fillMaxWidth()) }
        item { OutlinedTextField(deduction, { deduction = decimal(it) }, label = { Text("Deduction") }, modifier = Modifier.fillMaxWidth()) }
        item { OutlinedTextField(itemNote, { itemNote = it }, label = { Text("Item note") }, modifier = Modifier.fillMaxWidth()) }
        item {
            OutlinedButton(onClick = {
                runCatching {
                    val name = selectedItem?.name ?: manualItem.trim()
                    val metadata = listOf("Unit: ${selectedUnit?.name ?: "manual"}", "Locker: ${selectedLocker?.name ?: "not selected"}", itemNote.trim()).filter { it.isNotBlank() }.joinToString(" • ")
                    val candidate = MasterItemDraft(category, name, quantity.toIntOrNull() ?: 0, gross, deduction, metadata)
                    MasterWorkflowTransactions.validateItems(drafts + candidate)
                    drafts += candidate
                    manualItem = ""; quantity = "1"; gross = ""; deduction = ""; itemNote = ""; itemId = null
                    message = "Item added. Total ${drafts.size}"
                }.onFailure { message = it.message ?: "Item add failed" }
            }, modifier = Modifier.fillMaxWidth()) { Text("+ Item Add Karein") }
        }
        item { Text("Added Items: ${drafts.size}", fontWeight = FontWeight.Bold) }
        itemsIndexed(drafts) { index, draft ->
            Card(Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth().padding(10.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("${index + 1}. ${draft.categoryName} • ${draft.itemName} • Qty ${draft.quantity}")
                    TextButton(onClick = { drafts.removeAt(index) }) { Text("Remove") }
                }
            }
        }
        item { Text("Interest Plan", fontWeight = FontWeight.Bold); MasterChoices(plans, planId) { planId = it } }
        if (selectedPlan == null) item { OutlinedTextField(manualRate, { manualRate = decimal(it) }, label = { Text("Monthly rate %") }, modifier = Modifier.fillMaxWidth()) }
        item { Text("Locker / Storage", fontWeight = FontWeight.Bold); MasterChoices(lockers, lockerId) { lockerId = it } }
        item { OutlinedTextField(principal, { principal = decimal(it) }, label = { Text("Principal ₹") }, modifier = Modifier.fillMaxWidth()) }
        item { message?.let { Text(it, color = if (it.startsWith("Saved") || it.startsWith("Item")) Color(0xFF138A4A) else MaterialTheme.colorScheme.error) } }
        item {
            Button(onClick = {
                runCatching {
                    val name = customerName.trim(); require(name.length >= 2) { "Customer required" }
                    val items = MasterWorkflowTransactions.validateItems(drafts)
                    val amount = MoneyInput.rupeesToPaise(principal)
                    val rate = selectedPlan?.rateBasisPoints ?: ((manualRate.toDoubleOrNull() ?: error("Valid rate required")) * 100).roundToLong().toInt()
                    require(rate in 0..100_000) { "Interest rate invalid" }
                    val customer = CustomerRecord(name = name, mobile = mobile, address = address.trim())
                    val first = items.first()
                    val girvi = GirviRecord(
                        girviNumber = GirviSequence.nextNumber(snapshot.girvis.map { it.girviNumber }),
                        customerId = customer.id,
                        customerName = customer.name,
                        categoryName = first.categoryName,
                        itemName = first.itemName,
                        weightGrams = first.grossWeightGrams,
                        principalPaise = amount,
                        monthlyRateBasisPoints = rate,
                        items = items,
                    )
                    save(customer, girvi)
                    drafts.clear(); customerName = ""; mobile = ""; address = ""; principal = ""
                    message = "Saved ${girvi.girviNumber} with ${items.size} items"
                }.onFailure { message = it.message ?: "Girvi save failed" }
            }, enabled = drafts.isNotEmpty(), modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5146B8))) { Text("Multi-Item Girvi Save Karein") }
        }
    }
}

@Composable
private fun CustomSplitPayment(snapshot: AppSnapshot, catalog: MasterCatalog, save: (GirviRecord) -> Unit) {
    val active = snapshot.girvis.filter { it.status == "ACTIVE" }
    val modes = MasterCatalogOperations.active(catalog, MasterKind.PAYMENT_MODE)
    var girviId by rememberSaveable { mutableStateOf(active.firstOrNull()?.id) }
    var modeId by rememberSaveable { mutableStateOf(modes.firstOrNull()?.id) }
    var months by rememberSaveable { mutableStateOf("1") }
    var amount by rememberSaveable { mutableStateOf("") }
    var custom by rememberSaveable { mutableStateOf(false) }
    var principal by rememberSaveable { mutableStateOf("") }
    var interest by rememberSaveable { mutableStateOf("") }
    var charges by rememberSaveable { mutableStateOf("") }
    var interestFirst by rememberSaveable { mutableStateOf(true) }
    var note by rememberSaveable { mutableStateOf("") }
    var message by rememberSaveable { mutableStateOf<String?>(null) }
    val girvi = active.firstOrNull { it.id == girviId }
    val mode = modes.firstOrNull { it.id == modeId }
    val monthCount = months.toIntOrNull() ?: 0
    val view = girvi?.let { runCatching { GirviSettlementUseCase.settlementView(it, monthCount) }.getOrNull() }

    LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item { Text("Active Girvi", fontWeight = FontWeight.Bold) }
        itemsIndexed(active) { _, g -> TextButton(onClick = { girviId = g.id }) { Text(if (girviId == g.id) "✓ ${g.girviNumber} • ${g.customerName}" else "${g.girviNumber} • ${g.customerName}") } }
        item { OutlinedTextField(months, { months = it.filter(Char::isDigit).take(3) }, label = { Text("Settlement months") }, modifier = Modifier.fillMaxWidth()) }
        item { view?.let { Text("Due: Principal ${money(it.principalDuePaise)} • Interest ${money(it.interestDuePaise)} • Charges ${money(it.chargesDuePaise)} • Total ${money(it.totalDuePaise)}") } }
        item { Text("Payment Mode", fontWeight = FontWeight.Bold); MasterChoices(modes, modeId) { modeId = it } }
        item { OutlinedTextField(amount, { amount = decimal(it) }, label = { Text("Payment amount ₹") }, modifier = Modifier.fillMaxWidth()) }
        item { Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(custom, { custom = it }); Text("Exact custom split") } }
        if (custom) {
            item { OutlinedTextField(principal, { principal = decimal(it) }, label = { Text("Principal part ₹") }, modifier = Modifier.fillMaxWidth()) }
            item { OutlinedTextField(interest, { interest = decimal(it) }, label = { Text("Interest part ₹") }, modifier = Modifier.fillMaxWidth()) }
            item { OutlinedTextField(charges, { charges = decimal(it) }, label = { Text("Charges part ₹") }, modifier = Modifier.fillMaxWidth()) }
        } else {
            item { Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(interestFirst, { interestFirst = it }); Text(if (interestFirst) "Interest first" else "Principal first") } }
        }
        item { OutlinedTextField(note, { note = it }, label = { Text("Payment note") }, modifier = Modifier.fillMaxWidth()) }
        item { message?.let { Text(it, color = if (it.startsWith("Saved")) Color(0xFF138A4A) else MaterialTheme.colorScheme.error) } }
        item {
            Button(onClick = {
                runCatching {
                    val selected = requireNotNull(girvi) { "Active girvi select karein" }
                    val selectedMode = requireNotNull(mode) { "Payment mode select karein" }
                    require(monthCount in 0..1_200) { "Settlement months invalid" }
                    val entered = MoneyInput.rupeesToPaise(amount)
                    val customSplit = if (custom) {
                        val due = requireNotNull(view) { "Due calculate nahi hua" }
                        MasterWorkflowTransactions.validateCustomPayment(
                            CustomPaymentDraft(MoneyInput.rupeesToPaise(principal.ifBlank { "0" }), MoneyInput.rupeesToPaise(interest.ifBlank { "0" }), MoneyInput.rupeesToPaise(charges.ifBlank { "0" })),
                            AccountBalance(due.principalDuePaise, due.interestDuePaise, due.chargesDuePaise),
                            entered,
                        )
                    } else null
                    GirviSettlementUseCase.postPayment(
                        selected,
                        monthCount,
                        entered,
                        if (custom) PaymentAllocationMode.CUSTOM else if (interestFirst) PaymentAllocationMode.INTEREST_FIRST else PaymentAllocationMode.PRINCIPAL_FIRST,
                        selectedMode.name,
                        note,
                        snapshot.girvis.flatMap { it.payments }.map { it.receiptNumber },
                        customSplit,
                    )
                }.onSuccess { updated ->
                    save(updated)
                    amount = ""; principal = ""; interest = ""; charges = ""; note = ""
                    message = "Saved custom payment ${updated.payments.last().receiptNumber}"
                }.onFailure { message = it.message ?: "Payment save failed" }
            }, enabled = active.isNotEmpty() && modes.isNotEmpty(), modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF138A4A))) { Text("Payment Save Karein") }
        }
    }
}

@Composable
private fun MasterChoices(entries: List<MasterEntry>, selected: String?, select: (String) -> Unit) {
    if (entries.isEmpty()) { Text("Koi active option nahi", color = Color.Gray); return }
    entries.forEach { e -> TextButton(onClick = { select(e.id) }) { Text(if (selected == e.id) "✓ ${e.name}" else e.name) } }
}

@Composable
private fun TextChoices(values: List<String>, selected: String, select: (String) -> Unit) {
    values.forEach { value -> TextButton(onClick = { select(value) }) { Text(if (selected == value) "✓ $value" else value) } }
}

private fun decimal(value: String): String {
    val clean = value.filter { it.isDigit() || it == '.' }
    val dot = clean.indexOf('.')
    return if (dot < 0) clean.take(12) else clean.substring(0, dot + 1) + clean.substring(dot + 1).replace(".", "").take(2)
}

private fun money(paise: Long): String = String.format(Locale.US, "₹%.2f", paise / 100.0)
