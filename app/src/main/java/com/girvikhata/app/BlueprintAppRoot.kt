package com.girvikhata.app

import android.app.DatePickerDialog
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.girvikhata.app.custody.CustodyDisplayResolver
import com.girvikhata.app.custody.CustodyPlacementStore
import com.girvikhata.app.data.AppSnapshot
import com.girvikhata.app.data.BlueprintKhataRepository
import com.girvikhata.app.data.EncryptedRecordStore
import com.girvikhata.app.data.GirviRecord
import com.girvikhata.app.data.PaymentRecord
import com.girvikhata.app.domain.BlueprintLedgerEngine
import com.girvikhata.app.domain.GirviAdvanceMetadata
import com.girvikhata.app.domain.GirviInterestMetadata
import com.girvikhata.app.domain.InterestMode
import com.girvikhata.app.domain.InterestPeriodRule
import com.girvikhata.app.domain.InterestTerms
import com.girvikhata.app.security.BiometricAvailability
import com.girvikhata.app.security.PinVerificationResult
import com.girvikhata.app.security.SecurityPreferences
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DateFormat
import java.util.Calendar
import java.util.Date
import java.util.UUID
import kotlin.math.min

private val BlueprintNavy = Color(0xFF171752)
private val BlueprintPurple = Color(0xFF5146B8)
private val BlueprintGreen = Color(0xFF138A4A)
private val BlueprintBackground = Color(0xFFF6F7FB)
private val BlueprintDanger = Color(0xFFB3261E)

private enum class BlueprintSession { ENROLL_PIN, LOCKED, UNLOCKED }
private enum class BlueprintTab { HOME, CUSTOMERS, GIRVI, MORE }
private enum class AllocationChoice { INTEREST_FIRST, PRINCIPAL_FIRST, CUSTOM }

@Composable
fun BlueprintGirviKhataRoot(
    securityPreferences: SecurityPreferences,
    recordStore: EncryptedRecordStore,
    biometricAvailability: BiometricAvailability,
    lockSignal: Int,
    refreshSignal: Int = 0,
    requestBiometric: (() -> Unit, (String) -> Unit) -> Unit,
) {
    val repository = remember(recordStore) { BlueprintKhataRepository(recordStore) }
    var session by remember { mutableStateOf(if (securityPreferences.hasPin()) BlueprintSession.LOCKED else BlueprintSession.ENROLL_PIN) }
    var snapshot by remember { mutableStateOf(repository.snapshot()) }

    LaunchedEffect(lockSignal) { if (lockSignal > 0 && session == BlueprintSession.UNLOCKED) session = BlueprintSession.LOCKED }
    LaunchedEffect(refreshSignal) { if (refreshSignal > 0 && session == BlueprintSession.UNLOCKED) snapshot = repository.snapshot() }

    when (session) {
        BlueprintSession.ENROLL_PIN -> BlueprintPinEnrollment { pin ->
            runCatching { securityPreferences.savePin(pin.toCharArray()) }.onSuccess { snapshot = repository.snapshot(); session = BlueprintSession.UNLOCKED }
        }
        BlueprintSession.LOCKED -> BlueprintUnlock(
            biometricAvailability = biometricAvailability,
            verify = { securityPreferences.verify(it.toCharArray()) },
            biometric = requestBiometric,
            unlocked = { snapshot = repository.snapshot(); session = BlueprintSession.UNLOCKED },
        )
        BlueprintSession.UNLOCKED -> BlueprintMainShell(snapshot, repository, { snapshot = it }) { session = BlueprintSession.LOCKED }
    }
}

@Composable
private fun BlueprintPinEnrollment(save: (String) -> Unit) {
    var pin by rememberSaveable { mutableStateOf("") }
    var confirm by rememberSaveable { mutableStateOf("") }
    var message by rememberSaveable { mutableStateOf("Naya 6-digit owner PIN banayein") }
    BlueprintSecurePanel("Girvi Khata Setup", message) {
        BlueprintPinField("Naya PIN", pin) { pin = it.filter(Char::isDigit).take(6) }
        BlueprintPinField("PIN dobara", confirm) { confirm = it.filter(Char::isDigit).take(6) }
        Button(onClick = {
            when {
                pin.length != 6 -> message = "PIN 6 digits ka hona chahiye"
                pin != confirm -> message = "Dono PIN match nahi kar rahe"
                else -> save(pin)
            }
        }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = BlueprintPurple)) { Text("PIN Save Karein") }
    }
}

