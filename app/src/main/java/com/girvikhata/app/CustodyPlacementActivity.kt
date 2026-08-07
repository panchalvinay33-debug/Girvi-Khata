package com.girvikhata.app

import android.app.DatePickerDialog
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MoveToInbox
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DateFormat
import java.util.Calendar
import java.util.Date

class CustodyPlacementActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val businessStore = EncryptedRecordStore(applicationContext)
        val custodyStore = CustodyPlacementStore(applicationContext)
        val journal = DataSafetyJournal(applicationContext)
        setContent {
            MaterialTheme {
                var business by remember { mutableStateOf(businessStore.load()) }
                var custody by remember { mutableStateOf(custodyStore.load()) }
                CustodyPlacementScreen(
                    business = business,
                    custody = custody,
                    refresh = {
                        business = businessStore.load()
                        custody = custodyStore.load()
                    },
                    addLocation = { name, type, detail ->
                        custody = custodyStore.addLocation(name, type, detail)
                        journal.recordNamedEvent("LOCATION_CREATED", "Storage location created", name.trim())
                    },
                    setLocationActive = { id, active ->
                        custody = custodyStore.setLocationActive(id, active)
                        val name = custody.locations.firstOrNull { it.id == id }?.name ?: "Storage location"
                        journal.recordNamedEvent("LOCATION_UPDATED", "Storage location ${if (active) "enabled" else "disabled"}", name)
                    },
                    moveItem = { girviId, itemId, locationId, movedAt, note ->
                        custody = custodyStore.moveToLocation(girviId, itemId, locationId, movedAt, note)
                        val location = custody.locations.firstOrNull { it.id == locationId }?.name ?: "Storage location"
                        journal.recordNamedEvent("ITEM_MOVED", "Girvi item moved", "$girviId • $itemId → $location")
                    },
                    addParty = { name, mobile, address, rateBps, note ->
                        custody = custodyStore.addParty(name, mobile, address, rateBps, note)
                        journal.recordNamedEvent("EXTERNAL_PARTY_CREATED", "External party created", name.trim())
                    },
                    createLot = { lotNumber, partyId, refs, openedAt, amount, rateBps, rule, note ->
                        custody = custodyStore.createLot(lotNumber, partyId, refs, openedAt, amount, rateBps, note, rule)
                        val party = custody.parties.firstOrNull { it.id == partyId }?.name ?: "External party"
                        journal.recordNamedEvent("LOT_CREATED", "External placement lot created", "$lotNumber • $party • ${refs.size} items")
                    },
                    addItemsToLot = { lotId, refs, at, note ->
                        custody = custodyStore.addItemsToLot(lotId, refs, at, note)
                        val lot = custody.lots.first { it.id == lotId }
                        journal.recordNamedEvent("ITEMS_ADDED_TO_LOT", "Items added to external lot", "${lot.lotNumber} • ${refs.size} items")
                    },
                    addExternalAdvance = { lotId, amount, rate, at, rule, note ->
                        custody = custodyStore.addExternalAdvance(lotId, amount, rate, at, rule, note)
                        val lot = custody.lots.first { it.id == lotId }
                        journal.recordNamedEvent("EXTERNAL_ADVANCE", "External funding added", "${lot.lotNumber} • ${money(amount)}")
                    },
                    addExternalPayment = { lotId, amount, at, note ->
                        custody = custodyStore.addExternalPayment(lotId, amount, at, note)
                        val lot = custody.lots.first { it.id == lotId }
                        journal.recordNamedEvent("EXTERNAL_PAYMENT", "External party payment", "${lot.lotNumber} • ${money(amount)}")
                    },
                    reverseExternalPayment = { lotId, paymentId, at, reason ->
                        custody = custodyStore.reverseExternalPayment(lotId, paymentId, at, reason)
                        val lot = custody.lots.first { it.id == lotId }
                        journal.recordNamedEvent("EXTERNAL_PAYMENT_REVERSED", "External payment reversed", "${lot.lotNumber} • ${reason.trim().take(100)}")
                    },
                    closeLot = { lotId, at ->
                        custody = custodyStore.closeLot(lotId, at)
                        val lot = custody.lots.first { it.id == lotId }
                        journal.recordNamedEvent("LOT_CLOSED", "External placement lot closed", lot.lotNumber)
                    },
                    close = ::finish,
                )
            }
        }
    }
}

private enum class CustodyTab { ITEMS, LOCATIONS, EXTERNAL }

private data class ItemRow(
    val girviId: String,
    val girviNumber: String,
    val customerName: String,
    val itemId: String,
    val itemName: String,
)

