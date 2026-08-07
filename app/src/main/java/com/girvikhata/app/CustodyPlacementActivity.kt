package com.girvikhata.app

import android.app.DatePickerDialog
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.girvikhata.app.custody.CustodyPlacementSnapshot
import com.girvikhata.app.custody.CustodyPlacementStore
import com.girvikhata.app.custody.PlacementLot
import com.girvikhata.app.custody.StorageLocation
import com.girvikhata.app.data.AppSnapshot
import com.girvikhata.app.data.DataSafetyJournal
import com.girvikhata.app.data.EncryptedRecordStore
import com.girvikhata.app.domain.ExternalInterestRule
import com.girvikhata.app.domain.ExternalPlacementLedger
import com.girvikhata.app.security.BiometricAvailability
import com.girvikhata.app.security.BiometricCapability
import com.girvikhata.app.security.PinVerificationResult
import com.girvikhata.app.security.SecurityPreferences
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DateFormat
import java.util.Calendar
import java.util.Date

private val CustodyNavy = Color(0xFF171752)
private val CustodyPurple = Color(0xFF5146B8)
private val CustodyGreen = Color(0xFF138A4A)
private val CustodyBg = Color(0xFFF6F7FB)

class CustodyPlacementActivity : FragmentActivity() {
    private lateinit var security: SecurityPreferences
    private lateinit var biometricCapability: BiometricCapability

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        security = SecurityPreferences(applicationContext)
        biometricCapability = BiometricCapability(applicationContext)
        val businessStore = EncryptedRecordStore(applicationContext)
        val custodyStore = CustodyPlacementStore(applicationContext)
        val journal = DataSafetyJournal(applicationContext)

        setContent {
            MaterialTheme {
                var unlocked by rememberSaveable { mutableStateOf(false) }
                val availability = if (security.sessionSettings().biometricUnlockEnabled) biometricCapability.availability() else BiometricAvailability.UNSUPPORTED
                if (!unlocked) {
                    CustodyAuth(
                        availability = availability,
                        verifyPin = { security.verify(it.toCharArray()) },
                        requestBiometric = ::requestBiometric,
                        success = { unlocked = true },
                        close = ::finish,
                    )
                } else {
                    var business by remember { mutableStateOf(businessStore.load()) }
                    var custody by remember { mutableStateOf(custodyStore.load()) }
                    SimpleCustodyScreen(
                        business = business,
                        custody = custody,
                        refresh = { business = businessStore.load(); custody = custodyStore.load() },
                        addLocation = { name, type, detail ->
                            custody = custodyStore.addLocation(name, type, detail)
                            journal.recordNamedEvent("LOCATION_CREATED", "Samaan ki jagah banayi", name.trim())
                        },
                        setLocationActive = { id, active ->
                            custody = custodyStore.setLocationActive(id, active)
                            journal.recordNamedEvent("LOCATION_UPDATED", "Samaan ki jagah update", custody.locations.firstOrNull { it.id == id }?.name.orEmpty())
                        },
                        moveItems = { refs, locationId, at, note ->
                            custody = custodyStore.moveItemsToLocation(refs, locationId, at, note)
                            val name = custody.locations.firstOrNull { it.id == locationId }?.name ?: "Jagah"
                            journal.recordNamedEvent("ITEM_MOVED", "Samaan ki jagah badli", "${refs.size} item → $name")
                        },
                        addParty = { name, mobile, address, rate, note ->
                            custody = custodyStore.addParty(name, mobile, address, rate, note)
                            journal.recordNamedEvent("EXTERNAL_PARTY_CREATED", "Bahar rakhne ki party banayi", name.trim())
                        },
                        placeOutside = { partyId, refs, at, amount, rate, rule, note ->
                            val lotNo = "GRP-${System.currentTimeMillis().toString().takeLast(8)}"
                            custody = custodyStore.createLot(lotNo, partyId, refs, at, amount, rate, note, rule)
                            val party = custody.parties.firstOrNull { it.id == partyId }?.name ?: "Party"
                            journal.recordNamedEvent("LOT_CREATED", "Samaan bahar rakha", "$party • ${refs.size} items")
                        },
                        addItems = { lotId, refs, at, note ->
                            custody = custodyStore.addItemsToLot(lotId, refs, at, note)
                            journal.recordNamedEvent("ITEMS_ADDED_TO_LOT", "Bahar wale group me samaan joda", "${refs.size} items")
                        },
                        addAdvance = { lotId, amount, rate, at, rule, note ->
                            custody = custodyStore.addExternalAdvance(lotId, amount, rate, at, rule, note)
                            journal.recordNamedEvent("EXTERNAL_ADVANCE", "Wahan se aur paise mile", money(amount))
                        },
                        addPayment = { lotId, amount, at, note ->
                            custody = custodyStore.addExternalPayment(lotId, amount, at, note)
                            journal.recordNamedEvent("EXTERNAL_PAYMENT", "Wahan paise chukaye", money(amount))
                        },
                        reversePayment = { lotId, paymentId, at, reason ->
                            custody = custodyStore.reverseExternalPayment(lotId, paymentId, at, reason)
                            journal.recordNamedEvent("EXTERNAL_PAYMENT_REVERSED", "Bahar payment reverse", reason.take(100))
                        },
                        closeLot = { lotId, at ->
                            custody = custodyStore.closeLot(lotId, at)
                            journal.recordNamedEvent("LOT_CLOSED", "Bahar rakha group band", lotId)
                        },
                        close = ::finish,
                    )
                }
            }
        }
    }

    private fun requestBiometric(onSuccess: () -> Unit, onError: (String) -> Unit) {
        val prompt = BiometricPrompt(this, ContextCompat.getMainExecutor(this), object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) = onSuccess()
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) = onError(errString.toString())
            override fun onAuthenticationFailed() = onError("Fingerprint match nahi hua")
        })
        prompt.authenticate(BiometricPrompt.PromptInfo.Builder().setTitle("Samaan ki Jagah").setSubtitle("Owner verify karein").setNegativeButtonText("Use PIN").build())
    }
}

