package com.girvikhata.app

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Payment
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
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.girvikhata.app.data.AppSnapshot
import com.girvikhata.app.data.CategoryRecord
import com.girvikhata.app.data.ClassicVerifiedWriteGateway
import com.girvikhata.app.data.CustomerRecord
import com.girvikhata.app.data.EncryptedRecordStore
import com.girvikhata.app.data.GirviItemRecord
import com.girvikhata.app.data.GirviRecord
import com.girvikhata.app.data.VerifiedBusinessWriteCoordinator
import com.girvikhata.app.domain.CategoryRules
import com.girvikhata.app.domain.CustomerCandidate
import com.girvikhata.app.domain.CustomerMatcher
import com.girvikhata.app.domain.GirviSequence
import com.girvikhata.app.domain.GirviSettlementUseCase
import com.girvikhata.app.domain.MoneyInput
import com.girvikhata.app.domain.PaymentAllocationMode
import com.girvikhata.app.domain.PaymentSplit
import com.girvikhata.app.domain.SimpleInterestPreview
import com.girvikhata.app.security.BiometricAvailability
import com.girvikhata.app.security.PinVerificationResult
import com.girvikhata.app.security.SecurityPreferences
import java.math.BigDecimal
import java.text.DateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToLong

private val Navy = Color(0xFF171752)
private val Purple = Color(0xFF5146B8)
private val Green = Color(0xFF138A4A)
private val Background = Color(0xFFF6F7FB)

enum class SessionState { ENROLL_PIN, LOCKED, UNLOCKED }
private enum class Tab { HOME, CUSTOMERS, GIRVI, MASTERS, MORE }

@Composable
fun GirviKhataRoot(
    securityPreferences: SecurityPreferences,
    recordStore: EncryptedRecordStore,
    biometricAvailability: BiometricAvailability,
    lockSignal: Int,
    refreshSignal: Int = 0,
    requestBiometric: (() -> Unit, (String) -> Unit) -> Unit,
) {
    val applicationContext = LocalContext.current.applicationContext
    val verifiedGateway = remember(recordStore, applicationContext) {
        ClassicVerifiedWriteGateway(
            records = recordStore,
            coordinator = VerifiedBusinessWriteCoordinator(applicationContext, records = recordStore),
        )
    }
    var session by remember { mutableStateOf(if (securityPreferences.hasPin()) SessionState.LOCKED else SessionState.ENROLL_PIN) }
    var snapshot by remember { mutableStateOf(recordStore.load()) }

    LaunchedEffect(lockSignal) {
        if (lockSignal > 0 && session == SessionState.UNLOCKED) session = SessionState.LOCKED
    }
    LaunchedEffect(refreshSignal) {
        if (refreshSignal > 0 && session == SessionState.UNLOCKED) snapshot = recordStore.load()
    }

    fun persist(next: AppSnapshot) {
        snapshot = verifiedGateway.persist(snapshot, next)
    }

    when (session) {
        SessionState.ENROLL_PIN -> PinEnrollmentScreen { pin ->
            runCatching { securityPreferences.savePin(pin.toCharArray()) }
                .onSuccess { session = SessionState.UNLOCKED }
        }
        SessionState.LOCKED -> PinUnlockScreen(
            biometricAvailability = biometricAvailability,
            onVerify = { securityPreferences.verify(it.toCharArray()) },
            onBiometric = requestBiometric,
            onUnlocked = {
                snapshot = recordStore.load()
                session = SessionState.UNLOCKED
            },
        )
        SessionState.UNLOCKED -> MainShell(snapshot, ::persist) { session = SessionState.LOCKED }
    }
}

@Composable
private fun PinEnrollmentScreen(onSave: (String) -> Unit) {
    var pin by rememberSaveable { mutableStateOf("") }
    var confirm by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    SecurePanel("Apna 6-digit PIN Banaye", "Raw PIN kabhi save nahi hoga") {
        SecurePinField("Naya PIN", pin) { pin = it.digitsOnly() }
        SecurePinField("PIN Dobara", confirm) { confirm = it.digitsOnly() }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Button(
            onClick = { error = validatePin(pin, confirm); if (error == null) onSave(pin) },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Purple),
        ) { Text("PIN Secure Karke Aage Badhein") }
    }
}

