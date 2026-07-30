package com.girvikhata.app

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.girvikhata.app.data.EncryptedRecordStore
import com.girvikhata.app.data.EncryptedRelationalShadowStore
import com.girvikhata.app.data.RelationalShadowStatus
import com.girvikhata.app.security.PinVerificationResult
import com.girvikhata.app.security.SecurityPreferences
import java.text.DateFormat
import java.util.Date

class RelationalShadowActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val records = EncryptedRecordStore(applicationContext)
        val security = SecurityPreferences(applicationContext)
        setContent {
            MaterialTheme {
                RelationalShadowRoot(
                    verifyPin = { security.verify(it.toCharArray()) },
                    loadStatus = {
                        val snapshot = records.load()
                        EncryptedRelationalShadowStore(applicationContext).use { it.statusAgainst(snapshot) }
                    },
                    rebuild = {
                        val snapshot = records.load()
                        EncryptedRelationalShadowStore(applicationContext).use { it.replaceAll(snapshot) }
                    },
                    close = ::finish,
                )
            }
        }
    }
}

@Composable
private fun RelationalShadowRoot(
    verifyPin: (String) -> PinVerificationResult,
    loadStatus: () -> RelationalShadowStatus,
    rebuild: () -> RelationalShadowStatus,
    close: () -> Unit,
) {
    var unlocked by rememberSaveable { mutableStateOf(false) }
    if (!unlocked) {
        var pin by rememberSaveable { mutableStateOf("") }
        var message by rememberSaveable { mutableStateOf("Database migration status ke liye PIN verify karein") }
        Column(
            Modifier.fillMaxSize().background(Color(0xFFF6F7FB)).padding(22.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("Secure Migration Status", fontSize = 27.sp, fontWeight = FontWeight.Bold)
            Text(message, color = Color.Gray)
            OutlinedTextField(
                pin,
                { pin = it.filter(Char::isDigit).take(6) },
                label = { Text("6-digit PIN") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            )
            Button(onClick = {
                when (val result = verifyPin(pin)) {
                    PinVerificationResult.Success -> unlocked = true
                    PinVerificationResult.NotConfigured -> message = "PIN configured nahi hai"
                    is PinVerificationResult.Locked -> message = "Security lock active hai"
                    is PinVerificationResult.Failure -> message = "Galat PIN. Attempts: ${result.attempts}"
                }
                pin = ""
            }, enabled = pin.length == 6, modifier = Modifier.fillMaxWidth().padding(top = 10.dp)) { Text("PIN Verify") }
            OutlinedButton(onClick = close, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) { Text("Close") }
        }
        return
    }

    var status by remember { mutableStateOf(runCatching(loadStatus).getOrElse { RelationalShadowStatus(false, reason = it.message) }) }
    var message by remember { mutableStateOf<String?>(null) }
    val good = status.healthy
    Column(
        Modifier.fillMaxSize().background(Color(0xFFF6F7FB)).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("Database Migration Status", fontSize = 27.sp, fontWeight = FontWeight.Bold, color = Color(0xFF171752))
        Text("Encrypted snapshot source-of-truth • relational database shadow mode", color = Color.Gray)
        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(if (good) "SHADOW VERIFIED ✅" else "CUTOVER BLOCKED ⚠️", fontWeight = FontWeight.Bold, color = if (good) Color(0xFF138A4A) else MaterialTheme.colorScheme.error)
                Text("Expected: ${status.expectedCounts ?: "not available"}")
                Text("Database: ${status.actualCounts ?: "not available"}")
                Text("Expected fingerprint: ${status.expectedFingerprint?.take(24) ?: "—"}")
                Text("Database fingerprint: ${status.actualFingerprint?.take(24) ?: "—"}")
                Text("Last mirror: ${status.mirroredAt?.let { DateFormat.getDateTimeInstance().format(Date(it)) } ?: "Never"}")
                status.reason?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        }
        Text(
            if (good) "Counts aur decrypted semantic fingerprint match hain. Read cutover phir bhi owner-approved migration tests ke baad hi hoga."
            else "Business khata safe hai. Relational shadow ko rebuild/verify kiye bina database cutover allowed nahi hai.",
            color = Color.Gray,
        )
        message?.let { Text(it, color = if (it.startsWith("Verified")) Color(0xFF138A4A) else MaterialTheme.colorScheme.error) }
        Button(onClick = {
            runCatching(rebuild).onSuccess { status = it; message = "Verified relational shadow rebuilt" }
                .onFailure { message = it.message ?: "Relational rebuild failed" }
        }, modifier = Modifier.fillMaxWidth()) { Text("Transactional Shadow Rebuild & Verify") }
        OutlinedButton(onClick = {
            runCatching(loadStatus).onSuccess { status = it; message = "Status refreshed" }
                .onFailure { message = it.message ?: "Status refresh failed" }
        }, modifier = Modifier.fillMaxWidth()) { Text("Refresh Status") }
        OutlinedButton(onClick = close, modifier = Modifier.fillMaxWidth()) { Text("Close") }
    }
}