@Composable
private fun CustodyAuth(
    availability: BiometricAvailability,
    verifyPin: (String) -> PinVerificationResult,
    requestBiometric: (() -> Unit, (String) -> Unit) -> Unit,
    success: () -> Unit,
    close: () -> Unit,
) {
    val biometricFirst = availability == BiometricAvailability.AVAILABLE
    var usePin by rememberSaveable { mutableStateOf(!biometricFirst) }
    var pin by rememberSaveable { mutableStateOf("") }
    var message by rememberSaveable { mutableStateOf(if (biometricFirst) "Fingerprint se owner verify karein" else "Owner PIN daalein") }
    Column(Modifier.fillMaxSize().background(CustodyBg).padding(22.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(if (!usePin && biometricFirst) Icons.Default.Fingerprint else Icons.Default.Lock, null, tint = CustodyNavy)
        Text("सामान की जगह", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = CustodyNavy)
        Text(message, color = Color.Gray)
        if (!usePin && biometricFirst) {
            Button(onClick = { requestBiometric(success) { message = it } }, modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
                Icon(Icons.Default.Fingerprint, null); Text("  Fingerprint se Continue")
            }
            TextButton(onClick = { usePin = true; message = "6-digit PIN daalein" }, modifier = Modifier.fillMaxWidth()) { Text("Use PIN instead") }
        } else {
            OutlinedTextField(pin, { pin = it.filter(Char::isDigit).take(6) }, label = { Text("6-digit PIN") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword), visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth().padding(top = 12.dp))
            Button(onClick = {
                when (val result = verifyPin(pin)) {
                    PinVerificationResult.Success -> success()
                    PinVerificationResult.NotConfigured -> message = "PIN configured nahi hai"
                    is PinVerificationResult.Locked -> message = "Security lock active hai"
                    is PinVerificationResult.Failure -> message = "Galat PIN. Attempts: ${result.attempts}"
                }
                pin = ""
            }, enabled = pin.length == 6, modifier = Modifier.fillMaxWidth()) { Text("PIN se Continue") }
            if (biometricFirst) TextButton(onClick = { usePin = false }, modifier = Modifier.fillMaxWidth()) { Text("Use Fingerprint") }
        }
        OutlinedButton(onClick = close, modifier = Modifier.fillMaxWidth()) { Text("Band Karein") }
    }
}

private enum class SimpleTab { SAMAAN, JAGAH, BAHAR }
private data class ItemRow(val girviId: String, val girviNumber: String, val customerName: String, val itemId: String, val itemName: String)

@Composable
private fun SimpleCustodyScreen(
    business: AppSnapshot,
    custody: CustodyPlacementSnapshot,
    refresh: () -> Unit,
    addLocation: (String, String, String) -> Unit,
    setLocationActive: (String, Boolean) -> Unit,
    moveItems: (List<Pair<String, String>>, String, Long, String) -> Unit,
    addParty: (String, String, String, Int, String) -> Unit,
    placeOutside: (String, List<Pair<String, String>>, Long, Long, Int, ExternalInterestRule, String) -> Unit,
    addItems: (String, List<Pair<String, String>>, Long, String) -> Unit,
    addAdvance: (String, Long, Int, Long, ExternalInterestRule, String) -> Unit,
    addPayment: (String, Long, Long, String) -> Unit,
    reversePayment: (String, String, Long, String) -> Unit,
    closeLot: (String, Long) -> Unit,
    close: () -> Unit,
) {
    var tab by rememberSaveable { mutableStateOf(SimpleTab.SAMAAN) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    val rows = remember(business) { business.girvis.flatMap { g -> g.effectiveItems.map { i -> ItemRow(g.id, g.girviNumber, g.customerName, i.id, i.itemName) } } }
    val outsideIds = custody.lots.flatMap { it.items.filter { x -> x.removedAt == null }.map { x -> x.itemId } }.toSet()
    val assigned = rows.count { currentMovement(it.itemId, custody) != null }

    Scaffold(containerColor = CustodyBg, bottomBar = {
        NavigationBar {
            NavigationBarItem(tab == SimpleTab.SAMAAN, { tab = SimpleTab.SAMAAN }, { Icon(Icons.Default.Inventory2, null) }, label = { Text("सामान") })
            NavigationBarItem(tab == SimpleTab.JAGAH, { tab = SimpleTab.JAGAH }, { Icon(Icons.Default.LocationOn, null) }, label = { Text("जगहें") })
            NavigationBarItem(tab == SimpleTab.BAHAR, { tab = SimpleTab.BAHAR }, { Icon(Icons.Default.Store, null) }, label = { Text("बाहर रखा") })
        }
    }) { padding ->
        Column(Modifier.padding(padding).fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("सामान कहाँ है?", fontSize = 27.sp, fontWeight = FontWeight.Bold, color = CustodyNavy)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SmallStat("कुल", rows.size.toString(), Modifier.weight(1f))
                SmallStat("जगह तय", assigned.toString(), Modifier.weight(1f))
                SmallStat("बाहर", outsideIds.size.toString(), Modifier.weight(1f))
            }
            error?.let { Text(it, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold) }
            when (tab) {
                SimpleTab.SAMAAN -> ItemsTab(rows, custody, moveItems, placeOutside) { error = it }
                SimpleTab.JAGAH -> PlacesTab(rows, custody, addLocation, setLocationActive) { error = it }
                SimpleTab.BAHAR -> OutsideTab(rows, custody, addParty, placeOutside, addItems, moveItems, addAdvance, addPayment, reversePayment, closeLot) { error = it }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = refresh, modifier = Modifier.weight(1f)) { Text("Refresh") }
                OutlinedButton(onClick = close, modifier = Modifier.weight(1f)) { Text("Band Karein") }
            }
        }
    }
}

