package com.girvikhata.app.custody

data class CustodyIntegrityIssue(
    val code: String,
    val detail: String,
)

/**
 * Read-only consistency checks between business Girvi/item identities and custody/placement state.
 * This validator never repairs or mutates records automatically; callers can journal issues and let
 * the owner resolve them explicitly.
 */
object CustodyIntegrityValidator {
    fun validate(
        knownItems: Set<Pair<String, String>>,
        releasedGirviIds: Set<String>,
        snapshot: CustodyPlacementSnapshot,
    ): List<CustodyIntegrityIssue> {
        val issues = mutableListOf<CustodyIntegrityIssue>()
        val knownLocationIds = snapshot.locations.map { it.id }.toSet()
        val knownPartyIds = snapshot.parties.map { it.id }.toSet()
        val lotById = snapshot.lots.associateBy { it.id }

        snapshot.lots.forEach { lot ->
            if (lot.partyId !in knownPartyIds) {
                issues += CustodyIntegrityIssue("LOT_PARTY_MISSING", "${lot.lotNumber}: party missing")
            }
            lot.items.forEach { item ->
                if ((item.girviId to item.itemId) !in knownItems) {
                    issues += CustodyIntegrityIssue("LOT_ITEM_UNKNOWN", "${lot.lotNumber}: unknown item ${item.itemId}")
                }
                if (item.removedAt == null && item.girviId in releasedGirviIds) {
                    issues += CustodyIntegrityIssue("RELEASED_GIRVI_EXTERNAL", "${lot.lotNumber}: released Girvi ${item.girviId} still external")
                }
                if (item.removedAt != null && item.removedAt < item.addedAt) {
                    issues += CustodyIntegrityIssue("LOT_ITEM_DATE_INVALID", "${lot.lotNumber}: removal before placement for ${item.itemId}")
                }
            }
        }

        val activeExternalByItem = snapshot.lots
            .flatMap { lot -> lot.items.filter { it.removedAt == null }.map { it.itemId to lot.lotNumber } }
            .groupBy({ it.first }, { it.second })
        activeExternalByItem.filterValues { it.size > 1 }.forEach { (itemId, lots) ->
            issues += CustodyIntegrityIssue("ITEM_MULTI_ACTIVE_LOTS", "$itemId active in ${lots.joinToString()}")
        }

        snapshot.movements.forEach { movement ->
            if ((movement.girviId to movement.itemId) !in knownItems) {
                issues += CustodyIntegrityIssue("MOVEMENT_ITEM_UNKNOWN", "Unknown movement item ${movement.itemId}")
            }
            when (movement.destinationType) {
                "LOCATION" -> if (movement.destinationId !in knownLocationIds) {
                    issues += CustodyIntegrityIssue("MOVEMENT_LOCATION_MISSING", "${movement.itemId}: storage location missing")
                }
                "EXTERNAL" -> {
                    if (movement.destinationId !in knownPartyIds) {
                        issues += CustodyIntegrityIssue("MOVEMENT_PARTY_MISSING", "${movement.itemId}: external party missing")
                    }
                    val lotId = movement.lotId
                    if (lotId.isNullOrBlank() || lotId !in lotById) {
                        issues += CustodyIntegrityIssue("MOVEMENT_LOT_MISSING", "${movement.itemId}: external lot missing")
                    }
                }
                else -> issues += CustodyIntegrityIssue("MOVEMENT_TYPE_INVALID", "${movement.itemId}: ${movement.destinationType}")
            }
        }

        return issues.distinct()
    }
}
