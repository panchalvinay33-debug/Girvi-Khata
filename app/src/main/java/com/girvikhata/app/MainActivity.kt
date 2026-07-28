package com.girvikhata.app

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.biometric.BiometricPrompt
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
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.girvikhata.app.data.AppSnapshot
import com.girvikhata.app.data.CategoryRecord
import com.girvikhata.app.data.CustomerRecord
import com.girvikhata.app.data.EncryptedRecordStore
import com.girvikhata.app.data.GirviItemRecord
import com.girvikhata.app.data.GirviRecord
import com.girvikhata.app.domain.CategoryRules
import com.girvikhata.app.domain.CustomerCandidate
import com.girvikhata.app.domain.CustomerMatcher
import com.girvikhata.app.domain.GirviSequence
import com.girvikhata.app.domain.SimpleInterestPreview
import com.girvikhata.app.security.BiometricAvailability
import com.girvikhata.app.security.BiometricCapability
import com.girvikhata.app.security.PinVerificationResult
import com.girvikhata.app.security.SecurityPreferences
import com.girvikhata.app.security.SessionAutoLockPolicy
import java.math.BigDecimal
import java.text.DateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToLong

private val GirviNavy = Color(0xFF171752)
private val GirviPurple = Color(0xFF5146B8)
private val SecureGreen = Color(0xFF138A4A)
private val ScreenBackground = Color(0xFFF6F7FB)

class MainActivity : FragmentActivity() {
    private val lockPolicy = SessionAutoLockPolicy()
    private var backgroundedAt: Long? = null
    private var lockSignal by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val securityPreferences = SecurityPreferences(applicationContext)
        val recordStore = EncryptedRecordStore(applicationContext)
        val biometricCapability = BiometricCapability(applicationContext)
        setContent {
            MaterialTheme {
                GirviKhataApp(
                    securityPreferences = securityPreferences,
                    recordStore = recordStore,
                    biometricAvailability = biometricCapability.availability(),
                    lockSignal = lockSignal,
                    requestBiometric = ::requestBiometric,
                )
            }
        }
    }

    override fun onStop() {
        super.onStop()
        if (!isChangingConfigurations) backgroundedAt = System.currentTimeMillis()
    }

    override fun onStart() {
        super.onStart()
        if (lockPolicy.shouldLock(backgroundedAt, System.currentTimeMillis())) lockSignal++
        backgroundedAt = null
    }

    private fun requestBiometric(onSuccess: () -> Unit, onError: (String) -> Unit) {
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
                .setTitle("Girvi Khata Unlock")
                .setSubtitle("Apna fingerprint use karein")
                .setNegativeButtonText("PIN use karein")
                .build(),
        )
    }
}

private enum class SessionState { ENROLL_PIN, LOCKED, UNLOCKED }
private enum class AppTab { HOME, CUSTOMERS, GIRVI, MASTERS, MORE }

@Composable
private fun GirviKhataApp(
    securityPreferences: SecurityPreferences,
    recordStore: EncryptedRecordStore,
    biometricAvailability: BiometricAvailability,
    lockSignal: Int,
    requestBiometric: (() -> Unit, (String) -> Unit) -> Unit,
) {
    var session by remember { mutableStateOf(if (securityPreferences.hasPin()) SessionState.LOCKED else SessionState.ENROLL_PIN) }
    var snapshot by remember { mutableStateOf(recordStore.load()) }

    LaunchedEffect(lockSignal) {
        if (lockSignal > 0 && session == SessionState.UNLOCKED) session = SessionState.LOCKED
    }

    fun persist(next: AppSnapshot) {
        recordStore.save(next)
        snapshot = next
    }

    when (session) {
        SessionState.ENROLL_PIN -> PinEnrollmentScreen { pin ->
            runCatching { securityPreferences.savePin(pin.toCharArray()) }
                .onSuccess { session = SessionState.UNLOCKED }
        }
        SessionState.LOCKED -> PinUnlockScreen(
            biometricAvailability = biometricAvailability,
            onVerify = { securityPreferences.verify(it.toCharArray()) },
            onBiometric = { onSuccess, onError -> requestBiometric(onSuccess, onError) },
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
            onClick = { error = validatePinUi(pin, confirm); if (error == null) onSave(pin) },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = GirviPurple),
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
            colors = ButtonDefaults.buttonColors(containerColor = GirviPurple),
        ) { Text("PIN se Unlock") }
        if (biometricAvailability == BiometricAvailability.AVAILABLE) {
            OutlinedButton(
                onClick = { onBiometric(onUnlocked) { message = it } },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Default.Fingerprint, null)
                Spacer(Modifier.size(8.dp))
                Text("Fingerprint se Unlock")
            }
        } else {
            Text(biometricMessage(biometricAvailability), color = Color.Gray, fontSize = 12.sp)
        }
    }
}