@Composable
private fun SmallStat(label: String, value: String, modifier: Modifier) = Card(modifier, colors = CardDefaults.cardColors(containerColor = Color.White)) {
    Column(Modifier.padding(10.dp)) { Text(label, color = Color.Gray, fontSize = 11.sp); Text(value, fontWeight = FontWeight.Bold, fontSize = 18.sp) }
}

@Composable
private fun androidx.compose.foundation.layout.ColumnScope.ItemsTab(
    rows: List<ItemRow>, custody: CustodyPlacementSnapshot,
    moveItems: (List<Pair<String, String>>, String, Long, String) -> Unit,
    placeOutside: (String, List<Pair<String, String>>, Long, Long, Int, ExternalInterestRule, String) -> Unit,
    error: (String?) -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var selected by remember { mutableStateOf<ItemRow?>(null) }
    var outsideRow by remember { mutableStateOf<ItemRow?>(null) }
    OutlinedTextField(query, { query = it }, label = { Text("Customer / Girvi / Item search") }, modifier = Modifier.fillMaxWidth())
    val q = query.trim().lowercase()
    val filtered = rows.filter { q.isBlank() || it.customerName.lowercase().contains(q) || it.girviNumber.lowercase().contains(q) || it.itemName.lowercase().contains(q) }
    LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(filtered, key = { it.itemId }) { row ->
            val current = currentText(row.itemId, custody)
            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text("${row.girviNumber} • ${row.customerName}", fontWeight = FontWeight.Bold)
                    Text(row.itemName, fontSize = 16.sp)
                    Text("📍 $current", color = if (current == "जगह तय नहीं") Color(0xFF9A6700) else CustodyGreen, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedButton(onClick = { selected = row }, modifier = Modifier.weight(1f)) { Text("जगह बदलें") }
                        Button(onClick = { outsideRow = row }, modifier = Modifier.weight(1f), enabled = custody.parties.any { it.active }) { Text("बाहर रखें") }
                    }
                }
            }
        }
    }
    selected?.let { row -> MoveDialog(listOf(row), custody.locations.filter { it.active }, { selected = null }) { locationId, at, note ->
        runCatching { moveItems(listOf(row.girviId to row.itemId), locationId, at, note) }.onSuccess { selected = null; error(null) }.onFailure { error(it.message) }
    } }
    outsideRow?.let { row -> OutsideDialog(listOf(row), custody, { outsideRow = null }) { party, refs, at, amount, rate, rule, note ->
        runCatching { placeOutside(party, refs, at, amount, rate, rule, note) }.onSuccess { outsideRow = null; error(null) }.onFailure { error(it.message) }
    } }
}

