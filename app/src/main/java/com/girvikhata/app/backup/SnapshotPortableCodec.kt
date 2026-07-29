package com.girvikhata.app.backup

import com.girvikhata.app.data.AppSnapshot
import org.json.JSONArray
import org.json.JSONObject

/** Serializes the complete business snapshot before portable passphrase encryption. */
object SnapshotPortableCodec {
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
    }.toString().toByteArray(Charsets.UTF_8)

    fun inspect(payload: ByteArray): SnapshotInspection {
        val root = JSONObject(String(payload, Charsets.UTF_8))
        val schema = root.getInt("schemaVersion")
        val customers = root.optJSONArray("customers")?.length() ?: 0
        val categories = root.optJSONArray("categories")?.length() ?: 0
        val girvis = root.optJSONArray("girvis") ?: JSONArray()
        var payments = 0
        for (index in 0 until girvis.length()) {
            payments += girvis.getJSONObject(index).optJSONArray("payments")?.length() ?: 0
        }
        return SnapshotInspection(schema, customers, categories, girvis.length(), payments)
    }
}

data class SnapshotInspection(
    val schemaVersion: Int,
    val customerCount: Int,
    val categoryCount: Int,
    val girviCount: Int,
    val paymentEntryCount: Int,
)
