package com.girvikhata.app

import android.os.Bundle
import androidx.activity.compose.setContent
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.girvikhata.app.data.AppSnapshot
import com.girvikhata.app.data.DataSafetyJournal
import com.girvikhata.app.data.EncryptedRecordStore
import com.girvikhata.app.data.GirviRecord
import com.girvikhata.app.domain.GirviSettlementUseCase
import com.girvikhata.app.domain.ManualInterestAdjustment
import com.girvikhata.app.domain.SettlementReceiptText
import com.girvikhata.app.export.SecureShare
import com.girvikhata.app.security.PinVerificationResult
import com.girvikhata.app.security.SecurityPreferences
import java.math.BigDecimal

class SettlementCenterActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val security = SecurityPreferences(applicationContext)
        val store = EncryptedRecordStore(applicationContext)
        val journal = DataSafetyJournal(applicationContext)
        setContent {
            MaterialTheme {
                var snapshot by remember { mutableStateOf(store.load()) }
                SettlementRoot(
                    verifyPin = { security.verify(it.toCharArray()) },
                    snapshot = snapshot,
                    saveAdjustment = { girvi, amount, reason ->
                        val result = ManualInterestAdjustment.apply(girvi, amount, reason)
                        val next = snapshot.copy(girvis = snapshot.girvis.map { if (it.id == girvi.id) result.updatedGirvi else it })
                        store.save(next)
                        journal.recordNamedEvent(
                            "INTEREST_ADJUSTMENT_REASON",
                            "Interest adjustment reason",
                            "${girvi.girviNumber} • ${signedMoney(result.deltaPaise)} • ${result.reason}",
                        )
                        snapshot = next
                        result.updatedGirvi
                    },
                    shareReceipt = { girvi, months ->
                        SecureShare.shareText(this, "Settlement ${girvi.girviNumber}", SettlementReceiptText.create(girvi, months))
                    },
                    close = ::finish,
                )
            }
        }
    }
}

@Composable
private fun SettlementRoot(
    verifyPin: (String) -> PinVerificationResult,
    snapshot: AppSnapshot,
    saveAdjustment: (GirviRecord, String, String) -> GirviRecord,
    shareReceipt: (GirviRecord, Int) -> Unit,
    close: () -> Unit,
) {
    var unlocked by rememberSaveable { mutableStateOf(false) }
    if (!unlocked) {
        var pin by rememberSaveable { mutableStateOf("") }
        var message by rememberSaveable { mutableStateOf("Settlement Center ke liye PIN verify karein") }
        Column(Modifier.fillMaxSize().background(Color(0xFFF6F7FB)).padding(20.dp), verticalArrangement = Arrangement.Center) {
            Text("Settlement Center", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(message, color = Color.Gray)
            OutlinedTextField(pin, { pin = it.filter(Char::isDigit).take(6) }, label = { Text("6-digit PIN") }, visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword), modifier = Modifier.fillMaxWidth())
            Button(onClick = {
                when (val result = verifyPin(pin)) {
                    PinVerificationResult.Success -> unlocked = true
                    PinVerificationResult.NotConfigured -> message = "PIN configured nahi hai"
                    is PinVerificationResult.Locked -> message = "Security lock active hai"
                    is PinVerificationResult.Failure -> message = "Galat PIN. Attempts: ${result.attempts}"
                }
                pin = ""
            }, enabled = pin.length == 6, modifier = Modifier.fillMaxWidth()) { Text("PIN Verify") }
            OutlinedButton(onClick = close, modifier = Modifier.fillMaxWidth()) { Text("Close") }
        }
    } else SettlementDashboard(snapshot, saveAdjustment, shareReceipt, close)
}

