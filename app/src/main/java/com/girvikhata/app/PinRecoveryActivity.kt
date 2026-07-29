package com.girvikhata.app

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
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
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.girvikhata.app.security.SecurityPreferences

class PinRecoveryActivity : FragmentActivity() {
    private lateinit var security: SecurityPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        security = SecurityPreferences(applicationContext)
        setContent {
            MaterialTheme {
                var authenticated by rememberSaveable { mutableStateOf(false) }
                PinRecoveryScreen(
                    authenticated = authenticated,
                    verifierStatus = security.verifierStatus().name,
                    authenticate = { authenticateDevice { authenticated = true } },
                    saveNewPin = { pin ->
                        security.clearPinAfterAuthenticatedRecovery()
                        security.savePin(pin.toCharArray())
                    },
                    close = ::finish,
                )
            }
        }
    }

    private fun authenticateDevice(onSuccess: () -> Unit) {
        val authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG or
            BiometricManager.Authenticators.DEVICE_CREDENTIAL
        val prompt = BiometricPrompt(
            this,
            ContextCompat.getMainExecutor(this),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) = onSuccess()
            },
        )
        prompt.authenticate(
            BiometricPrompt.PromptInfo.Builder()
                .setTitle("Girvi Khata PIN Recovery")
                .setSubtitle("Fingerprint ya phone screen lock verify karein")
                .setAllowedAuthenticators(authenticators)
                .build(),
        )
    }
}

@Composable
private fun PinRecoveryScreen(
    authenticated: Boolean,
    verifierStatus: String,
    authenticate: () -> Unit,
    saveNewPin: (String) -> Unit,
    close: () -> Unit,
) {
    var pin by rememberSaveable { mutableStateOf("") }
    var confirm by rememberSaveable { mutableStateOf("") }
    var message by rememberSaveable { mutableStateOf("Stored PIN status: $verifierStatus") }
    val navy = Color(0xFF171752)
    val purple = Color(0xFF5146B8)

    Column(
        Modifier.fillMaxSize().background(Color(0xFFF6F7FB)).padding(20.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(if (authenticated) Icons.Default.LockReset else Icons.Default.Fingerprint, null, tint = navy)
        Spacer(Modifier.height(10.dp))
        Text("PIN Recovery", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = navy)
        Text(message, color = Color.Gray)
        Spacer(Modifier.height(18.dp))
        Card(
            Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
        ) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Is process se customer, girvi, payment ya backup data delete nahi hoga. Sirf PIN verifier replace hoga.",
                    color = Color.Gray,
                )
                if (!authenticated) {
                    Button(
                        onClick = authenticate,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = purple),
                    ) { Text("Fingerprint / Phone Lock Verify") }
                } else {
                    OutlinedTextField(
                        value = pin,
                        onValueChange = { pin = it.filter(Char::isDigit).take(6) },
                        label = { Text("Naya 6-digit PIN") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = confirm,
                        onValueChange = { confirm = it.filter(Char::isDigit).take(6) },
                        label = { Text("PIN dobara") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Button(
                        onClick = {
                            runCatching {
                                require(pin == confirm) { "Dono PIN match nahi kar rahe" }
                                require(pin.length == 6) { "PIN 6 digits ka hona chahiye" }
                                saveNewPin(pin)
                            }.onSuccess {
                                pin = ""
                                confirm = ""
                                message = "PIN reset successful. Main app ko naye PIN se kholein."
                            }.onFailure { message = it.message ?: "PIN reset nahi hua" }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = navy),
                    ) { Text("Naya PIN Save Karein") }
                }
                OutlinedButton(onClick = close, modifier = Modifier.fillMaxWidth()) { Text("Close") }
            }
        }
    }
}
