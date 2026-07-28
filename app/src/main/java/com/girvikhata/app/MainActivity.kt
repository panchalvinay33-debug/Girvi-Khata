package com.girvikhata.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CloudDone
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.girvikhata.app.data.AppSnapshot
import com.girvikhata.app.data.CategoryRecord
import com.girvikhata.app.data.CustomerRecord
import com.girvikhata.app.data.EncryptedRecordStore
import com.girvikhata.app.data.GirviRecord
import com.girvikhata.app.security.PinVerificationResult
import com.girvikhata.app.security.SecurityPreferences
import java.text.DateFormat
import java.util.Date
import kotlin.math.roundToLong

private val GirviNavy = Color(0xFF171752)
private val GirviPurple = Color(0xFF5146B8)
private val SecureGreen = Color(0xFF138A4A)
private val ScreenBackground = Color(0xFFF6F7FB)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val securityPreferences = SecurityPreferences(applicationContext)
        val recordStore = EncryptedRecordStore(applicationContext)
        setContent { MaterialTheme { GirviKhataApp(securityPreferences, recordStore) } }
    }
}

private enum class SessionState { ENROLL_PIN, LOCKED, UNLOCKED }
private enum class AppTab { HOME, CUSTOMERS, GIRVI, MASTERS, MORE }

@Composable
private fun GirviKhataApp(
    securityPreferences: SecurityPreferences,
    recordStore: EncryptedRecordStore,
) {
    var session by remember {
        mutableStateOf(if (securityPreferences.hasPin()) SessionState.LOCKED else SessionState.ENROLL_PIN)
    }
    var snapshot by remember { mutableStateOf(recordStore.load()) }

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
            onVerify = { securityPreferences.verify(it.toCharArray()) },
            onUnlocked = {
                snapshot = recordStore.load()
                session = SessionState.UNLOCKED
            },
        )
        SessionState.UNLOCKED -> MainShell(
            snapshot = snapshot,
            onSnapshotChange = ::persist,
            onLock = { session = SessionState.LOCKED },
        )
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
            onClick = {
                error = validatePinUi(pin, confirm)
                if (error == null) onSave(pin)
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = GirviPurple),
        ) { Text("PIN Secure Karke Aage Badhein") }
    }
}

@Composable
private fun PinUnlockScreen(
    onVerify: (String) -> PinVerificationResult,
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
        ) { Text("Unlock") }
    }
}

private fun String.digitsOnly(max: Int = 6): String = take(max).filter(Char::isDigit)

private fun validatePinUi(pin: String, confirm: String): String? = when {
    pin.length != 6 -> "PIN 6 digit ka hona chahiye"
    pin != confirm -> "Dono PIN match nahi kar rahe"
    pin.toSet().size == 1 -> "Ek hi digit wala PIN allowed nahi hai"
    pin in setOf("123456", "654321", "121212", "112233") -> "Ye PIN bahut aasaan hai"
    else -> null
}

@Composable
private fun SecurePanel(title: String, subtitle: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().background(GirviNavy).padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(60.dp))
        Icon(Icons.Default.Lock, null, tint = Color(0xFFFFC54D), modifier = Modifier.size(68.dp))
        Text("Girvi Khata", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold)
        Text("Aapki digital tijori", color = Color.White.copy(alpha = .75f))
        Spacer(Modifier.height(32.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                content = {
                    Text(title, fontSize = 21.sp, fontWeight = FontWeight.Bold)
                    Text(subtitle, color = Color.Gray)
                    content()
                },
            )
        }
    }
}

