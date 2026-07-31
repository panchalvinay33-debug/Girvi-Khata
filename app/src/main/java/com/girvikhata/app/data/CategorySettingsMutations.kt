package com.girvikhata.app.data

/** Dedicated owner-settings mutations so normal category administration is never audited as restore. */
data class RenameCategoryMutation(
    val categoryId: String,
    val requestedName: String,
) : VerifiedBusinessMutation {
    override val auditLabel: String = "CATEGORY_RENAME"

    override fun apply(snapshot: AppSnapshot): AppSnapshot {
        val current = snapshot.categories.firstOrNull { it.id == categoryId }
            ?: error("Category missing")
        val clean = requestedName.trim()
        require(clean.isNotBlank()) { "Category name required" }
        require(!current.name.equals(clean, ignoreCase = false)) { "Category name unchanged" }
        require(snapshot.categories.none { it.id != categoryId && it.name.equals(clean, ignoreCase = true) }) {
            "Duplicate category name"
        }

        return snapshot.copy(
            categories = snapshot.categories.map {
                if (it.id == categoryId) it.copy(name = clean) else it
            },
            girvis = snapshot.girvis.map { girvi ->
                val legacyName = if (girvi.categoryName.equals(current.name, ignoreCase = true)) clean else girvi.categoryName
                val items = girvi.items.map { item ->
                    if (item.categoryName.equals(current.name, ignoreCase = true)) item.copy(categoryName = clean) else item
                }
                girvi.copy(categoryName = legacyName, items = items)
            },
        )
    }
}

data class ReorderCategoryMutation(
    val categoryId: String,
    val direction: Int,
) : VerifiedBusinessMutation {
    override val auditLabel: String = "CATEGORY_REORDER"

    override fun apply(snapshot: AppSnapshot): AppSnapshot {
        require(direction == -1 || direction == 1) { "Category direction invalid" }
        val index = snapshot.categories.indexOfFirst { it.id == categoryId }
        require(index >= 0) { "Category missing" }
        val target = index + direction
        require(target in snapshot.categories.indices) { "Category already at boundary" }

        val reordered = snapshot.categories.toMutableList().apply {
            val category = removeAt(index)
            add(target, category)
        }
        return snapshot.copy(categories = reordered)
    }
}