@Composable
private fun MainShell(snapshot: AppSnapshot, onSnapshotChange: (AppSnapshot) -> Unit, onLock: () -> Unit) {
    var tab by rememberSaveable { mutableStateOf(AppTab.HOME) }
    var showNewGirvi by rememberSaveable { mutableStateOf(false) }
    var selectedGirviId by rememberSaveable { mutableStateOf<String?>(null) }

    selectedGirviId?.let { id ->
        snapshot.girvis.firstOrNull { it.id == id }?.let { girvi ->
            GirviDetailScreen(girvi) { selectedGirviId = null }
            return
        }
        selectedGirviId = null
    }

    if (showNewGirvi) {
        NewGirviScreen(
            snapshot = snapshot,
            onCancel = { showNewGirvi = false },
            onSave = { customer, girvi ->
                val customers = if (snapshot.customers.any { it.id == customer.id }) snapshot.customers else snapshot.customers + customer
                onSnapshotChange(snapshot.copy(customers = customers, girvis = snapshot.girvis + girvi))
                showNewGirvi = false
                tab = AppTab.GIRVI
            },
        )
        return
    }

    Scaffold(
        containerColor = ScreenBackground,
        bottomBar = {
            NavigationBar {
                NavItem(AppTab.HOME, tab, "Home", Icons.Default.Home) { tab = it }
                NavItem(AppTab.CUSTOMERS, tab, "Customers", Icons.Default.Groups) { tab = it }
                NavItem(AppTab.GIRVI, tab, "Girvi", Icons.Default.Inventory2) { tab = it }
                NavItem(AppTab.MASTERS, tab, "Masters", Icons.Default.Category) { tab = it }
                NavItem(AppTab.MORE, tab, "More", Icons.Default.Settings) { tab = it }
            }
        },
    ) { padding ->
        Box(Modifier.padding(padding)) {
            when (tab) {
                AppTab.HOME -> DashboardScreen(snapshot, { tab = AppTab.CUSTOMERS }) { showNewGirvi = true }
                AppTab.CUSTOMERS -> CustomersScreen(snapshot) { selectedGirviId = it }
                AppTab.GIRVI -> GirviListScreen(snapshot, { showNewGirvi = true }) { selectedGirviId = it }
                AppTab.MASTERS -> MastersScreen(snapshot, onSnapshotChange)
                AppTab.MORE -> MoreScreen(snapshot, onLock)
            }
        }
    }
}

@Composable
private fun RowScope.NavItem(tab: AppTab, selected: AppTab, label: String, icon: ImageVector, onClick: (AppTab) -> Unit) {
    NavigationBarItem(selected = tab == selected, onClick = { onClick(tab) }, icon = { Icon(icon, null) }, label = { Text(label) })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppPage(title: String, onBack: (() -> Unit)? = null, content: @Composable ColumnScope.() -> Unit) {
    Scaffold(
        containerColor = ScreenBackground,
        topBar = {
            TopAppBar(
                navigationIcon = { if (onBack != null) IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null, tint = Color.White) } },
                title = { Text(title, color = Color.White, fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = GirviNavy),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).padding(16.dp).fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content,
        )
    }
}