@Composable
private fun CustodyPlacementScreen(
    business: AppSnapshot,
    custody: CustodyPlacementSnapshot,
    refresh: () -> Unit,
    addLocation: (String, String, String) -> Unit,
    setLocationActive: (String, Boolean) -> Unit,
    moveItem: (String, String, String, Long, String) -> Unit,
    addParty: (String, String, String, Int, String) -> Unit,
    createLot: (String, String, List<Pair<String, String>>, Long, Long, Int, ExternalInterestRule, String) -> Unit,
    addItemsToLot: (String, List<Pair<String, String>>, Long, String) -> Unit,
    addExternalAdvance: (String, Long, Int, Long, ExternalInterestRule, String) -> Unit,
    addExternalPayment: (String, Long, Long, String) -> Unit,
    reverseExternalPayment: (String, String, Long, String) -> Unit,
    closeLot: (String, Long) -> Unit,
    close: () -> Unit,
) {
    var tab by rememberSaveable { mutableStateOf(CustodyTab.ITEMS) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    val itemRows = remember(business) {
        business.girvis.flatMap { girvi ->
            girvi.effectiveItems.map { item -> ItemRow(girvi.id, girvi.girviNumber, girvi.customerName, item.id, item.itemName) }
        }
    }
    Scaffold(
        containerColor = Color(0xFFF6F7FB),
        bottomBar = {
            NavigationBar {
                NavigationBarItem(tab == CustodyTab.ITEMS, { tab = CustodyTab.ITEMS }, { Icon(Icons.Default.Inventory2, null) }, label = { Text("Items") })
                NavigationBarItem(tab == CustodyTab.LOCATIONS, { tab = CustodyTab.LOCATIONS }, { Icon(Icons.Default.LocationOn, null) }, label = { Text("Lockers") })
                NavigationBarItem(tab == CustodyTab.EXTERNAL, { tab = CustodyTab.EXTERNAL }, { Icon(Icons.Default.Store, null) }, label = { Text("External") })
            }
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Storage & Placement", fontSize = 27.sp, fontWeight = FontWeight.Bold, color = Color(0xFF171752))
            Text("Item custody + owner-only external placement ledger", color = Color.Gray)
            error?.let { Text(it, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold) }
            when (tab) {
                CustodyTab.ITEMS -> ItemCustodyTab(itemRows, custody, moveItem) { error = it }
                CustodyTab.LOCATIONS -> LocationTab(custody.locations, addLocation, setLocationActive) { error = it }
                CustodyTab.EXTERNAL -> ExternalTab(
                    itemRows, custody, addParty, createLot, addItemsToLot, moveItem,
                    addExternalAdvance, addExternalPayment, reverseExternalPayment, closeLot,
                ) { error = it }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = refresh, modifier = Modifier.weight(1f)) { Text("Refresh") }
                OutlinedButton(onClick = close, modifier = Modifier.weight(1f)) { Text("Close") }
            }
        }
    }
}

@Composable
private fun ColumnScope.ItemCustodyTab(
    rows: List<ItemRow>,
    custody: CustodyPlacementSnapshot,
    moveItem: (String, String, String, Long, String) -> Unit,
    error: (String?) -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var moving by remember { mutableStateOf<ItemRow?>(null) }
    OutlinedTextField(query, { query = it }, label = { Text("Customer / Girvi / Item search") }, modifier = Modifier.fillMaxWidth())
    val q = query.trim().lowercase()
    val filtered = rows.filter { q.isBlank() || it.customerName.lowercase().contains(q) || it.girviNumber.lowercase().contains(q) || it.itemName.lowercase().contains(q) }
    LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(filtered, key = { it.itemId }) { row ->
            val current = currentCustodyText(row.itemId, custody)
            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("${row.girviNumber} • ${row.customerName}", fontWeight = FontWeight.Bold)
                    Text(row.itemName)
                    Text("Current: $current", color = if (current == "Not assigned") Color(0xFF9A6700) else Color(0xFF138A4A), fontWeight = FontWeight.Bold)
                    custody.movements.filter { it.itemId == row.itemId }.sortedByDescending { it.movedAt }.take(3).forEach { movement ->
                        Text("${formatDate(movement.movedAt)} • ${movementDestinationText(movement.destinationType, movement.destinationId, movement.lotId, custody)}", color = Color.Gray, fontSize = 11.sp)
                    }
                    Button(onClick = { moving = row }, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.MoveToInbox, null); Text("  Move / Locker Set Karein") }
                }
            }
        }
    }
    moving?.let { row ->
        MoveItemDialog(row, custody.locations.filter { it.active }, { moving = null }) { locationId, date, note ->
            runCatching { moveItem(row.girviId, row.itemId, locationId, date, note) }
                .onSuccess { moving = null; error(null) }
                .onFailure { error(it.message ?: "Item move nahi hua") }
        }
    }
}

