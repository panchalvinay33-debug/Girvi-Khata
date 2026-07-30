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
    fun syncIncremental(snapshot: AppSnapshot, failurePoint: ShadowFailurePoint = ShadowFailurePoint.NONE): RelationalShadowStatus {
        val current = runCatching { readSnapshot() }.getOrElse { return replaceAll(snapshot, failurePoint) }
        val delta = RelationalShadowDeltaPlanner.plan(current, snapshot)
        if (delta.isEmpty) return finalizeVerifiedSync(snapshot, "NO_CHANGE", 0)

        val db = writableDatabase
        db.beginTransaction()
        try {
            val girvisToReplace = delta.deleteGirviIds + delta.upsertGirvis.map { it.id }
            girvisToReplace.forEach { db.delete("girvis", "id=?", arrayOf(it)) }
            failurePoint.throwIf(ShadowFailurePoint.AFTER_CHILD_DELETES)

            delta.deleteCustomerIds.forEach { db.delete("customers", "id=?", arrayOf(it)) }
            delta.deleteCategoryIds.forEach { db.delete("categories", "id=?", arrayOf(it)) }
            delta.upsertCustomers.forEach(::upsertCustomer)
            delta.upsertCategories.forEach(::upsertCategory)
            failurePoint.throwIf(ShadowFailurePoint.AFTER_PARENT_UPSERTS)

            delta.upsertGirvis.forEach(::insertGirviTree)
            failurePoint.throwIf(ShadowFailurePoint.AFTER_GIRVI_UPSERTS)

            writeVerificationMetadata(snapshot, "INCREMENTAL", delta.changedRows)
            failurePoint.throwIf(ShadowFailurePoint.BEFORE_COMMIT)
            db.setTransactionSuccessful()
        } catch (error: Throwable) {
            recordFailureOutsideTransaction(error)
            throw error
        } finally {
            db.endTransaction()
        }
        return verifiedStatus(snapshot).also { check(it.healthy) { it.reason ?: "Incremental shadow verification failed" } }
    }

    @Synchronized
    fun replaceAll(snapshot: AppSnapshot, failurePoint: ShadowFailurePoint = ShadowFailurePoint.NONE): RelationalShadowStatus {
        val db = writableDatabase
        db.beginTransaction()
        try {
            db.delete("payments", null, null)
            db.delete("items", null, null)
            db.delete("girvis", null, null)
            db.delete("categories", null, null)
            db.delete("customers", null, null)
            db.delete("metadata", null, null)
            failurePoint.throwIf(ShadowFailurePoint.AFTER_CHILD_DELETES)

            snapshot.customers.forEach(::insertCustomer)
            snapshot.categories.forEach(::insertCategory)
            failurePoint.throwIf(ShadowFailurePoint.AFTER_PARENT_UPSERTS)
            snapshot.girvis.forEach(::insertGirviTree)
            failurePoint.throwIf(ShadowFailurePoint.AFTER_GIRVI_UPSERTS)

            writeVerificationMetadata(snapshot, "FULL_REBUILD", RelationalShadowFingerprint.expectedCounts(snapshot).let { it.customers + it.categories + it.girvis + it.items + it.payments })
            failurePoint.throwIf(ShadowFailurePoint.BEFORE_COMMIT)
            db.setTransactionSuccessful()
        } catch (error: Throwable) {
            recordFailureOutsideTransaction(error)
            throw error
        } finally {
            db.endTransaction()
        }
        return verifiedStatus(snapshot).also { check(it.healthy) { it.reason ?: "Shadow verification failed" } }
    }

    @Synchronized
    fun statusAgainst(snapshot: AppSnapshot): RelationalShadowStatus = verifiedStatus(snapshot)

    @Synchronized
    fun dualReadComparison(snapshot: AppSnapshot): RelationalDualReadReport = runCatching {
        val relational = readSnapshot()
        val expected = RelationalShadowFingerprint.sha256(snapshot)
        val actual = RelationalShadowFingerprint.sha256(relational)
        RelationalDualReadReport(
            matches = expected == actual,
            snapshotFingerprint = expected,
            relationalFingerprint = actual,
            snapshotCounts = RelationalShadowFingerprint.expectedCounts(snapshot),
            relationalCounts = RelationalShadowFingerprint.expectedCounts(relational),
            comparedAt = System.currentTimeMillis(),
        )
    }.getOrElse {
        RelationalDualReadReport(matches = false, reason = it.message ?: "Dual-read comparison failed")
    }

    fun cutoverEvidence(ownerApproved: Boolean = false): RelationalCutoverEvidence = RelationalCutoverEvidence(
        consecutiveHealthySyncs = getMeta("healthy_syncs")?.toIntOrNull() ?: 0,
        lastHealthyAt = getMeta("mirrored_at")?.toLongOrNull(),
        lastFailureAt = getMeta("last_failure_at")?.toLongOrNull(),
        stressDatasetVerified = getMeta("stress_verified") == "1",
        rollbackSimulationVerified = getMeta("rollback_verified") == "1",
        ownerPhysicalTestApproved = ownerApproved,
    )

    private fun finalizeVerifiedSync(snapshot: AppSnapshot, mode: String, changedRows: Int): RelationalShadowStatus {
        val status = verifiedStatus(snapshot)
        check(status.healthy) { status.reason ?: "Unchanged shadow verification failed" }
        writableDatabase.beginTransaction()
        try {
            writeVerificationMetadata(snapshot, mode, changedRows)
            writableDatabase.setTransactionSuccessful()
        } finally {
            writableDatabase.endTransaction()
        }
        return verifiedStatus(snapshot)
    }

    private fun verifiedStatus(snapshot: AppSnapshot): RelationalShadowStatus = runCatching {
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
            syncMode = getMeta("sync_mode"),
            changedRows = getMeta("changed_rows")?.toIntOrNull(),
            consecutiveHealthySyncs = getMeta("healthy_syncs")?.toIntOrNull() ?: 0,
            lastFailureAt = getMeta("last_failure_at")?.toLongOrNull(),
            reason = if (healthy) null else "Relational shadow counts/fingerprint mismatch",
        )
    }.getOrElse {
        RelationalShadowStatus(healthy = false, reason = it.message ?: "Relational shadow unreadable")
    }

    private fun writeVerificationMetadata(snapshot: AppSnapshot, mode: String, changedRows: Int) {
        val previousHealthy = getMeta("healthy_syncs")?.toIntOrNull() ?: 0
        putMeta("schema", snapshot.schemaVersion.toString())
        putMeta("fingerprint", RelationalShadowFingerprint.sha256(snapshot))
        putMeta("mirrored_at", System.currentTimeMillis().toString())
        putMeta("sync_mode", mode)
        putMeta("changed_rows", changedRows.toString())
        putMeta("healthy_syncs", (previousHealthy + 1).toString())
    }

    private fun recordFailureOutsideTransaction(error: Throwable) {
        runCatching {
            val values = ContentValues().apply {
                put("key", "last_failure_at")
                put("value", System.currentTimeMillis().toString())
            }
            writableDatabase.insertWithOnConflict("metadata", null, values, SQLiteDatabase.CONFLICT_REPLACE)
            val reason = ContentValues().apply {
                put("key", "last_failure_reason")
                put("value", (error.message ?: error.javaClass.simpleName).take(240))
            }
            writableDatabase.insertWithOnConflict("metadata", null, reason, SQLiteDatabase.CONFLICT_REPLACE)
        }
    }

    private fun insertCustomer(c: CustomerRecord) {
        writableDatabase.insertOrThrow("customers", null, customerValues(c))
    }

    private fun upsertCustomer(c: CustomerRecord) {
        val values = customerValues(c)
        if (writableDatabase.update("customers", values, "id=?", arrayOf(c.id)) == 0) insertCustomer(c)
    }

    private fun customerValues(c: CustomerRecord) = ContentValues().apply {
        put("id", c.id)
        put("name", seal(c.name, "customers:${c.id}:name"))
        put("mobile", seal(c.mobile, "customers:${c.id}:mobile"))
        put("address", seal(c.address, "customers:${c.id}:address"))
        put("created_at", c.createdAt)
    }

    private fun insertCategory(c: CategoryRecord) {
        writableDatabase.insertOrThrow("categories", null, categoryValues(c))
    }

    private fun upsertCategory(c: CategoryRecord) {
        val values = categoryValues(c)
        if (writableDatabase.update("categories", values, "id=?", arrayOf(c.id)) == 0) insertCategory(c)
    }

    private fun categoryValues(c: CategoryRecord) = ContentValues().apply {
        put("id", c.id)
        put("name", seal(c.name, "categories:${c.id}:name"))
        put("active", if (c.active) 1 else 0)
    }

    private fun insertGirviTree(g: GirviRecord) {
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

    private fun readSnapshot(): AppSnapshot {
        val customers = mutableListOf<CustomerRecord>()
        readableDatabase.query("customers", null, null, null, null, null, "id").use { c ->
            while (c.moveToNext()) { val id = c.getString(c.getColumnIndexOrThrow("id")); customers += CustomerRecord(id, open(c.getBlob(c.getColumnIndexOrThrow("name")), "customers:$id:name"), open(c.getBlob(c.getColumnIndexOrThrow("mobile")), "customers:$id:mobile"), open(c.getBlob(c.getColumnIndexOrThrow("address")), "customers:$id:address"), c.getLong(c.getColumnIndexOrThrow("created_at"))) }
        }
        val categories = mutableListOf<CategoryRecord>()
        readableDatabase.query("categories", null, null, null, null, null, "id").use { c ->
            while (c.moveToNext()) { val id = c.getString(c.getColumnIndexOrThrow("id")); categories += CategoryRecord(id, open(c.getBlob(c.getColumnIndexOrThrow("name")), "categories:$id:name"), c.getInt(c.getColumnIndexOrThrow("active")) == 1) }
        }
        val girvis = mutableListOf<GirviRecord>()
        readableDatabase.query("girvis", null, null, null, null, null, "id").use { c ->
            while (c.moveToNext()) {
                val id = c.getString(c.getColumnIndexOrThrow("id")); val items = readItems(id); val payments = readPayments(id)
                girvis += GirviRecord(id, c.getString(c.getColumnIndexOrThrow("girvi_number")), c.getString(c.getColumnIndexOrThrow("customer_id")), open(c.getBlob(c.getColumnIndexOrThrow("customer_name")), "girvis:$id:customer_name"), items.firstOrNull()?.categoryName.orEmpty(), items.firstOrNull()?.itemName.orEmpty(), items.firstOrNull()?.grossWeightGrams.orEmpty(), c.getLong(c.getColumnIndexOrThrow("principal_paise")), c.getInt(c.getColumnIndexOrThrow("monthly_rate_bp")), c.getLong(c.getColumnIndexOrThrow("created_at")), c.getString(c.getColumnIndexOrThrow("status")), items, payments, c.getLong(c.getColumnIndexOrThrow("adjustment_paise")), c.getColumnIndexOrThrow("released_at").let { if (c.isNull(it)) null else c.getLong(it) }, open(c.getBlob(c.getColumnIndexOrThrow("release_note")), "girvis:$id:release_note"))
            }
        }
        return AppSnapshot(getMeta("schema")?.toIntOrNull() ?: 3, customers, categories, girvis)
    }

    private fun readItems(girviId: String): List<GirviItemRecord> {
        val result = mutableListOf<GirviItemRecord>()
        readableDatabase.query("items", null, "girvi_id=?", arrayOf(girviId), null, null, "position").use { c -> while (c.moveToNext()) { val id = c.getString(c.getColumnIndexOrThrow("id")); result += GirviItemRecord(id, open(c.getBlob(c.getColumnIndexOrThrow("category_name")), "items:$id:category"), open(c.getBlob(c.getColumnIndexOrThrow("item_name")), "items:$id:name"), c.getInt(c.getColumnIndexOrThrow("quantity")), open(c.getBlob(c.getColumnIndexOrThrow("gross_weight")), "items:$id:gross"), open(c.getBlob(c.getColumnIndexOrThrow("deduction_weight")), "items:$id:deduction"), open(c.getBlob(c.getColumnIndexOrThrow("description")), "items:$id:description")) } }
        return result
    }

    private fun readPayments(girviId: String): List<PaymentRecord> {
        val result = mutableListOf<PaymentRecord>()
        readableDatabase.query("payments", null, "girvi_id=?", arrayOf(girviId), null, null, "position").use { c -> while (c.moveToNext()) { val id = c.getString(c.getColumnIndexOrThrow("id")); val reversedIndex = c.getColumnIndexOrThrow("reversed_payment_id"); result += PaymentRecord(id, c.getString(c.getColumnIndexOrThrow("receipt_number")), c.getLong(c.getColumnIndexOrThrow("amount_paise")), c.getLong(c.getColumnIndexOrThrow("principal_paise")), c.getLong(c.getColumnIndexOrThrow("interest_paise")), c.getLong(c.getColumnIndexOrThrow("charges_paise")), open(c.getBlob(c.getColumnIndexOrThrow("mode")), "payments:$id:mode"), open(c.getBlob(c.getColumnIndexOrThrow("note")), "payments:$id:note"), c.getLong(c.getColumnIndexOrThrow("created_at")), c.getInt(c.getColumnIndexOrThrow("is_reversal")) == 1, if (c.isNull(reversedIndex)) null else c.getString(reversedIndex)) } }
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
        return String(keyManager.decrypt(EncryptedPayload(value.copyOfRange(1 + ivLength, value.size), value.copyOfRange(1, 1 + ivLength)), aad.toByteArray(Charsets.UTF_8)), Charsets.UTF_8)
    }

    private fun count(table: String): Int = readableDatabase.rawQuery("SELECT COUNT(*) FROM $table", null).use { it.moveToFirst(); it.getInt(0) }
    private fun putMeta(key: String, value: String) { writableDatabase.insertWithOnConflict("metadata", null, ContentValues().apply { put("key", key); put("value", value) }, SQLiteDatabase.CONFLICT_REPLACE) }
    private fun getMeta(key: String): String? = readableDatabase.query("metadata", arrayOf("value"), "key=?", arrayOf(key), null, null, null).use { if (it.moveToFirst()) it.getString(0) else null }

    private companion object { const val DATABASE_NAME = "girvi_relational_shadow_v1.db"; const val DATABASE_VERSION = 1 }
}

enum class ShadowFailurePoint { NONE, AFTER_CHILD_DELETES, AFTER_PARENT_UPSERTS, AFTER_GIRVI_UPSERTS, BEFORE_COMMIT;
    fun throwIf(point: ShadowFailurePoint) { if (this == point) error("Injected relational failure at $point") }
}

data class RelationalDualReadReport(
    val matches: Boolean,
    val snapshotFingerprint: String? = null,
    val relationalFingerprint: String? = null,
    val snapshotCounts: RelationalShadowCounts? = null,
    val relationalCounts: RelationalShadowCounts? = null,
    val comparedAt: Long? = null,
    val reason: String? = null,
)

data class RelationalShadowStatus(
    val healthy: Boolean,
    val expectedCounts: RelationalShadowCounts? = null,
    val actualCounts: RelationalShadowCounts? = null,
    val expectedFingerprint: String? = null,
    val actualFingerprint: String? = null,
    val mirroredAt: Long? = null,
    val syncMode: String? = null,
    val changedRows: Int? = null,
    val consecutiveHealthySyncs: Int = 0,
    val lastFailureAt: Long? = null,
    val reason: String? = null,
)