@Composable
private fun DashboardScreen(snapshot: AppSnapshot, onOpenCustomers: () -> Unit, onNewGirvi: () -> Unit) = AppPage("Dashboard") {
    val active = snapshot.girvis.filter { it.status == "ACTIVE" }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Column {
            Text("Namaste, Malik Ji", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text(DateFormat.getDateInstance().format(Date()), color = Color.Gray)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.CloudDone, null, tint = SecureGreen)
            Text(" Encrypted local", color = SecureGreen)
        }
    }
    SearchCard("Customer, mobile ya girvi number khoje", onOpenCustomers)
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        ActionCard("Naya Girvi", "Multiple items add karein", Modifier.weight(1f), onNewGirvi)
        ActionCard("Customers", "${snapshot.customers.size} saved", Modifier.weight(1f), onOpenCustomers)
    }
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        StatCard("Active Girvi", active.size.toString(), Modifier.weight(1f))
        StatCard("Principal", formatPaise(active.sumOf { it.principalPaise }), Modifier.weight(1f))
    }
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        StatCard("Total Items", active.sumOf { it.effectiveItems.sumOf { item -> item.quantity } }.toString(), Modifier.weight(1f))
        StatCard("Categories", snapshot.categories.count { it.active }.toString(), Modifier.weight(1f))
    }
    StatusBanner("Background mein 30 seconds ke baad app automatic lock hogi")
}

@Composable
private fun CustomersScreen(snapshot: AppSnapshot, onOpenGirvi: (String) -> Unit) = AppPage("Customers") {
    var query by rememberSaveable { mutableStateOf("") }
    OutlinedTextField(query, { query = it }, label = { Text("Naam, mobile ya village search") }, leadingIcon = { Icon(Icons.Default.Search, null) }, modifier = Modifier.fillMaxWidth())
    val candidates = snapshot.customers.map { CustomerCandidate(it.id, it.name, it.mobile, it.address) }
    val filtered = CustomerMatcher.search(candidates, query)
    if (filtered.isEmpty()) StatusBanner("Koi matching customer nahi mila") else LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items(filtered, key = { it.id }) { candidate ->
            val girvis = snapshot.girvis.filter { it.customerId == candidate.id }
            ClickableListCard(
                candidate.name,
                "${candidate.mobile.ifBlank { "No mobile" }} • ${candidate.address.ifBlank { "No address" }} • Active: ${girvis.count { it.status == "ACTIVE" }}",
            ) { girvis.maxByOrNull { it.createdAt }?.let { onOpenGirvi(it.id) } }
        }
    }
}

@Composable
private fun GirviListScreen(snapshot: AppSnapshot, onNewGirvi: () -> Unit, onOpenGirvi: (String) -> Unit) = AppPage("Girvi Records") {
    Button(onClick = onNewGirvi, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = GirviPurple)) {
        Icon(Icons.Default.Add, null); Spacer(Modifier.size(8.dp)); Text("Naya Girvi")
    }
    if (snapshot.girvis.isEmpty()) StatusBanner("Abhi koi girvi record nahi hai") else LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items(snapshot.girvis.sortedByDescending { it.createdAt }, key = { it.id }) { girvi ->
            ClickableListCard(
                girvi.girviNumber,
                "${girvi.customerName} • ${girvi.effectiveItems.size} item type • ${formatPaise(girvi.principalPaise)} • ${girvi.monthlyRateBasisPoints / 100.0}%/month",
            ) { onOpenGirvi(girvi.id) }
        }
    }
}

