package com.girvikhata.app.backup

import java.time.Instant

enum class BackupTrigger { CRITICAL_TRANSACTION, DAILY, MANUAL, PRE_UPDATE, POST_RESTORE }
enum class BackupState { IDLE, QUEUED, PACKAGING, ENCRYPTING, UPLOADING, VERIFYING, VERIFIED, FAILED }

data class BackupManifest(
    val formatVersion: Int,
    val appVersion: String,
    val schemaVersion: Int,
    val createdAt: Instant,
    val trigger: BackupTrigger,
    val databaseBytes: Long,
    val mediaBytes: Long,
    val customerCount: Long,
    val activeGirviCount: Long,
    val paymentCount: Long,
    val chunks: List<BackupChunk>,
)

data class BackupChunk(
    val id: String,
    val encryptedSize: Long,
    val sha256: String,
)

data class BackupStatus(
    val state: BackupState = BackupState.IDLE,
    val pendingCriticalChanges: Int = 0,
    val lastVerifiedAt: Instant? = null,
    val lastFailureCode: String? = null,
) {
    val isProtected: Boolean get() = state == BackupState.VERIFIED && pendingCriticalChanges == 0
}

interface BackupCoordinator {
    suspend fun queue(trigger: BackupTrigger)
    suspend fun createAndVerify(trigger: BackupTrigger): BackupStatus
    suspend fun discoverVerifiedBackups(): List<BackupManifest>
    suspend fun restore(manifest: BackupManifest, passphrase: CharArray): RestoreResult
}

sealed interface RestoreResult {
    data class Success(val restoredManifest: BackupManifest) : RestoreResult
    data class Failure(val safeErrorCode: String) : RestoreResult
}