@Composable
private fun BlueprintUnlock(
    biometricAvailability: BiometricAvailability,
    verify: (String) -> PinVerificationResult,
    biometric: (() -> Unit, (String) -> Unit) -> Unit,
    unlocked: () -> Unit,
) {
    val context = LocalContext.current
    val biometricFirst = biometricAvailability == BiometricAvailability.AVAILABLE
    var usePin by rememberSaveable { mutableStateOf(!biometricFirst) }
    var pin by rememberSaveable { mutableStateOf("") }
    var message by rememberSaveable { mutableStateOf(if (biometricFirst) "Fingerprint se unlock karein" else "PIN se unlock karein") }
    BlueprintSecurePanel("Girvi Khata Unlock", message) {
        if (!usePin && biometricFirst) {
            Button(onClick = { biometric(unlocked) { message = it } }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = BlueprintPurple)) {
                Icon(Icons.Default.Fingerprint, null); Spacer(Modifier.size(8.dp)); Text("Fingerprint se Unlock")
            }
            TextButton(onClick = { usePin = true; message = "6-digit PIN daalein" }, modifier = Modifier.fillMaxWidth()) { Text("Use PIN instead") }
        } else {
            BlueprintPinField("6-digit PIN", pin) { pin = it.filter(Char::isDigit).take(6) }
            Button(onClick = {
                when (val result = verify(pin)) {
                    PinVerificationResult.Success -> unlocked()
                    PinVerificationResult.NotConfigured -> message = "PIN setup nahi mila"
                    is PinVerificationResult.Locked -> message = "Security lock active hai"
                    is PinVerificationResult.Failure -> message = "Galat PIN. Attempts: ${result.attempts}"
                }
                pin = ""
            }, enabled = pin.length == 6, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = BlueprintPurple)) { Text("PIN se Unlock") }
            if (biometricFirst) TextButton(onClick = { usePin = false; pin = ""; message = "Fingerprint se unlock karein" }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Fingerprint, null); Spacer(Modifier.size(6.dp)); Text("Use Fingerprint")
            }
            TextButton(onClick = { context.startActivity(Intent(context, PinRecoveryActivity::class.java)) }, modifier = Modifier.fillMaxWidth()) { Text("PIN Recovery / PIN भूल गए") }
        }
    }
}

@Composable
private fun BlueprintSecurePanel(title: String, subtitle: String, content: @Composable () -> Unit) {
    Column(Modifier.fillMaxSize().background(BlueprintBackground).padding(24.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Default.Lock, null, tint = BlueprintNavy); Spacer(Modifier.height(10.dp)); Text(title, fontSize = 27.sp, fontWeight = FontWeight.Bold, color = BlueprintNavy); Text(subtitle, color = Color.Gray); Spacer(Modifier.height(18.dp))
        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) { Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) { content() } }
    }
}

@Composable
private fun BlueprintPinField(label: String, value: String, changed: (String) -> Unit) = OutlinedTextField(
    value = value, onValueChange = changed, label = { Text(label) }, visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword), singleLine = true, modifier = Modifier.fillMaxWidth(),
)