@Composable
private fun GirviDetailScreen(girvi: GirviRecord, onBack: () -> Unit) = AppPage(girvi.girviNumber, onBack) {
    var months by rememberSaveable { mutableStateOf("1") }
    val monthCount = months.toIntOrNull()?.coerceIn(0, 120) ?: 0
    val calculation = remember(girvi.principalPaise, girvi.monthlyRateBasisPoints, monthCount) {
        SimpleInterestPreview.calculate(girvi.principalPaise, girvi.monthlyRateBasisPoints, monthCount)
    }
    ListCard("Customer", girvi.customerName)
    Text("Girvi Items", fontWeight = FontWeight.Bold, fontSize = 18.sp)
    LazyColumn(modifier = Modifier.weight(1f, fill = false), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(girvi.effectiveItems, key = { it.id }) { item ->
            val gross = item.grossWeightGrams.toBigDecimalOrNull()
            val deduction = item.deductionWeightGrams.toBigDecimalOrNull() ?: BigDecimal.ZERO
            val net = gross?.subtract(deduction)
            ListCard(
                "${item.itemName} × ${item.quantity}",
                "${item.categoryName} • Gross: ${item.grossWeightGrams.ifBlank { "-" }}g • Deduction: ${item.deductionWeightGrams.ifBlank { "0" }}g • Net: ${net?.stripTrailingZeros()?.toPlainString() ?: "-"}g${item.description.takeIf(String::isNotBlank)?.let { " • $it" }.orEmpty()}",
            )
        }
    }
    OutlinedTextField(months, { months = it.digitsOnly(3) }, label = { Text("Calculation months") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        StatCard("Principal", formatPaise(calculation.principalPaise), Modifier.weight(1f))
        StatCard("Interest", formatPaise(calculation.totalInterestPaise), Modifier.weight(1f))
    }
    StatusBanner("Total payable after $monthCount month: ${formatPaise(calculation.totalPayablePaise)}")
    if (calculation.rows.isNotEmpty()) {
        Text("Month-wise Breakup", fontWeight = FontWeight.Bold)
        calculation.rows.takeLast(6).forEach { row ->
            ListCard("Month ${row.monthNumber}", "Interest ${formatPaise(row.interestPaise)} • Total ${formatPaise(row.closingPayablePaise)}")
        }
    }
}

@Composable
private fun MastersScreen(snapshot: AppSnapshot, onSnapshotChange: (AppSnapshot) -> Unit) = AppPage("Categories") {
    var showAddCategory by rememberSaveable { mutableStateOf(false) }
    var warning by rememberSaveable { mutableStateOf<String?>(null) }
    Button(onClick = { showAddCategory = true }, modifier = Modifier.fillMaxWidth()) {
        Icon(Icons.Default.Add, null); Spacer(Modifier.size(8.dp)); Text("Nayi Category Jodein")
    }
    warning?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items(snapshot.categories, key = { it.id }) { category ->
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(category.name, fontWeight = FontWeight.Bold)
                        Text(if (category.active) "Active" else "Inactive", color = if (category.active) SecureGreen else Color.Gray)
                    }
                    TextButton(onClick = {
                        if (category.active) {
                            val activeNames = snapshot.girvis.filter { it.status == "ACTIVE" }.flatMap { it.effectiveItems }.map { it.categoryName }
                            if (!CategoryRules.canDeactivate(category.name, activeNames)) {
                                warning = "${category.name} active girvi mein use ho rahi hai; deactivate nahi kar sakte"
                            } else {
                                onSnapshotChange(snapshot.copy(categories = snapshot.categories.map { if (it.id == category.id) it.copy(active = false) else it }))
                                warning = null
                            }
                        } else {
                            onSnapshotChange(snapshot.copy(categories = snapshot.categories.map { if (it.id == category.id) it.copy(active = true) else it }))
                            warning = null
                        }
                    }) { Text(if (category.active) "Deactivate" else "Activate") }
                }
            }
        }
    }
    if (showAddCategory) TextInputDialog("Nayi Category", "Category name", { showAddCategory = false }) { name ->
        val clean = name.trim()
        if (clean.isNotEmpty() && snapshot.categories.none { it.name.equals(clean, true) }) {
            onSnapshotChange(snapshot.copy(categories = snapshot.categories + CategoryRecord(name = clean)))
        }
        showAddCategory = false
    }
}

@Composable
private fun MoreScreen(snapshot: AppSnapshot, onLock: () -> Unit) = AppPage("Security & Settings") {
    ListCard("Encrypted Local Store", "${snapshot.customers.size} customers • ${snapshot.girvis.size} girvi records • schema v${snapshot.schemaVersion}")
    ListCard("Automatic Lock", "App 30 seconds background mein rehne par PIN/fingerprint lock")
    ListCard("Fingerprint", "Device par enrolled strong biometric available ho to unlock button dikhega")
    ListCard("Backup Status", "Google Drive encrypted backup integration next major phase")
    ListCard("Privacy", "No central business database • Android automatic backup disabled")
    Button(onClick = onLock, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = GirviNavy)) {
        Icon(Icons.Default.Lock, null); Spacer(Modifier.size(8.dp)); Text("App Lock Karein")
    }
}

private data class ItemDraftUi(
    val category: String = "",
    val name: String = "",
    val quantity: String = "1",
    val gross: String = "",
    val deduction: String = "",
    val description: String = "",
)