@Composable
private fun SecurePinField(label: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun MainShell(
    snapshot: AppSnapshot,
    onSnapshotChange: (AppSnapshot) -> Unit,
    onLock: () -> Unit,
) {
    var tab by rememberSaveable { mutableStateOf(AppTab.HOME) }
    var showNewGirvi by rememberSaveable { mutableStateOf(false) }

    if (showNewGirvi) {
        NewGirviScreen(
            snapshot = snapshot,
            onCancel = { showNewGirvi = false },
            onSave = { customer, girvi ->
                val customers = if (snapshot.customers.any { it.id == customer.id }) {
                    snapshot.customers
                } else {
                    snapshot.customers + customer
                }
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
                AppTab.HOME -> DashboardScreen(snapshot, onOpenCustomers = { tab = AppTab.CUSTOMERS }, onNewGirvi = { showNewGirvi = true })
                AppTab.CUSTOMERS -> CustomersScreen(snapshot)
                AppTab.GIRVI -> GirviListScreen(snapshot, onNewGirvi = { showNewGirvi = true })
                AppTab.MASTERS -> MastersScreen(snapshot, onSnapshotChange)
                AppTab.MORE -> MoreScreen(snapshot, onLock)
            }
        }
    }
}

@Composable
private fun RowScope.NavItem(
    tab: AppTab,
    selected: AppTab,
    label: String,
    icon: ImageVector,
    onClick: (AppTab) -> Unit,
) {
    NavigationBarItem(
        selected = tab == selected,
        onClick = { onClick(tab) },
        icon = { Icon(icon, null) },
        label = { Text(label) },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppPage(title: String, content: @Composable ColumnScope.() -> Unit) {
    Scaffold(
        containerColor = ScreenBackground,
        topBar = {
            TopAppBar(
                title = { Text(title, color = Color.White, fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = GirviNavy),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).padding(16.dp).fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            content = content,
        )
    }
}

@Composable
private fun DashboardScreen(
    snapshot: AppSnapshot,
    onOpenCustomers: () -> Unit,
    onNewGirvi: () -> Unit,
) = AppPage("Dashboard") {
    val active = snapshot.girvis.count { it.status == "ACTIVE" }
    val principal = snapshot.girvis.filter { it.status == "ACTIVE" }.sumOf { it.principalPaise }
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
        ActionCard("Naya Girvi", "Entry shuru karein", Modifier.weight(1f), onNewGirvi)
        ActionCard("Customers", "${snapshot.customers.size} saved", Modifier.weight(1f), onOpenCustomers)
    }
    Text("Aaj ka summary", fontWeight = FontWeight.Bold, fontSize = 18.sp)
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        StatCard("Active Girvi", active.toString(), Modifier.weight(1f))
        StatCard("Principal", formatPaise(principal), Modifier.weight(1f))
    }
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        StatCard("Customers", snapshot.customers.size.toString(), Modifier.weight(1f))
        StatCard("Categories", snapshot.categories.count { it.active }.toString(), Modifier.weight(1f))
    }
    StatusBanner("Records AES-GCM encrypted app-private storage mein save ho rahe hain")
}

@Composable
private fun CustomersScreen(snapshot: AppSnapshot) = AppPage("Customers") {
    var query by rememberSaveable { mutableStateOf("") }
    OutlinedTextField(
        value = query,
        onValueChange = { query = it },
        label = { Text("Naam ya mobile search karein") },
        leadingIcon = { Icon(Icons.Default.Search, null) },
        modifier = Modifier.fillMaxWidth(),
    )
    val filtered = snapshot.customers.filter {
        it.name.contains(query, ignoreCase = true) || it.mobile.contains(query)
    }
    if (filtered.isEmpty()) {
        StatusBanner("Abhi customer nahi hai. Naya girvi banate waqt customer save hoga.")
    } else {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(filtered, key = { it.id }) { customer ->
                val active = snapshot.girvis.count { it.customerId == customer.id && it.status == "ACTIVE" }
                val pending = snapshot.girvis.filter { it.customerId == customer.id && it.status == "ACTIVE" }.sumOf { it.principalPaise }
                ListCard(customer.name, "${customer.mobile.ifBlank { "No mobile" }} • Active: $active • ${formatPaise(pending)}")
            }
        }
    }
}