@Composable
private fun BlueprintMainShell(snapshot: AppSnapshot, repository: BlueprintKhataRepository, updateSnapshot: (AppSnapshot) -> Unit, lock: () -> Unit) {
    val context = LocalContext.current
    var tab by rememberSaveable { mutableStateOf(BlueprintTab.HOME) }
    var selectedGirviId by rememberSaveable { mutableStateOf<String?>(null) }
    val launchEntry = { context.startActivity(Intent(context, PracticalEntryActivity::class.java)) }

    selectedGirviId?.let { id ->
        val girvi = snapshot.girvis.firstOrNull { it.id == id }
        if (girvi != null) {
            BlueprintGirviDetail(girvi, repository, updateSnapshot) { selectedGirviId = null }
            return
        }
        selectedGirviId = null
    }

    Scaffold(containerColor = BlueprintBackground, bottomBar = {
        NavigationBar {
            BlueprintNav(BlueprintTab.HOME, tab, "Home", Icons.Default.Home) { tab = it }
            BlueprintNav(BlueprintTab.CUSTOMERS, tab, "Customers", Icons.Default.Groups) { tab = it }
            BlueprintNav(BlueprintTab.GIRVI, tab, "Girvi", Icons.Default.Inventory2) { tab = it }
            BlueprintNav(BlueprintTab.MORE, tab, "More", Icons.Default.Settings) { tab = it }
        }
    }) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            when (tab) {
                BlueprintTab.HOME -> BlueprintDashboard(snapshot, launchEntry, { tab = BlueprintTab.CUSTOMERS }) { selectedGirviId = it }
                BlueprintTab.CUSTOMERS -> BlueprintCustomers(snapshot) { selectedGirviId = it }
                BlueprintTab.GIRVI -> BlueprintGirviList(snapshot, launchEntry) { selectedGirviId = it }
                BlueprintTab.MORE -> BlueprintMore(lock)
            }
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.BlueprintNav(tab: BlueprintTab, selected: BlueprintTab, label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, click: (BlueprintTab) -> Unit) = NavigationBarItem(
    selected = tab == selected, onClick = { click(tab) }, icon = { Icon(icon, null) }, label = { Text(label) },
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BlueprintPage(title: String, back: (() -> Unit)? = null, content: @Composable () -> Unit) {
    Scaffold(containerColor = BlueprintBackground, topBar = {
        TopAppBar(title = { Text(title, color = Color.White, fontWeight = FontWeight.Bold) }, navigationIcon = { if (back != null) TextButton(onClick = back) { Text("← वापस", color = Color.White) } }, colors = TopAppBarDefaults.topAppBarColors(containerColor = BlueprintNavy))
    }) { padding -> Column(Modifier.padding(padding).padding(14.dp).fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) { content() } }
}

@Composable
private fun BlueprintDashboard(snapshot: AppSnapshot, newGirvi: () -> Unit, customers: () -> Unit, openGirvi: (String) -> Unit) {
    val context = LocalContext.current
    val profile = remember { OwnerBusinessProfileStore(context).load() }
    val title = profile.businessName.ifBlank { "Girvi Khata" }
    BlueprintPage(title) {
        val today = blueprintStartOfToday()
        val active = snapshot.girvis.filter { it.status == "ACTIVE" }
        val totalOutstanding = active.sumOf { girvi -> runCatching { BlueprintLedgerEngine.project(girvi, today).totalDuePaise }.getOrDefault(girvi.principalPaise) }
        Text(if (profile.ownerName.isBlank()) "नमस्ते मालिक जी" else "नमस्ते ${profile.ownerName}", fontSize = 22.sp, fontWeight = FontWeight.Bold)
        if (profile.businessName.isNotBlank()) Text(profile.businessName, color = BlueprintPurple, fontWeight = FontWeight.Bold)
        Text(DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date()), color = Color.Gray)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            BlueprintActionCard("नया गिरवी", "Contact + photo", Modifier.weight(1f), newGirvi)
            BlueprintActionCard("Customers", "${snapshot.customers.size} saved", Modifier.weight(1f), customers)
        }
        BlueprintActionCard("📦 सामान कहाँ है?", "Locker • बाहर रखा • जगह बदलें", Modifier.fillMaxWidth()) {
            context.startActivity(Intent(context, CustodyPlacementActivity::class.java))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            BlueprintStat("Active Girvi", active.size.toString(), Modifier.weight(1f)); BlueprintStat("आज का कुल Due", blueprintMoney(totalOutstanding), Modifier.weight(1f))
        }
        Text("हाल की गिरवी", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(snapshot.girvis.sortedByDescending { it.createdAt }.take(5), key = { it.id }) { girvi ->
                BlueprintClickCard(girvi.girviNumber, "${girvi.customerName} • ${blueprintMoney(girvi.principalPaise)} • ${girvi.status}") { openGirvi(girvi.id) }
            }
        }
    }
}

@Composable
private fun BlueprintCustomers(snapshot: AppSnapshot, openGirvi: (String) -> Unit) = BlueprintPage("Customers") {
    var query by rememberSaveable { mutableStateOf("") }
    OutlinedTextField(value = query, onValueChange = { query = it }, label = { Text("नाम / Mobile / Item / Girvi search") }, leadingIcon = { Icon(Icons.Default.Search, null) }, modifier = Modifier.fillMaxWidth())
    val q = query.trim().lowercase()
    val customerRows = snapshot.customers.filter { customer ->
        q.isBlank() || customer.name.lowercase().contains(q) || customer.mobile.contains(q) || customer.address.lowercase().contains(q) || snapshot.girvis.filter { it.customerId == customer.id }.any { girvi -> girvi.girviNumber.lowercase().contains(q) || girvi.effectiveItems.any { it.itemName.lowercase().contains(q) } }
    }.sortedBy { it.name.lowercase() }
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(customerRows, key = { it.id }) { customer ->
            val girvis = snapshot.girvis.filter { it.customerId == customer.id }
            BlueprintClickCard(customer.name, "${customer.mobile.ifBlank { "No mobile" }} • Active ${girvis.count { it.status == "ACTIVE" }}") { girvis.maxByOrNull { it.createdAt }?.let { openGirvi(it.id) } }
        }
    }
}