@Composable
private fun PinUnlockScreen(
    biometricAvailability: BiometricAvailability,
    onVerify: (String) -> PinVerificationResult,
    onBiometric: (() -> Unit, (String) -> Unit) -> Unit,
    onUnlocked: () -> Unit,
) {
    val context = LocalContext.current
    var pin by rememberSaveable { mutableStateOf("") }
    var message by rememberSaveable { mutableStateOf("PIN daalein") }
    SecurePanel("Girvi Khata Unlock Kare", message) {
        SecurePinField("6-digit PIN", pin) { pin = it.digitsOnly() }
        Button(
            onClick = {
                when (val result = onVerify(pin)) {
                    PinVerificationResult.Success -> onUnlocked()
                    PinVerificationResult.NotConfigured -> message = "PIN setup nahi mila"
                    is PinVerificationResult.Locked -> message = "Security lock active hai. Baad mein try karein."
                    is PinVerificationResult.Failure -> message = "Galat PIN. Attempts: ${result.attempts}"
                }
                pin = ""
            },
            enabled = pin.length == 6,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Purple),
        ) { Text("PIN se Unlock") }
        if (biometricAvailability == BiometricAvailability.AVAILABLE) {
            OutlinedButton(onClick = { onBiometric(onUnlocked) { message = it } }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Fingerprint, null)
                Spacer(Modifier.size(8.dp))
                Text("Fingerprint se Unlock")
            }
        } else Text(biometricMessage(biometricAvailability), color = Color.Gray, fontSize = 12.sp)
        TextButton(
            onClick = { context.startActivity(Intent(context, PinRecoveryActivity::class.java)) },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("PIN Recovery / PIN भूल गए") }
    }
}

@Composable
private fun MainShell(snapshot: AppSnapshot, onSnapshotChange: (AppSnapshot) -> Unit, onLock: () -> Unit) {
    var tab by rememberSaveable { mutableStateOf(Tab.HOME) }
    var newGirvi by rememberSaveable { mutableStateOf(false) }
    var selectedGirviId by rememberSaveable { mutableStateOf<String?>(null) }

    selectedGirviId?.let { id ->
        snapshot.girvis.firstOrNull { it.id == id }?.let { girvi ->
            GirviDetailScreen(
                girvi = girvi,
                allReceiptNumbers = snapshot.girvis.flatMap { it.payments }.map { it.receiptNumber },
                onBack = { selectedGirviId = null },
                onUpdate = { updated ->
                    onSnapshotChange(snapshot.copy(girvis = snapshot.girvis.map { if (it.id == updated.id) updated else it }))
                },
            )
            return
        }
        selectedGirviId = null
    }

    if (newGirvi) {
        NewGirviScreen(snapshot, { newGirvi = false }) { _, _ -> }
        return
    }

    Scaffold(
        containerColor = Background,
        bottomBar = {
            NavigationBar {
                NavItem(Tab.HOME, tab, "Home", Icons.Default.Home) { tab = it }
                NavItem(Tab.CUSTOMERS, tab, "Customers", Icons.Default.Groups) { tab = it }
                NavItem(Tab.GIRVI, tab, "Girvi", Icons.Default.Inventory2) { tab = it }
                NavItem(Tab.MASTERS, tab, "Masters", Icons.Default.Category) { tab = it }
                NavItem(Tab.MORE, tab, "More", Icons.Default.Settings) { tab = it }
            }
        },
    ) { padding ->
        Box(Modifier.padding(padding)) {
            when (tab) {
                Tab.HOME -> Dashboard(snapshot, { tab = Tab.CUSTOMERS }) { newGirvi = true }
                Tab.CUSTOMERS -> Customers(snapshot) { selectedGirviId = it }
                Tab.GIRVI -> GirviList(snapshot, { newGirvi = true }) { selectedGirviId = it }
                Tab.MASTERS -> Masters(snapshot, onSnapshotChange)
                Tab.MORE -> More(snapshot, onLock)
            }
        }
    }
}