@Composable
private fun ColumnScope.LocationTab(
    locations: List<StorageLocation>,
    addLocation: (String, String, String) -> Unit,
    setActive: (String, Boolean) -> Unit,
    error: (String?) -> Unit,
) {
    var add by rememberSaveable { mutableStateOf(false) }
    Button(onClick = { add = true }, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.Add, null); Text("  Locker / Storage Location Add Karein") }
    LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(locations.sortedWith(compareByDescending<StorageLocation> { it.active }.thenBy { it.name.lowercase() }), key = { it.id }) { location ->
            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(location.name, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    Text("${location.type} • ${location.detail.ifBlank { "No detail" }}", color = Color.Gray)
                    Text(if (location.active) "ACTIVE" else "INACTIVE", color = if (location.active) Color(0xFF138A4A) else Color.Gray, fontWeight = FontWeight.Bold)
                    OutlinedButton(onClick = {
                        runCatching { setActive(location.id, !location.active) }.onSuccess { error(null) }.onFailure { error(it.message) }
                    }, modifier = Modifier.fillMaxWidth()) { Text(if (location.active) "Disable" else "Enable") }
                }
            }
        }
    }
    if (add) AddLocationDialog({ add = false }) { name, type, detail ->
        runCatching { addLocation(name, type, detail) }.onSuccess { add = false; error(null) }.onFailure { error(it.message ?: "Location save nahi hui") }
    }
}

@Composable
private fun ColumnScope.ExternalTab(
    rows: List<ItemRow>,
    custody: CustodyPlacementSnapshot,
    addParty: (String, String, String, Int, String) -> Unit,
    createLot: (String, String, List<Pair<String, String>>, Long, Long, Int, ExternalInterestRule, String) -> Unit,
    addItemsToLot: (String, List<Pair<String, String>>, Long, String) -> Unit,
    moveItem: (String, String, String, Long, String) -> Unit,
    addExternalAdvance: (String, Long, Int, Long, ExternalInterestRule, String) -> Unit,
    addExternalPayment: (String, Long, Long, String) -> Unit,
    reverseExternalPayment: (String, String, Long, String) -> Unit,
    closeLot: (String, Long) -> Unit,
    error: (String?) -> Unit,
) {
    var addPartyDialog by rememberSaveable { mutableStateOf(false) }
    var lotDialog by rememberSaveable { mutableStateOf(false) }
    var selectedLotId by rememberSaveable { mutableStateOf<String?>(null) }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = { addPartyDialog = true }, modifier = Modifier.weight(1f)) { Text("+ Party") }
        Button(onClick = { lotDialog = true }, enabled = custody.parties.any { it.active } && rows.isNotEmpty(), modifier = Modifier.weight(1f)) { Text("+ Lot") }
    }
    LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item { Text("External Parties", fontWeight = FontWeight.Bold, fontSize = 18.sp) }
        items(custody.parties, key = { "p-${it.id}" }) { party ->
            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(Modifier.padding(12.dp)) {
                    Text(party.name, fontWeight = FontWeight.Bold)
                    Text(party.mobile.ifBlank { "No mobile" }, color = Color.Gray)
                    Text("Finance defaults hidden", color = Color.Gray, fontSize = 11.sp)
                }
            }
        }
        item { Text("Placement Lots", fontWeight = FontWeight.Bold, fontSize = 18.sp) }
        items(custody.lots.sortedByDescending { it.openedAt }, key = { "l-${it.id}" }) { lot ->
            LotCard(lot, custody) { selectedLotId = lot.id }
        }
    }
    if (addPartyDialog) AddPartyDialog({ addPartyDialog = false }) { name, mobile, address, rateBps, note ->
        runCatching { addParty(name, mobile, address, rateBps, note) }.onSuccess { addPartyDialog = false; error(null) }.onFailure { error(it.message ?: "Party save nahi hui") }
    }
    if (lotDialog) CreateLotDialog(rows, custody, { lotDialog = false }) { number, partyId, refs, openedAt, amount, rateBps, rule, note ->
        runCatching { createLot(number, partyId, refs, openedAt, amount, rateBps, rule, note) }.onSuccess { lotDialog = false; error(null) }.onFailure { error(it.message ?: "Lot create nahi hua") }
    }
    selectedLotId?.let { id ->
        custody.lots.firstOrNull { it.id == id }?.let { lot ->
            LotDetailDialog(
                lot = lot,
                rows = rows,
                custody = custody,
                dismiss = { selectedLotId = null },
                addItems = { refs, at, note -> runCatching { addItemsToLot(id, refs, at, note) }.onFailure { error(it.message) } },
                moveItem = { row, locationId, at, note -> runCatching { moveItem(row.girviId, row.itemId, locationId, at, note) }.onFailure { error(it.message) } },
                addAdvance = { amount, rate, at, rule, note -> runCatching { addExternalAdvance(id, amount, rate, at, rule, note) }.onFailure { error(it.message) } },
                addPayment = { amount, at, note -> runCatching { addExternalPayment(id, amount, at, note) }.onFailure { error(it.message) } },
                reversePayment = { paymentId, at, reason -> runCatching { reverseExternalPayment(id, paymentId, at, reason) }.onFailure { error(it.message) } },
                closeLot = { at -> runCatching { closeLot(id, at) }.onSuccess { selectedLotId = null }.onFailure { error(it.message) } },
            )
        }
    }
}