@Composable
private fun BlueprintGirviList(snapshot: AppSnapshot, newGirvi: () -> Unit, openGirvi: (String) -> Unit) = BlueprintPage("Girvi Records") {
    Button(onClick = newGirvi, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = BlueprintPurple)) { Icon(Icons.Default.Add, null); Spacer(Modifier.size(8.dp)); Text("नया गिरवी / New Girvi") }
    var query by rememberSaveable { mutableStateOf("") }
    OutlinedTextField(query, { query = it }, label = { Text("Girvi, customer, mobile, item search") }, modifier = Modifier.fillMaxWidth())
    val q = query.trim().lowercase()
    val rows = snapshot.girvis.filter { girvi ->
        val mobile = snapshot.customers.firstOrNull { it.id == girvi.customerId }?.mobile.orEmpty()
        q.isBlank() || girvi.girviNumber.lowercase().contains(q) || girvi.customerName.lowercase().contains(q) || mobile.contains(q) || girvi.effectiveItems.any { it.itemName.lowercase().contains(q) || it.description.lowercase().contains(q) }
    }.sortedByDescending { it.createdAt }
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) { items(rows, key = { it.id }) { girvi -> BlueprintClickCard(girvi.girviNumber, "${girvi.customerName} • ${girvi.effectiveItems.joinToString { it.itemName }} • ${girvi.status}") { openGirvi(girvi.id) } } }
}