@Composable
private fun SettlementDashboard(
    snapshot: AppSnapshot,
    saveAdjustment: (GirviRecord, String, String) -> GirviRecord,
    shareReceipt: (GirviRecord, Int) -> Unit,
    close: () -> Unit,
) {
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
    var months by rememberSaveable { mutableStateOf("1") }
    var adjustmentFor by remember { mutableStateOf<GirviRecord?>(null) }
    var message by rememberSaveable { mutableStateOf("Payment, reversal ya release ke baad verified external backup banayein.") }
    val selected = snapshot.girvis.firstOrNull { it.id == selectedId }
    val monthCount = months.toIntOrNull()?.coerceIn(0, 120) ?: 0

    Column(Modifier.fillMaxSize().background(Color(0xFFF6F7FB)).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Settlement & Release Center", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(message, color = Color.Gray)
        if (selected == null) {
            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(snapshot.girvis.sortedByDescending { it.createdAt }, key = { it.id }) { girvi ->
                    Card(Modifier.fillMaxWidth().clickable { selectedId = girvi.id }) {
                        Column(Modifier.padding(14.dp)) {
                            Text(girvi.girviNumber, fontWeight = FontWeight.Bold)
                            Text("${girvi.customerName} • ${girvi.status} • ${money(girvi.principalPaise)}", color = Color.Gray)
                        }
                    }
                }
            }
            OutlinedButton(onClick = close, modifier = Modifier.fillMaxWidth()) { Text("Close") }
        } else {
            val settlement = GirviSettlementUseCase.settlementView(selected, monthCount)
            OutlinedTextField(months, { months = it.filter(Char::isDigit).take(3) }, label = { Text("Settlement months") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text(selected.girviNumber, fontWeight = FontWeight.Bold)
                    Text(selected.customerName)
                    Text("Principal due: ${money(settlement.principalDuePaise)}")
                    Text("Interest due: ${money(settlement.interestDuePaise)}")
                    Text("Manual adjustment total: ${signedMoney(selected.manualInterestAdjustmentPaise)}")
                    Text("Total outstanding: ${money(settlement.totalDuePaise)}", fontWeight = FontWeight.Bold)
                    if (selected.status == "RELEASED") Text("Release note: ${selected.releaseNote}")
                }
            }
            if (selected.status == "ACTIVE") Button(onClick = { adjustmentFor = selected }, modifier = Modifier.fillMaxWidth()) { Text("Manual Interest Adjustment") }
            Button(onClick = { shareReceipt(selected, monthCount) }, modifier = Modifier.fillMaxWidth()) { Text("Settlement / Release Receipt Share") }
            Text("Critical change ke baad Data Safety Status mein backup due check karein.", color = MaterialTheme.colorScheme.error)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { selectedId = null }, modifier = Modifier.weight(1f)) { Text("Back") }
                OutlinedButton(onClick = close, modifier = Modifier.weight(1f)) { Text("Close") }
            }
        }
    }

    adjustmentFor?.let { girvi ->
        var amount by rememberSaveable(girvi.id) { mutableStateOf("") }
        var reason by rememberSaveable(girvi.id) { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { adjustmentFor = null },
            title = { Text("Interest Adjustment") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Positive amount interest badhata hai; negative amount kam karta hai.")
                    OutlinedTextField(amount, { amount = it.filter { ch -> ch.isDigit() || ch == '.' || ch == '-' } }, label = { Text("Signed amount ₹, e.g. -150") })
                    OutlinedTextField(reason, { reason = it }, label = { Text("Mandatory reason") })
                }
            },
            confirmButton = {
                Button(onClick = {
                    runCatching { saveAdjustment(girvi, amount, reason) }
                        .onSuccess { selectedId = it.id; message = "Adjustment saved. Reason encrypted audit journal mein record hua. External backup banayein."; adjustmentFor = null }
                        .onFailure { message = it.message ?: "Adjustment save nahi hua" }
                }) { Text("Save Adjustment") }
            },
            dismissButton = { OutlinedButton(onClick = { adjustmentFor = null }) { Text("Cancel") } },
        )
    }
}

private fun money(paise: Long): String = "₹" + BigDecimal.valueOf(paise, 2).setScale(2).toPlainString()
private fun signedMoney(paise: Long): String = (if (paise >= 0) "+" else "-") + money(kotlin.math.abs(paise))