@Composable
private fun androidx.compose.foundation.layout.ColumnScope.PlacesTab(
    rows: List<ItemRow>, custody: CustodyPlacementSnapshot,
    addLocation: (String, String, String) -> Unit,
    setActive: (String, Boolean) -> Unit,
    error: (String?) -> Unit,
) {
    var add by rememberSaveable { mutableStateOf(false) }
    Button(onClick = { add = true }, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.Add, null); Text("  नई जगह बनाएं") }
    LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(custody.locations.sortedWith(compareByDescending<StorageLocation> { it.active }.thenBy { it.name }), key = { it.id }) { location ->
            val count = rows.count { row -> currentMovement(row.itemId, custody)?.let { it.destinationType == "LOCATION" && it.destinationId == location.id } == true }
            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(placeIcon(location.type) + " " + location.name, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text("$count सामान • ${location.detail.ifBlank { "कोई detail नहीं" }}", color = Color.Gray)
                    OutlinedButton(onClick = { runCatching { setActive(location.id, !location.active) }.onFailure { error(it.message) } }, modifier = Modifier.fillMaxWidth()) { Text(if (location.active) "इस जगह को बंद करें" else "फिर से चालू करें") }
                }
            }
        }
    }
    if (add) AddPlaceDialog({ add = false }) { name, type, detail -> runCatching { addLocation(name, type, detail) }.onSuccess { add = false; error(null) }.onFailure { error(it.message) } }
}

@Composable
private fun androidx.compose.foundation.layout.ColumnScope.OutsideTab(
    rows: List<ItemRow>, custody: CustodyPlacementSnapshot,
    addParty: (String, String, String, Int, String) -> Unit,
    placeOutside: (String, List<Pair<String, String>>, Long, Long, Int, ExternalInterestRule, String) -> Unit,
    addItems: (String, List<Pair<String, String>>, Long, String) -> Unit,
    moveItems: (List<Pair<String, String>>, String, Long, String) -> Unit,
    addAdvance: (String, Long, Int, Long, ExternalInterestRule, String) -> Unit,
    addPayment: (String, Long, Long, String) -> Unit,
    reversePayment: (String, String, Long, String) -> Unit,
    closeLot: (String, Long) -> Unit,
    error: (String?) -> Unit,
) {
    var addPartyDialog by rememberSaveable { mutableStateOf(false) }
    var newOutside by rememberSaveable { mutableStateOf(false) }
    var openLot by rememberSaveable { mutableStateOf<String?>(null) }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = { addPartyDialog = true }, modifier = Modifier.weight(1f)) { Text("+ जिसके पास रखें") }
        Button(onClick = { newOutside = true }, enabled = custody.parties.any { it.active } && rows.isNotEmpty(), modifier = Modifier.weight(1f)) { Text("+ बाहर रखें") }
    }
    LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item { Text("किसके पास सामान है", fontWeight = FontWeight.Bold, fontSize = 18.sp) }
        items(custody.lots.sortedByDescending { it.openedAt }, key = { it.id }) { lot ->
            val party = custody.parties.firstOrNull { it.id == lot.partyId }?.name ?: "Unknown"
            val active = lot.items.count { it.removedAt == null }
            Card(Modifier.fillMaxWidth().clickable { openLot = lot.id }, colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text("$party • ${formatDate(lot.openedAt)}", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    Text("$active सामान • Ref ${lot.lotNumber}", color = Color.Gray)
                    Text(if (lot.status == "ACTIVE") "Tap karke dekhein / manage karein" else "Band group", color = if (lot.status == "ACTIVE") CustodyGreen else Color.Gray)
                }
            }
        }
    }
    if (addPartyDialog) AddPartyDialog({ addPartyDialog = false }) { n, m, a, r, note -> runCatching { addParty(n, m, a, r, note) }.onSuccess { addPartyDialog = false; error(null) }.onFailure { error(it.message) } }
    if (newOutside) OutsideDialog(rows, custody, { newOutside = false }) { party, refs, at, amount, rate, rule, note -> runCatching { placeOutside(party, refs, at, amount, rate, rule, note) }.onSuccess { newOutside = false; error(null) }.onFailure { error(it.message) } }
    openLot?.let { id -> custody.lots.firstOrNull { it.id == id }?.let { lot -> SimpleLotDialog(lot, rows, custody, { openLot = null }, addItems, moveItems, addAdvance, addPayment, reversePayment, closeLot, error) } }
}