@Composable
private fun NewGirviScreen(snapshot: AppSnapshot, onCancel: () -> Unit, onSave: (CustomerRecord, GirviRecord) -> Unit) = AppPage("Naya Girvi", onCancel) {
    var customerQuery by rememberSaveable { mutableStateOf("") }
    var selectedCustomerId by rememberSaveable { mutableStateOf<String?>(null) }
    var mobile by rememberSaveable { mutableStateOf("") }
    var address by rememberSaveable { mutableStateOf("") }
    var amount by rememberSaveable { mutableStateOf("") }
    var rate by rememberSaveable { mutableStateOf("2") }
    var message by rememberSaveable { mutableStateOf<String?>(null) }
    val firstCategory = snapshot.categories.firstOrNull { it.active }?.name.orEmpty()
    val drafts = remember { mutableStateListOf(ItemDraftUi(category = firstCategory)) }

    OutlinedTextField(customerQuery, {
        customerQuery = it
        if (selectedCustomerId != null) selectedCustomerId = null
    }, label = { Text("Customer name / search *") }, modifier = Modifier.fillMaxWidth())

    val customerMatches = remember(customerQuery, snapshot.customers) {
        CustomerMatcher.search(snapshot.customers.map { CustomerCandidate(it.id, it.name, it.mobile, it.address) }, customerQuery).take(5)
    }
    if (customerQuery.isNotBlank() && selectedCustomerId == null && customerMatches.isNotEmpty()) {
        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column {
                customerMatches.forEach { match ->
                    Row(
                        Modifier.fillMaxWidth().clickable {
                            selectedCustomerId = match.id
                            customerQuery = match.name
                            mobile = match.mobile
                            address = match.address
                        }.padding(12.dp),
                    ) {
                        Column { Text(match.name, fontWeight = FontWeight.Bold); Text("${match.mobile} • ${match.address}", color = Color.Gray) }
                    }
                }
            }
        }
    }
    selectedCustomerId?.let { Text("Existing customer selected ✓", color = SecureGreen, fontWeight = FontWeight.Bold) }
    OutlinedTextField(mobile, { mobile = it.digitsOnly(10) }, label = { Text("Mobile") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), modifier = Modifier.fillMaxWidth())
    OutlinedTextField(address, { address = it }, label = { Text("Address / village") }, modifier = Modifier.fillMaxWidth())

    Text("Girvi Items", fontWeight = FontWeight.Bold, fontSize = 18.sp)
    LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        itemsIndexed(drafts) { index, draft ->
            ItemEditor(
                index = index,
                draft = draft,
                categories = snapshot.categories.filter { it.active }.map { it.name },
                canRemove = drafts.size > 1,
                onChange = { drafts[index] = it },
                onRemove = { drafts.removeAt(index) },
            )
        }
        item {
            OutlinedButton(onClick = { drafts.add(ItemDraftUi(category = firstCategory)) }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Add, null); Spacer(Modifier.size(8.dp)); Text("Ek Aur Item Jodein")
            }
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(amount, { amount = it.decimalOnly() }, label = { Text("Principal amount ₹ *") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
            OutlinedTextField(rate, { rate = it.decimalOnly() }, label = { Text("Monthly interest % *") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
            val amountValue = amount.toDoubleOrNull()
            val rateValue = rate.toDoubleOrNull()
            if (amountValue != null && rateValue != null && amountValue > 0) {
                val preview = SimpleInterestPreview.calculate((amountValue * 100).roundToLong(), (rateValue * 100).roundToLong().toInt(), 6)
                StatusBanner("1 month interest: ${formatPaise(preview.rows.firstOrNull()?.interestPaise ?: 0)} • 6 month total: ${formatPaise(preview.totalPayablePaise)}")
            }
            message?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = onCancel, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)) { Text("Cancel") }
                Button(
                    onClick = {
                        val amountNumber = amount.toDoubleOrNull()
                        val rateNumber = rate.toDoubleOrNull()
                        val itemError = drafts.asSequence().mapIndexedNotNull { i, d -> validateDraft(d)?.let { "Item ${i + 1}: $it" } }.firstOrNull()
                        message = when {
                            customerQuery.trim().length < 2 -> "Customer name required"
                            itemError != null -> itemError
                            amountNumber == null || amountNumber <= 0 -> "Valid amount required"
                            rateNumber == null || rateNumber < 0 || rateNumber > 100 -> "Valid monthly rate required"
                            else -> null
                        }
                        if (message == null) {
                            val selected = selectedCustomerId?.let { id -> snapshot.customers.firstOrNull { it.id == id } }
                            val matched = selected ?: CustomerMatcher.findBestMatch(
                                snapshot.customers.map { CustomerCandidate(it.id, it.name, it.mobile, it.address) }, customerQuery, mobile,
                            )?.let { match -> snapshot.customers.first { it.id == match.id } }
                            val customer = matched ?: CustomerRecord(name = customerQuery.trim(), mobile = mobile, address = address.trim())
                            val itemRecords = drafts.map { d ->
                                GirviItemRecord(
                                    categoryName = d.category.trim(),
                                    itemName = d.name.trim(),
                                    quantity = d.quantity.toInt(),
                                    grossWeightGrams = d.gross,
                                    deductionWeightGrams = d.deduction,
                                    description = d.description.trim(),
                                )
                            }
                            val first = itemRecords.first()
                            onSave(
                                customer,
                                GirviRecord(
                                    girviNumber = GirviSequence.nextNumber(snapshot.girvis.map { it.girviNumber }),
                                    customerId = customer.id,
                                    customerName = customer.name,
                                    categoryName = first.categoryName,
                                    itemName = first.itemName,
                                    weightGrams = first.grossWeightGrams,
                                    principalPaise = (amountNumber!! * 100).roundToLong(),
                                    monthlyRateBasisPoints = (rateNumber!! * 100).roundToLong().toInt(),
                                    items = itemRecords,
                                ),
                            )
                        }
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = GirviPurple),
                ) { Text("Save Girvi") }
            }
        }
    }
}

