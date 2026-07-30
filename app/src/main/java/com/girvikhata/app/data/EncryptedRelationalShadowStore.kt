package com.girvikhata.app.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.girvikhata.app.security.DeviceKeyManager
import com.girvikhata.app.security.EncryptedPayload

/**
 * Transactional relational shadow of the current encrypted snapshot.
 * Sensitive text columns are individually AES-GCM encrypted with field-specific associated data.
 * The snapshot remains authoritative until a later owner-approved cutover.
 */
class EncryptedRelationalShadowStore(
    context: Context,
    private val keyManager: DeviceKeyManager = DeviceKeyManager(),
) : SQLiteOpenHelper(context.applicationContext, DATABASE_NAME, null, DATABASE_VERSION) {

    override fun onConfigure(db: SQLiteDatabase) {
        db.setForeignKeyConstraintsEnabled(true)
        db.enableWriteAheadLogging()
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE customers(id TEXT PRIMARY KEY, name BLOB NOT NULL, mobile BLOB NOT NULL, address BLOB NOT NULL, created_at INTEGER NOT NULL)")
        db.execSQL("CREATE TABLE categories(id TEXT PRIMARY KEY, name BLOB NOT NULL, active INTEGER NOT NULL CHECK(active IN (0,1)))")
        db.execSQL("CREATE TABLE girvis(id TEXT PRIMARY KEY, girvi_number TEXT NOT NULL UNIQUE, customer_id TEXT NOT NULL REFERENCES customers(id) ON DELETE RESTRICT, customer_name BLOB NOT NULL, principal_paise INTEGER NOT NULL CHECK(principal_paise > 0), monthly_rate_bp INTEGER NOT NULL CHECK(monthly_rate_bp >= 0), created_at INTEGER NOT NULL, status TEXT NOT NULL CHECK(status IN ('ACTIVE','RELEASED')), adjustment_paise INTEGER NOT NULL, released_at INTEGER, release_note BLOB NOT NULL)")
        db.execSQL("CREATE TABLE items(id TEXT PRIMARY KEY, girvi_id TEXT NOT NULL REFERENCES girvis(id) ON DELETE CASCADE, position INTEGER NOT NULL, category_name BLOB NOT NULL, item_name BLOB NOT NULL, quantity INTEGER NOT NULL CHECK(quantity > 0), gross_weight BLOB NOT NULL, deduction_weight BLOB NOT NULL, description BLOB NOT NULL, UNIQUE(girvi_id, position))")
        db.execSQL("CREATE TABLE payments(id TEXT PRIMARY KEY, girvi_id TEXT NOT NULL REFERENCES girvis(id) ON DELETE CASCADE, position INTEGER NOT NULL, receipt_number TEXT NOT NULL UNIQUE, amount_paise INTEGER NOT NULL CHECK(amount_paise > 0), principal_paise INTEGER NOT NULL CHECK(principal_paise >= 0), interest_paise INTEGER NOT NULL CHECK(interest_paise >= 0), charges_paise INTEGER NOT NULL CHECK(charges_paise >= 0), mode BLOB NOT NULL, note BLOB NOT NULL, created_at INTEGER NOT NULL, is_reversal INTEGER NOT NULL CHECK(is_reversal IN (0,1)), reversed_payment_id TEXT, UNIQUE(girvi_id, position))")
        db.execSQL("CREATE TABLE metadata(key TEXT PRIMARY KEY, value TEXT NOT NULL)")
        db.execSQL("CREATE INDEX idx_girvis_customer ON girvis(customer_id)")
        db.execSQL("CREATE INDEX idx_girvis_status ON girvis(status)")
        db.execSQL("CREATE INDEX idx_items_girvi ON items(girvi_id)")
        db.execSQL("CREATE INDEX idx_payments_girvi ON payments(girvi_id)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        error("Shadow database upgrade $oldVersion->$newVersion requires explicit migration")
    }

    @Synchronized
    fun replaceAll(snapshot: AppSnapshot): RelationalShadowStatus {
        val expectedFingerprint = RelationalShadowFingerprint.sha256(snapshot)
        val expectedCounts = RelationalShadowFingerprint.expectedCounts(snapshot)
        writableDatabase.beginTransaction()
        try {
            writableDatabase.delete("payments", null, null)
            writableDatabase.delete("items", null, null)
            writableDatabase.delete("girvis", null, null)
            writableDatabase.delete("categories", null, null)
            writableDatabase.delete("customers", null, null)
            writableDatabase.delete("metadata", null, null)

            snapshot.customers.forEach { c ->
                writableDatabase.insertOrThrow("customers", null, ContentValues().apply {
                    put("id", c.id); put("name", seal(c.name, "customers:${c.id}:name")); put("mobile", seal(c.mobile, "customers:${c.id}:mobile")); put("address", seal(c.address, "customers:${c.id}:address")); put("created_at", c.createdAt)
                })
            }
            snapshot.categories.forEach { c ->
                writableDatabase.insertOrThrow("categories", null, ContentValues().apply {
                    put("id", c.id); put("name", seal(c.name, "categories:${c.id}:name")); put("active", if (c.active) 1 else 0)
                })
            }
            snapshot.girvis.forEach { g ->
                writableDatabase.insertOrThrow("girvis", null, ContentValues().apply {
                    put("id", g.id); put("girvi_number", g.girviNumber); put("customer_id", g.customerId); put("customer_name", seal(g.customerName, "girvis:${g.id}:customer_name")); put("principal_paise", g.principalPaise); put("monthly_rate_bp", g.monthlyRateBasisPoints); put("created_at", g.createdAt); put("status", g.status); put("adjustment_paise", g.manualInterestAdjustmentPaise); if (g.releasedAt == null) putNull("released_at") else put("released_at", g.releasedAt); put("release_note", seal(g.releaseNote, "girvis:${g.id}:release_note"))
                })
                RelationalShadowFingerprint.stableItems(g).forEachIndexed { index, item ->
                    writableDatabase.insertOrThrow("items", null, ContentValues().apply {
                        put("id", item.id); put("girvi_id", g.id); put("position", index); put("category_name", seal(item.categoryName, "items:${item.id}:category")); put("item_name", seal(item.itemName, "items:${item.id}:name")); put("quantity", item.quantity); put("gross_weight", seal(item.grossWeightGrams, "items:${item.id}:gross")); put("deduction_weight", seal(item.deductionWeightGrams, "items:${item.id}:deduction")); put("description", seal(item.description, "items:${item.id}:description"))
                    })
                }
                g.payments.forEachIndexed { index, p ->
                    writableDatabase.insertOrThrow("payments", null, ContentValues().apply {
                        put("id", p.id); put("girvi_id", g.id); put("position", index); put("receipt_number", p.receiptNumber); put("amount_paise", p.amountPaise); put("principal_paise", p.principalPaise); put("interest_paise", p.interestPaise); put("charges_paise", p.chargesPaise); put("mode", seal(p.mode, "payments:${p.id}:mode")); put("note", seal(p.note, "payments:${p.id}:note")); put("created_at", p.createdAt); put("is_reversal", if (p.isReversal) 1 else 0); if (p.reversedPaymentId == null) putNull("reversed_payment_id") else put("reversed_payment_id", p.reversedPaymentId)
                    })
                }
            }
            putMeta("schema", snapshot.schemaVersion.toString())
            putMeta("fingerprint", expectedFingerprint)
            putMeta("mirrored_at", System.currentTimeMillis().toString())
            writableDatabase.setTransactionSuccessful()
        } finally {
            writableDatabase.endTransaction()
        }
        return statusAgainst(snapshot).also { check(it.healthy) { it.reason ?: "Shadow verification failed" } }
    }

    @Synchronized
    fun statusAgainst(snapshot: AppSnapshot): RelationalShadowStatus = runCatching {
        val expectedCounts = RelationalShadowFingerprint.expectedCounts(snapshot)
        val actualCounts = RelationalShadowCounts(count("customers"), count("categories"), count("girvis"), count("items"), count("payments"))
        val expectedFingerprint = RelationalShadowFingerprint.sha256(snapshot)
        val storedFingerprint = getMeta("fingerprint")
        val reconstructedFingerprint = RelationalShadowFingerprint.sha256(readSnapshot())
        val healthy = expectedCounts == actualCounts && expectedFingerprint == storedFingerprint && expectedFingerprint == reconstructedFingerprint
        RelationalShadowStatus(
            healthy = healthy,
            expectedCounts = expectedCounts,
            actualCounts = actualCounts,
            expectedFingerprint = expectedFingerprint,
            actualFingerprint = reconstructedFingerprint,
            mirroredAt = getMeta("mirrored_at")?.toLongOrNull(),
            reason = if (healthy) null else "Relational shadow counts/fingerprint mismatch",
        )
    }.getOrElse {
        RelationalShadowStatus(healthy = false, reason = it.message ?: "Relational shadow unreadable")
    }

    private fun readSnapshot(): AppSnapshot {
        val customers = mutableListOf<CustomerRecord>()
        readableDatabase.query("customers", null, null, null, null, null, "id").use { c ->
            while (c.moveToNext()) customers += CustomerRecord(c.getString(c.getColumnIndexOrThrow("id")), open(c.getBlob(c.getColumnIndexOrThrow("name")), "customers:${c.getString(0)}:name"), open(c.getBlob(c.getColumnIndexOrThrow("mobile")), "customers:${c.getString(0)}:mobile"), open(c.getBlob(c.getColumnIndexOrThrow("address")), "customers:${c.getString(0)}:address"), c.getLong(c.getColumnIndexOrThrow("created_at")))
        }
        val categories = mutableListOf<CategoryRecord>()
        readableDatabase.query("categories", null, null, null, null, null, "id").use { c ->
            while (c.moveToNext()) { val id = c.getString(c.getColumnIndexOrThrow("id")); categories += CategoryRecord(id, open(c.getBlob(c.getColumnIndexOrThrow("name")), "categories:$id:name"), c.getInt(c.getColumnIndexOrThrow("active")) == 1) }
        }
        val girvis = mutableListOf<GirviRecord>()
        readableDatabase.query("girvis", null, null, null, null, null, "id").use { c ->
            while (c.moveToNext()) {
                val id = c.getString(c.getColumnIndexOrThrow("id"))
                val items = readItems(id)
                val payments = readPayments(id)
                girvis += GirviRecord(
                    id = id,
                    girviNumber = c.getString(c.getColumnIndexOrThrow("girvi_number")),
                    customerId = c.getString(c.getColumnIndexOrThrow("customer_id")),
                    customerName = open(c.getBlob(c.getColumnIndexOrThrow("customer_name")), "girvis:$id:customer_name"),
                    categoryName = items.firstOrNull()?.categoryName.orEmpty(),
                    itemName = items.firstOrNull()?.itemName.orEmpty(),
                    weightGrams = items.firstOrNull()?.grossWeightGrams.orEmpty(),
                    principalPaise = c.getLong(c.getColumnIndexOrThrow("principal_paise")),
                    monthlyRateBasisPoints = c.getInt(c.getColumnIndexOrThrow("monthly_rate_bp")),
                    createdAt = c.getLong(c.getColumnIndexOrThrow("created_at")),
                    status = c.getString(c.getColumnIndexOrThrow("status")),
                    items = items,
                    payments = payments,
                    manualInterestAdjustmentPaise = c.getLong(c.getColumnIndexOrThrow("adjustment_paise")),
                    releasedAt = c.getColumnIndexOrThrow("released_at").let { if (c.isNull(it)) null else c.getLong(it) },
                    releaseNote = open(c.getBlob(c.getColumnIndexOrThrow("release_note")), "girvis:$id:release_note"),
                )
            }
        }
        return AppSnapshot(schemaVersion = getMeta("schema")?.toIntOrNull() ?: 3, customers = customers, categories = categories, girvis = girvis)
    }

    private fun readItems(girviId: String): List<GirviItemRecord> {
        val result = mutableListOf<GirviItemRecord>()
        readableDatabase.query("items", null, "girvi_id=?", arrayOf(girviId), null, null, "position").use { c ->
            while (c.moveToNext()) { val id = c.getString(c.getColumnIndexOrThrow("id")); result += GirviItemRecord(id, open(c.getBlob(c.getColumnIndexOrThrow("category_name")), "items:$id:category"), open(c.getBlob(c.getColumnIndexOrThrow("item_name")), "items:$id:name"), c.getInt(c.getColumnIndexOrThrow("quantity")), open(c.getBlob(c.getColumnIndexOrThrow("gross_weight")), "items:$id:gross"), open(c.getBlob(c.getColumnIndexOrThrow("deduction_weight")), "items:$id:deduction"), open(c.getBlob(c.getColumnIndexOrThrow("description")), "items:$id:description")) }
        }
        return result
    }

    private fun readPayments(girviId: String): List<PaymentRecord> {
        val result = mutableListOf<PaymentRecord>()
        readableDatabase.query("payments", null, "girvi_id=?", arrayOf(girviId), null, null, "position").use { c ->
            while (c.moveToNext()) { val id = c.getString(c.getColumnIndexOrThrow("id")); val reversedIndex = c.getColumnIndexOrThrow("reversed_payment_id"); result += PaymentRecord(id, c.getString(c.getColumnIndexOrThrow("receipt_number")), c.getLong(c.getColumnIndexOrThrow("amount_paise")), c.getLong(c.getColumnIndexOrThrow("principal_paise")), c.getLong(c.getColumnIndexOrThrow("interest_paise")), c.getLong(c.getColumnIndexOrThrow("charges_paise")), open(c.getBlob(c.getColumnIndexOrThrow("mode")), "payments:$id:mode"), open(c.getBlob(c.getColumnIndexOrThrow("note")), "payments:$id:note"), c.getLong(c.getColumnIndexOrThrow("created_at")), c.getInt(c.getColumnIndexOrThrow("is_reversal")) == 1, if (c.isNull(reversedIndex)) null else c.getString(reversedIndex)) }
        }
        return result
    }

    private fun seal(value: String, aad: String): ByteArray {
        val encrypted = keyManager.encrypt(value.toByteArray(Charsets.UTF_8), aad.toByteArray(Charsets.UTF_8))
        return byteArrayOf(encrypted.iv.size.toByte()) + encrypted.iv + encrypted.ciphertext
    }

    private fun open(value: ByteArray, aad: String): String {
        require(value.isNotEmpty()) { "Encrypted relational value empty" }
        val ivLength = value[0].toInt() and 0xFF
        require(ivLength == 12 && value.size > 1 + ivLength) { "Encrypted relational value invalid" }
        val plaintext = keyManager.decrypt(EncryptedPayload(value.copyOfRange(1 + ivLength, value.size), value.copyOfRange(1, 1 + ivLength)), aad.toByteArray(Charsets.UTF_8))
        return String(plaintext, Charsets.UTF_8)
    }

    private fun count(table: String): Int = readableDatabase.rawQuery("SELECT COUNT(*) FROM $table", null).use { it.moveToFirst(); it.getInt(0) }
    private fun putMeta(key: String, value: String) = writableDatabase.insertOrThrow("metadata", null, ContentValues().apply { put("key", key); put("value", value) })
    private fun getMeta(key: String): String? = readableDatabase.query("metadata", arrayOf("value"), "key=?", arrayOf(key), null, null, null).use { if (it.moveToFirst()) it.getString(0) else null }

    private companion object {
        const val DATABASE_NAME = "girvi_relational_shadow_v1.db"
        const val DATABASE_VERSION = 1
    }
}

data class RelationalShadowStatus(
    val healthy: Boolean,
    val expectedCounts: RelationalShadowCounts? = null,
    val actualCounts: RelationalShadowCounts? = null,
    val expectedFingerprint: String? = null,
    val actualFingerprint: String? = null,
    val mirroredAt: Long? = null,
    val reason: String? = null,
)