@Composable
private fun BlueprintGirviDetail(girvi: GirviRecord, repository: BlueprintKhataRepository, updateSnapshot: (AppSnapshot) -> Unit, back: () -> Unit) = BlueprintPage(girvi.girviNumber, back) {
    val context = LocalContext.current
    val custody = remember(girvi.id) { CustodyPlacementStore(context.applicationContext).load() }
    val custodySummary = remember(girvi, custody) { CustodyDisplayResolver.girviSummary(custody, girvi.effectiveItems.map { it.id }) }
    var settlementAt by rememberSaveable(girvi.id) { mutableStateOf(blueprintStartOfToday().coerceAtLeast(girvi.createdAt)) }
    var showAdvance by rememberSaveable { mutableStateOf(false) }
    var showPayment by rememberSaveable { mutableStateOf(false) }
    var showRelease by rememberSaveable { mutableStateOf(false) }
    var reversePayment by remember { mutableStateOf<PaymentRecord?>(null) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    val projection = remember(girvi, settlementAt) { runCatching { BlueprintLedgerEngine.project(girvi, settlementAt) }.getOrNull() }

    LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            BlueprintListCard("Customer", girvi.customerName)
            BlueprintListCard("Status", girvi.status)
            BlueprintListCard("सामान की जगह", custodySummary)
            OutlinedButton(onClick = { context.startActivity(Intent(context, CustodyPlacementActivity::class.java)) }, modifier = Modifier.fillMaxWidth()) { Text("📦 जगह बदलें / बाहर रखें") }
            OutlinedButton(onClick = { blueprintDatePicker(context, settlementAt) { picked -> if (picked in girvi.createdAt..blueprintEndOfToday()) settlementAt = picked } }, modifier = Modifier.fillMaxWidth()) { Text("हिसाब की तारीख / Settlement date: ${blueprintDate(settlementAt)}") }
        }
        projection?.let { p ->
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { BlueprintStat("कुल दिया", blueprintMoney(p.totalAdvancedPaise), Modifier.weight(1f)); BlueprintStat("Principal बाकी", blueprintMoney(p.principalOutstandingPaise), Modifier.weight(1f)) }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { BlueprintStat("Interest बना", blueprintMoney(p.grossInterestAccruedPaise), Modifier.weight(1f)); BlueprintStat("Interest बाकी", blueprintMoney(p.interestOutstandingPaise), Modifier.weight(1f)) }
                Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFFEDE7F6))) { Text("आज/चुनी तारीख का कुल हिसाब: ${blueprintMoney(p.totalDuePaise)}", Modifier.padding(14.dp), fontWeight = FontWeight.Bold, fontSize = 18.sp) }
            }
            item { Text("दो तरफ़ा Ledger", fontWeight = FontWeight.Bold, fontSize = 18.sp); Text("दुकानदार ने दिया  ↔  ग्राहक ने दिया", color = Color.Gray) }
            items(p.lines, key = { it.id }) { line ->
                val side = if (line.side == BlueprintLedgerEngine.Side.SHOPKEEPER_GAVE) "दिया / Gave" else "मिला / Received"
                BlueprintListCard("$side • ${blueprintDate(line.createdAt)}", "${line.type.replace('_', ' ')} • ${blueprintMoney(line.amountPaise)}${if (line.note.isBlank()) "" else " • ${line.note}"}")
            }
        }
        item { Text("सामान / Items", fontWeight = FontWeight.Bold, fontSize = 18.sp) }
        items(girvi.effectiveItems, key = { it.id }) { item ->
            val current = CustodyDisplayResolver.currentItem(custody, item.id)
            BlueprintListCard("${item.itemName} × ${item.quantity}", "${item.categoryName} • ${item.grossWeightGrams}${if (item.deductionWeightGrams.isBlank()) "" else " - ${item.deductionWeightGrams}"} • ${GirviInterestMetadata.strip(item.description)} • 📍 ${current.label}")
        }
        item {
            error?.let { Text(it, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold) }
            if (girvi.status == "ACTIVE" && projection != null) {
                Button(onClick = { showAdvance = true }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = BlueprintNavy)) { Icon(Icons.Default.AccountBalanceWallet, null); Spacer(Modifier.size(8.dp)); Text("और पैसे दें / Add More Amount") }
                Button(onClick = { showPayment = true }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = BlueprintPurple)) { Icon(Icons.Default.Payment, null); Spacer(Modifier.size(8.dp)); Text("भुगतान लें / Receive Payment") }
                OutlinedButton(onClick = { showRelease = true }, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.Calculate, null); Spacer(Modifier.size(8.dp)); Text("आज का हिसाब / Settlement & Release") }
            }
        }
        if (girvi.payments.isNotEmpty()) {
            item { Text("Payment History", fontWeight = FontWeight.Bold, fontSize = 18.sp) }
            items(girvi.payments.sortedByDescending { it.createdAt }, key = { it.id }) { payment ->
                val reversed = girvi.payments.any { it.isReversal && it.reversedPaymentId == payment.id }
                Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(payment.receiptNumber, fontWeight = FontWeight.Bold); Text("${blueprintDate(payment.createdAt)} • ${blueprintMoney(payment.amountPaise)}"); Text("Principal ${blueprintMoney(payment.principalPaise)} • Interest ${blueprintMoney(payment.interestPaise)}", color = Color.Gray)
                        if (reversed || payment.isReversal) Text("Reversed", color = BlueprintDanger, fontWeight = FontWeight.Bold) else if (girvi.status == "ACTIVE") TextButton(onClick = { reversePayment = payment }) { Text("गलती सुधारें / Reverse") }
                    }
                }
            }
        }
        item { Spacer(Modifier.height(30.dp)) }
    }

    if (showAdvance) BlueprintAdvanceDialog(girvi, { showAdvance = false }) { amount, date, terms, note ->
        runCatching { repository.addAdditionalAdvance(girvi.id, blueprintRupeesToPaise(amount), date, terms, note) }.onSuccess { updateSnapshot(it); showAdvance = false; error = null }.onFailure { error = it.message ?: "Additional advance save nahi hua" }
    }

    if (showPayment && projection != null) BlueprintPaymentDialog(girvi, settlementAt, { showPayment = false }) { amountText, date, allocation, customPrincipal, customInterest, note ->
        runCatching {
            val p = BlueprintLedgerEngine.project(girvi, date); val amount = blueprintRupeesToPaise(amountText); require(amount > 0) { "Payment amount required" }; require(amount <= p.totalDuePaise) { "Payment total due se zyada hai" }
            val split = blueprintAllocate(amount, p, allocation, customPrincipal, customInterest)
            repository.appendPayment(girvi.id, PaymentRecord(id = UUID.randomUUID().toString(), receiptNumber = blueprintReceiptNumber(), amountPaise = amount, principalPaise = split.first, interestPaise = split.second, chargesPaise = 0, mode = "CASH", note = note.trim(), createdAt = date))
        }.onSuccess { updateSnapshot(it); showPayment = false; error = null }.onFailure { error = it.message ?: "Payment save nahi hua" }
    }

    reversePayment?.let { original ->
        BlueprintReasonDialog("Payment Reverse", "Reason जरूरी है", { reversePayment = null }) { reason ->
            runCatching {
                require(reason.isNotBlank()) { "Reason required" }
                repository.reversePayment(girvi.id, original.id, PaymentRecord(id = UUID.randomUUID().toString(), receiptNumber = blueprintReceiptNumber("REV"), amountPaise = original.amountPaise, principalPaise = original.principalPaise, interestPaise = original.interestPaise, chargesPaise = original.chargesPaise, mode = original.mode, note = "Reversal: ${reason.trim()}", createdAt = blueprintStartOfToday(), isReversal = true, reversedPaymentId = original.id))
            }.onSuccess { updateSnapshot(it); reversePayment = null; error = null }.onFailure { error = it.message ?: "Reversal save nahi hua" }
        }
    }

    if (showRelease && projection != null) BlueprintReleaseDialog(projection.totalDuePaise, settlementAt, { showRelease = false }) { note, override ->
        runCatching {
            require(projection.totalDuePaise == 0L || override) { "Outstanding baki hai; owner override select karein" }; require(projection.totalDuePaise == 0L || note.isNotBlank()) { "Outstanding release par reason/note required" }; repository.releaseGirvi(girvi.id, settlementAt, note)
        }.onSuccess { updateSnapshot(it); showRelease = false; error = null }.onFailure { error = it.message ?: "Release nahi hua" }
    }
}

