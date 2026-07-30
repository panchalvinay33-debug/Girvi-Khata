package com.girvikhata.app.data

import com.girvikhata.app.domain.MasterCatalog
import com.girvikhata.app.domain.MasterKind

/** Schema-neutral extraction used before dedicated relational master-id columns are introduced. */
object RelationalMasterLinkResolver {
    private val unitPattern = Regex("(?:^| • )Unit: ([^•]+)")
    private val lockerPattern = Regex("(?:^| • )Locker: ([^•]+)")

    fun resolve(item: GirviItemRecord, catalogs: MasterCatalog): RelationalMasterLinks {
        val unitName = unitPattern.find(item.description)?.groupValues?.getOrNull(1)?.trim().orEmpty()
        val lockerName = lockerPattern.find(item.description)?.groupValues?.getOrNull(1)?.trim().orEmpty()
        return RelationalMasterLinks(
            unitId = catalogs.entries.firstOrNull { it.kind == MasterKind.UNIT && it.name.equals(unitName, true) }?.id,
            lockerId = catalogs.entries.firstOrNull { it.kind == MasterKind.LOCKER && it.name.equals(lockerName, true) }?.id,
            itemMasterId = catalogs.entries.firstOrNull {
                it.kind == MasterKind.ITEM && it.name.equals(item.itemName, true) &&
                    (it.categoryName.isBlank() || it.categoryName.equals(item.categoryName, true))
            }?.id,
        )
    }
}

data class RelationalMasterLinks(
    val itemMasterId: String?,
    val unitId: String?,
    val lockerId: String?,
)