@Composable
private fun RowScope.NavItem(tab: Tab, selected: Tab, label: String, icon: ImageVector, onClick: (Tab) -> Unit) =
    NavigationBarItem(selected = tab == selected, onClick = { onClick(tab) }, icon = { Icon(icon, null) }, label = { Text(label) })

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Page(title: String, onBack: (() -> Unit)? = null, content: @Composable ColumnScope.() -> Unit) {
    Scaffold(
        containerColor = Background,
        topBar = {
            TopAppBar(
                navigationIcon = { if (onBack != null) IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null, tint = Color.White) } },
                title = { Text(title, color = Color.White, fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Navy),
            )
        },
    ) { padding ->
        Column(Modifier.padding(padding).padding(16.dp).fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
    }
}

@Composable
private fun Dashboard(snapshot: AppSnapshot, openCustomers: () -> Unit, newGirvi: () -> Unit) = Page("Dashboard") {
    val active = snapshot.girvis.filter { it.status == "ACTIVE" }
    val received = snapshot.girvis.flatMap { it.payments }.filterNot { it.isReversal }.sumOf { it.amountPaise }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Column { Text("Namaste, Malik Ji", fontSize = 20.sp, fontWeight = FontWeight.Bold); Text(DateFormat.getDateInstance().format(Date()), color = Color.Gray) }
        Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.CloudDone, null, tint = Green); Text(" Encrypted local", color = Green) }
    }
    SearchCard("Customer, mobile ya girvi number khoje", openCustomers)
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        ActionCard("Naya Girvi", "Contact + photo entry", Modifier.weight(1f), newGirvi)
        ActionCard("Customers", "${snapshot.customers.size} saved", Modifier.weight(1f), openCustomers)
    }
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Stat("Active Girvi", active.size.toString(), Modifier.weight(1f))
        Stat("Principal", money(active.sumOf { it.principalPaise }), Modifier.weight(1f))
    }
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Stat("Payments", money(received), Modifier.weight(1f))
        Stat("Released", snapshot.girvis.count { it.status == "RELEASED" }.toString(), Modifier.weight(1f))
    }
    Banner("Payments immutable ledger mein save hote hain; galti reversal se correct hogi")
}

@Composable
private fun Customers(snapshot: AppSnapshot, openGirvi: (String) -> Unit) = Page("Customers") {
    var query by rememberSaveable { mutableStateOf("") }
    OutlinedTextField(query, { query = it }, label = { Text("Naam, mobile ya village search") }, leadingIcon = { Icon(Icons.Default.Search, null) }, modifier = Modifier.fillMaxWidth())
    val filtered = CustomerMatcher.search(snapshot.customers.map { CustomerCandidate(it.id, it.name, it.mobile, it.address) }, query)
    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items(filtered, key = { it.id }) { customer ->
            val girvis = snapshot.girvis.filter { it.customerId == customer.id }
            ClickCard(customer.name, "${customer.mobile.ifBlank { "No mobile" }} • Active ${girvis.count { it.status == "ACTIVE" }}") {
                girvis.maxByOrNull { it.createdAt }?.let { openGirvi(it.id) }
            }
        }
    }
}

@Composable
private fun GirviList(snapshot: AppSnapshot, newGirvi: () -> Unit, openGirvi: (String) -> Unit) = Page("Girvi Records") {
    Button(onClick = newGirvi, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Purple)) {
        Icon(Icons.Default.Add, null); Spacer(Modifier.size(8.dp)); Text("Naya Girvi")
    }
    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items(snapshot.girvis.sortedByDescending { it.createdAt }, key = { it.id }) { girvi ->
            ClickCard(
                girvi.girviNumber,
                "${girvi.customerName} • ${girvi.effectiveItems.size} item type • ${money(girvi.principalPaise)} • ${girvi.status}",
            ) { openGirvi(girvi.id) }
        }
    }
}