@Composable
private fun BlueprintAdvanceDialog(girvi: GirviRecord, onDismiss: () -> Unit, onSave: (String, Long, InterestTerms, String) -> Unit) {
    val context = LocalContext.current
    val originalTerms = GirviInterestMetadata.read(girvi.items.firstOrNull()?.description) ?: InterestTerms(monthlyRateBasisPoints = girvi.monthlyRateBasisPoints)
    var amount by rememberSaveable { mutableStateOf("") }; var date by rememberSaveable { mutableStateOf(blueprintStartOfToday().coerceAtLeast(girvi.createdAt)) }; var sameTerms by rememberSaveable { mutableStateOf(true) }; var mode by rememberSaveable { mutableStateOf(originalTerms.mode) }; var rate by rememberSaveable { mutableStateOf((originalTerms.monthlyRateBasisPoints / 100.0).toString()) }; var flat by rememberSaveable { mutableStateOf((originalTerms.flatMonthlyChargePaise / 100.0).toString()) }; var rule by rememberSaveable { mutableStateOf(originalTerms.periodRule) }; var compound by rememberSaveable { mutableStateOf(originalTerms.compoundEveryMonths ?: 0) }; var note by rememberSaveable { mutableStateOf("") }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("और पैसे दें / Add More Amount") }, text = {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item { OutlinedTextField(amount, { amount = blueprintDecimal(it) }, label = { Text("राशि ₹ / Amount") }) }
            item { OutlinedButton(onClick = { blueprintDatePicker(context, date) { picked -> if (picked in girvi.createdAt..blueprintEndOfToday()) date = picked } }) { Text("तारीख: ${blueprintDate(date)}") } }
            item { Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(sameTerms, { sameTerms = it }); Text("पुराना ब्याज नियम रखें / Same interest rule") } }
            if (!sameTerms) {
                item { Row { TextButton(onClick = { mode = InterestMode.PERCENT_PER_MONTH }) { Text(if (mode == InterestMode.PERCENT_PER_MONTH) "✓ %/Month" else "%/Month") }; TextButton(onClick = { mode = InterestMode.FLAT_PER_MONTH; compound = 0 }) { Text(if (mode == InterestMode.FLAT_PER_MONTH) "✓ Flat/Month" else "Flat/Month") } } }
                item { if (mode == InterestMode.PERCENT_PER_MONTH) OutlinedTextField(rate, { rate = blueprintDecimal(it) }, label = { Text("मासिक %") }) else OutlinedTextField(flat, { flat = blueprintDecimal(it) }, label = { Text("Flat ₹/month") }) }
                item { Column { InterestPeriodRule.entries.forEach { option -> TextButton(onClick = { rule = option }) { Text(if (rule == option) "✓ ${blueprintRuleLabel(option)}" else blueprintRuleLabel(option)) } } } }
                if (mode == InterestMode.PERCENT_PER_MONTH) item { Text("Compound interval (0 = simple)", fontWeight = FontWeight.Bold); Row { listOf(0, 1, 2, 3, 6, 12, 24, 36).forEach { value -> TextButton(onClick = { compound = value }) { Text(if (compound == value) "✓${if (value == 0) "Simple" else "${value}m"}" else if (value == 0) "Simple" else "${value}m", fontSize = 10.sp) } } } }
            }
            item { OutlinedTextField(note, { note = it }, label = { Text("Note") }) }
        }
    }, confirmButton = { TextButton(onClick = {
        val terms = if (sameTerms) originalTerms else InterestTerms(mode = mode, monthlyRateBasisPoints = if (mode == InterestMode.PERCENT_PER_MONTH) ((rate.toBigDecimalOrNull() ?: BigDecimal.ZERO) * BigDecimal(100)).setScale(0, RoundingMode.HALF_UP).toInt() else 0, flatMonthlyChargePaise = if (mode == InterestMode.FLAT_PER_MONTH) blueprintRupeesToPaise(flat) else 0, periodRule = rule, compoundEveryMonths = compound.takeIf { it > 0 })
        onSave(amount, date, terms, note)
    }) { Text("Confirm Advance") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}

@Composable
private fun BlueprintPaymentDialog(girvi: GirviRecord, defaultDate: Long, onDismiss: () -> Unit, onSave: (String, Long, AllocationChoice, String, String, String) -> Unit) {
    val context = LocalContext.current
    var amount by rememberSaveable { mutableStateOf("") }; var date by rememberSaveable { mutableStateOf(defaultDate) }; var allocation by rememberSaveable { mutableStateOf(AllocationChoice.INTEREST_FIRST) }; var principal by rememberSaveable { mutableStateOf("") }; var interest by rememberSaveable { mutableStateOf("") }; var note by rememberSaveable { mutableStateOf("") }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("भुगतान लें / Receive Payment") }, text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(amount, { amount = blueprintDecimal(it) }, label = { Text("Amount ₹") }); OutlinedButton(onClick = { blueprintDatePicker(context, date) { picked -> if (picked in girvi.createdAt..blueprintEndOfToday()) date = picked } }) { Text("तारीख: ${blueprintDate(date)}") }
        AllocationChoice.entries.forEach { choice -> TextButton(onClick = { allocation = choice }) { Text(if (allocation == choice) "✓ ${blueprintAllocationLabel(choice)}" else blueprintAllocationLabel(choice)) } }
        if (allocation == AllocationChoice.CUSTOM) { OutlinedTextField(principal, { principal = blueprintDecimal(it) }, label = { Text("Principal ₹") }); OutlinedTextField(interest, { interest = blueprintDecimal(it) }, label = { Text("Interest ₹") }) }
        OutlinedTextField(note, { note = it }, label = { Text("Note") })
    } }, confirmButton = { TextButton(onClick = { onSave(amount, date, allocation, principal, interest, note) }) { Text("Save Payment") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}

@Composable
private fun BlueprintReleaseDialog(due: Long, settlementAt: Long, onDismiss: () -> Unit, onConfirm: (String, Boolean) -> Unit) {
    var note by rememberSaveable { mutableStateOf("") }; var override by rememberSaveable { mutableStateOf(false) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Settlement / Release") }, text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("तारीख: ${blueprintDate(settlementAt)}"); Text("कुल बाकी: ${blueprintMoney(due)}", fontWeight = FontWeight.Bold, fontSize = 18.sp); if (due > 0) Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(override, { override = it }); Text("Owner override: बाकी के साथ release") }; OutlinedTextField(note, { note = it }, label = { Text("Release/override reason") })
    } }, confirmButton = { TextButton(onClick = { onConfirm(note, override) }) { Text("Confirm Release") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}

@Composable
private fun BlueprintReasonDialog(title: String, subtitle: String, dismiss: () -> Unit, save: (String) -> Unit) {
    var reason by rememberSaveable { mutableStateOf("") }
    AlertDialog(onDismissRequest = dismiss, title = { Text(title) }, text = { Column { Text(subtitle); OutlinedTextField(reason, { reason = it }, label = { Text("Reason") }) } }, confirmButton = { TextButton(onClick = { save(reason) }) { Text("Confirm") } }, dismissButton = { TextButton(onClick = dismiss) { Text("Cancel") } })
}

@Composable
private fun BlueprintMore(lock: () -> Unit) = BlueprintPage("More / Tools") {
    val context = LocalContext.current
    BlueprintToolButton(Icons.Default.Backup, "Encrypted Backup", "Business + masters + encrypted photos") { context.startActivity(Intent(context, BackupActivity::class.java)) }
    BlueprintToolButton(Icons.Default.Restore, "Verified Restore", "Preview + rollback-safe restore") { context.startActivity(Intent(context, RestoreActivity::class.java)) }
    BlueprintToolButton(Icons.Default.Calculate, "Reports", "Khata + सामान की reports") { context.startActivity(Intent(context, ReportsActivity::class.java)) }
    BlueprintToolButton(Icons.Default.Inventory2, "सामान की जगह", "Locker • बाहर रखा • जगह बदलें • private हिसाब") { context.startActivity(Intent(context, CustodyPlacementActivity::class.java)) }
    BlueprintToolButton(Icons.Default.Settings, "Owner Settings", "Security and owner controls") { context.startActivity(Intent(context, OwnerSettingsActivity::class.java)) }
    BlueprintToolButton(Icons.Default.Inventory2, "Masters", "Items, units, plans and modes") { context.startActivity(Intent(context, MasterCatalogActivity::class.java)) }
    OutlinedButton(onClick = lock, modifier = Modifier.fillMaxWidth()) { Text("App Lock Karein") }
}

@Composable
private fun BlueprintToolButton(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String, click: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable(onClick = click), colors = CardDefaults.cardColors(containerColor = Color.White)) { Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) { Icon(icon, null, tint = BlueprintPurple); Spacer(Modifier.size(12.dp)); Column { Text(title, fontWeight = FontWeight.Bold); Text(subtitle, color = Color.Gray, fontSize = 12.sp) } } }
}

