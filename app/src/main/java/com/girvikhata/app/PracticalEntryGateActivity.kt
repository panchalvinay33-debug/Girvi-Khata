package com.girvikhata.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.girvikhata.app.data.VerifiedWriteRecoveryRepair
import com.girvikhata.app.security.PinVerificationResult
import com.girvikhata.app.security.SecurityPreferences

/**
 * Deliberate second gate for the standalone Alpha 25A entry surface.
 * The main activity may be visible while its Compose root is still locked, so a shortcut must
 * never be allowed to create records without independently verifying the owner PIN.
 */
class PracticalEntryGateActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val security = SecurityPreferences(applicationContext)
        val recoveryRepair = VerifiedWriteRecoveryRepair(applicationContext)
        setContent {
            MaterialTheme {
                var pin by remember { mutableStateOf("") }
                var message by remember { mutableStateOf("6-digit owner PIN डालें / Enter owner PIN") }
                Column(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text("सुरक्षित नई एंट्री / Secure New Entry", style = MaterialTheme.typography.headlineSmall)
                    Text(message, modifier = Modifier.padding(vertical = 12.dp))
                    OutlinedTextField(
                        value = pin,
                        onValueChange = { pin = it.filter(Char::isDigit).take(6) },
                        label = { Text("PIN") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Button(
                        onClick = {
                            when (val result = security.verify(pin.toCharArray())) {
                                PinVerificationResult.Success -> {
                                    runCatching { recoveryRepair.repairIfBlocked() }
                                        .onSuccess { repair ->
                                            if (repair.repaired) {
                                                message = "पिछला अधूरा save सुरक्षित रूप से ठीक किया गया / Previous interrupted save repaired"
                                            }
                                            startActivity(Intent(this@PracticalEntryGateActivity, PracticalEntryActivity::class.java))
                                            finish()
                                        }
                                        .onFailure { failure ->
                                            message = "Save recovery blocked: ${failure.message ?: failure::class.java.simpleName}"
                                        }
                                }
                                PinVerificationResult.NotConfigured -> message = "पहले main app में PIN setup करें / Set up PIN first"
                                is PinVerificationResult.Locked -> message = "Security lock active है; बाद में कोशिश करें"
                                is PinVerificationResult.Failure -> message = "गलत PIN / Wrong PIN • Attempts: ${result.attempts}"
                            }
                            pin = ""
                        },
                        enabled = pin.length == 6,
                        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                    ) { Text("खोलें / Continue") }
                }
            }
        }
    }
}