@Composable
private fun SimpleLotDialog(
    lot: PlacementLot, rows: List<ItemRow>, custody: CustodyPlacementSnapshot, dismiss: () -> Unit,
    addItems: (String, List<Pair<String, String>>, Long, String) -> Unit,
    moveItems: (List<Pair<String, String>>, String, Long, String) -> Unit,
    addAdvance: (String, Long, Int, Long, ExternalInterestRule, String) -> Unit,
    addPayment: (String, Long, Long, String) -> Unit,
    reversePayment: (String, String, Long, String) -> Unit,
    closeLot: (String, Long) -> Unit,
    error: (String?) -> Unit,
) {
    val party = custody.parties.firstOrNull { it.id == lot.partyId }?.name ?: "Party"
    val activeIds = lot.items.filter { it.removedAt == null }.map { it.itemId }.toSet()
    val activeRows = rows.filter { it.itemId in activeIds }
    val projection = runCatching { ExternalPlacementLedger.project(lot.fundingAdvances, lot.fundingPayments, todayNoon()) }.getOrNull()
    var showFinance by rememberSaveable(lot.id) { mutableStateOf(false) }
    var returnRow by remember { mutableStateOf<ItemRow?>(null) }
    var addMoreItems by rememberSaveable(lot.id) { mutableStateOf(false) }
    var addMoney by rememberSaveable(lot.id) { mutableStateOf(false) }
    var pay by rememberSaveable(lot.id) { mutableStateOf(false) }
    var reverseId by rememberSaveable(lot.id) { mutableStateOf<String?>(null) }

    AlertDialog(onDismissRequest = dismiss, title = { Text(party) }, text = {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item { Text("${formatDate(lot.openedAt)} • ${activeRows.size} सामान • Ref ${lot.lotNumber}", color = Color.Gray) }
            items(activeRows, key = { it.itemId }) { row ->
                Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(9.dp)) {
                    Text("${row.customerName} • ${row.girviNumber}", fontWeight = FontWeight.Bold); Text(row.itemName)
                    if (lot.status == "ACTIVE") TextButton(onClick = { returnRow = row }) { Text("वापस लें / दूसरी जगह रखें") }
                } }
            }
            if (lot.status == "ACTIVE") item { OutlinedButton(onClick = { addMoreItems = true }, modifier = Modifier.fillMaxWidth()) { Text("+ और सामान जोड़ें") } }
            item { Button(onClick = { showFinance = !showFinance }, modifier = Modifier.fillMaxWidth()) { Text(if (showFinance) "🔒 पैसे का हिसाब छुपाएं" else "🔐 पैसे का हिसाब देखें") } }
            if (showFinance) {
                item { Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E1))) { Column(Modifier.padding(10.dp)) {
                    Text("Owner का private हिसाब", fontWeight = FontWeight.Bold)
                    Text("वहाँ से कुल मिले: ${money(projection?.totalAdvancedPaise ?: 0)}")
                    Text("Principal बाकी: ${money(projection?.principalOutstandingPaise ?: 0)}")
                    Text("Interest बाकी: ${money(projection?.interestOutstandingPaise ?: 0)}")
                    Text("आज कुल देना: ${money(projection?.totalDuePaise ?: 0)}", fontWeight = FontWeight.Bold)
                } } }
                if (lot.status == "ACTIVE") item { Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Button(onClick = { addMoney = true }, modifier = Modifier.weight(1f)) { Text("और पैसे मिले") }
                    Button(onClick = { pay = true }, modifier = Modifier.weight(1f), enabled = (projection?.totalDuePaise ?: 0) > 0) { Text("पैसे चुकाएं") }
                } }
                items(lot.fundingPayments.sortedByDescending { it.createdAt }, key = { it.id }) { p ->
                    val reversed = lot.fundingPayments.any { it.isReversal && it.reversedPaymentId == p.id }
                    Text("${formatDate(p.createdAt)} • ${money(p.amountPaise)}${if (p.isReversal) " • Reverse" else ""}")
                    if (!p.isReversal && !reversed && lot.status == "ACTIVE") TextButton(onClick = { reverseId = p.id }) { Text("गलती सुधारें") }
                }
            }
            if (lot.status == "ACTIVE" && activeRows.isEmpty() && (projection?.totalDuePaise ?: Long.MAX_VALUE) == 0L) item { Button(onClick = { closeLot(lot.id, todayNoon()) }, modifier = Modifier.fillMaxWidth()) { Text("यह group बंद करें") } }
        }
    }, confirmButton = { TextButton(onClick = dismiss) { Text("Done") } })

    returnRow?.let { row -> MoveDialog(listOf(row), custody.locations.filter { it.active }, { returnRow = null }) { location, at, note -> runCatching { moveItems(listOf(row.girviId to row.itemId), location, at, note) }.onSuccess { returnRow = null }.onFailure { error(it.message) } } }
    if (addMoreItems) SelectItemsDialog(rows.filter { it.itemId !in custody.lots.flatMap { l -> l.items.filter { i -> i.removedAt == null }.map { i -> i.itemId } }.toSet() }, "और सामान चुनें", { addMoreItems = false }) { refs, at, note -> runCatching { addItems(lot.id, refs, at, note) }.onSuccess { addMoreItems = false }.onFailure { error(it.message) } }
    if (addMoney) MoneyDialog("वहाँ से और पैसे मिले", lot.openedAt, true, lot.monthlyRateBasisPoints, { addMoney = false }) { amount, rate, at, rule, note -> runCatching { addAdvance(lot.id, amount, rate, at, rule, note) }.onSuccess { addMoney = false }.onFailure { error(it.message) } }
    if (pay && projection != null) PaymentDialog(projection.totalDuePaise, lot.openedAt, { pay = false }) { amount, at, note -> runCatching { addPayment(lot.id, amount, at, note) }.onSuccess { pay = false }.onFailure { error(it.message) } }
    reverseId?.let { pid -> ReasonDialog("Payment ki galti sudharein", { reverseId = null }) { at, reason -> runCatching { reversePayment(lot.id, pid, at, reason) }.onSuccess { reverseId = null }.onFailure { error(it.message) } } }
}