@Composable
private fun LotCard(lot: PlacementLot, custody: CustodyPlacementSnapshot, open: () -> Unit) {
    val party = custody.parties.firstOrNull { it.id == lot.partyId }?.name ?: "Unknown party"
    val activeItems = lot.items.count { it.removedAt == null }
    Card(Modifier.fillMaxWidth().clickable(onClick = open), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(lot.lotNumber, fontWeight = FontWeight.Bold, fontSize = 17.sp)
            Text(party)
            Text("${formatDate(lot.openedAt)} • $activeItems active items", color = Color.Gray)
            Text("Owner finance hidden • Tap lot to manage", color = Color(0xFF7A4D00), fontWeight = FontWeight.Bold)
            Text(lot.status, color = if (lot.status == "ACTIVE") Color(0xFF138A4A) else Color.Gray)
        }
    }
}

@Composable
private fun LotDetailDialog(
    lot: PlacementLot,
    rows: List<ItemRow>,
    custody: CustodyPlacementSnapshot,
    dismiss: () -> Unit,
    addItems: (List<Pair<String, String>>, Long, String) -> Unit,
    moveItem: (ItemRow, String, Long, String) -> Unit,
    addAdvance: (Long, Int, Long, ExternalInterestRule, String) -> Unit,
    addPayment: (Long, Long, String) -> Unit,
    reversePayment: (String, Long, String) -> Unit,
    closeLot: (Long) -> Unit,
) {
    var showFinance by rememberSaveable(lot.id) { mutableStateOf(false) }
    var addItemsDialog by rememberSaveable(lot.id) { mutableStateOf(false) }
    var moveRow by remember { mutableStateOf<ItemRow?>(null) }
    var addAdvanceDialog by rememberSaveable(lot.id) { mutableStateOf(false) }
    var addPaymentDialog by rememberSaveable(lot.id) { mutableStateOf(false) }
    var reversePaymentId by rememberSaveable(lot.id) { mutableStateOf<String?>(null) }
    val party = custody.parties.firstOrNull { it.id == lot.partyId }?.name ?: "Unknown party"
    val projection = runCatching { ExternalPlacementLedger.project(lot.fundingAdvances, lot.fundingPayments, todayNoon()) }.getOrNull()
    val activeItemIds = lot.items.filter { it.removedAt == null }.map { it.itemId }.toSet()
    val activeRows = rows.filter { it.itemId in activeItemIds }
    AlertDialog(
        onDismissRequest = dismiss,
        title = { Text("${lot.lotNumber} • $party") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item { Text("Opened ${formatDate(lot.openedAt)} • ${lot.status}", color = Color.Gray) }
                item { Text("Active Items (${activeRows.size})", fontWeight = FontWeight.Bold) }
                items(activeRows, key = { "lot-item-${it.itemId}" }) { row ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(9.dp)) {
                            Text("${row.girviNumber} • ${row.customerName}", fontWeight = FontWeight.Bold)
                            Text(row.itemName)
                            if (lot.status == "ACTIVE") TextButton(onClick = { moveRow = row }) { Text("Return / Move to Locker") }
                        }
                    }
                }
                if (lot.status == "ACTIVE") item {
                    OutlinedButton(onClick = { addItemsDialog = true }, modifier = Modifier.fillMaxWidth()) { Text("+ Add Items to Lot") }
                }
                item {
                    Button(onClick = { showFinance = !showFinance }, modifier = Modifier.fillMaxWidth()) {
                        Text(if (showFinance) "Hide Owner Finance" else "Show Owner Finance")
                    }
                }
                if (showFinance) {
                    item {
                        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E1))) {
                            Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Text("🔐 Owner Finance", fontWeight = FontWeight.Bold)
                                Text("Total funding: ${money(projection?.totalAdvancedPaise ?: 0L)}")
                                Text("Principal due: ${money(projection?.principalOutstandingPaise ?: 0L)}")
                                Text("Interest accrued: ${money(projection?.grossInterestPaise ?: 0L)}")
                                Text("Interest due: ${money(projection?.interestOutstandingPaise ?: 0L)}")
                                Text("Total due today: ${money(projection?.totalDuePaise ?: 0L)}", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    if (lot.status == "ACTIVE") item {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Button(onClick = { addAdvanceDialog = true }, modifier = Modifier.weight(1f)) { Text("+ Funding") }
                            Button(onClick = { addPaymentDialog = true }, modifier = Modifier.weight(1f), enabled = (projection?.totalDuePaise ?: 0L) > 0L) { Text("Pay Party") }
                        }
                    }
                    item { Text("Funding History", fontWeight = FontWeight.Bold) }
                    items(lot.fundingAdvances.sortedByDescending { it.createdAt }, key = { "adv-${it.id}" }) { advance ->
                        Text("${formatDate(advance.createdAt)} • ${money(advance.amountPaise)} • ${advance.monthlyRateBasisPoints / 100.0}% • ${ruleLabel(advance.interestRule)}", fontSize = 12.sp)
                    }
                    item { Text("External Payment History", fontWeight = FontWeight.Bold) }
                    items(lot.fundingPayments.sortedByDescending { it.createdAt }, key = { "pay-${it.id}" }) { payment ->
                        val reversed = lot.fundingPayments.any { it.isReversal && it.reversedPaymentId == payment.id }
                        Column {
                            Text("${formatDate(payment.createdAt)} • ${money(payment.amountPaise)}${if (payment.isReversal) " • REVERSAL" else ""}", fontSize = 12.sp)
                            if (!payment.isReversal && !reversed && lot.status == "ACTIVE") TextButton(onClick = { reversePaymentId = payment.id }) { Text("Reverse") }
                            if (reversed) Text("Reversed", color = MaterialTheme.colorScheme.error, fontSize = 11.sp)
                        }
                    }
                }
                if (lot.status == "ACTIVE" && activeRows.isEmpty() && (projection?.totalDuePaise ?: Long.MAX_VALUE) == 0L) item {
                    Button(onClick = { closeLot(todayNoon()) }, modifier = Modifier.fillMaxWidth()) { Text("Close Settled Lot") }
                }
            }
        },
        confirmButton = { TextButton(onClick = dismiss) { Text("Done") } },
    )

    if (addItemsDialog) AddItemsToLotDialog(rows, custody, lot, { addItemsDialog = false }) { refs, at, note ->
        addItems(refs, at, note); addItemsDialog = false
    }
    moveRow?.let { row ->
        MoveItemDialog(row, custody.locations.filter { it.active }, { moveRow = null }) { locationId, at, note ->
            moveItem(row, locationId, at, note); moveRow = null
        }
    }
    if (addAdvanceDialog) ExternalAdvanceDialog(lot, { addAdvanceDialog = false }) { amount, rate, at, rule, note ->
        addAdvance(amount, rate, at, rule, note); addAdvanceDialog = false
    }
    if (addPaymentDialog && projection != null) ExternalPaymentDialog(projection.totalDuePaise, lot.openedAt, { addPaymentDialog = false }) { amount, at, note ->
        addPayment(amount, at, note); addPaymentDialog = false
    }
    reversePaymentId?.let { paymentId ->
        ReasonDialog("Reverse External Payment", { reversePaymentId = null }) { at, reason ->
            reversePayment(paymentId, at, reason); reversePaymentId = null
        }
    }
}

