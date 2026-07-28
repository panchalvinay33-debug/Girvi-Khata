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
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.girvikhata.app.security.PinVerificationResult
import com.girvikhata.app.security.SecurityPreferences
import java.text.DateFormat
import java.util.Date

private val GirviNavy = Color(0xFF171752)
private val GirviPurple = Color(0xFF5146B8)
private val SecureGreen = Color(0xFF138A4A)
private val ScreenBackground = Color(0xFFF6F7FB)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val securityPreferences = SecurityPreferences(applicationContext)
        setContent { MaterialTheme { GirviKhataApp(securityPreferences) } }
    }
}

private enum class SessionState { ENROLL_PIN, LOCKED, UNLOCKED }
private enum class AppTab { HOME, CUSTOMERS, GIRVI, MASTERS, MORE }

@Composable
private fun GirviKhataApp(securityPreferences: SecurityPreferences) {
    var session by remember {
        mutableStateOf(if (securityPreferences.hasPin()) SessionState.LOCKED else SessionState.ENROLL_PIN)
    }

    when (session) {
        SessionState.ENROLL_PIN -> PinEnrollmentScreen { pin ->
            runCatching { securityPreferences.savePin(pin.toCharArray()) }
                .onSuccess { session = SessionState.UNLOCKED }
        }
        SessionState.LOCKED -> PinUnlockScreen(
            onVerify = { securityPreferences.verify(it.toCharArray()) },
            onUnlocked = { session = SessionState.UNLOCKED },
        )
        SessionState.UNLOCKED -> MainShell(onLock = { session = SessionState.LOCKED })
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

private fun String.digitsOnly(): String = take(6).filter(Char::isDigit)

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
private fun MainShell(onLock: () -> Unit) {
    var tab by rememberSaveable { mutableStateOf(AppTab.HOME) }
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
                AppTab.HOME -> DashboardScreen { tab = AppTab.CUSTOMERS }
                AppTab.CUSTOMERS -> CustomersScreen()
                AppTab.GIRVI -> GirviListScreen()
                AppTab.MASTERS -> MastersScreen()
                AppTab.MORE -> MoreScreen(onLock)
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
private fun DashboardScreen(onOpenCustomers: () -> Unit) = AppPage("Dashboard") {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Column {
            Text("Namaste, Malik Ji", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text(DateFormat.getDateInstance().format(Date()), color = Color.Gray)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.CloudDone, null, tint = SecureGreen)
            Text(" Local secure", color = SecureGreen)
        }
    }
    SearchCard("Customer, mobile ya girvi number khoje", onOpenCustomers)
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        ActionCard("Naya Girvi", "Entry shuru karein", Modifier.weight(1f))
        ActionCard("Payment Lein", "Hisaab jama karein", Modifier.weight(1f))
    }
    Text("Aaj ka summary", fontWeight = FontWeight.Bold, fontSize = 18.sp)
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        StatCard("Active Girvi", "0", Modifier.weight(1f))
        StatCard("Principal", "₹0", Modifier.weight(1f))
    }
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        StatCard("Received", "₹0", Modifier.weight(1f))
        StatCard("Due", "0", Modifier.weight(1f))
    }
    StatusBanner("PIN security active • business records sirf device par")
}

@Composable
private fun CustomersScreen() = AppPage("Customers") {
    val customers = remember { mutableStateListOf("Ramesh Bhuriya", "Mahesh Patel", "Suresh Kumar") }
    var query by rememberSaveable { mutableStateOf("") }
    OutlinedTextField(
        value = query,
        onValueChange = { query = it },
        label = { Text("Naam ya mobile search karein") },
        leadingIcon = { Icon(Icons.Default.Search, null) },
        modifier = Modifier.fillMaxWidth(),
    )
    Button(
        onClick = { customers.add("Naya Customer ${customers.size + 1}") },
        modifier = Modifier.fillMaxWidth(),
    ) {
        Icon(Icons.Default.Add, null)
        Spacer(Modifier.size(8.dp))
        Text("Naya Customer")
    }
    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items(customers.filter { it.contains(query, ignoreCase = true) }) { name ->
            ListCard(name, "Active girvi: 0 • Pending: ₹0")
        }
    }
}

@Composable
private fun GirviListScreen() = AppPage("Girvi Records") {
    StatusBanner("Naya Girvi wizard ko agle block mein real engine se connect kiya jayega")
    Button(
        onClick = {},
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(containerColor = GirviPurple),
    ) {
        Icon(Icons.Default.Add, null)
        Spacer(Modifier.size(8.dp))
        Text("Naya Girvi")
    }
    ListCard("Active Girvi", "Abhi koi record nahi")
    ListCard("Due Accounts", "Abhi koi due record nahi")
    ListCard("Closed Girvi", "History yahan dikhegi")
}

@Composable
private fun MastersScreen() = AppPage("Apni Lists") {
    Text("Har dukandar apni list khud banayega", fontWeight = FontWeight.Bold)
    ListCard("Categories", "Jewellery, Electronics, Documents ya manual")
    ListCard("Items", "Ring, Chain, Mobile ya custom item")
    ListCard("Units", "Gram, Kg, Piece, Pair, Set ya custom")
    ListCard("Interest Plans", "Simple, fixed, compound aur manual")
    ListCard("Payment Modes", "Cash, UPI, Bank ya custom")
}

@Composable
private fun MoreScreen(onLock: () -> Unit) = AppPage("Security & Settings") {
    ListCard("Backup Status", "Google Drive encrypted backup integration pending")
    ListCard("Privacy", "No central business database")
    ListCard("Security", "Salted PIN verifier + progressive lockout active")
    Button(
        onClick = onLock,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(containerColor = GirviNavy),
    ) {
        Icon(Icons.Default.Lock, null)
        Spacer(Modifier.size(8.dp))
        Text("App Lock Karein")
    }
}

@Composable
private fun SearchCard(text: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
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
private fun ActionCard(title: String, subtitle: String, modifier: Modifier) {
    Card(
        modifier = modifier.height(100.dp),
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
