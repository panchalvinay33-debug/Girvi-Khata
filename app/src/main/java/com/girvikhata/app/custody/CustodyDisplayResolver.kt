package com.girvikhata.app.custody

data class ItemCustodyDisplay(
    val itemId: String,
    val label: String,
    val destinationType: String?,
    val movedAt: Long?,
    val isExternal: Boolean,
    val lotNumber: String? = null,
)

object CustodyDisplayResolver {
    fun currentItem(snapshot: CustodyPlacementSnapshot, itemId: String): ItemCustodyDisplay {
        val movement = snapshot.movements
            .filter { it.itemId == itemId }
            .maxWithOrNull(compareBy<CustodyMovement> { it.movedAt }.thenBy { it.createdAt })
            ?: return ItemCustodyDisplay(itemId, "Not assigned", null, null, false)

        return when (movement.destinationType) {
            "LOCATION" -> {
                val location = snapshot.locations.firstOrNull { it.id == movement.destinationId }
                ItemCustodyDisplay(
                    itemId = itemId,
                    label = location?.name ?: "Unknown location",
                    destinationType = "LOCATION",
                    movedAt = movement.movedAt,
                    isExternal = false,
                )
            }
            "EXTERNAL" -> {
                val party = snapshot.parties.firstOrNull { it.id == movement.destinationId }
                val lot = movement.lotId?.let { id -> snapshot.lots.firstOrNull { it.id == id } }
                val partyName = party?.name ?: "External party"
                val label = if (lot == null) partyName else "$partyName • ${lot.lotNumber}"
                ItemCustodyDisplay(
                    itemId = itemId,
                    label = label,
                    destinationType = "EXTERNAL",
                    movedAt = movement.movedAt,
                    isExternal = true,
                    lotNumber = lot?.lotNumber,
                )
            }
            else -> ItemCustodyDisplay(itemId, "Unknown", movement.destinationType, movement.movedAt, false)
        }
    }

    fun girviSummary(snapshot: CustodyPlacementSnapshot, itemIds: Collection<String>): String {
        if (itemIds.isEmpty()) return "No items"
        val displays = itemIds.map { currentItem(snapshot, it) }
        val grouped = displays.groupingBy { it.label }.eachCount()
        return grouped.entries
            .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key.lowercase() })
            .joinToString(" • ") { (label, count) -> if (count == 1) label else "$count× $label" }
    }
}