@Composable
private fun GirviListScreen(snapshot: AppSnapshot, onNewGirvi: () -> Unit) = AppPage("Girvi Records") {
    Button(
        onClick = onNewGirvi,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(containerColor = GirviPurple),
    ) {
        Icon(Icons.Default.Add, null)
        Spacer(Modifier.size(8.dp))
        Text("Naya Girvi")
    }
    if (snapshot.girvis.isEmpty()) {
        StatusBanner("Abhi koi girvi record nahi hai")
    } else {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(snapshot.girvis.sortedByDescending { it.createdAt }, key = { it.id }) { girvi ->
                ListCard(
                    girvi.girviNumber,
                    "${girvi.customerName} • ${girvi.itemName} • ${formatPaise(girvi.principalPaise)} • ${girvi.monthlyRateBasisPoints / 100.0}%/month",
                )
            }
        }
    }
}

@Composable
private fun MastersScreen(snapshot: AppSnapshot, onSnapshotChange: (AppSnapshot) -> Unit) = AppPage("Apni Lists") {
    var showAddCategory by rememberSaveable { mutableStateOf(false) }
    Text("Har dukandar apni list khud banayega", fontWeight = FontWeight.Bold)
    Button(onClick = { showAddCategory = true }, modifier = Modifier.fillMaxWidth()) {
        Icon(Icons.Default.Add, null)
        Spacer(Modifier.size(8.dp))
        Text("Nayi Category Jodein")
    }
    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items(snapshot.categories.filter { it.active }, key = { it.id }) { category ->
            ListCard(category.name, "Active category")
        }
    }
    if (showAddCategory) {
        TextInputDialog(
            title = "Nayi Category",
            label = "Category name",
            onDismiss = { showAddCategory = false },
            onConfirm = { name ->
                val clean = name.trim()
                if (clean.isNotEmpty() && snapshot.categories.none { it.name.equals(clean, true) }) {
                    onSnapshotChange(snapshot.copy(categories = snapshot.categories + CategoryRecord(name = clean)))
                }
                showAddCategory = false
            },
        )
    }
}

@Composable
private fun MoreScreen(snapshot: AppSnapshot, onLock: () -> Unit) = AppPage("Security & Settings") {
    ListCard("Encrypted Local Store", "${snapshot.customers.size} customers • ${snapshot.girvis.size} girvi records")
    ListCard("Backup Status", "Google Drive encrypted backup integration pending")
    ListCard("Privacy", "No central business database")
    ListCard("Security", "PIN hashing + Android Keystore AES-GCM")
    Button(onClick = onLock, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = GirviNavy)) {
        Icon(Icons.Default.Lock, null)
        Spacer(Modifier.size(8.dp))
        Text("App Lock Karein")
    }
}

