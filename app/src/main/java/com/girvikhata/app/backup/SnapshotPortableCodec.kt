package com.girvikhata.app.backup

import com.girvikhata.app.data.AppSnapshot
import com.girvikhata.app.data.CategoryRecord
import com.girvikhata.app.data.CustomerRecord
import com.girvikhata.app.data.GirviItemRecord
import com.girvikhata.app.data.GirviRecord
import com.girvikhata.app.data.PaymentRecord
import org.json.JSONArray
import org.json.JSONObject

/** Complete portable snapshot codec used before/after passphrase encryption. */
object SnapshotPortableCodec {
    private const val MAX_SUPPORTED_SCHEMA = 3

    fun encode(snapshot: AppSnapshot): ByteArray = JSONObject().apply {
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
                        portableItems(girvi).forEach { item ->
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
    }.toString().toByteArray(Charsets.UTF_8)

    fun decode(payload: ByteArray): AppSnapshot {
        require(payload.isNotEmpty()) { "Backup snapshot is empty" }
        val root = runCatching { JSONObject(String(payload, Charsets.UTF_8)) }
            .getOrElse { throw IllegalArgumentException("Backup snapshot JSON is damaged") }
        val schema = root.optInt("schemaVersion", 0)
        require(schema in 1..MAX_SUPPORTED_SCHEMA) { "Unsupported backup schema: $schema" }

        val customersJson = root.optJSONArray("customers") ?: JSONArray()
        val categoriesJson = root.optJSONArray("categories") ?: JSONArray()
        val girvisJson = root.optJSONArray("girvis") ?: JSONArray()

        val customers = List(customersJson.length()) { index ->
            customersJson.getJSONObject(index).run {
                CustomerRecord(
                    id = requiredText("id"),
                    name = requiredText("name"),
                    mobile = optString("mobile"),
                    address = optString("address"),
                    createdAt = optLong("createdAt").also { require(it > 0) { "Invalid customer timestamp" } },
                )
            }
        }
        require(customers.map { it.id }.distinct().size == customers.size) { "Duplicate customer IDs in backup" }

        val categories = List(categoriesJson.length()) { index ->
            categoriesJson.getJSONObject(index).run {
                CategoryRecord(
                    id = requiredText("id"),
                    name = requiredText("name"),
                    active = optBoolean("active", true),
                )
            }
        }
        require(categories.map { it.id }.distinct().size == categories.size) { "Duplicate category IDs in backup" }

        val girvis = List(girvisJson.length()) { index ->
            girvisJson.getJSONObject(index).run {
                val itemsJson = optJSONArray("items") ?: JSONArray()
                val items = List(itemsJson.length()) { itemIndex ->
                    itemsJson.getJSONObject(itemIndex).run {
                        GirviItemRecord(
                            id = requiredText("id"),
                            categoryName = requiredText("categoryName"),
                            itemName = requiredText("itemName"),
                            quantity = optInt("quantity", 1).also { require(it > 0) { "Invalid item quantity" } },
                            grossWeightGrams = optString("grossWeightGrams"),
                            deductionWeightGrams = optString("deductionWeightGrams"),
                            description = optString("description"),
                        )
                    }
                }
                val paymentsJson = optJSONArray("payments") ?: JSONArray()
                val payments = List(paymentsJson.length()) { paymentIndex ->
                    paymentsJson.getJSONObject(paymentIndex).run {
                        PaymentRecord(
                            id = requiredText("id"),
                            receiptNumber = requiredText("receiptNumber"),
                            amountPaise = getLong("amountPaise"),
                            principalPaise = getLong("principalPaise"),
                            interestPaise = getLong("interestPaise"),
                            chargesPaise = optLong("chargesPaise", 0L),
                            mode = optString("mode", "CASH"),
                            note = optString("note"),
                            createdAt = getLong("createdAt").also { require(it > 0) { "Invalid payment timestamp" } },
                            isReversal = optBoolean("isReversal", false),
                            reversedPaymentId = nullableText("reversedPaymentId"),
                        )
                    }
                }
                val status = optString("status", "ACTIVE")
                require(status == "ACTIVE" || status == "RELEASED") { "Invalid girvi status" }
                GirviRecord(
                    id = requiredText("id"),
                    girviNumber = requiredText("girviNumber"),
                    customerId = requiredText("customerId"),
                    customerName = requiredText("customerName"),
                    categoryName = requiredText("categoryName"),
                    itemName = requiredText("itemName"),
                    weightGrams = optString("weightGrams"),
                    principalPaise = getLong("principalPaise").also { require(it > 0) { "Invalid principal" } },
                    monthlyRateBasisPoints = getInt("monthlyRateBasisPoints").also { require(it >= 0) { "Invalid interest rate" } },
                    createdAt = getLong("createdAt").also { require(it > 0) { "Invalid girvi timestamp" } },
                    status = status,
                    items = items,
                    payments = payments,
                    manualInterestAdjustmentPaise = optLong("manualInterestAdjustmentPaise", 0L),
                    releasedAt = nullableLong("releasedAt"),
                    releaseNote = optString("releaseNote"),
                )
            }
        }
        require(girvis.map { it.id }.distinct().size == girvis.size) { "Duplicate girvi IDs in backup" }
        require(girvis.map { it.girviNumber }.distinct().size == girvis.size) { "Duplicate girvi numbers in backup" }
        val customerIds = customers.map { it.id }.toSet()
        require(girvis.all { it.customerId in customerIds }) { "Girvi references missing customer" }

        return AppSnapshot(schemaVersion = schema, customers = customers, categories = categories, girvis = girvis)
    }

    fun inspect(payload: ByteArray): SnapshotInspection {
        val snapshot = decode(payload)
        return SnapshotInspection(
            schemaVersion = snapshot.schemaVersion,
            customerCount = snapshot.customers.size,
            categoryCount = snapshot.categories.size,
            girviCount = snapshot.girvis.size,
            paymentEntryCount = snapshot.girvis.sumOf { it.payments.size },
        )
    }

    /**
     * Older records may only have the legacy top-level item fields. GirviRecord.effectiveItems
     * creates a compatibility item, but its default ID is intentionally runtime-generated.
     * Portable backup bytes must be deterministic, so the compatibility item's ID is derived
     * from the stable girvi ID instead.
     */
    private fun portableItems(girvi: GirviRecord): List<GirviItemRecord> =
        if (girvi.items.isNotEmpty()) {
            girvi.items
        } else {
            listOf(
                GirviItemRecord(
                    id = "legacy-item-${girvi.id}",
                    categoryName = girvi.categoryName,
                    itemName = girvi.itemName,
                    quantity = 1,
                    grossWeightGrams = girvi.weightGrams,
                    deductionWeightGrams = "",
                    description = "",
                ),
            )
        }

    private fun JSONObject.requiredText(name: String): String = optString(name).trim().also {
        require(it.isNotEmpty()) { "Missing $name in backup" }
    }

    private fun JSONObject.nullableText(name: String): String? =
        if (!has(name) || isNull(name)) null else optString(name).takeIf { it.isNotBlank() }

    private fun JSONObject.nullableLong(name: String): Long? =
        if (!has(name) || isNull(name)) null else getLong(name)
}

data class SnapshotInspection(
    val schemaVersion: Int,
    val customerCount: Int,
    val categoryCount: Int,
    val girviCount: Int,
    val paymentEntryCount: Int,
)