@Composable
private fun GirviDetailScreen(
    girvi: GirviRecord,
    allReceiptNumbers: Collection<String>,
    onBack: () -> Unit,
    onUpdate: (GirviRecord) -> Unit,
) = Page(girvi.girviNumber, onBack) {
    var months by rememberSaveable { mutableStateOf("1") }
    var showPayment by rememberSaveable { mutableStateOf(false) }
    var reverseId by rememberSaveable { mutableStateOf<String?>(null) }
    var showRelease by rememberSaveable { mutableStateOf(false) }
    var message by rememberSaveable { mutableStateOf<String?>(null) }
    val monthCount = months.toIntOrNull()?.coerceIn(0, 120) ?: 0
    val settlement = remember(girvi, monthCount) { GirviSettlementUseCase.settlementView(girvi, monthCount) }

    LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            ListCard("Customer", girvi.customerName)
            StatusCard("Status", girvi.status, if (girvi.status == "ACTIVE") Green else Color.Gray)
            OutlinedTextField(months, { months = it.digitsOnly(3) }, label = { Text("Settlement months") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Stat("Principal Due", money(settlement.principalDuePaise), Modifier.weight(1f))
                Stat("Interest Due", money(settlement.interestDuePaise), Modifier.weight(1f))
            }
            Banner("Total outstanding: ${money(settlement.totalDuePaise)}")
        }
        item { Text("Girvi Items", fontWeight = FontWeight.Bold, fontSize = 18.sp) }
        items(girvi.effectiveItems, key = { it.id }) { item ->
            val gross = item.grossWeightGrams.toBigDecimalOrNull()
            val less = item.deductionWeightGrams.toBigDecimalOrNull() ?: BigDecimal.ZERO
            ListCard("${item.itemName} × ${item.quantity}", "${item.categoryName} • Net ${gross?.subtract(less)?.stripTrailingZeros()?.toPlainString() ?: "-"}g • ${item.description}")
        }
        item {
            Text("Payment History", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            if (girvi.payments.isEmpty()) Banner("Abhi payment entry nahi hai")
        }
        items(girvi.payments.sortedByDescending { it.createdAt }, key = { it.id }) { payment ->
            val reversed = girvi.payments.any { it.isReversal && it.reversedPaymentId == payment.id }
            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(14.dp)) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(payment.receiptNumber, fontWeight = FontWeight.Bold)
                    Text("${if (payment.isReversal) "REVERSAL" else payment.mode} • ${money(payment.amountPaise)}", color = if (payment.isReversal || reversed) MaterialTheme.colorScheme.error else Green)
                    Text("Principal ${money(payment.principalPaise)} • Interest ${money(payment.interestPaise)}", color = Color.Gray)
                    if (payment.note.isNotBlank()) Text(payment.note, color = Color.Gray)
                    if (!payment.isReversal && !reversed && girvi.status == "ACTIVE") TextButton(onClick = { reverseId = payment.id }) { Text("Reverse Payment") }
                    if (reversed) Text("Reversed", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            }
        }
        item {
            message?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            if (girvi.status == "ACTIVE") {
                Button(onClick = { showPayment = true }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Purple)) {
                    Icon(Icons.Default.Payment, null); Spacer(Modifier.size(8.dp)); Text("Payment Receive Karein")
                }
                OutlinedButton(onClick = { showRelease = true }, modifier = Modifier.fillMaxWidth()) { Text("Girvi Release / Close") }
            } else ListCard("Release Note", girvi.releaseNote.ifBlank { "Released" })
        }
    }

    if (showPayment) PaymentDialog(
        balancePrincipal = settlement.principalDuePaise,
        balanceInterest = settlement.interestDuePaise,
        onDismiss = { showPayment = false },
        onConfirm = { amount, mode, paymentMode, note, custom ->
            runCatching {
                GirviSettlementUseCase.postPayment(girvi, monthCount, MoneyInput.rupeesToPaise(amount), mode, paymentMode, note, allReceiptNumbers, custom)
            }.onSuccess { onUpdate(it); showPayment = false; message = null }
                .onFailure { message = it.message ?: "Payment save nahi hua" }
        },
    )

    reverseId?.let { id ->
        TextInputDialog("Payment Reverse", "Reason likhein", { reverseId = null }) { reason ->
            runCatching { GirviSettlementUseCase.reversePayment(girvi, id, reason, allReceiptNumbers) }
                .onSuccess { onUpdate(it); reverseId = null; message = null }
                .onFailure { message = it.message ?: "Reversal failed" }
        }
    }

    if (showRelease) ReleaseDialog(
        outstanding = settlement.totalDuePaise,
        onDismiss = { showRelease = false },
        onConfirm = { note, override ->
            runCatching { GirviSettlementUseCase.release(girvi, monthCount, note, override) }
                .onSuccess { onUpdate(it); showRelease = false; message = null }
                .onFailure { message = it.message ?: "Release blocked" }
        },
    )
}