@Composable
private fun NewGirviScreen(
    snapshot: AppSnapshot,
    onCancel: () -> Unit,
    onSave: (CustomerRecord, GirviRecord) -> Unit,
) = AppPage("Naya Girvi") {
    var customerName by rememberSaveable { mutableStateOf("") }
    var mobile by rememberSaveable { mutableStateOf("") }
    var address by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf(snapshot.categories.firstOrNull { it.active }?.name.orEmpty()) }
    var itemName by rememberSaveable { mutableStateOf("") }
    var weight by rememberSaveable { mutableStateOf("") }
    var amount by rememberSaveable { mutableStateOf("") }
    var rate by rememberSaveable { mutableStateOf("2") }
    var message by rememberSaveable { mutableStateOf<String?>(null) }

    OutlinedTextField(customerName, { customerName = it }, label = { Text("Customer name *") }, modifier = Modifier.fillMaxWidth())
    OutlinedTextField(mobile, { mobile = it.digitsOnly(10) }, label = { Text("Mobile") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), modifier = Modifier.fillMaxWidth())
    OutlinedTextField(address, { address = it }, label = { Text("Address / village") }, modifier = Modifier.fillMaxWidth())
    OutlinedTextField(category, { category = it }, label = { Text("Category *") }, modifier = Modifier.fillMaxWidth())
    OutlinedTextField(itemName, { itemName = it }, label = { Text("Item name *") }, modifier = Modifier.fillMaxWidth())
    OutlinedTextField(weight, { weight = it.filter { ch -> ch.isDigit() || ch == '.' } }, label = { Text("Weight grams") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
    OutlinedTextField(amount, { amount = it.filter { ch -> ch.isDigit() || ch == '.' } }, label = { Text("Principal amount ₹ *") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
    OutlinedTextField(rate, { rate = it.filter { ch -> ch.isDigit() || ch == '.' } }, label = { Text("Monthly interest % *") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())

    val amountValue = amount.toDoubleOrNull()
    val rateValue = rate.toDoubleOrNull()
    if (amountValue != null && rateValue != null) {
        val monthInterest = amountValue * rateValue / 100.0
        StatusBanner("1 month interest: ₹${"%.2f".format(monthInterest)} • 6 months estimate: ₹${"%.2f".format(amountValue + monthInterest * 6)}")
    }
    message?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Button(onClick = onCancel, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)) { Text("Cancel") }
        Button(
            onClick = {
                message = when {
                    customerName.trim().length < 2 -> "Customer name required"
                    category.trim().isEmpty() -> "Category required"
                    itemName.trim().isEmpty() -> "Item name required"
                    amountValue == null || amountValue <= 0 -> "Valid amount required"
                    rateValue == null || rateValue < 0 || rateValue > 100 -> "Valid monthly rate required"
                    else -> null
                }
                if (message == null) {
                    val existing = snapshot.customers.firstOrNull {
                        mobile.isNotBlank() && it.mobile == mobile || it.name.equals(customerName.trim(), true)
                    }
                    val customer = existing ?: CustomerRecord(
                        name = customerName.trim(),
                        mobile = mobile,
                        address = address.trim(),
                    )
                    val nextSequence = snapshot.girvis.size + 1
                    val number = "GK-${java.text.SimpleDateFormat("yyyyMMdd", java.util.Locale.US).format(Date())}-${nextSequence.toString().padStart(4, '0')}"
                    val girvi = GirviRecord(
                        girviNumber = number,
                        customerId = customer.id,
                        customerName = customer.name,
                        categoryName = category.trim(),
                        itemName = itemName.trim(),
                        weightGrams = weight,
                        principalPaise = (amountValue!! * 100.0).roundToLong(),
                        monthlyRateBasisPoints = (rateValue!! * 100.0).roundToLong().toInt(),
                    )
                    onSave(customer, girvi)
                }
            },
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(containerColor = GirviPurple),
        ) { Text("Save Girvi") }
    }
}

@Composable
private fun TextInputDialog(
    title: String,
    label: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
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
private fun SearchCard(text: String, onClick: () -> Unit) {
    Card(
        Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Search, null, tint = GirviPurple)
            Spacer(Modifier.size(10.dp))
            Text(text, color = Color.Gray)
        }
    }
}

@Composable
private fun ActionCard(title: String, subtitle: String, modifier: Modifier, onClick: () -> Unit) {
    Card(
        modifier.height(100.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.Center) {
            Text(title, fontWeight = FontWeight.Bold, color = GirviNavy)
            Text(subtitle, color = Color.Gray, fontSize = 12.sp)
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier) {
    Card(modifier, shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(Modifier.padding(16.dp)) {
            Text(label, color = Color.Gray)
            Text(value, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = GirviNavy)
        }
    }
}

@Composable
private fun ListCard(title: String, subtitle: String) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(Modifier.padding(16.dp)) {
            Text(title, fontWeight = FontWeight.Bold)
            Text(subtitle, color = Color.Gray)
        }
    }
}

@Composable
private fun StatusBanner(message: String) {
    Box(Modifier.fillMaxWidth().background(Color(0xFFEAF7EF), RoundedCornerShape(14.dp)).padding(14.dp)) {
        Text(message, color = SecureGreen, fontWeight = FontWeight.Medium)
    }
}

private fun formatPaise(paise: Long): String = "₹%,.2f".format(paise / 100.0)