@Composable
private fun AddLocationDialog(dismiss: () -> Unit, save: (String, String, String) -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    var type by rememberSaveable { mutableStateOf("SHOP") }
    var detail by rememberSaveable { mutableStateOf("") }
    AlertDialog(onDismissRequest = dismiss, title = { Text("Add Storage / Locker") }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(name, { name = it.take(80) }, label = { Text("Name *") }, modifier = Modifier.fillMaxWidth())
            Row { listOf("SHOP", "HOME", "BANK", "OTHER").forEach { option -> TextButton(onClick = { type = option }) { Text(if (type == option) "✓$option" else option) } } }
            OutlinedTextField(detail, { detail = it.take(200) }, label = { Text("Locker/Box/Address detail") }, modifier = Modifier.fillMaxWidth())
        }
    }, confirmButton = { TextButton(onClick = { save(name, type, detail) }) { Text("Save") } }, dismissButton = { TextButton(onClick = dismiss) { Text("Cancel") } })
}

@Composable
private fun MoveItemDialog(row: ItemRow, locations: List<StorageLocation>, dismiss: () -> Unit, save: (String, Long, String) -> Unit) {
    val context = LocalContext.current
    var locationId by rememberSaveable { mutableStateOf(locations.firstOrNull()?.id.orEmpty()) }
    var date by rememberSaveable { mutableStateOf(todayNoon()) }
    var note by rememberSaveable { mutableStateOf("") }
    AlertDialog(onDismissRequest = dismiss, title = { Text("Move ${row.itemName}") }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("${row.girviNumber} • ${row.customerName}", color = Color.Gray)
            if (locations.isEmpty()) Text("Pehle Locker/Location add karein", color = MaterialTheme.colorScheme.error)
            locations.forEach { location -> OutlinedButton(onClick = { locationId = location.id }, modifier = Modifier.fillMaxWidth()) { Text(if (locationId == location.id) "✓ ${location.name}" else location.name) } }
            OutlinedButton(onClick = { pickDate(context, date) { date = it } }) { Text("Date: ${formatDate(date)}") }
            OutlinedTextField(note, { note = it.take(250) }, label = { Text("Note / reason") }, modifier = Modifier.fillMaxWidth())
        }
    }, confirmButton = { TextButton(onClick = { if (locationId.isNotBlank()) save(locationId, date, note) }, enabled = locationId.isNotBlank()) { Text("Confirm Move") } }, dismissButton = { TextButton(onClick = dismiss) { Text("Cancel") } })
}