@Composable
private fun PaymentDialog(
    balancePrincipal: Long,
    balanceInterest: Long,
    onDismiss: () -> Unit,
    onConfirm: (String, PaymentAllocationMode, String, String, PaymentSplit?) -> Unit,
) {
    var amount by rememberSaveable { mutableStateOf("") }
    var allocation by rememberSaveable { mutableStateOf(PaymentAllocationMode.INTEREST_FIRST) }
    var paymentMode by rememberSaveable { mutableStateOf("CASH") }
    var note by rememberSaveable { mutableStateOf("") }
    var principal by rememberSaveable { mutableStateOf("") }
    var interest by rememberSaveable { mutableStateOf("") }
    var charges by rememberSaveable { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Payment Receive Karein") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Due: Principal ${money(balancePrincipal)} • Interest ${money(balanceInterest)}")
                OutlinedTextField(amount, { amount = it.decimalOnly() }, label = { Text("Amount ₹") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                Row { PaymentAllocationMode.entries.forEach { mode -> TextButton(onClick = { allocation = mode }) { Text(if (allocation == mode) "✓ ${mode.label()}" else mode.label(), fontSize = 11.sp) } } }
                Row { listOf("CASH", "UPI", "BANK").forEach { mode -> TextButton(onClick = { paymentMode = mode }) { Text(if (paymentMode == mode) "✓ $mode" else mode) } } }
                if (allocation == PaymentAllocationMode.CUSTOM) {
                    OutlinedTextField(principal, { principal = it.decimalOnly() }, label = { Text("Principal ₹") })
                    OutlinedTextField(interest, { interest = it.decimalOnly() }, label = { Text("Interest ₹") })
                    OutlinedTextField(charges, { charges = it.decimalOnly() }, label = { Text("Charges ₹") })
                }
                OutlinedTextField(note, { note = it }, label = { Text("Note") })
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val custom = if (allocation == PaymentAllocationMode.CUSTOM) PaymentSplit(
                    principalPaise = principal.moneyOrZero(),
                    interestPaise = interest.moneyOrZero(),
                    chargesPaise = charges.moneyOrZero(),
                ) else null
                onConfirm(amount, allocation, paymentMode, note, custom)
            }) { Text("Save Payment") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun ReleaseDialog(outstanding: Long, onDismiss: () -> Unit, onConfirm: (String, Boolean) -> Unit) {
    var note by rememberSaveable { mutableStateOf("") }
    var override by rememberSaveable { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Girvi Release Karein") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Outstanding: ${money(outstanding)}")
                if (outstanding > 0) Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = override, onCheckedChange = { override = it })
                    Text("Owner override se outstanding ke saath release")
                }
                OutlinedTextField(note, { note = it }, label = { Text("Release note / kis ko saman diya") })
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(note, override) }) { Text("Confirm Release") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun Masters(snapshot: AppSnapshot, update: (AppSnapshot) -> Unit) = Page("Categories") {
    var add by rememberSaveable { mutableStateOf(false) }
    var warning by rememberSaveable { mutableStateOf<String?>(null) }
    Button(onClick = { add = true }, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.Add, null); Text(" Nayi Category") }
    warning?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items(snapshot.categories, key = { it.id }) { category ->
            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) { Text(category.name, fontWeight = FontWeight.Bold); Text(if (category.active) "Active" else "Inactive") }
                    TextButton(onClick = {
                        val activeNames = snapshot.girvis.filter { it.status == "ACTIVE" }.flatMap { it.effectiveItems }.map { it.categoryName }
                        if (category.active && !CategoryRules.canDeactivate(category.name, activeNames)) warning = "Active girvi mein category use ho rahi hai"
                        else { update(snapshot.copy(categories = snapshot.categories.map { if (it.id == category.id) it.copy(active = !it.active) else it })); warning = null }
                    }) { Text(if (category.active) "Deactivate" else "Activate") }
                }
            }
        }
    }
    if (add) TextInputDialog("Nayi Category", "Category name", { add = false }) { name ->
        val clean = name.trim()
        if (clean.isNotEmpty() && snapshot.categories.none { it.name.equals(clean, true) }) update(snapshot.copy(categories = snapshot.categories + CategoryRecord(name = clean)))
        add = false
    }
}

