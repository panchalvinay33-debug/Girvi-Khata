package com.girvikhata.app.data

import com.girvikhata.app.custody.CustodyPlacementSnapshot
import com.girvikhata.app.custody.CustodyPlacementStore

/** Owner-authorized destructive purge of one customer's complete local account. */
data class CustomerPurgeResult(
    val customerName: String,
    val girviCount: Int,
    val itemCount: Int,
    val paymentCount: Int,
    val custodyMovementCount: Int,
    val placementItemCount: Int,
    val mediaDeleted: Int,
    val mediaDeleteFailed: Int,
)

class CustomerPurgeCoordinator(
    private val records: EncryptedRecordStore,
    private val custody: CustodyPlacementStore,
    private val media: SecureMediaVault,
) {
    @Synchronized
    fun purge(customerId: String): CustomerPurgeResult {
        val beforeBusiness = records.load()
        val beforeCustody = custody.load()
        val customer = beforeBusiness.customers.firstOrNull { it.id == customerId }
            ?: error("Customer nahi mila")
        val girvis = beforeBusiness.girvis.filter { it.customerId == customerId }
        val girviIds = girvis.map { it.id }.toSet()
        val itemIds = girvis.flatMap { it.effectiveItems }.map { it.id }.toSet()

        val afterBusiness = DeleteCustomerCompleteMutation(customerId).apply(beforeBusiness)
        val afterCustody = purgeCustomerCustody(beforeCustody, girviIds)

        try {
            records.save(afterBusiness)
            custody.save(afterCustody)
            check(records.load() == afterBusiness) { "Customer purge business verification failed" }
            check(custody.load() == afterCustody) { "Customer purge custody verification failed" }
        } catch (failure: Throwable) {
            runCatching { records.save(beforeBusiness) }
            runCatching { custody.save(beforeCustody) }
            throw failure
        }

        val mediaIds = buildList {
            add("customer-$customerId")
            itemIds.forEach { add("item-$it") }
        }
        var mediaDeleted = 0
        var mediaDeleteFailed = 0
        mediaIds.forEach { id ->
            if (media.exists(id)) {
                if (runCatching { media.delete(id) }.getOrDefault(false)) mediaDeleted++ else mediaDeleteFailed++
            }
        }

        val movementsRemoved = beforeCustody.movements.count { it.girviId in girviIds }
        val placementItemsRemoved = beforeCustody.lots.sumOf { lot -> lot.items.count { it.girviId in girviIds } }
        return CustomerPurgeResult(
            customerName = customer.name,
            girviCount = girvis.size,
            itemCount = itemIds.size,
            paymentCount = girvis.sumOf { it.payments.size },
            custodyMovementCount = movementsRemoved,
            placementItemCount = placementItemsRemoved,
            mediaDeleted = mediaDeleted,
            mediaDeleteFailed = mediaDeleteFailed,
        )
    }
}

data class DeleteCustomerCompleteMutation(
    val customerId: String,
) : VerifiedBusinessMutation {
    override val auditLabel: String = "CUSTOMER_ACCOUNT_PURGE"

    override fun apply(snapshot: AppSnapshot): AppSnapshot {
        require(snapshot.customers.any { it.id == customerId }) { "Customer nahi mila" }
        return snapshot.copy(
            customers = snapshot.customers.filterNot { it.id == customerId },
            girvis = snapshot.girvis.filterNot { it.customerId == customerId },
        )
    }
}

internal fun purgeCustomerCustody(
    snapshot: CustodyPlacementSnapshot,
    girviIds: Set<String>,
): CustodyPlacementSnapshot {
    if (girviIds.isEmpty()) return snapshot
    return snapshot.copy(
        movements = snapshot.movements.filterNot { it.girviId in girviIds },
        lots = snapshot.lots.map { lot ->
            lot.copy(items = lot.items.filterNot { it.girviId in girviIds })
        },
    )
}