@Composable
private fun AddPartyDialog(dismiss: () -> Unit, save: (String, String, String, Int, String) -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    var mobile by rememberSaveable { mutableStateOf("") }
    var address by rememberSaveable { mutableStateOf("") }
    var rate by rememberSaveable { mutableStateOf("") }
    var note by rememberSaveable { mutableStateOf("") }
    AlertDialog(onDismissRequest = dismiss, title = { Text("External Party Add Karein") }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(name, { name = it.take(80) }, label = { Text("Party name *") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(mobile, { mobile = it.filter(Char::isDigit).take(12) }, label = { Text("Mobile") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), modifier = Modifier.fillMaxWidth())
            OutlinedTextField(address, { address = it.take(200) }, label = { Text("Address") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(rate, { rate = decimalInput(it) }, label = { Text("Default monthly interest %") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
            OutlinedTextField(note, { note = it.take(250) }, label = { Text("Note") }, modifier = Modifier.fillMaxWidth())
        }
    }, confirmButton = { TextButton(onClick = { save(name, mobile, address, percentToBps(rate), note) }) { Text("Save Party") } }, dismissButton = { TextButton(onClick = dismiss) { Text("Cancel") } })
}

@Composable
private fun CreateLotDialog(
    rows: List<ItemRow>, custody: CustodyPlacementSnapshot, dismiss: () -> Unit,
    save: (String, String, List<Pair<String, String>>, Long, Long, Int, ExternalInterestRule, String) -> Unit,
) {
    val context = LocalContext.current
    val activeParties = custody.parties.filter { it.active }
    val externallyActive = custody.lots.flatMap { it.items.filter { item -> item.removedAt == null }.map { item -> item.itemId } }.toSet()
    val selectable = rows.filter { it.itemId !in externallyActive }
    val firstParty = activeParties.firstOrNull()
    var number by rememberSaveable { mutableStateOf("LOT-${System.currentTimeMillis().toString().takeLast(6)}") }
    var partyId by rememberSaveable { mutableStateOf(firstParty?.id.orEmpty()) }
    var date by rememberSaveable { mutableStateOf(todayNoon()) }
    var amount by rememberSaveable { mutableStateOf("") }
    var rate by rememberSaveable { mutableStateOf(firstParty?.let { (it.defaultMonthlyRateBasisPoints / 100.0).toString() }.orEmpty()) }
    var ruleName by rememberSaveable { mutableStateOf(ExternalInterestRule.EXACT_DAYS.name) }
    var note by rememberSaveable { mutableStateOf("") }
    var selected by remember { mutableStateOf(setOf<String>()) }
    AlertDialog(onDismissRequest = dismiss, title = { Text("Create External Lot") }, text = {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item { OutlinedTextField(number, { number = it.take(40) }, label = { Text("Lot number") }, modifier = Modifier.fillMaxWidth()) }
            item { Text("Party", fontWeight = FontWeight.Bold) }
            items(activeParties, key = { "party-${it.id}" }) { party ->
                OutlinedButton(onClick = { partyId = party.id; rate = (party.defaultMonthlyRateBasisPoints / 100.0).toString() }, modifier = Modifier.fillMaxWidth()) { Text(if (partyId == party.id) "✓ ${party.name}" else party.name) }
            }
            item { OutlinedButton(onClick = { pickDate(context, date) { date = it } }) { Text("Placement date: ${formatDate(date)}") } }
            item { OutlinedTextField(amount, { amount = decimalInput(it) }, label = { Text("Amount received ₹") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth()) }
            item { OutlinedTextField(rate, { rate = decimalInput(it) }, label = { Text("External monthly interest %") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth()) }
            item { InterestRuleChooser(ruleName) { ruleName = it.name } }
            item { Text("Items select karein (${selected.size})", fontWeight = FontWeight.Bold) }
            items(selectable, key = { "item-${it.itemId}" }) { row -> SelectableItemRow(row, row.itemId in selected) { checked -> selected = if (checked) selected + row.itemId else selected - row.itemId } }
            item { OutlinedTextField(note, { note = it.take(250) }, label = { Text("Note") }, modifier = Modifier.fillMaxWidth()) }
        }
    }, confirmButton = {
        TextButton(onClick = {
            val refs = selectable.filter { it.itemId in selected }.map { it.girviId to it.itemId }
            save(number, partyId, refs, date, rupeesToPaiseAllowZero(amount), percentToBps(rate), ExternalInterestRule.valueOf(ruleName), note)
        }, enabled = partyId.isNotBlank() && selected.isNotEmpty()) { Text("Create Lot") }
    }, dismissButton = { TextButton(onClick = dismiss) { Text("Cancel") } })
}

@Composable
private fun AddItemsToLotDialog(rows: List<ItemRow>, custody: CustodyPlacementSnapshot, lot: PlacementLot, dismiss: () -> Unit, save: (List<Pair<String, String>>, Long, String) -> Unit) {
    val context = LocalContext.current
    val externallyActive = custody.lots.flatMap { it.items.filter { item -> item.removedAt == null }.map { item -> item.itemId } }.toSet()
    val selectable = rows.filter { it.itemId !in externallyActive }
    var selected by remember { mutableStateOf(setOf<String>()) }
    var date by rememberSaveable { mutableStateOf(todayNoon().coerceAtLeast(lot.openedAt)) }
    var note by rememberSaveable { mutableStateOf("") }
    AlertDialog(onDismissRequest = dismiss, title = { Text("Add Items • ${lot.lotNumber}") }, text = {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            item { OutlinedButton(onClick = { pickDate(context, date) { if (it >= lot.openedAt) date = it } }) { Text("Date: ${formatDate(date)}") } }
            items(selectable, key = { "add-${it.itemId}" }) { row -> SelectableItemRow(row, row.itemId in selected) { checked -> selected = if (checked) selected + row.itemId else selected - row.itemId } }
            item { OutlinedTextField(note, { note = it.take(250) }, label = { Text("Note") }, modifier = Modifier.fillMaxWidth()) }
        }
    }, confirmButton = { TextButton(onClick = { save(selectable.filter { it.itemId in selected }.map { it.girviId to it.itemId }, date, note) }, enabled = selected.isNotEmpty()) { Text("Add") } }, dismissButton = { TextButton(onClick = dismiss) { Text("Cancel") } })
}

@Composable
private fun ExternalAdvanceDialog(lot: PlacementLot, dismiss: () -> Unit, save: (Long, Int, Long, ExternalInterestRule, String) -> Unit) {
    val context = LocalContext.current
    var amount by rememberSaveable { mutableStateOf("") }
    var rate by rememberSaveable { mutableStateOf((lot.monthlyRateBasisPoints / 100.0).toString()) }
    var date by rememberSaveable { mutableStateOf(todayNoon().coerceAtLeast(lot.openedAt)) }
    var ruleName by rememberSaveable { mutableStateOf(ExternalInterestRule.EXACT_DAYS.name) }
    var note by rememberSaveable { mutableStateOf("") }
    AlertDialog(onDismissRequest = dismiss, title = { Text("Additional External Funding") }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
            OutlinedTextField(amount, { amount = decimalInput(it) }, label = { Text("Amount ₹") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
            OutlinedTextField(rate, { rate = decimalInput(it) }, label = { Text("Monthly %") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
            InterestRuleChooser(ruleName) { ruleName = it.name }
            OutlinedButton(onClick = { pickDate(context, date) { if (it >= lot.openedAt) date = it } }) { Text("Date: ${formatDate(date)}") }
            OutlinedTextField(note, { note = it.take(250) }, label = { Text("Note") }, modifier = Modifier.fillMaxWidth())
        }
    }, confirmButton = { TextButton(onClick = { save(rupeesToPaisePositive(amount), percentToBps(rate), date, ExternalInterestRule.valueOf(ruleName), note) }) { Text("Save") } }, dismissButton = { TextButton(onClick = dismiss) { Text("Cancel") } })
}

@Composable
private fun ExternalPaymentDialog(totalDuePaise: Long, openedAt: Long, dismiss: () -> Unit, save: (Long, Long, String) -> Unit) {
    val context = LocalContext.current
    var amount by rememberSaveable { mutableStateOf("") }
    var date by rememberSaveable { mutableStateOf(todayNoon().coerceAtLeast(openedAt)) }
    var note by rememberSaveable { mutableStateOf("") }
    AlertDialog(onDismissRequest = dismiss, title = { Text("Pay External Party") }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Text("Current due: ${money(totalDuePaise)}", fontWeight = FontWeight.Bold)
            OutlinedTextField(amount, { amount = decimalInput(it) }, label = { Text("Payment ₹") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
            OutlinedButton(onClick = { pickDate(context, date) { if (it >= openedAt) date = it } }) { Text("Date: ${formatDate(date)}") }
            OutlinedTextField(note, { note = it.take(250) }, label = { Text("Note / reference") }, modifier = Modifier.fillMaxWidth())
        }
    }, confirmButton = { TextButton(onClick = { save(rupeesToPaisePositive(amount), date, note) }) { Text("Save Payment") } }, dismissButton = { TextButton(onClick = dismiss) { Text("Cancel") } })
}

@Composable
private fun ReasonDialog(title: String, dismiss: () -> Unit, save: (Long, String) -> Unit) {
    val context = LocalContext.current
    var reason by rememberSaveable { mutableStateOf("") }
    var date by rememberSaveable { mutableStateOf(todayNoon()) }
    AlertDialog(onDismissRequest = dismiss, title = { Text(title) }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
            OutlinedButton(onClick = { pickDate(context, date) { date = it } }) { Text("Date: ${formatDate(date)}") }
            OutlinedTextField(reason, { reason = it.take(220) }, label = { Text("Reason *") }, modifier = Modifier.fillMaxWidth())
        }
    }, confirmButton = { TextButton(onClick = { save(date, reason) }, enabled = reason.trim().length >= 3) { Text("Confirm") } }, dismissButton = { TextButton(onClick = dismiss) { Text("Cancel") } })
}

@Composable
private fun InterestRuleChooser(selectedName: String, changed: (ExternalInterestRule) -> Unit) {
    Column {
        Text("Interest rule", fontWeight = FontWeight.Bold)
        ExternalInterestRule.entries.forEach { rule -> TextButton(onClick = { changed(rule) }) { Text(if (selectedName == rule.name) "✓ ${ruleLabel(rule)}" else ruleLabel(rule)) } }
    }
}

@Composable
private fun SelectableItemRow(row: ItemRow, checked: Boolean, changed: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().clickable { changed(!checked) }.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked = checked, onCheckedChange = changed)
        Column { Text("${row.girviNumber} • ${row.customerName}", fontWeight = FontWeight.Bold); Text(row.itemName, color = Color.Gray) }
    }
}

private fun currentCustodyText(itemId: String, snapshot: CustodyPlacementSnapshot): String {
    val movement = snapshot.movements.filter { it.itemId == itemId }.maxWithOrNull(compareBy<com.girvikhata.app.custody.CustodyMovement> { it.movedAt }.thenBy { it.createdAt }) ?: return "Not assigned"
    return movementDestinationText(movement.destinationType, movement.destinationId, movement.lotId, snapshot)
}

private fun movementDestinationText(type: String, destinationId: String, lotId: String?, snapshot: CustodyPlacementSnapshot): String = when (type) {
    "LOCATION" -> snapshot.locations.firstOrNull { it.id == destinationId }?.name ?: "Unknown location"
    "EXTERNAL" -> {
        val party = snapshot.parties.firstOrNull { it.id == destinationId }?.name ?: "External party"
        val lot = snapshot.lots.firstOrNull { it.id == lotId }?.lotNumber
        if (lot.isNullOrBlank()) party else "$party • $lot"
    }
    else -> "Unknown"
}

private fun formatDate(value: Long): String = DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(value))
private fun money(paise: Long): String = "₹" + BigDecimal.valueOf(paise, 2).setScale(2).toPlainString()
private fun ruleLabel(rule: ExternalInterestRule): String = when (rule) {
    ExternalInterestRule.EXACT_DAYS -> "Exact days"
    ExternalInterestRule.STARTED_MONTH_FULL -> "Started month = full"
    ExternalInterestRule.COMPLETED_MONTHS_PLUS_DAYS -> "Months + remaining days"
}

private fun decimalInput(value: String): String {
    var dot = false
    return buildString { value.forEach { char -> if (char.isDigit()) append(char) else if (char == '.' && !dot) { append(char); dot = true } } }.take(14)
}

private fun percentToBps(value: String): Int {
    val decimal = value.trim().toBigDecimalOrNull() ?: BigDecimal.ZERO
    require(decimal >= BigDecimal.ZERO && decimal <= BigDecimal(1000)) { "Interest rate invalid" }
    return decimal.multiply(BigDecimal(100)).setScale(0, RoundingMode.HALF_UP).intValueExact()
}

private fun rupeesToPaiseAllowZero(value: String): Long {
    val decimal = value.trim().toBigDecimalOrNull() ?: BigDecimal.ZERO
    require(decimal >= BigDecimal.ZERO) { "Amount invalid" }
    return decimal.multiply(BigDecimal(100)).setScale(0, RoundingMode.HALF_UP).longValueExact()
}

private fun rupeesToPaisePositive(value: String): Long {
    val result = rupeesToPaiseAllowZero(value)
    require(result > 0L) { "Positive amount required" }
    return result
}

private fun todayNoon(): Long = Calendar.getInstance().apply {
    set(Calendar.HOUR_OF_DAY, 12); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
}.timeInMillis

private fun pickDate(context: android.content.Context, current: Long, selected: (Long) -> Unit) {
    val cal = Calendar.getInstance().apply { timeInMillis = current }
    DatePickerDialog(context, { _, year, month, day -> selected(Calendar.getInstance().apply { clear(); set(year, month, day, 12, 0, 0) }.timeInMillis) }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
}
