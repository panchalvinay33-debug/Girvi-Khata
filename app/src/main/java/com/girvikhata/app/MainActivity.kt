package com.girvikhata.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.girvikhata.app.data.EncryptedRecordStore
import com.girvikhata.app.data.RecordStoreLoadState
import com.girvikhata.app.security.BiometricAvailability
import com.girvikhata.app.security.BiometricCapability
import com.girvikhata.app.security.SecurityPreferences
import com.girvikhata.app.security.SessionAutoLockPolicy

class MainActivity : FragmentActivity() {
    private var backgroundedAt: Long? = null
    private var lockSignal by mutableIntStateOf(0)
    private var settingsSignal by mutableIntStateOf(0)
    private lateinit var securityPreferences: SecurityPreferences
    private lateinit var biometricCapability: BiometricCapability

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        securityPreferences = SecurityPreferences(applicationContext)
        val recordStore = EncryptedRecordStore(applicationContext)
        biometricCapability = BiometricCapability(applicationContext)
        setContent {
            MaterialTheme {
                settingsSignal
                val sessionSettings = securityPreferences.sessionSettings()
                val biometricAvailability = if (sessionSettings.biometricUnlockEnabled) {
                    biometricCapability.availability()
                } else {
                    BiometricAvailability.UNSUPPORTED
                }
                var storeState by remember { mutableStateOf(recordStore.loadState()) }
                var startFreshKhata by remember { mutableStateOf(false) }
                when (val state = storeState) {
                    is RecordStoreLoadState.Ready -> {
                        val untouchedFreshInstall = !securityPreferences.hasPin() &&
                            state.snapshot.customers.isEmpty() && state.snapshot.girvis.isEmpty()
                        if (untouchedFreshInstall && !startFreshKhata) {
                            FirstRunRecoveryChoice(
                                newKhata = { startFreshKhata = true },
                                recoverExisting = { startActivity(Intent(this@MainActivity, RestoreActivity::class.java)) },
                            )
                        } else {
                            BlueprintGirviKhataRoot(
                                securityPreferences = securityPreferences,
                                recordStore = recordStore,
                                biometricAvailability = biometricAvailability,
                                lockSignal = lockSignal,
                                refreshSignal = settingsSignal,
                                requestBiometric = ::requestBiometric,
                            )
                        }
                    }
                    is RecordStoreLoadState.Corrupt -> DataRecoveryRequired(
                        reason = state.reason,
                        copiesChecked = state.safetyCopiesChecked,
                        openRestore = { startActivity(Intent(this@MainActivity, RestoreActivity::class.java)) },
                        retry = { storeState = recordStore.loadState() },
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (::securityPreferences.isInitialized) settingsSignal++
    }

    override fun onStop() {
        super.onStop()
        if (!isChangingConfigurations) backgroundedAt = System.currentTimeMillis()
    }

    override fun onStart() {
        super.onStart()
        if (::securityPreferences.isInitialized) {
            val timeout = securityPreferences.sessionSettings().autoLockTimeoutMillis
            if (SessionAutoLockPolicy(timeout).shouldLock(backgroundedAt, System.currentTimeMillis())) lockSignal++
        }
        backgroundedAt = null
    }

    private fun requestBiometric(onSuccess: () -> Unit, onError: (String) -> Unit) {
        if (!securityPreferences.sessionSettings().biometricUnlockEnabled) {
            onError("Biometric unlock Owner Settings mein disabled hai")
            return
        }
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

@androidx.compose.runtime.Composable
private fun FirstRunRecoveryChoice(newKhata: () -> Unit, recoverExisting: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Girvi Khata", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("Naya khata shuru karein ya purana mobile ka data recover karein")
        Card(Modifier.fillMaxWidth().padding(vertical = 18.dp)) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = newKhata, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.AddCircle, null)
                    Text("  Naya Khata Setup")
                }
                OutlinedButton(onClick = recoverExisting, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Restore, null)
                    Text("  Purana Khata Recover Karein")
                }
                Text("Recovery ke liye encrypted .gkb backup aur Recovery Key chahiye.")
            }
        }
    }
}

@androidx.compose.runtime.Composable
private fun DataRecoveryRequired(
    reason: String,
    copiesChecked: Int,
    openRestore: () -> Unit,
    retry: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(Icons.Default.Restore, null, tint = MaterialTheme.colorScheme.error)
        Text("Data Recovery Required", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("Khali khata nahi khola gaya. Aapke encrypted records verify nahi ho sake.")
        Card(Modifier.fillMaxWidth().padding(vertical = 16.dp)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(reason)
                Text("$copiesChecked local safety copies check ki gayi.")
                Text("External .gkb backup restore karein. Restore complete hone tak naya record save na karein.")
            }
        }
        Button(onClick = openRestore, modifier = Modifier.fillMaxWidth()) { Text("Backup Restore Kholein") }
        OutlinedButton(onClick = retry, modifier = Modifier.fillMaxWidth()) { Text("Restore Ke Baad Dobara Check Karein") }
    }
}
