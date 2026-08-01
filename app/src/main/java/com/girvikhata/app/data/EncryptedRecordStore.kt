package com.girvikhata.app.data

import android.content.Context
import com.girvikhata.app.security.DeviceKeyManager
import com.girvikhata.app.security.EncryptedPayload
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

sealed interface RecordStoreLoadState {
    data class Ready(val snapshot: AppSnapshot, val recoveredFromSafetyCopy: Boolean = false) : RecordStoreLoadState
    data class Corrupt(val reason: String, val safetyCopiesChecked: Int) : RecordStoreLoadState
}

class RecordStoreCorruptionException(message: String) : IllegalStateException(message)

/**
 * App-private encrypted snapshot store.
 *
 * Alpha 11 removes the old silent-corruption fallback. Existing data is now protected by:
 * - verified temporary writes before primary replacement,
 * - a rotating set of encrypted pre-save safety copies,
 * - automatic recovery from the newest valid safety copy,
 * - quarantine of a damaged primary file,
 * - explicit corruption failure when no valid copy remains.
 */
class EncryptedRecordStore(
    context: Context,
    private val keyManager: DeviceKeyManager = DeviceKeyManager(),
) {
    private val file = File(context.filesDir, FILE_NAME)
    private val safetyDirectory = File(context.filesDir, SAFETY_DIRECTORY)
    private val quarantineDirectory = File(context.filesDir, QUARANTINE_DIRECTORY)

    @Synchronized
    fun loadState(): RecordStoreLoadState {
        if (!file.exists()) return RecordStoreLoadState.Ready(AppSnapshot.defaults())

        runCatching { readEnvelope(file) }
            .onSuccess { return RecordStoreLoadState.Ready(it) }

        val safetyCopies = safetyFilesNewestFirst()
        safetyCopies.forEach { candidate ->
            runCatching { readEnvelope(candidate) }.onSuccess { recovered ->
                runCatching { promoteSafetyCopy(candidate) }
                    .getOrElse { return RecordStoreLoadState.Corrupt("Safety copy mili, lekin primary recovery save nahi ho saki", safetyCopies.size) }
                return RecordStoreLoadState.Ready(recovered, recoveredFromSafetyCopy = true)
            }
        }

        return RecordStoreLoadState.Corrupt(
            reason = "Encrypted local records verify/decrypt nahi ho sake",
            safetyCopiesChecked = safetyCopies.size,
        )
    }

    @Synchronized
    fun load(): AppSnapshot = when (val state = loadState()) {
        is RecordStoreLoadState.Ready -> state.snapshot
        is RecordStoreLoadState.Corrupt -> throw RecordStoreCorruptionException(
            "${state.reason}. ${state.safetyCopiesChecked} safety copies checked.",
        )
    }

    @Synchronized
    fun save(snapshot: AppSnapshot) {
        val normalized = snapshot.copy(schemaVersion = CURRENT_SCHEMA)
        validateSnapshot(normalized)

        if (file.exists()) {
            readEnvelope(file)
            createSafetyCopy(file)
        }

        val temporary = File(file.parentFile, "$FILE_NAME.tmp")
        runCatching {
            writeEnvelope(temporary, normalized)
            val verified = readEnvelope(temporary)
            check(verified == normalized) { "Encrypted store read-back verification failed" }
            replacePrimaryWith(temporary)
            check(readEnvelope(file) == normalized) { "Primary encrypted store verification failed" }
            pruneFiles(safetyDirectory, MAX_SAFETY_COPIES)
            pruneFiles(quarantineDirectory, MAX_QUARANTINE_COPIES)
        }.onFailure {
            temporary.delete()
            throw it
        }
    }

    private fun writeEnvelope(target: File, snapshot: AppSnapshot) {
        target.parentFile?.mkdirs()
        val plaintext = encode(snapshot).toByteArray(Charsets.UTF_8)
        val encrypted = keyManager.encrypt(plaintext, associatedData = ASSOCIATED_DATA)
        FileOutputStream(target).use { stream ->
            val output = DataOutputStream(BufferedOutputStream(stream))
            output.writeInt(MAGIC)
            output.writeInt(FORMAT_VERSION)
            output.writeInt(encrypted.iv.size)
            output.write(encrypted.iv)
            output.writeInt(encrypted.ciphertext.size)
            output.write(encrypted.ciphertext)
            output.flush()
            runCatching { stream.fd.sync() }
        }
    }

    private fun readEnvelope(source: File): AppSnapshot {
        require(source.exists() && source.isFile) { "Encrypted store file missing" }
        require(source.length() in MIN_ENVELOPE_BYTES..MAX_ENVELOPE_BYTES) { "Encrypted store size invalid" }
        return DataInputStream(source.inputStream().buffered()).use { input ->
            require(input.readInt() == MAGIC) { "Invalid encrypted store" }
            require(input.readInt() == FORMAT_VERSION) { "Unsupported encrypted store version" }
            val ivLength = input.readInt()
            require(ivLength in MIN_IV_BYTES..MAX_IV_BYTES) { "Encrypted store IV length invalid" }
            val iv = ByteArray(ivLength).also(input::readFully)
            val ciphertextLength = input.readInt()
            require(ciphertextLength in MIN_CIPHERTEXT_BYTES..MAX_CIPHERTEXT_BYTES) { "Encrypted store payload length invalid" }
            val ciphertext = ByteArray(ciphertextLength).also(input::readFully)
            require(input.read() == -1) { "Encrypted store has trailing bytes" }
            val plaintext = keyManager.decrypt(
                EncryptedPayload(ciphertext = ciphertext, iv = iv),
                associatedData = ASSOCIATED_DATA,
            )
            decode(String(plaintext, Charsets.UTF_8)).also(::validateSnapshot)
        }
    }

    private fun createSafetyCopy(primary: File) {
        safetyDirectory.mkdirs()
        val target = File(safetyDirectory, "records-${System.currentTimeMillis()}.bin")
        primary.copyTo(target, overwrite = false)
        check(readEnvelope(target) == readEnvelope(primary)) { "Safety-copy verification failed" }
        pruneFiles(safetyDirectory, MAX_SAFETY_COPIES)
    }

    private fun promoteSafetyCopy(candidate: File) {
        quarantineDirectory.mkdirs()
        if (file.exists()) {
            val quarantine = File(quarantineDirectory, "damaged-${System.currentTimeMillis()}.bin")
            file.copyTo(quarantine, overwrite = false)
        }
        val temporary = File(file.parentFile, "$FILE_NAME.recovery.tmp")
        candidate.copyTo(temporary, overwrite = true)
        readEnvelope(temporary)
        replacePrimaryWith(temporary)
        readEnvelope(file)
        pruneFiles(quarantineDirectory, MAX_QUARANTINE_COPIES)
    }

    private fun replacePrimaryWith(temporary: File) {
        if (file.exists() && !file.delete()) error("Old encrypted store replace nahi ho saka")
        if (!temporary.renameTo(file)) {
            temporary.copyTo(file, overwrite = true)
            check(temporary.delete()) { "Temporary encrypted file cleanup failed" }
        }
    }

    private fun safetyFilesNewestFirst(): List<File> =
        safetyDirectory.listFiles()?.filter(File::isFile)?.sortedByDescending(File::lastModified).orEmpty()

    private fun pruneFiles(directory: File, keep: Int) {
        directory.listFiles()?.filter(File::isFile)?.sortedByDescending(File::lastModified)?.drop(keep)?.forEach(File::delete)
    }

    private fun validateSnapshot(snapshot: AppSnapshot) {
        require(snapshot.schemaVersion == CURRENT_SCHEMA) { "Unsupported local schema" }
        require(snapshot.customers.map { it.id }.distinct().size == snapshot.customers.size) { "Duplicate customer ID" }
        require(snapshot.girvis.map { it.id }.distinct().size == snapshot.girvis.size) { "Duplicate girvi ID" }
        require(snapshot.girvis.map { it.girviNumber }.distinct().size == snapshot.girvis.size) { "Duplicate girvi number" }
        val customerIds = snapshot.customers.map { it.id }.toSet()
        require(snapshot.girvis.all { it.customerId in customerIds }) { "Girvi customer link missing" }
        require(snapshot.girvis.all { it.principalPaise > 0L && it.createdAt > 0L }) { "Girvi amount/timestamp invalid" }
        require(snapshot.girvis.all { it.status in setOf("ACTIVE", "RELEASED") }) { "Girvi status invalid" }
    }

    private fun encode(snapshot: AppSnapshot): String = JSONObject().apply {
        put("schemaVersion", snapshot.schemaVersion)
        put("customers", JSONArray().apply {
            snapshot.customers.forEach { customer ->
                put(JSONObject().apply {
                    put("id", customer.id)
                    put("name", customer.name)
                    put("mobile", customer.mobile)
                    put("address", customer.address)
                    put("createdAt", customer.createdAt)
                })
            }
        })
        put("categories", JSONArray().apply {
            snapshot.categories.forEach { category ->
                put(JSONObject().apply {
                    put("id", category.id)
                    put("name", category.name)
                    put("active", category.active)
                })
            }
        })
        put("girvis", JSONArray().apply {
            snapshot.girvis.forEach { girvi ->
                put(JSONObject().apply {
                    put("id", girvi.id)
                    put("girviNumber", girvi.girviNumber)
                    put("customerId", girvi.customerId)
                    put("customerName", girvi.customerName)
                    put("categoryName", girvi.categoryName)
                    put("itemName", girvi.itemName)
                    put("weightGrams", girvi.weightGrams)
                    put("principalPaise", girvi.principalPaise)
                    put("monthlyRateBasisPoints", girvi.monthlyRateBasisPoints)
                    put("createdAt", girvi.createdAt)
                    put("status", girvi.status)
                    put("releasedAt", girvi.releasedAt ?: JSONObject.NULL)
                    put("releaseNote", girvi.releaseNote)
                    put("manualInterestAdjustmentPaise", girvi.manualInterestAdjustmentPaise)
                    put("items", JSONArray().apply {
                        girvi.effectiveItems.forEach { item ->
                            put(JSONObject().apply {
                                put("id", item.id)
                                put("categoryName", item.categoryName)
                                put("itemName", item.itemName)
                                put("quantity", item.quantity)
                                put("grossWeightGrams", item.grossWeightGrams)
                                put("deductionWeightGrams", item.deductionWeightGrams)
                                put("description", item.description)
                            })
                        }
                    })
                    put("payments", JSONArray().apply {
                        girvi.payments.forEach { payment ->
                            put(JSONObject().apply {
                                put("id", payment.id)
                                put("receiptNumber", payment.receiptNumber)
                                put("amountPaise", payment.amountPaise)
                                put("principalPaise", payment.principalPaise)
                                put("interestPaise", payment.interestPaise)
                                put("chargesPaise", payment.chargesPaise)
                                put("mode", payment.mode)
                                put("note", payment.note)
                                put("createdAt", payment.createdAt)
                                put("isReversal", payment.isReversal)
                                put("reversedPaymentId", payment.reversedPaymentId ?: JSONObject.NULL)
                            })
                        }
                    })
                })
            }
        })
    }.toString()

    private fun decode(value: String): AppSnapshot {
        val root = JSONObject(value)
        require(root.optInt("schemaVersion", CURRENT_SCHEMA) <= CURRENT_SCHEMA) { "Future local schema unsupported" }
        val customers = root.optJSONArray("customers") ?: JSONArray()
        val categories = root.optJSONArray("categories") ?: JSONArray()
        val girvis = root.optJSONArray("girvis") ?: JSONArray()
        return AppSnapshot(
            schemaVersion = CURRENT_SCHEMA,
            customers = List(customers.length()) { index ->
                customers.getJSONObject(index).run {
                    CustomerRecord(
                        id = getString("id"),
                        name = getString("name"),
                        mobile = optString("mobile"),
                        address = optString("address"),
                        createdAt = optLong("createdAt"),
                    )
                }
            },
            categories = List(categories.length()) { index ->
                categories.getJSONObject(index).run {
                    CategoryRecord(
                        id = getString("id"),
                        name = getString("name"),
                        active = optBoolean("active", true),
                    )
                }
            },
            girvis = List(girvis.length()) { index ->
                girvis.getJSONObject(index).run {
                    val girviId = getString("id")
                    val legacyCategory = optString("categoryName")
                    val legacyItem = optString("itemName")
                    val legacyWeight = optString("weightGrams")
                    val itemArray = optJSONArray("items")
                    val decodedItems = if (itemArray != null && itemArray.length() > 0) {
                        List(itemArray.length()) { itemIndex ->
                            itemArray.getJSONObject(itemIndex).run {
                                GirviItemRecord(
                                    id = optString("id").takeIf { it.isNotBlank() }
                                        ?: legacyChildId("item", girviId, itemIndex),
                                    categoryName = optString("categoryName", legacyCategory),
                                    itemName = optString("itemName", legacyItem),
                                    quantity = optInt("quantity", 1),
                                    grossWeightGrams = optString("grossWeightGrams", legacyWeight),
                                    deductionWeightGrams = optString("deductionWeightGrams"),
                                    description = optString("description"),
                                )
                            }
                        }
                    } else {
                        listOf(
                            GirviItemRecord(
                                id = legacyChildId("item", girviId, 0),
                                categoryName = legacyCategory,
                                itemName = legacyItem,
                                grossWeightGrams = legacyWeight,
                            ),
                        )
                    }
                    val paymentArray = optJSONArray("payments") ?: JSONArray()
                    val decodedPayments = List(paymentArray.length()) { paymentIndex ->
                        paymentArray.getJSONObject(paymentIndex).run {
                            PaymentRecord(
                                id = optString("id").takeIf { it.isNotBlank() }
                                    ?: legacyChildId("payment", girviId, paymentIndex),
                                receiptNumber = optString("receiptNumber"),
                                amountPaise = optLong("amountPaise"),
                                principalPaise = optLong("principalPaise"),
                                interestPaise = optLong("interestPaise"),
                                chargesPaise = optLong("chargesPaise"),
                                mode = optString("mode", "CASH"),
                                note = optString("note"),
                                createdAt = optLong("createdAt"),
                                isReversal = optBoolean("isReversal", false),
                                reversedPaymentId = optNullableString("reversedPaymentId"),
                            )
                        }
                    }
                    GirviRecord(
                        id = girviId,
                        girviNumber = getString("girviNumber"),
                        customerId = getString("customerId"),
                        customerName = getString("customerName"),
                        categoryName = legacyCategory.ifBlank { decodedItems.firstOrNull()?.categoryName.orEmpty() },
                        itemName = legacyItem.ifBlank { decodedItems.firstOrNull()?.itemName.orEmpty() },
                        weightGrams = legacyWeight.ifBlank { decodedItems.firstOrNull()?.grossWeightGrams.orEmpty() },
                        principalPaise = getLong("principalPaise"),
                        monthlyRateBasisPoints = getInt("monthlyRateBasisPoints"),
                        createdAt = getLong("createdAt"),
                        status = optString("status", "ACTIVE"),
                        items = decodedItems,
                        payments = decodedPayments,
                        manualInterestAdjustmentPaise = optLong("manualInterestAdjustmentPaise", 0L),
                        releasedAt = optNullableLong("releasedAt"),
                        releaseNote = optString("releaseNote"),
                    )
                }
            },
        )
    }

    private fun JSONObject.optNullableString(name: String): String? =
        if (!has(name) || isNull(name)) null else optString(name).takeIf { it.isNotBlank() }

    private fun JSONObject.optNullableLong(name: String): Long? =
        if (!has(name) || isNull(name)) null else optLong(name)

    companion object {
        private const val FILE_NAME = "business_records_v1.bin"
        private const val SAFETY_DIRECTORY = "record_safety_copies"
        private const val QUARANTINE_DIRECTORY = "record_quarantine"
        private const val MAGIC = 0x474B5631
        private const val FORMAT_VERSION = 1
        private const val CURRENT_SCHEMA = 3
        private const val MAX_SAFETY_COPIES = 5
        private const val MAX_QUARANTINE_COPIES = 2
        private const val MIN_IV_BYTES = 12
        private const val MAX_IV_BYTES = 32
        private const val MIN_CIPHERTEXT_BYTES = 16
        private const val MAX_CIPHERTEXT_BYTES = 128 * 1024 * 1024
        private const val MIN_ENVELOPE_BYTES = 28L
        private const val MAX_ENVELOPE_BYTES = 129L * 1024L * 1024L
        private val ASSOCIATED_DATA = "girvi-khata-local-store-v1".toByteArray(Charsets.UTF_8)
    }
}