@Composable
private fun More(snapshot: AppSnapshot, lock: () -> Unit) = Page("Security & Settings") {
    ListCard("Encrypted Local Store", "${snapshot.customers.size} customers • ${snapshot.girvis.size} girvi • schema v${snapshot.schemaVersion}")
    ListCard("Payment Ledger", "${snapshot.girvis.sumOf { it.payments.size }} immutable entries")
    ListCard("Automatic Lock", "30 seconds background ke baad lock")
    ListCard("Backup", "Google Drive encrypted backup integration pending")
    Button(onClick = lock, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Navy)) { Icon(Icons.Default.Lock, null); Text(" App Lock Karein") }
}

private data class ItemDraft(val category: String = "", val name: String = "", val quantity: String = "1", val gross: String = "", val deduction: String = "", val description: String = "")

@Composable
private fun NewGirviScreen(snapshot: AppSnapshot, cancel: () -> Unit, save: (CustomerRecord, GirviRecord) -> Unit) {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        context.startActivity(Intent(context, PracticalEntryActivity::class.java))
        cancel()
    }
    Page("Naya Girvi", cancel) {
        Text("नया contact + photo entry form खोला जा रहा है… / Opening unified practical entry…")
    }
}

@Composable
private fun ItemEditor(index: Int, draft: ItemDraft, categories: List<String>, removable: Boolean, change: (ItemDraft) -> Unit, remove: () -> Unit) {
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) { Text("Item ${index + 1}", Modifier.weight(1f), fontWeight = FontWeight.Bold); if (removable) IconButton(onClick = remove) { Icon(Icons.Default.Delete, null) } }
            Row { categories.take(4).forEach { category -> TextButton(onClick = { change(draft.copy(category = category)) }) { Text(if (draft.category == category) "✓$category" else category, fontSize = 11.sp) } } }
            OutlinedTextField(draft.name, { change(draft.copy(name = it)) }, label = { Text("Item name") }, modifier = Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedTextField(draft.quantity, { change(draft.copy(quantity = it.digitsOnly(3))) }, label = { Text("Qty") }, modifier = Modifier.weight(1f))
                OutlinedTextField(draft.gross, { change(draft.copy(gross = it.decimalOnly())) }, label = { Text("Gross g") }, modifier = Modifier.weight(1f))
                OutlinedTextField(draft.deduction, { change(draft.copy(deduction = it.decimalOnly())) }, label = { Text("Less g") }, modifier = Modifier.weight(1f))
            }
            OutlinedTextField(draft.description, { change(draft.copy(description = it)) }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth())
        }
    }
}

private fun validateItem(item: ItemDraft): String? = when {
    item.category.isBlank() -> "Category required"
    item.name.isBlank() -> "Item name required"
    item.quantity.toIntOrNull()?.let { it <= 0 } != false -> "Quantity invalid"
    item.gross.toBigDecimalOrNull()?.let { gross -> (item.deduction.toBigDecimalOrNull() ?: BigDecimal.ZERO) > gross } == true -> "Deduction gross se zyada hai"
    else -> null
}

@Composable
private fun SecurePanel(title: String, subtitle: String, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxSize().background(Navy).padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(60.dp)); Icon(Icons.Default.Lock, null, tint = Color(0xFFFFC54D), modifier = Modifier.size(68.dp)); Text("Girvi Khata", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold); Text("Aapki digital tijori", color = Color.White.copy(alpha = .75f)); Spacer(Modifier.height(30.dp))
        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(24.dp)) { Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) { Text(title, fontSize = 21.sp, fontWeight = FontWeight.Bold); Text(subtitle, color = Color.Gray); content() } }
    }
}

@Composable
private fun SecurePinField(label: String, value: String, change: (String) -> Unit) = OutlinedTextField(value, change, label = { Text(label) }, singleLine = true, visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword), modifier = Modifier.fillMaxWidth())

@Composable
private fun TextInputDialog(title: String, label: String, dismiss: () -> Unit, confirm: (String) -> Unit) {
    var value by rememberSaveable { mutableStateOf("") }
    AlertDialog(onDismissRequest = dismiss, title = { Text(title) }, text = { OutlinedTextField(value, { value = it }, label = { Text(label) }) }, confirmButton = { TextButton(onClick = { confirm(value) }) { Text("Save") } }, dismissButton = { TextButton(onClick = dismiss) { Text("Cancel") } })
}

