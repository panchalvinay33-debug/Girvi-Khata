package com.girvikhata.app.domain

import java.util.Locale
import java.util.UUID

enum class MasterKind { ITEM, UNIT, INTEREST_PLAN, PAYMENT_MODE, LOCKER }

data class MasterEntry(
    val id: String = UUID.randomUUID().toString(),
    val kind: MasterKind,
    val name: String,
    val active: Boolean = true,
    val categoryName: String = "",
    val rateBasisPoints: Int = 0,
)

data class MasterCatalog(val entries: List<MasterEntry> = defaults()) {
    companion object {
        fun defaults() = listOf(
            MasterEntry(kind = MasterKind.UNIT, name = "piece"),
            MasterEntry(kind = MasterKind.UNIT, name = "gram"),
            MasterEntry(kind = MasterKind.UNIT, name = "kilogram"),
            MasterEntry(kind = MasterKind.PAYMENT_MODE, name = "CASH"),
            MasterEntry(kind = MasterKind.PAYMENT_MODE, name = "UPI"),
            MasterEntry(kind = MasterKind.PAYMENT_MODE, name = "BANK"),
            MasterEntry(kind = MasterKind.INTEREST_PLAN, name = "Monthly 2%", rateBasisPoints = 200),
            MasterEntry(kind = MasterKind.LOCKER, name = "Main Locker"),
        )
    }
}

object MasterCatalogOperations {
    fun add(
        catalog: MasterCatalog,
        kind: MasterKind,
        name: String,
        categoryName: String = "",
        rateBasisPoints: Int = 0,
    ): MasterCatalog {
        val clean = normalizeDisplay(name)
        require(clean.length in 1..60) { "Name required (max 60)" }
        require(rateBasisPoints in 0..100_000) { "Interest rate invalid" }
        val category = normalizeDisplay(categoryName)
        require(catalog.entries.none { it.kind == kind && key(it.name) == key(clean) && key(it.categoryName) == key(category) }) {
            "Duplicate master entry"
        }
        return catalog.copy(entries = catalog.entries + MasterEntry(kind = kind, name = clean, categoryName = category, rateBasisPoints = rateBasisPoints))
    }

    fun rename(catalog: MasterCatalog, id: String, name: String): MasterCatalog {
        val current = catalog.entries.firstOrNull { it.id == id } ?: error("Master entry not found")
        val clean = normalizeDisplay(name)
        require(clean.length in 1..60) { "Name required (max 60)" }
        require(catalog.entries.none { it.id != id && it.kind == current.kind && key(it.name) == key(clean) && key(it.categoryName) == key(current.categoryName) }) {
            "Duplicate master entry"
        }
        return catalog.copy(entries = catalog.entries.map { if (it.id == id) it.copy(name = clean) else it })
    }

    fun toggle(catalog: MasterCatalog, id: String): MasterCatalog {
        require(catalog.entries.any { it.id == id }) { "Master entry not found" }
        return catalog.copy(entries = catalog.entries.map { if (it.id == id) it.copy(active = !it.active) else it })
    }

    fun move(catalog: MasterCatalog, id: String, direction: Int): MasterCatalog {
        require(direction == -1 || direction == 1) { "Direction invalid" }
        val list = catalog.entries.toMutableList()
        val index = list.indexOfFirst { it.id == id }
        require(index >= 0) { "Master entry not found" }
        val target = index + direction
        if (target !in list.indices || list[target].kind != list[index].kind) return catalog
        val value = list.removeAt(index)
        list.add(target, value)
        return catalog.copy(entries = list)
    }

    fun active(catalog: MasterCatalog, kind: MasterKind): List<MasterEntry> = catalog.entries.filter { it.kind == kind && it.active }

    private fun normalizeDisplay(value: String): String = value.trim().replace(Regex("\\s+"), " ")
    private fun key(value: String): String = normalizeDisplay(value).lowercase(Locale.ROOT)
}