internal fun legacyChildId(kind: String, girviId: String, index: Int): String =
    "legacy-$kind-$girviId-$index"

data class AppSnapshot(
    val schemaVersion: Int = 3,
    val customers: List<CustomerRecord> = emptyList(),
    val categories: List<CategoryRecord> = emptyList(),
    val girvis: List<GirviRecord> = emptyList(),
) {
    companion object {
        fun defaults() = AppSnapshot(
            categories = listOf(
                CategoryRecord(name = "Jewellery"),
                CategoryRecord(name = "Electronics"),
                CategoryRecord(name = "Documents"),
                CategoryRecord(name = "Other"),
            ),
        )
    }
}

data class CustomerRecord(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val mobile: String = "",
    val address: String = "",
    val createdAt: Long = System.currentTimeMillis(),
)

data class CategoryRecord(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val active: Boolean = true,
)

data class GirviItemRecord(
    val id: String = UUID.randomUUID().toString(),
    val categoryName: String,
    val itemName: String,
    val quantity: Int = 1,
    val grossWeightGrams: String = "",
    val deductionWeightGrams: String = "",
    val description: String = "",
)

data class PaymentRecord(
    val id: String = UUID.randomUUID().toString(),
    val receiptNumber: String,
    val amountPaise: Long,
    val principalPaise: Long,
    val interestPaise: Long,
    val chargesPaise: Long = 0,
    val mode: String = "CASH",
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val isReversal: Boolean = false,
    val reversedPaymentId: String? = null,
) {
    init {
        require(amountPaise > 0)
        require(principalPaise >= 0 && interestPaise >= 0 && chargesPaise >= 0)
        require(principalPaise + interestPaise + chargesPaise == amountPaise)
        if (isReversal) require(!reversedPaymentId.isNullOrBlank())
    }
}

data class GirviRecord(
    val id: String = UUID.randomUUID().toString(),
    val girviNumber: String,
    val customerId: String,
    val customerName: String,
    val categoryName: String,
    val itemName: String,
    val weightGrams: String,
    val principalPaise: Long,
    val monthlyRateBasisPoints: Int,
    val createdAt: Long = System.currentTimeMillis(),
    val status: String = "ACTIVE",
    val items: List<GirviItemRecord> = emptyList(),
    val payments: List<PaymentRecord> = emptyList(),
    val manualInterestAdjustmentPaise: Long = 0,
    val releasedAt: Long? = null,
    val releaseNote: String = "",
) {
    val effectiveItems: List<GirviItemRecord>
        get() = items.ifEmpty {
            listOf(
                GirviItemRecord(
                    categoryName = categoryName,
                    itemName = itemName,
                    grossWeightGrams = weightGrams,
                ),
            )
        }
}