@Composable
private fun ItemEditor(
    index: Int,
    draft: ItemDraftUi,
    categories: List<String>,
    canRemove: Boolean,
    onChange: (ItemDraftUi) -> Unit,
    onRemove: () -> Unit,
) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Item ${index + 1}", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, color = GirviNavy)
                if (canRemove) IconButton(onClick = onRemove) { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) }
            }
            Text("Category: ${draft.category.ifBlank { "Select below" }}", fontWeight = FontWeight.Medium)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                categories.take(4).forEach { category ->
                    TextButton(onClick = { onChange(draft.copy(category = category)) }) { Text(category, fontSize = 11.sp) }
                }
            }
            OutlinedTextField(draft.name, { onChange(draft.copy(name = it)) }, label = { Text("Item name *") }, modifier = Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(draft.quantity, { onChange(draft.copy(quantity = it.digitsOnly(3))) }, label = { Text("Qty") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                OutlinedTextField(draft.gross, { onChange(draft.copy(gross = it.decimalOnly())) }, label = { Text("Gross g") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(1f))
                OutlinedTextField(draft.deduction, { onChange(draft.copy(deduction = it.decimalOnly())) }, label = { Text("Less g") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(1f))
            }
            val gross = draft.gross.toBigDecimalOrNull()
            val less = draft.deduction.toBigDecimalOrNull() ?: BigDecimal.ZERO
            if (gross != null) Text("Net weight: ${gross.subtract(less).stripTrailingZeros().toPlainString()} g", color = SecureGreen)
            OutlinedTextField(draft.description, { onChange(draft.copy(description = it)) }, label = { Text("Description / पहचान") }, modifier = Modifier.fillMaxWidth())
        }
    }
}

private fun validateDraft(draft: ItemDraftUi): String? {
    val gross = draft.gross.toBigDecimalOrNull()
    val deduction = draft.deduction.toBigDecimalOrNull() ?: BigDecimal.ZERO
    return when {
        draft.category.isBlank() -> "Category required"
        draft.name.trim().isEmpty() -> "Item name required"
        draft.quantity.toIntOrNull() == null || draft.quantity.toInt() <= 0 -> "Quantity must be positive"
        gross != null && gross < BigDecimal.ZERO -> "Gross weight invalid"
        deduction < BigDecimal.ZERO -> "Deduction invalid"
        gross != null && deduction > gross -> "Deduction gross weight se zyada hai"
        else -> null
    }
}

@Composable
private fun SecurePanel(title: String, subtitle: String, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxSize().background(GirviNavy).padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(60.dp))
        Icon(Icons.Default.Lock, null, tint = Color(0xFFFFC54D), modifier = Modifier.size(68.dp))
        Text("Girvi Khata", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold)
        Text("Aapki digital tijori", color = Color.White.copy(alpha = .75f))
        Spacer(Modifier.height(32.dp))
        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(title, fontSize = 21.sp, fontWeight = FontWeight.Bold)
                Text(subtitle, color = Color.Gray)
                content()
            }
        }
    }
}

