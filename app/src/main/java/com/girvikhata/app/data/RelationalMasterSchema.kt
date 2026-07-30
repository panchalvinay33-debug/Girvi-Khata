package com.girvikhata.app.data

import com.girvikhata.app.domain.MasterCatalog
import com.girvikhata.app.domain.MasterEntry
import com.girvikhata.app.domain.MasterKind

/** SQL schema and deterministic backfill plan for relational master IDs. */
object RelationalMasterSchema {
    const val VERSION = 2

    val createMastersTable = """
        CREATE TABLE IF NOT EXISTS masters(
            id TEXT PRIMARY KEY,
            kind TEXT NOT NULL CHECK(kind IN ('ITEM','UNIT','INTEREST_PLAN','PAYMENT_MODE','LOCKER')),
            name BLOB NOT NULL,
            active INTEGER NOT NULL CHECK(active IN (0,1)),
            category_name BLOB NOT NULL,
            rate_basis_points INTEGER NOT NULL CHECK(rate_basis_points >= 0)
        )
    """.trimIndent()

    val createIndexes = listOf(
        "CREATE INDEX IF NOT EXISTS idx_masters_kind_active ON masters(kind, active)",
        "CREATE INDEX IF NOT EXISTS idx_items_item_master ON items(item_master_id)",
        "CREATE INDEX IF NOT EXISTS idx_items_unit ON items(unit_id)",
        "CREATE INDEX IF NOT EXISTS idx_items_locker ON items(locker_id)",
        "CREATE INDEX IF NOT EXISTS idx_girvis_interest_plan ON girvis(interest_plan_id)",
        "CREATE INDEX IF NOT EXISTS idx_payments_mode_master ON payments(payment_mode_id)",
    )

    val upgradeV1ToV2 = listOf(
        createMastersTable,
        "ALTER TABLE items ADD COLUMN item_master_id TEXT REFERENCES masters(id) ON DELETE SET NULL",
        "ALTER TABLE items ADD COLUMN unit_id TEXT REFERENCES masters(id) ON DELETE SET NULL",
        "ALTER TABLE items ADD COLUMN locker_id TEXT REFERENCES masters(id) ON DELETE SET NULL",
        "ALTER TABLE girvis ADD COLUMN interest_plan_id TEXT REFERENCES masters(id) ON DELETE SET NULL",
        "ALTER TABLE payments ADD COLUMN payment_mode_id TEXT REFERENCES masters(id) ON DELETE SET NULL",
    ) + createIndexes

    fun normalized(catalog: MasterCatalog): List<MasterEntry> {
        require(catalog.entries.map { it.id }.toSet().size == catalog.entries.size) { "Duplicate master IDs" }
        return catalog.entries.sortedWith(compareBy<MasterEntry>({ it.kind.name }, { it.id })).onEach {
            require(it.id.isNotBlank()) { "Master ID required" }
            require(it.name.isNotBlank()) { "Master name required" }
            require(it.rateBasisPoints in 0..100_000) { "Master rate invalid" }
            if (it.kind != MasterKind.INTEREST_PLAN) require(it.rateBasisPoints == 0) { "Rate only valid for interest plan" }
        }
    }
}

data class RelationalMasterCoverage(
    val totalItems: Int,
    val itemMasterLinked: Int,
    val unitLinked: Int,
    val lockerLinked: Int,
    val totalGirvis: Int,
    val interestPlanLinked: Int,
    val totalPayments: Int,
    val paymentModeLinked: Int,
) {
    val complete: Boolean
        get() = totalItems == itemMasterLinked && totalItems == unitLinked && totalItems == lockerLinked &&
            totalGirvis == interestPlanLinked && totalPayments == paymentModeLinked
}
