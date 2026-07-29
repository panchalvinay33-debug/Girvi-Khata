package com.girvikhata.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.girvikhata.app.data.EncryptedRecordStore
import com.girvikhata.app.security.BiometricCapability
import com.girvikhata.app.security.SecurityPreferences
import com.girvikhata.app.security.SessionAutoLockPolicy

class MainActivity : FragmentActivity() {
    private val lockPolicy = SessionAutoLockPolicy()
    private var backgroundedAt: Long? = null
    private var lockSignal by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val securityPreferences = SecurityPreferences(applicationContext)
        val recordStore = EncryptedRecordStore(applicationContext)
        val biometricCapability = BiometricCapability(applicationContext)
        setContent {
            MaterialTheme {
                Box(Modifier.fillMaxSize()) {
                    GirviKhataRoot(
                        securityPreferences = securityPreferences,
                        recordStore = recordStore,
                        biometricAvailability = biometricCapability.availability(),
                        lockSignal = lockSignal,
                        requestBiometric = ::requestBiometric,
                    )
                    FloatingActionButton(
                        onClick = { startActivity(Intent(this@MainActivity, ToolsActivity::class.java)) },
                        modifier = Modifier.align(Alignment.BottomEnd).padding(end = 18.dp, bottom = 92.dp),
                        containerColor = MaterialTheme.colorScheme.primary,
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = "Reports, backup aur security tools")
                    }
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        if (!isChangingConfigurations) backgroundedAt = System.currentTimeMillis()
    }

    override fun onStart() {
        super.onStart()
        if (lockPolicy.shouldLock(backgroundedAt, System.currentTimeMillis())) lockSignal++
        backgroundedAt = null
    }

    private fun requestBiometric(onSuccess: () -> Unit, onError: (String) -> Unit) {
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
