package com.girvikhata.app.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import com.girvikhata.app.domain.MasterCatalog
import com.girvikhata.app.domain.MasterEntry
import com.girvikhata.app.security.DeviceKeyManager

/**
 * Idempotently extends the rebuildable relational shadow with master-ID links.
 * It never writes the authoritative encrypted business snapshot.
 */
class RelationalMasterSchemaInstaller(
    context: Context,
    private val keyManager: DeviceKeyManager = DeviceKeyManager(),
) {
    private val appContext = context.applicationContext

    fun synchronize(snapshot: AppSnapshot, catalog: MasterCatalog): RelationalMasterInstallResult {
        val plan = RelationalMasterBackfill.plan(snapshot, catalog)
        EncryptedRelationalShadowStore(appContext).use { helper ->
            val db = helper.writableDatabase
            db.beginTransaction()
            try {
                ensureSchema(db)
                db.delete("masters", null, null)
                plan.masters.forEach { insertMaster(db, it) }
                plan.items.forEach { link ->
                    db.update("items", ContentValues().apply {
                        putNullable("item_master_id", link.itemMasterId)
                        putNullable("unit_id", link.unitId)
                        putNullable("locker_id", link.lockerId)
                    }, "id=?", arrayOf(link.itemId))
                }
                plan.girvis.forEach { link ->
                    db.update("girvis", ContentValues().apply { putNullable("interest_plan_id", link.interestPlanId) }, "id=?", arrayOf(link.girviId))
                }
                plan.payments.forEach { link ->
                    db.update("payments", ContentValues().apply { putNullable("payment_mode_id", link.paymentModeId) }, "id=?", arrayOf(link.paymentId))
                }
                val actual = readCoverage(db)
                check(actual == plan.coverage) { "Relational master coverage mismatch" }
                check(count(db, "masters") == plan.masters.size) { "Relational master row count mismatch" }
                putMeta(db, "master_schema", RelationalMasterSchema.VERSION.toString())
                putMeta(db, "master_rows", plan.masters.size.toString())
                putMeta(db, "master_links_verified_at", System.currentTimeMillis().toString())
                putMeta(db, "master_link_coverage", encode(actual))
                db.setTransactionSuccessful()
                return RelationalMasterInstallResult(true, plan.masters.size, actual)
            } finally {
                db.endTransaction()
            }
        }
    }

    fun status(): RelationalMasterInstallResult = runCatching {
        EncryptedRelationalShadowStore(appContext).use { helper ->
            val db = helper.readableDatabase
            if (!tableExists(db, "masters") || !columnExists(db, "items", "item_master_id")) {
                return RelationalMasterInstallResult(false, reason = "Master schema not installed")
            }
            RelationalMasterInstallResult(
                installed = getMeta(db, "master_schema") == RelationalMasterSchema.VERSION.toString(),
                masterRows = count(db, "masters"),
                coverage = readCoverage(db),
                verifiedAt = getMeta(db, "master_links_verified_at")?.toLongOrNull(),
            )
        }
    }.getOrElse { RelationalMasterInstallResult(false, reason = it.message ?: "Master schema unreadable") }

    private fun ensureSchema(db: SQLiteDatabase) {
        db.execSQL(RelationalMasterSchema.createMastersTable)
        addColumnIfMissing(db, "items", "item_master_id", "TEXT REFERENCES masters(id) ON DELETE SET NULL")
        addColumnIfMissing(db, "items", "unit_id", "TEXT REFERENCES masters(id) ON DELETE SET NULL")
        addColumnIfMissing(db, "items", "locker_id", "TEXT REFERENCES masters(id) ON DELETE SET NULL")
        addColumnIfMissing(db, "girvis", "interest_plan_id", "TEXT REFERENCES masters(id) ON DELETE SET NULL")
        addColumnIfMissing(db, "payments", "payment_mode_id", "TEXT REFERENCES masters(id) ON DELETE SET NULL")
        RelationalMasterSchema.createIndexes.forEach(db::execSQL)
    }

    private fun insertMaster(db: SQLiteDatabase, entry: MasterEntry) {
        db.insertOrThrow("masters", null, ContentValues().apply {
            put("id", entry.id)
            put("kind", entry.kind.name)
            put("name", seal(entry.name, "masters:${entry.id}:name"))
            put("active", if (entry.active) 1 else 0)
            put("category_name", seal(entry.categoryName, "masters:${entry.id}:category"))
            put("rate_basis_points", entry.rateBasisPoints)
        })
    }

    private fun seal(value: String, aad: String): ByteArray {
        val encrypted = keyManager.encrypt(value.toByteArray(Charsets.UTF_8), aad.toByteArray(Charsets.UTF_8))
        return byteArrayOf(encrypted.iv.size.toByte()) + encrypted.iv + encrypted.ciphertext
    }

    private fun addColumnIfMissing(db: SQLiteDatabase, table: String, column: String, declaration: String) {
        if (!columnExists(db, table, column)) db.execSQL("ALTER TABLE $table ADD COLUMN $column $declaration")
    }

    private fun tableExists(db: SQLiteDatabase, table: String): Boolean =
        db.rawQuery("SELECT 1 FROM sqlite_master WHERE type='table' AND name=?", arrayOf(table)).use { it.moveToFirst() }

    private fun columnExists(db: SQLiteDatabase, table: String, column: String): Boolean =
        db.rawQuery("PRAGMA table_info($table)", null).use { cursor ->
            val index = cursor.getColumnIndexOrThrow("name")
            var found = false
            while (cursor.moveToNext()) if (cursor.getString(index) == column) found = true
            found
        }

    private fun readCoverage(db: SQLiteDatabase) = RelationalMasterCoverage(
        totalItems = count(db, "items"),
        itemMasterLinked = countNonNull(db, "items", "item_master_id"),
        unitLinked = countNonNull(db, "items", "unit_id"),
        lockerLinked = countNonNull(db, "items", "locker_id"),
        totalGirvis = count(db, "girvis"),
        interestPlanLinked = countNonNull(db, "girvis", "interest_plan_id"),
        totalPayments = count(db, "payments"),
        paymentModeLinked = countNonNull(db, "payments", "payment_mode_id"),
    )

    private fun count(db: SQLiteDatabase, table: String): Int = db.rawQuery("SELECT COUNT(*) FROM $table", null).use { it.moveToFirst(); it.getInt(0) }
    private fun countNonNull(db: SQLiteDatabase, table: String, column: String): Int = db.rawQuery("SELECT COUNT(*) FROM $table WHERE $column IS NOT NULL", null).use { it.moveToFirst(); it.getInt(0) }
    private fun putMeta(db: SQLiteDatabase, key: String, value: String) { db.insertWithOnConflict("metadata", null, ContentValues().apply { put("key", key); put("value", value) }, SQLiteDatabase.CONFLICT_REPLACE) }
    private fun getMeta(db: SQLiteDatabase, key: String): String? = db.query("metadata", arrayOf("value"), "key=?", arrayOf(key), null, null, null).use { if (it.moveToFirst()) it.getString(0) else null }
    private fun encode(c: RelationalMasterCoverage) = listOf(c.totalItems, c.itemMasterLinked, c.unitLinked, c.lockerLinked, c.totalGirvis, c.interestPlanLinked, c.totalPayments, c.paymentModeLinked).joinToString(":")
    private fun ContentValues.putNullable(key: String, value: String?) { if (value == null) putNull(key) else put(key, value) }
}

data class RelationalMasterInstallResult(
    val installed: Boolean,
    val masterRows: Int = 0,
    val coverage: RelationalMasterCoverage? = null,
    val verifiedAt: Long? = null,
    val reason: String? = null,
)
