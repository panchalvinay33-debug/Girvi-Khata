package com.girvikhata.app.domain

import com.girvikhata.app.data.AppSnapshot

object CategorySettingsOperations {
    fun rename(snapshot: AppSnapshot, categoryId: String, requestedName: String): AppSnapshot {
        val normalized = requestedName.trim().replace(Regex("\\s+"), " ")
        require(normalized.isNotBlank()) { "Category name required" }
        val target = snapshot.categories.firstOrNull { it.id == categoryId }
            ?: error("Category not found")
        require(snapshot.categories.none { it.id != categoryId && it.name.equals(normalized, ignoreCase = true) }) {
            "Category name already exists"
        }
        if (target.name == normalized) return snapshot

        return snapshot.copy(
            categories = snapshot.categories.map { if (it.id == categoryId) it.copy(name = normalized) else it },
            girvis = snapshot.girvis.map { girvi ->
                val legacyMatches = girvi.categoryName.equals(target.name, ignoreCase = true)
                val updatedItems = girvi.items.map { item ->
                    if (item.categoryName.equals(target.name, ignoreCase = true)) item.copy(categoryName = normalized) else item
                }
                if (legacyMatches || updatedItems != girvi.items) {
                    girvi.copy(
                        categoryName = if (legacyMatches) normalized else girvi.categoryName,
                        items = updatedItems,
                    )
                } else girvi
            },
        )
    }

    fun move(snapshot: AppSnapshot, categoryId: String, direction: Int): AppSnapshot {
        require(direction == -1 || direction == 1) { "Direction must be -1 or 1" }
        val index = snapshot.categories.indexOfFirst { it.id == categoryId }
        require(index >= 0) { "Category not found" }
        val target = index + direction
        if (target !in snapshot.categories.indices) return snapshot
        val reordered = snapshot.categories.toMutableList()
        val moving = reordered.removeAt(index)
        reordered.add(target, moving)
        return snapshot.copy(categories = reordered)
    }
}