@Composable private fun SearchCard(text: String, click: () -> Unit) = Card(Modifier.fillMaxWidth().clickable(onClick = click), colors = CardDefaults.cardColors(containerColor = Color.White)) { Row(Modifier.padding(16.dp)) { Icon(Icons.Default.Search, null); Text(" $text") } }
@Composable private fun ActionCard(title: String, subtitle: String, modifier: Modifier, click: () -> Unit) = Card(modifier.height(95.dp).clickable(onClick = click), colors = CardDefaults.cardColors(containerColor = Color.White)) { Column(Modifier.padding(14.dp)) { Text(title, fontWeight = FontWeight.Bold); Text(subtitle, color = Color.Gray) } }
@Composable private fun Stat(label: String, value: String, modifier: Modifier) = Card(modifier, colors = CardDefaults.cardColors(containerColor = Color.White)) { Column(Modifier.padding(14.dp)) { Text(label, color = Color.Gray); Text(value, fontWeight = FontWeight.Bold, fontSize = 19.sp) } }
@Composable private fun ListCard(title: String, subtitle: String) = Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White)) { Column(Modifier.padding(14.dp)) { Text(title, fontWeight = FontWeight.Bold); Text(subtitle, color = Color.Gray) } }
@Composable private fun ClickCard(title: String, subtitle: String, click: () -> Unit) = Card(Modifier.fillMaxWidth().clickable(onClick = click), colors = CardDefaults.cardColors(containerColor = Color.White)) { Column(Modifier.padding(14.dp)) { Text(title, fontWeight = FontWeight.Bold); Text(subtitle, color = Color.Gray) } }
@Composable private fun Banner(text: String) = Box(Modifier.fillMaxWidth().background(Color(0xFFEAF7EF), RoundedCornerShape(12.dp)).padding(12.dp)) { Text(text, color = Green, fontWeight = FontWeight.Medium) }
@Composable private fun StatusCard(label: String, value: String, color: Color) = Card(Modifier.fillMaxWidth()) { Row(Modifier.padding(12.dp)) { Text(label, Modifier.weight(1f)); Text(value, color = color, fontWeight = FontWeight.Bold) } }

private fun PaymentAllocationMode.label(): String = when (this) { PaymentAllocationMode.INTEREST_FIRST -> "Interest first"; PaymentAllocationMode.PRINCIPAL_FIRST -> "Principal first"; PaymentAllocationMode.CUSTOM -> "Custom" }
private fun String.moneyOrZero(): Long = if (isBlank()) 0 else runCatching { MoneyInput.rupeesToPaise(this) }.getOrDefault(0)
private fun String.digitsOnly(max: Int = 6): String = filter(Char::isDigit).take(max)
private fun String.decimalOnly(): String { val filtered = filter { it.isDigit() || it == '.' }; val dot = filtered.indexOf('.'); return if (dot < 0) filtered else filtered.substring(0, dot + 1) + filtered.substring(dot + 1).replace(".", "") }
private fun validatePin(pin: String, confirm: String): String? = when { pin.length != 6 -> "PIN 6 digit ka hona chahiye"; pin != confirm -> "Dono PIN match nahi kar rahe"; pin.toSet().size == 1 -> "Ek hi digit wala PIN allowed nahi hai"; pin in setOf("123456", "654321", "121212", "112233") -> "Ye PIN bahut aasaan hai"; else -> null }
private fun biometricMessage(value: BiometricAvailability): String = when (value) { BiometricAvailability.AVAILABLE -> "Fingerprint available"; BiometricAvailability.NONE_ENROLLED -> "Phone settings mein fingerprint enroll nahi hai"; BiometricAvailability.NO_HARDWARE -> "Biometric hardware nahi mila"; BiometricAvailability.TEMPORARILY_UNAVAILABLE -> "Fingerprint temporarily unavailable"; BiometricAvailability.UNSUPPORTED -> "Strong biometric supported nahi hai" }
private fun money(paise: Long): String = "₹%,.2f".format(Locale("en", "IN"), paise / 100.0)