@Composable
private fun AddPlaceDialog(dismiss: () -> Unit, save: (String, String, String) -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }; var type by rememberSaveable { mutableStateOf("SHOP") }; var detail by rememberSaveable { mutableStateOf("") }
    AlertDialog(onDismissRequest = dismiss, title = { Text("नई जगह बनाएं") }, text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(name, { name = it.take(80) }, label = { Text("जगह का नाम *") }, modifier = Modifier.fillMaxWidth())
        Row { listOf("SHOP" to "दुकान", "HOME" to "घर", "BANK" to "Bank", "OTHER" to "अन्य").forEach { (v,l) -> TextButton(onClick = { type = v }) { Text(if (type == v) "✓ $l" else l) } } }
        OutlinedTextField(detail, { detail = it.take(200) }, label = { Text("Locker / Box / Address detail") }, modifier = Modifier.fillMaxWidth())
    } }, confirmButton = { TextButton(onClick = { save(name, type, detail) }) { Text("Save") } }, dismissButton = { TextButton(onClick = dismiss) { Text("Cancel") } })
}

@Composable
private fun MoveDialog(rows: List<ItemRow>, locations: List<StorageLocation>, dismiss: () -> Unit, save: (String, Long, String) -> Unit) {
    val context = LocalContext.current; var location by rememberSaveable { mutableStateOf(locations.firstOrNull()?.id.orEmpty()) }; var at by rememberSaveable { mutableStateOf(todayNoon()) }; var note by rememberSaveable { mutableStateOf("") }
    AlertDialog(onDismissRequest = dismiss, title = { Text("अब कहाँ रखना है?") }, text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(rows.joinToString { it.itemName }, color = Color.Gray)
        if (locations.isEmpty()) Text("पहले नई जगह बनाएं", color = MaterialTheme.colorScheme.error)
        locations.forEach { p -> OutlinedButton(onClick = { location = p.id }, modifier = Modifier.fillMaxWidth()) { Text(if (location == p.id) "✓ ${p.name}" else p.name) } }
        OutlinedButton(onClick = { pickDate(context, at) { at = it } }) { Text("तारीख: ${formatDate(at)}") }
        OutlinedTextField(note, { note = it.take(250) }, label = { Text("Note") }, modifier = Modifier.fillMaxWidth())
    } }, confirmButton = { TextButton(onClick = { save(location, at, note) }, enabled = location.isNotBlank()) { Text("जगह Save करें") } }, dismissButton = { TextButton(onClick = dismiss) { Text("Cancel") } })
}