@Composable
private fun SecurePinField(label: String, value: String, onChange: (String) -> Unit) = OutlinedTextField(
    value, onChange, label = { Text(label) }, singleLine = true, visualTransformation = PasswordVisualTransformation(),
    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword), modifier = Modifier.fillMaxWidth(),
)

@Composable
private fun TextInputDialog(title: String, label: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var value by rememberSaveable { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { OutlinedTextField(value, { value = it }, label = { Text(label) }, singleLine = true) },
        confirmButton = { TextButton(onClick = { onConfirm(value) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun SearchCard(text: String, onClick: () -> Unit) = Card(
    Modifier.fillMaxWidth().clickable(onClick = onClick), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White),
) { Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Search, null, tint = GirviPurple); Spacer(Modifier.size(10.dp)); Text(text, color = Color.Gray) } }

@Composable
private fun ActionCard(title: String, subtitle: String, modifier: Modifier, onClick: () -> Unit) = Card(
    modifier.height(100.dp).clickable(onClick = onClick), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White),
) { Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.Center) { Text(title, fontWeight = FontWeight.Bold, color = GirviNavy); Text(subtitle, color = Color.Gray, fontSize = 12.sp) } }

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier) = Card(modifier, shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
    Column(Modifier.padding(16.dp)) { Text(label, color = Color.Gray); Text(value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = GirviNavy) }
}

@Composable
private fun ListCard(title: String, subtitle: String) = Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
    Column(Modifier.padding(16.dp)) { Text(title, fontWeight = FontWeight.Bold); Text(subtitle, color = Color.Gray) }
}

@Composable
private fun ClickableListCard(title: String, subtitle: String, onClick: () -> Unit) = Card(
    Modifier.fillMaxWidth().clickable(onClick = onClick), shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = Color.White),
) { Column(Modifier.padding(16.dp)) { Text(title, fontWeight = FontWeight.Bold); Text(subtitle, color = Color.Gray) } }

@Composable
private fun StatusBanner(message: String) = Box(Modifier.fillMaxWidth().background(Color(0xFFEAF7EF), RoundedCornerShape(14.dp)).padding(14.dp)) {
    Text(message, color = SecureGreen, fontWeight = FontWeight.Medium)
}

private fun String.digitsOnly(max: Int = 6): String = filter(Char::isDigit).take(max)
private fun String.decimalOnly(): String {
    val filtered = filter { it.isDigit() || it == '.' }
    val firstDot = filtered.indexOf('.')
    return if (firstDot < 0) filtered else filtered.substring(0, firstDot + 1) + filtered.substring(firstDot + 1).replace(".", "")
}
private fun validatePinUi(pin: String, confirm: String): String? = when {
    pin.length != 6 -> "PIN 6 digit ka hona chahiye"
    pin != confirm -> "Dono PIN match nahi kar rahe"
    pin.toSet().size == 1 -> "Ek hi digit wala PIN allowed nahi hai"
    pin in setOf("123456", "654321", "121212", "112233") -> "Ye PIN bahut aasaan hai"
    else -> null
}
private fun biometricMessage(value: BiometricAvailability): String = when (value) {
    BiometricAvailability.AVAILABLE -> "Fingerprint available"
    BiometricAvailability.NONE_ENROLLED -> "Phone settings mein fingerprint enroll nahi hai"
    BiometricAvailability.NO_HARDWARE -> "Is phone mein biometric hardware nahi mila"
    BiometricAvailability.TEMPORARILY_UNAVAILABLE -> "Fingerprint abhi temporarily unavailable hai"
    BiometricAvailability.UNSUPPORTED -> "Strong biometric unlock supported nahi hai"
}
private fun formatPaise(paise: Long): String = "₹%,.2f".format(Locale("en", "IN"), paise / 100.0)
