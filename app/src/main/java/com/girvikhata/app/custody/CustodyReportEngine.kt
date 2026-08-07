package com.girvikhata.app.custody

import com.girvikhata.app.domain.ExternalPlacementLedger

data class StorageReportRow(
    val locationId: String,
    val locationName: String,
    val itemCount: Int,
)

data class ExternalPartyReportRow(
    val partyId: String,
    val partyName: String,
    val activeLotCount: Int,
    val activeItemCount: Int,
    val principalOutstandingPaise: Long,
    val interestOutstandingPaise: Long,
    val totalDuePaise: Long,
)

data class CustodyMovementReportRow(
    val movementId: String,
    val girviId: String,
    val itemId: String,
    val destinationLabel: String,
    val movedAt: Long,
    val note: String,
)

data class CustodyReport(
    val storage: List<StorageReportRow>,
    val externalParties: List<ExternalPartyReportRow>,
    val recentMovements: List<CustodyMovementReportRow>,
    val unassignedItemIds: Set<String>,
)

object CustodyReportEngine {
    fun build(
        snapshot: CustodyPlacementSnapshot,
        allItemIds: Set<String>,
        at: Long,
        movementLimit: Int = 100,
    ): CustodyReport {
        val latestByItem = snapshot.movements
            .groupBy { it.itemId }
            .mapValues { (_, entries) -> entries.maxWithOrNull(compareBy<CustodyMovement> { it.movedAt }.thenBy { it.createdAt })!! }

        val storageRows = snapshot.locations.map { location ->
            StorageReportRow(
                locationId = location.id,
                locationName = location.name,
                itemCount = latestByItem.values.count { it.destinationType == "LOCATION" && it.destinationId == location.id },
            )
        }.filter { it.itemCount > 0 }.sortedWith(compareByDescending<StorageReportRow> { it.itemCount }.thenBy { it.locationName.lowercase() })

        val partyRows = snapshot.parties.map { party ->
            val activeLots = snapshot.lots.filter { it.partyId == party.id && it.status == "ACTIVE" }
            val activeItems = activeLots.flatMap { lot -> lot.items.filter { it.removedAt == null }.map { it.itemId } }.distinct()
            val projections = activeLots.map { lot ->
                runCatching { ExternalPlacementLedger.project(lot.fundingAdvances, lot.fundingPayments, at) }.getOrNull()
            }
            ExternalPartyReportRow(
                partyId = party.id,
                partyName = party.name,
                activeLotCount = activeLots.size,
                activeItemCount = activeItems.size,
                principalOutstandingPaise = projections.sumOf { it?.principalOutstandingPaise ?: 0L },
                interestOutstandingPaise = projections.sumOf { it?.interestOutstandingPaise ?: 0L },
                totalDuePaise = projections.sumOf { it?.totalDuePaise ?: 0L },
            )
        }.filter { it.activeLotCount > 0 || it.totalDuePaise > 0L }
            .sortedWith(compareByDescending<ExternalPartyReportRow> { it.totalDuePaise }.thenBy { it.partyName.lowercase() })

        val recent = snapshot.movements.sortedWith(
            compareByDescending<CustodyMovement> { it.movedAt }.thenByDescending { it.createdAt },
        ).take(movementLimit.coerceIn(1, 500)).map { movement ->
            val destination = when (movement.destinationType) {
                "LOCATION" -> snapshot.locations.firstOrNull { it.id == movement.destinationId }?.name ?: "Unknown location"
                "EXTERNAL" -> {
                    val party = snapshot.parties.firstOrNull { it.id == movement.destinationId }?.name ?: "External party"
                    val lot = movement.lotId?.let { id -> snapshot.lots.firstOrNull { it.id == id }?.lotNumber }
                    if (lot.isNullOrBlank()) party else "$party • $lot"
                }
                else -> "Unknown"
            }
            CustodyMovementReportRow(
                movementId = movement.id,
                girviId = movement.girviId,
                itemId = movement.itemId,
                destinationLabel = destination,
                movedAt = movement.movedAt,
                note = movement.note,
            )
        }

        return CustodyReport(
            storage = storageRows,
            externalParties = partyRows,
            recentMovements = recent,
            unassignedItemIds = allItemIds - latestByItem.keys,
        )
    }
}