@Composable
private fun AddPartyDialog(dismiss: () -> Unit, save: (String, String, String, Int, String) -> Unit) {
    var n by rememberSaveable { mutableStateOf("") }; var m by rememberSaveable { mutableStateOf("") }; var a by rememberSaveable { mutableStateOf("") }; var r by rememberSaveable { mutableStateOf("") }; var note by rememberSaveable { mutableStateOf("") }
    AlertDialog(onDismissRequest = dismiss, title = { Text("जिसके पास सामान रखते हैं") }, text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(n, { n = it.take(80) }, label = { Text("नाम *") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(m, { m = it.filter(Char::isDigit).take(10) }, label = { Text("Mobile") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), modifier = Modifier.fillMaxWidth())
        OutlinedTextField(a, { a = it.take(200) }, label = { Text("Address") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(r, { r = decimalInput(it) }, label = { Text("Default ब्याज % / month") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
        OutlinedTextField(note, { note = it.take(250) }, label = { Text("Note") }, modifier = Modifier.fillMaxWidth())
    } }, confirmButton = { TextButton(onClick = { save(n, m, a, percentToBps(r), note) }) { Text("Save") } }, dismissButton = { TextButton(onClick = dismiss) { Text("Cancel") } })
}

@Composable
private fun OutsideDialog(rows: List<ItemRow>, custody: CustodyPlacementSnapshot, dismiss: () -> Unit, save: (String, List<Pair<String,String>>, Long, Long, Int, ExternalInterestRule, String) -> Unit) {
    val context = LocalContext.current
    val activeParties = custody.parties.filter { it.active }
    val activeOutside = custody.lots.flatMap { it.items.filter { x -> x.removedAt == null }.map { x -> x.itemId } }.toSet()
    val selectable = rows.filter { it.itemId !in activeOutside }
    var party by rememberSaveable { mutableStateOf(activeParties.firstOrNull()?.id.orEmpty()) }
    var selected by remember { mutableStateOf(if (rows.size == 1 && selectable.isNotEmpty()) setOf(selectable.first().itemId) else emptySet()) }
    var at by rememberSaveable { mutableStateOf(todayNoon()) }; var amount by rememberSaveable { mutableStateOf("") }; var rate by rememberSaveable { mutableStateOf("") }; var rule by rememberSaveable { mutableStateOf(ExternalInterestRule.EXACT_DAYS) }; var note by rememberSaveable { mutableStateOf("") }
    AlertDialog(onDismissRequest = dismiss, title = { Text("सामान बाहर रखें") }, text = { LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item { Text("किसके पास?", fontWeight = FontWeight.Bold) }
        items(activeParties, key = { it.id }) { p -> OutlinedButton(onClick = { party = p.id; rate = (p.defaultMonthlyRateBasisPoints / 100.0).toString() }, modifier = Modifier.fillMaxWidth()) { Text(if (party == p.id) "✓ ${p.name}" else p.name) } }
        item { Text("कौन सा सामान? (${selected.size})", fontWeight = FontWeight.Bold) }
        items(selectable, key = { it.itemId }) { row -> SelectableRow(row, row.itemId in selected) { checked -> selected = if (checked) selected + row.itemId else selected - row.itemId } }
        item { OutlinedTextField(amount, { amount = decimalInput(it) }, label = { Text("वहाँ से कितने रुपये मिले ₹") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth()) }
        item { OutlinedTextField(rate, { rate = decimalInput(it) }, label = { Text("ब्याज % प्रति माह") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth()) }
        item { RuleChooser(rule) { rule = it } }
        item { OutlinedButton(onClick = { pickDate(context, at) { at = it } }) { Text("तारीख: ${formatDate(at)}") } }
        item { OutlinedTextField(note, { note = it.take(250) }, label = { Text("Note") }, modifier = Modifier.fillMaxWidth()) }
    } }, confirmButton = { TextButton(onClick = { save(party, selectable.filter { it.itemId in selected }.map { it.girviId to it.itemId }, at, rupeesToPaiseAllowZero(amount), percentToBps(rate), rule, note) }, enabled = party.isNotBlank() && selected.isNotEmpty()) { Text("Entry Save करें") } }, dismissButton = { TextButton(onClick = dismiss) { Text("Cancel") } })
}

@Composable
private fun SelectItemsDialog(rows: List<ItemRow>, title: String, dismiss: () -> Unit, save: (List<Pair<String,String>>, Long, String) -> Unit) {
    val context = LocalContext.current; var selected by remember { mutableStateOf(setOf<String>()) }; var at by rememberSaveable { mutableStateOf(todayNoon()) }; var note by rememberSaveable { mutableStateOf("") }
    AlertDialog(onDismissRequest = dismiss, title = { Text(title) }, text = { LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        items(rows, key = { it.itemId }) { row -> SelectableRow(row, row.itemId in selected) { checked -> selected = if (checked) selected + row.itemId else selected - row.itemId } }
        item { OutlinedButton(onClick = { pickDate(context, at) { at = it } }) { Text("तारीख: ${formatDate(at)}") } }
        item { OutlinedTextField(note, { note = it.take(250) }, label = { Text("Note") }, modifier = Modifier.fillMaxWidth()) }
    } }, confirmButton = { TextButton(onClick = { save(rows.filter { it.itemId in selected }.map { it.girviId to it.itemId }, at, note) }, enabled = selected.isNotEmpty()) { Text("जोड़ें") } }, dismissButton = { TextButton(onClick = dismiss) { Text("Cancel") } })
}

@Composable
private fun MoneyDialog(title: String, openedAt: Long, withRate: Boolean, defaultRate: Int, dismiss: () -> Unit, save: (Long, Int, Long, ExternalInterestRule, String) -> Unit) {
    val context = LocalContext.current; var amount by rememberSaveable { mutableStateOf("") }; var rate by rememberSaveable { mutableStateOf((defaultRate / 100.0).toString()) }; var at by rememberSaveable { mutableStateOf(todayNoon().coerceAtLeast(openedAt)) }; var rule by rememberSaveable { mutableStateOf(ExternalInterestRule.EXACT_DAYS) }; var note by rememberSaveable { mutableStateOf("") }
    AlertDialog(onDismissRequest = dismiss, title = { Text(title) }, text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(amount, { amount = decimalInput(it) }, label = { Text("Amount ₹") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
        if (withRate) OutlinedTextField(rate, { rate = decimalInput(it) }, label = { Text("ब्याज % / month") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
        RuleChooser(rule) { rule = it }; OutlinedButton(onClick = { pickDate(context, at) { if (it >= openedAt) at = it } }) { Text("तारीख: ${formatDate(at)}") }; OutlinedTextField(note, { note = it.take(250) }, label = { Text("Note") }, modifier = Modifier.fillMaxWidth())
    } }, confirmButton = { TextButton(onClick = { save(rupeesToPaisePositive(amount), percentToBps(rate), at, rule, note) }) { Text("Save") } }, dismissButton = { TextButton(onClick = dismiss) { Text("Cancel") } })
}

@Composable
private fun PaymentDialog(totalDue: Long, openedAt: Long, dismiss: () -> Unit, save: (Long, Long, String) -> Unit) {
    val context = LocalContext.current; var amount by rememberSaveable { mutableStateOf("") }; var at by rememberSaveable { mutableStateOf(todayNoon().coerceAtLeast(openedAt)) }; var note by rememberSaveable { mutableStateOf("") }
    AlertDialog(onDismissRequest = dismiss, title = { Text("वहाँ पैसे चुकाएं") }, text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("आज कुल बाकी: ${money(totalDue)}", fontWeight = FontWeight.Bold); OutlinedTextField(amount, { amount = decimalInput(it) }, label = { Text("Payment ₹") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth()); OutlinedButton(onClick = { pickDate(context, at) { if (it >= openedAt) at = it } }) { Text("तारीख: ${formatDate(at)}") }; OutlinedTextField(note, { note = it.take(250) }, label = { Text("Note / reference") }, modifier = Modifier.fillMaxWidth())
    } }, confirmButton = { TextButton(onClick = { save(rupeesToPaisePositive(amount), at, note) }) { Text("Payment Save") } }, dismissButton = { TextButton(onClick = dismiss) { Text("Cancel") } })
}

@Composable
private fun ReasonDialog(title: String, dismiss: () -> Unit, save: (Long, String) -> Unit) {
    val context = LocalContext.current; var reason by rememberSaveable { mutableStateOf("") }; var at by rememberSaveable { mutableStateOf(todayNoon()) }
    AlertDialog(onDismissRequest = dismiss, title = { Text(title) }, text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { OutlinedButton(onClick = { pickDate(context, at) { at = it } }) { Text("तारीख: ${formatDate(at)}") }; OutlinedTextField(reason, { reason = it.take(220) }, label = { Text("Reason *") }, modifier = Modifier.fillMaxWidth()) } }, confirmButton = { TextButton(onClick = { save(at, reason) }, enabled = reason.trim().length >= 3) { Text("Confirm") } }, dismissButton = { TextButton(onClick = dismiss) { Text("Cancel") } })
}

@Composable
private fun RuleChooser(selected: ExternalInterestRule, changed: (ExternalInterestRule) -> Unit) = Column {
    Text("ब्याज का नियम", fontWeight = FontWeight.Bold)
    ExternalInterestRule.entries.forEach { r -> TextButton(onClick = { changed(r) }) { Text(if (selected == r) "✓ ${ruleLabel(r)}" else ruleLabel(r)) } }
}

@Composable
private fun SelectableRow(row: ItemRow, checked: Boolean, changed: (Boolean) -> Unit) = Row(Modifier.fillMaxWidth().clickable { changed(!checked) }.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
    Checkbox(checked, changed); Column { Text("${row.customerName} • ${row.girviNumber}", fontWeight = FontWeight.Bold); Text(row.itemName, color = Color.Gray) }
}

private fun currentMovement(itemId: String, s: CustodyPlacementSnapshot) = s.movements.filter { it.itemId == itemId }.maxWithOrNull(compareBy<com.girvikhata.app.custody.CustodyMovement> { it.movedAt }.thenBy { it.createdAt })
private fun currentText(itemId: String, s: CustodyPlacementSnapshot): String {
    val m = currentMovement(itemId, s) ?: return "जगह तय नहीं"
    return when (m.destinationType) {
        "LOCATION" -> s.locations.firstOrNull { it.id == m.destinationId }?.name ?: "Unknown"
        "EXTERNAL" -> {
            val p = s.parties.firstOrNull { it.id == m.destinationId }?.name ?: "बाहर"
            val lot = s.lots.firstOrNull { it.id == m.lotId }?.lotNumber
            if (lot.isNullOrBlank()) p else "$p • $lot"
        }
        else -> "Unknown"
    }
}
private fun placeIcon(type: String) = when (type) { "HOME" -> "🏠"; "SHOP" -> "🏪"; "BANK" -> "🏦"; else -> "📦" }
private fun formatDate(value: Long): String = DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(value))
private fun money(paise: Long): String = "₹" + BigDecimal.valueOf(paise, 2).setScale(2).toPlainString()
private fun ruleLabel(rule: ExternalInterestRule) = when (rule) { ExternalInterestRule.EXACT_DAYS -> "Exact days"; ExternalInterestRule.STARTED_MONTH_FULL -> "Started month full"; ExternalInterestRule.COMPLETED_MONTHS_PLUS_DAYS -> "Month + बाकी days" }
private fun decimalInput(value: String): String { var dot = false; return buildString { value.forEach { c -> if (c.isDigit()) append(c) else if (c == '.' && !dot) { append(c); dot = true } } }.take(14) }
private fun percentToBps(value: String): Int { val d = value.trim().toBigDecimalOrNull() ?: BigDecimal.ZERO; require(d >= BigDecimal.ZERO && d <= BigDecimal(1000)); return d.multiply(BigDecimal(100)).setScale(0, RoundingMode.HALF_UP).intValueExact() }
private fun rupeesToPaiseAllowZero(value: String): Long { val d = value.trim().toBigDecimalOrNull() ?: BigDecimal.ZERO; require(d >= BigDecimal.ZERO); return d.multiply(BigDecimal(100)).setScale(0, RoundingMode.HALF_UP).longValueExact() }
private fun rupeesToPaisePositive(value: String): Long = rupeesToPaiseAllowZero(value).also { require(it > 0) { "Positive amount required" } }
private fun todayNoon(): Long = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, 12); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }.timeInMillis
private fun pickDate(context: android.content.Context, current: Long, selected: (Long) -> Unit) { val c = Calendar.getInstance().apply { timeInMillis = current }; DatePickerDialog(context, { _, y, m, d -> selected(Calendar.getInstance().apply { clear(); set(y, m, d, 12, 0, 0) }.timeInMillis) }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show() }