@Composable
private fun BlueprintActionCard(title: String, subtitle: String, modifier: Modifier, click: () -> Unit) {
    Card(modifier.clickable(onClick = click), colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(16.dp)) { Column(Modifier.padding(14.dp)) { Text(title, fontWeight = FontWeight.Bold); Text(subtitle, color = Color.Gray, fontSize = 12.sp) } }
}

@Composable
private fun BlueprintStat(title: String, value: String, modifier: Modifier) { Card(modifier, colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(16.dp)) { Column(Modifier.padding(14.dp)) { Text(title, color = Color.Gray, fontSize = 12.sp); Text(value, fontWeight = FontWeight.Bold, fontSize = 18.sp) } } }
@Composable
private fun BlueprintClickCard(title: String, subtitle: String, click: () -> Unit) { Card(Modifier.fillMaxWidth().clickable(onClick = click), colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(14.dp)) { Column(Modifier.padding(14.dp)) { Text(title, fontWeight = FontWeight.Bold); Text(subtitle, color = Color.Gray) } } }
@Composable
private fun BlueprintListCard(title: String, value: String) { Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(14.dp)) { Column(Modifier.padding(12.dp)) { Text(title, fontWeight = FontWeight.Bold); Text(value, color = Color.Gray) } } }

private fun blueprintAllocate(amount: Long, projection: BlueprintLedgerEngine.SettlementProjection, choice: AllocationChoice, customPrincipal: String, customInterest: String): Pair<Long, Long> = when (choice) {
    AllocationChoice.INTEREST_FIRST -> { val interest = min(amount, projection.interestOutstandingPaise); val principal = amount - interest; require(principal <= projection.principalOutstandingPaise) { "Principal allocation outstanding se zyada hai" }; principal to interest }
    AllocationChoice.PRINCIPAL_FIRST -> { val principal = min(amount, projection.principalOutstandingPaise); val interest = amount - principal; require(interest <= projection.interestOutstandingPaise) { "Interest allocation outstanding se zyada hai" }; principal to interest }
    AllocationChoice.CUSTOM -> { val principal = blueprintRupeesToPaise(customPrincipal); val interest = blueprintRupeesToPaise(customInterest); require(principal + interest == amount) { "Custom principal + interest payment amount ke barabar hona chahiye" }; require(principal <= projection.principalOutstandingPaise) { "Principal split outstanding se zyada hai" }; require(interest <= projection.interestOutstandingPaise) { "Interest split outstanding se zyada hai" }; principal to interest }
}
private fun blueprintReceiptNumber(prefix: String = "R"): String = "$prefix-${System.currentTimeMillis()}-${UUID.randomUUID().toString().take(4)}"
private fun blueprintRupeesToPaise(value: String): Long { val decimal = value.trim().toBigDecimalOrNull() ?: error("Valid amount required"); require(decimal > BigDecimal.ZERO) { "Amount positive hona chahiye" }; return decimal.multiply(BigDecimal(100)).setScale(0, RoundingMode.HALF_UP).longValueExact() }
private fun blueprintMoney(paise: Long): String = "₹" + BigDecimal.valueOf(paise, 2).setScale(2).toPlainString()
private fun blueprintDecimal(value: String): String { var dot = false; return buildString { value.forEach { c -> if (c.isDigit()) append(c) else if (c == '.' && !dot) { append(c); dot = true } } }.take(14) }
private fun blueprintDate(value: Long): String = DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(value))
private fun blueprintStartOfToday(): Long = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, 12); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }.timeInMillis
private fun blueprintEndOfToday(): Long = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59); set(Calendar.SECOND, 59); set(Calendar.MILLISECOND, 999) }.timeInMillis
private fun blueprintDatePicker(context: android.content.Context, current: Long, selected: (Long) -> Unit) { val cal = Calendar.getInstance().apply { timeInMillis = current }; DatePickerDialog(context, { _, year, month, day -> selected(Calendar.getInstance().apply { clear(); set(year, month, day, 12, 0, 0) }.timeInMillis) }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show() }
private fun blueprintRuleLabel(rule: InterestPeriodRule): String = when (rule) { InterestPeriodRule.EXACT_DAYS -> "Exact days"; InterestPeriodRule.FULL_MONTH_STARTED -> "Started month = full"; InterestPeriodRule.COMPLETED_MONTHS_PLUS_DAYS -> "Months + remaining days" }
private fun blueprintAllocationLabel(choice: AllocationChoice): String = when (choice) { AllocationChoice.INTEREST_FIRST -> "Interest first"; AllocationChoice.PRINCIPAL_FIRST -> "Principal first"; AllocationChoice.CUSTOM -> "Custom split" }
