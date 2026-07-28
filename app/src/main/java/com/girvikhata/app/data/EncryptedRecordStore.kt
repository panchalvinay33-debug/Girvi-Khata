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
 * Small encrypted snapshot store for the first testing milestones.
 *
 * Business records are serialized in memory, encrypted with AES-GCM through Android Keystore,
 * and only ciphertext is written to app-private storage. This is intentionally replaceable by
 * an encrypted relational database once migrations and backup integration are ready.
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
        val plaintext = encode(snapshot).toByteArray(Charsets.UTF_8)
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
            schemaVersion = root.optInt("schemaVersion", 1),
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
                    GirviRecord(
                        id = getString("id"),
                        girviNumber = getString("girviNumber"),
                        customerId = getString("customerId"),
                        customerName = getString("customerName"),
                        categoryName = getString("categoryName"),
                        itemName = getString("itemName"),
                        weightGrams = optString("weightGrams"),
                        principalPaise = getLong("principalPaise"),
                        monthlyRateBasisPoints = getInt("monthlyRateBasisPoints"),
                        createdAt = getLong("createdAt"),
                        status = optString("status", "ACTIVE"),
                    )
                }
            },
        )
    }

    companion object {
        private const val FILE_NAME = "business_records_v1.bin"
        private const val MAGIC = 0x474B5631
        private const val FORMAT_VERSION = 1
        private val ASSOCIATED_DATA = "girvi-khata-local-store-v1".toByteArray(Charsets.UTF_8)
    }
}

data class AppSnapshot(
    val schemaVersion: Int = 1,
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
)
