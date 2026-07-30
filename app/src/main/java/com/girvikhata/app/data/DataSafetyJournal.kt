package com.girvikhata.app.data

import android.content.Context
import com.girvikhata.app.security.DeviceKeyManager
import com.girvikhata.app.security.EncryptedPayload
import org.json.JSONArray
import org.json.JSONObject
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.UUID

/** Separate encrypted, hash-chained operational journal. It never replaces business records. */
class DataSafetyJournal(
    context: Context,
    private val keyManager: DeviceKeyManager = DeviceKeyManager(),
) {
    private val file = File(context.filesDir, FILE_NAME)

    @Synchronized
    fun status(): DataSafetyStatus {
        if (!file.exists()) return DataSafetyStatus()
        return runCatching { readState().also(::verifyChain).toStatus(true) }
            .getOrElse { DataSafetyStatus(journalValid = false, journalError = it.message ?: "Safety journal verify nahi hua") }
    }

    @Synchronized
    fun recordSnapshotChange(before: AppSnapshot?, after: AppSnapshot, explicitReason: String? = null) {
        val state = runCatching { if (file.exists()) readState().also(::verifyChain) else JournalState() }
            .getOrElse { JournalState(journalError = it.message ?: "Previous journal unreadable") }
        val detected = if (!explicitReason.isNullOrBlank()) {
            listOf(EventDraft(explicitReason, explicitReason.replace('_', ' ').lowercase().replaceFirstChar(Char::uppercase), snapshotSummary(after)))
        } else detectChanges(before, after)
        if (detected.isEmpty()) return
        var events = state.events
        var lastHash = events.lastOrNull()?.hash.orEmpty()
        detected.forEach { draft ->
            val createdAt = System.currentTimeMillis()
            val id = UUID.randomUUID().toString()
            val hash = eventHash(id, draft.type, draft.title, draft.detail, createdAt, lastHash)
            events = events + SafetyEvent(id, draft.type, draft.title, draft.detail, createdAt, lastHash, hash)
            lastHash = hash
        }
        val next = state.copy(
            events = events.takeLast(MAX_EVENTS),
            changesSinceBackup = state.changesSinceBackup + detected.size,
            journalError = null,
        )
        writeState(next)
    }

    @Synchronized
    fun markVerifiedBackup(sha256: String, customerCount: Int, girviCount: Int, ledgerCount: Int) {
        val state = runCatching { if (file.exists()) readState().also(::verifyChain) else JournalState() }.getOrElse { JournalState() }
        val now = System.currentTimeMillis()
        val previous = state.events.lastOrNull()?.hash.orEmpty()
        val detail = "$customerCount customers • $girviCount girvi • $ledgerCount ledger entries • SHA ${sha256.take(16)}…"
        val id = UUID.randomUUID().toString()
        val hash = eventHash(id, "BACKUP_VERIFIED", "Encrypted backup verified", detail, now, previous)
        writeState(
            state.copy(
                events = (state.events + SafetyEvent(id, "BACKUP_VERIFIED", "Encrypted backup verified", detail, now, previous, hash)).takeLast(MAX_EVENTS),
                lastVerifiedBackupAt = now,
                lastVerifiedBackupSha256 = sha256,
                changesSinceBackup = 0,
                journalError = null,
            ),
        )
    }

    private fun detectChanges(before: AppSnapshot?, after: AppSnapshot): List<EventDraft> {
        if (before == null) return listOf(EventDraft("STORE_CREATED", "Local encrypted khata created", snapshotSummary(after)))
        val events = mutableListOf<EventDraft>()
        val oldCustomers = before.customers.associateBy { it.id }
        val newCustomers = after.customers.associateBy { it.id }
        (newCustomers.keys - oldCustomers.keys).forEach { id -> events += EventDraft("CUSTOMER_ADDED", "Customer added", newCustomers.getValue(id).name) }
        (oldCustomers.keys - newCustomers.keys).forEach { id -> events += EventDraft("CUSTOMER_DELETED", "Unused customer deleted", oldCustomers.getValue(id).name) }
        (oldCustomers.keys intersect newCustomers.keys).forEach { id ->
            val old = oldCustomers.getValue(id); val new = newCustomers.getValue(id)
            if (old != new) events += EventDraft("CUSTOMER_EDITED", "Customer profile edited", new.name)
        }
        val oldGirvis = before.girvis.associateBy { it.id }
        val newGirvis = after.girvis.associateBy { it.id }
        (newGirvis.keys - oldGirvis.keys).forEach { id ->
            val g = newGirvis.getValue(id); events += EventDraft("GIRVI_CREATED", "Girvi created", "${g.girviNumber} • ${g.customerName}")
        }
        (oldGirvis.keys intersect newGirvis.keys).forEach { id ->
            val old = oldGirvis.getValue(id); val new = newGirvis.getValue(id)
            val oldPayments = old.payments.associateBy { it.id }
            (new.payments.map { it.id }.toSet() - oldPayments.keys).forEach { paymentId ->
                val p = new.payments.first { it.id == paymentId }
                val type = if (p.isReversal) "PAYMENT_REVERSED" else "PAYMENT_RECEIVED"
                val title = if (p.isReversal) "Payment reversed" else "Payment received"
                events += EventDraft(type, title, "${new.girviNumber} • ${p.receiptNumber} • ₹${"%.2f".format(p.amountPaise / 100.0)}")
            }
            if (old.status != "RELEASED" && new.status == "RELEASED") events += EventDraft("GIRVI_RELEASED", "Girvi released", "${new.girviNumber} • ${new.customerName}")
            if (old.manualInterestAdjustmentPaise != new.manualInterestAdjustmentPaise) events += EventDraft("INTEREST_ADJUSTED", "Interest adjusted", new.girviNumber)
        }
        if (before.categories != after.categories) events += EventDraft("CATEGORIES_CHANGED", "Categories changed", "${after.categories.size} categories")
        return events
    }

    private fun snapshotSummary(snapshot: AppSnapshot): String =
        "${snapshot.customers.size} customers • ${snapshot.girvis.size} girvi • ${snapshot.girvis.sumOf { it.payments.size }} ledger entries"

    private fun verifyChain(state: JournalState) {
        var previous = ""
        state.events.forEach { event ->
            require(event.previousHash == previous) { "Safety journal chain break" }
            require(event.hash == eventHash(event.id, event.type, event.title, event.detail, event.createdAt, event.previousHash)) { "Safety journal hash mismatch" }
            previous = event.hash
        }
    }

    private fun eventHash(id: String, type: String, title: String, detail: String, createdAt: Long, previousHash: String): String =
        sha256("$id|$type|$title|$detail|$createdAt|$previousHash".toByteArray(Charsets.UTF_8))

    private fun writeState(state: JournalState) {
        val json = JSONObject().apply {
            put("lastVerifiedBackupAt", state.lastVerifiedBackupAt)
            put("lastVerifiedBackupSha256", state.lastVerifiedBackupSha256)
            put("changesSinceBackup", state.changesSinceBackup)
            put("events", JSONArray().apply { state.events.forEach { e -> put(JSONObject().apply {
                put("id", e.id); put("type", e.type); put("title", e.title); put("detail", e.detail)
                put("createdAt", e.createdAt); put("previousHash", e.previousHash); put("hash", e.hash)
            }) } })
        }.toString().toByteArray(Charsets.UTF_8)
        val encrypted = keyManager.encrypt(json, AAD)
        val temp = File(file.parentFile, "$FILE_NAME.tmp")
        FileOutputStream(temp).use { stream ->
            DataOutputStream(stream.buffered()).use { output ->
                output.writeInt(MAGIC); output.writeInt(VERSION)
                output.writeInt(encrypted.iv.size); output.write(encrypted.iv)
                output.writeInt(encrypted.ciphertext.size); output.write(encrypted.ciphertext); output.flush()
            }
            stream.fd.sync()
        }
        readState(temp).also(::verifyChain)
        if (file.exists() && !file.delete()) error("Old safety journal replace nahi hua")
        if (!temp.renameTo(file)) { temp.copyTo(file, overwrite = true); check(temp.delete()) }
    }

    private fun readState(source: File = file): JournalState = DataInputStream(source.inputStream().buffered()).use { input ->
        require(input.readInt() == MAGIC && input.readInt() == VERSION) { "Safety journal format invalid" }
        val ivSize = input.readInt(); require(ivSize == 12)
        val iv = ByteArray(ivSize).also(input::readFully)
        val payloadSize = input.readInt(); require(payloadSize in 16..MAX_BYTES)
        val payload = ByteArray(payloadSize).also(input::readFully); require(input.read() == -1)
        val root = JSONObject(String(keyManager.decrypt(EncryptedPayload(payload, iv), AAD), Charsets.UTF_8))
        val array = root.optJSONArray("events") ?: JSONArray()
        JournalState(
            events = List(array.length()) { index -> array.getJSONObject(index).run {
                SafetyEvent(getString("id"), getString("type"), getString("title"), optString("detail"), getLong("createdAt"), optString("previousHash"), getString("hash"))
            } },
            lastVerifiedBackupAt = root.optLong("lastVerifiedBackupAt"),
            lastVerifiedBackupSha256 = root.optString("lastVerifiedBackupSha256"),
            changesSinceBackup = root.optInt("changesSinceBackup"),
        )
    }

    private data class JournalState(
        val events: List<SafetyEvent> = emptyList(),
        val lastVerifiedBackupAt: Long = 0,
        val lastVerifiedBackupSha256: String = "",
        val changesSinceBackup: Int = 0,
        val journalError: String? = null,
    ) { fun toStatus(valid: Boolean) = DataSafetyStatus(events, lastVerifiedBackupAt, lastVerifiedBackupSha256, changesSinceBackup, valid, journalError) }
    private data class EventDraft(val type: String, val title: String, val detail: String)

    companion object {
        fun sha256(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
        private const val FILE_NAME = "data_safety_journal_v1.bin"
        private const val MAGIC = 0x474B4A31
        private const val VERSION = 1
        private const val MAX_BYTES = 4 * 1024 * 1024
        private const val MAX_EVENTS = 500
        private val AAD = "girvi-khata-safety-journal-v1".toByteArray(Charsets.UTF_8)
    }
}

data class SafetyEvent(
    val id: String,
    val type: String,
    val title: String,
    val detail: String,
    val createdAt: Long,
    val previousHash: String,
    val hash: String,
)

data class DataSafetyStatus(
    val events: List<SafetyEvent> = emptyList(),
    val lastVerifiedBackupAt: Long = 0,
    val lastVerifiedBackupSha256: String = "",
    val changesSinceBackup: Int = 0,
    val journalValid: Boolean = true,
    val journalError: String? = null,
) {
    val backupDue: Boolean get() = lastVerifiedBackupAt == 0L || changesSinceBackup >= 5 || System.currentTimeMillis() - lastVerifiedBackupAt >= 7L * 24 * 60 * 60 * 1000
}
