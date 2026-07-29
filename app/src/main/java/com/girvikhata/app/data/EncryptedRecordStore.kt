package com.girvikhata.app.data

import android.content.Context
import com.girvikhata.app.security.DeviceKeyManager
import com.girvikhata.app.security.EncryptedPayload
import org.json.JSONArray
import org.json.JSONObject
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.util.UUID

/**
 * Encrypted snapshot store used during testing milestones.
 *
 * The binary envelope stays at format v1 while the JSON schema is versioned independently.
 * Schema v3 adds immutable payment-ledger entries and release metadata, while retaining
 * backward compatibility with Alpha 2/3 records.
 */
class EncryptedRecordStore(
    context: Context,
    private val keyManager: DeviceKeyManager = DeviceKeyManager(),
) {
    private val file = File(context.filesDir, FILE_NAME)

    @Synchronized
    fun load(): AppSnapshot {
        if (!file.exists()) return AppSnapshot.defaults()
        return runCatching {
            DataInputStream(file.inputStream().buffered()).use { input ->
                require(input.readInt() == MAGIC) { "Invalid encrypted store" }
                val version = input.readInt()
                require(version == FORMAT_VERSION) { "Unsupported encrypted store version" }
                val iv = ByteArray(input.readInt()).also(input::readFully)
                val ciphertext = ByteArray(input.readInt()).also(input::readFully)
                val plaintext = keyManager.decrypt(
                    EncryptedPayload(ciphertext = ciphertext, iv = iv),
                    associatedData = ASSOCIATED_DATA,
                )
                decode(String(plaintext, Charsets.UTF_8))
            }
        }.getOrElse { AppSnapshot.defaults() }
    }

    @Synchronized
    fun save(snapshot: AppSnapshot) {
        val plaintext = encode(snapshot.copy(schemaVersion = CURRENT_SCHEMA)).toByteArray(Charsets.UTF_8)
        val encrypted = keyManager.encrypt(plaintext, associatedData = ASSOCIATED_DATA)
        val temporary = File(file.parentFile, "$FILE_NAME.tmp")
        DataOutputStream(temporary.outputStream().buffered()).use { output ->
            output.writeInt(MAGIC)
            output.writeInt(FORMAT_VERSION)
            output.writeInt(encrypted.iv.size)
            output.write(encrypted.iv)
            output.writeInt(encrypted.ciphertext.size)
            output.write(encrypted.ciphertext)
            output.flush()
        }
        check(temporary.renameTo(file) || temporary.copyTo(file, overwrite = true).let { temporary.delete(); true })
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
                    val legacyCategory = optString("categoryName")
                    val legacyItem = optString("itemName")
                    val legacyWeight = optString("weightGrams")
                    val itemArray = optJSONArray("items")
                    val decodedItems = if (itemArray != null && itemArray.length() > 0) {
                        List(itemArray.length()) { itemIndex ->
                            itemArray.getJSONObject(itemIndex).run {
                                GirviItemRecord(
                                    id = optString("id", UUID.randomUUID().toString()),
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
                                id = optString("id", UUID.randomUUID().toString()),
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
                        id = getString("id"),
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
        private const val MAGIC = 0x474B5631
        private const val FORMAT_VERSION = 1
        private const val CURRENT_SCHEMA = 3
        private val ASSOCIATED_DATA = "girvi-khata-local-store-v1".toByteArray(Charsets.UTF_8)
    }
}

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
