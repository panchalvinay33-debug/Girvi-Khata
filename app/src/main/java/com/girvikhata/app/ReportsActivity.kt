package com.girvikhata.app

import android.os.Bundle
import androidx.activity.compose.setContent
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
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.girvikhata.app.data.AppSnapshot
import com.girvikhata.app.data.EncryptedRecordStore
import com.girvikhata.app.domain.DateRange
import com.girvikhata.app.domain.GirviStatusFilter
import com.girvikhata.app.domain.ReportingEngine
import com.girvikhata.app.export.CsvExportBuilder
import com.girvikhata.app.export.ReceiptTextBuilder
import com.girvikhata.app.export.SecureShare
import com.girvikhata.app.security.PinVerificationResult
import com.girvikhata.app.security.SecurityPreferences
import java.text.DateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private val ReportsNavy = Color(0xFF171752)
private val ReportsPurple = Color(0xFF5146B8)
private val ReportsGreen = Color(0xFF138A4A)
private val ReportsBackground = Color(0xFFF6F7FB)

class ReportsActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val security = SecurityPreferences(applicationContext)
        val store = EncryptedRecordStore(applicationContext)
        setContent {
            MaterialTheme {
                SecureReportsRoot(
                    verifyPin = { security.verify(it.toCharArray()) },
                    loadSnapshot = store::load,
                    close = ::finish,
                )
            }
        }
    }
}

private enum class ReportsPage { OVERVIEW, KHATA, GIRVI, COLLECTIONS }

@Composable
private fun SecureReportsRoot(
    verifyPin: (String) -> PinVerificationResult,
    loadSnapshot: () -> AppSnapshot,
    close: () -> Unit,
) {
    var unlocked by rememberSaveable { mutableStateOf(false) }
    var snapshot by remember { mutableStateOf<AppSnapshot?>(null) }
    if (!unlocked) {
        ReportsPinScreen(verifyPin, {
            snapshot = loadSnapshot()
            unlocked = true
        }, close)
    } else {
        ReportsHome(snapshot ?: AppSnapshot.defaults(), close)
    }
}

@Composable
private fun ReportsPinScreen(
    verifyPin: (String) -> PinVerificationResult,
    unlocked: () -> Unit,
    close: () -> Unit,
) {
    var pin by rememberSaveable { mutableStateOf("") }
    var message by rememberSaveable { mutableStateOf("Reports dekhne ke liye PIN daalein") }
    Column(
        Modifier.fillMaxSize().background(ReportsNavy).padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(Icons.Default.Lock, null, tint = Color(0xFFFFC54D))
        Spacer(Modifier.height(12.dp))
        Text("Girvi Khata Reports", color = Color.White, fontSize = 27.sp, fontWeight = FontWeight.Bold)
        Text(message, color = Color.White.copy(alpha = .8f))
        Spacer(Modifier.height(22.dp))
        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    pin,
                    { pin = it.filter(Char::isDigit).take(6) },
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
                TextButton(onClick = close, modifier = Modifier.fillMaxWidth()) { Text("Close") }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReportsHome(snapshot: AppSnapshot, close: () -> Unit) {
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
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                ReportsPage.entries.forEach { item ->
                    TextButton(onClick = { page = item }, modifier = Modifier.weight(1f)) {
                        Text(if (page == item) "✓ ${item.label()}" else item.label(), fontSize = 11.sp)
                    }
                }
            }
            when (page) {
                ReportsPage.OVERVIEW -> OverviewReport(snapshot)
                ReportsPage.KHATA -> CustomerLedgerReport(snapshot)
                ReportsPage.GIRVI -> GirviFilterReport(snapshot)
                ReportsPage.COLLECTIONS -> CollectionReport(snapshot)
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
        item {
            OutlinedTextField(months, { months = it.filter(Char::isDigit).take(3) }, label = { Text("Settlement months") }, modifier = Modifier.fillMaxWidth())
        }
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
private fun CustomerLedgerReport(snapshot: AppSnapshot) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var query by rememberSaveable { mutableStateOf("") }
    var months by rememberSaveable { mutableStateOf("1") }
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
    val monthCount = months.toIntOrNull()?.coerceIn(0, 120) ?: 0
    val customers = snapshot.customers.filter {
        query.isBlank() || it.name.contains(query, true) || it.mobile.contains(query) || it.address.contains(query, true)
    }
    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            OutlinedTextField(query, { query = it }, label = { Text("Customer search") }, leadingIcon = { Icon(Icons.Default.Search, null) }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(months, { months = it.filter(Char::isDigit).take(3) }, label = { Text("Settlement months") }, modifier = Modifier.fillMaxWidth())
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
            val id = selectedId!!
            val girvis = snapshot.girvis.filter { it.customerId == id }
            val ledger = ReportingEngine.customerLedger(snapshot, id, monthCount)
            item {
                ReportsBanner("${ledger.customerName}: ${reportsMoney(ledger.totalOutstandingPaise)} outstanding")
                SummaryRow("Total Girvi", ledger.totalGirvi.toString(), "Active", ledger.activeGirvi.toString())
                SummaryRow("Received", reportsMoney(ledger.effectiveReceivedPaise), "Principal Due", reportsMoney(ledger.outstandingPrincipalPaise))
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
private fun CollectionReport(snapshot: AppSnapshot) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var days by rememberSaveable { mutableStateOf(1) }
    val now = System.currentTimeMillis()
    val from = if (days == 0) 0L else Calendar.getInstance().apply {
        timeInMillis = now
        add(Calendar.DAY_OF_YEAR, -(days - 1))
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
    val rows = remember(snapshot, days) { ReportingEngine.collections(snapshot, DateRange(from, now)) }
    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Row {
                listOf(1 to "Today", 7 to "7 Days", 30 to "30 Days", 0 to "All").forEach { (value, label) ->
                    TextButton(onClick = { days = value }, modifier = Modifier.weight(1f)) { Text(if (days == value) "✓ $label" else label, fontSize = 11.sp) }
                }
            }
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
}

private fun reportsMoney(paise: Long): String = "₹%,.2f".format(Locale("en", "IN"), paise / 100.0)
