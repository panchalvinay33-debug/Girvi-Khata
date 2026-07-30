package com.girvikhata.app.security

import android.content.Context
import androidx.biometric.BiometricManager

class SessionAutoLockPolicy(
    private val timeoutMillis: Long = DEFAULT_TIMEOUT_MILLIS,
) {
    init {
        require(timeoutMillis >= 0) { "Timeout cannot be negative" }
    }

    fun shouldLock(backgroundedAtMillis: Long?, nowMillis: Long): Boolean {
        if (backgroundedAtMillis == null) return false
        if (nowMillis < backgroundedAtMillis) return true
        return nowMillis - backgroundedAtMillis >= timeoutMillis
    }

    companion object {
        const val DEFAULT_TIMEOUT_MILLIS = 30_000L
    }
}

enum class BiometricAvailability {
    AVAILABLE,
    NONE_ENROLLED,
    NO_HARDWARE,
    TEMPORARILY_UNAVAILABLE,
    UNSUPPORTED,
}

class BiometricCapability(context: Context) {
    private val manager = BiometricManager.from(context)

    fun availability(): BiometricAvailability = when (
        manager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)
    ) {
        BiometricManager.BIOMETRIC_SUCCESS -> BiometricAvailability.AVAILABLE
        BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> BiometricAvailability.NONE_ENROLLED
        BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> BiometricAvailability.NO_HARDWARE
        BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> BiometricAvailability.TEMPORARILY_UNAVAILABLE
        else -> BiometricAvailability.UNSUPPORTED
    }
}
