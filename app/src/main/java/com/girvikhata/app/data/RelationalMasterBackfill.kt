package com.girvikhata.app.data

import com.girvikhata.app.domain.MasterCatalog
import com.girvikhata.app.domain.MasterEntry
import com.girvikhata.app.domain.MasterKind
import java.util.Locale

data class ItemMasterLink(
    val itemId: String,
    val itemMasterId: String?,
    val unitId: String?,
    val lockerId: String?,
)

data class GirviMasterLink(val girviId: String, val interestPlanId: String?)
data class PaymentMasterLink(val paymentId: String, val paymentModeId: String?)

data class RelationalMasterBackfillPlan(
    val masters: List<MasterEntry>,
    val items: List<ItemMasterLink>,
    val girvis: List<GirviMasterLink>,
    val payments: List<PaymentMasterLink>,
    val coverage: RelationalMasterCoverage,
)

object RelationalMasterBackfill {
    fun plan(snapshot: AppSnapshot, catalog: MasterCatalog): RelationalMasterBackfillPlan {
        val masters = RelationalMasterSchema.normalized(catalog)
        val itemLinks = snapshot.girvis.flatMap { girvi ->
            RelationalShadowFingerprint.stableItems(girvi).map { item ->
                val legacy = RelationalMasterLinkResolver.resolve(item, MasterCatalog(masters))
                ItemMasterLink(item.id, legacy.itemMasterId, legacy.unitId, legacy.lockerId)
            }
        }.sortedBy { it.itemId }

        val girviLinks = snapshot.girvis.map { girvi ->
            GirviMasterLink(girvi.id, unique(masters) {
                it.kind == MasterKind.INTEREST_PLAN && it.rateBasisPoints == girvi.monthlyRateBasisPoints
            })
        }.sortedBy { it.girviId }

        val paymentLinks = snapshot.girvis.flatMap { girvi ->
            girvi.payments.map { payment ->
                PaymentMasterLink(payment.id, unique(masters) {
                    it.kind == MasterKind.PAYMENT_MODE && key(it.name) == key(payment.mode)
                })
            }
        }.sortedBy { it.paymentId }

        return RelationalMasterBackfillPlan(
            masters = masters,
            items = itemLinks,
            girvis = girviLinks,
            payments = paymentLinks,
            coverage = RelationalMasterCoverage(
                totalItems = itemLinks.size,
                itemMasterLinked = itemLinks.count { it.itemMasterId != null },
                unitLinked = itemLinks.count { it.unitId != null },
                lockerLinked = itemLinks.count { it.lockerId != null },
                totalGirvis = girviLinks.size,
                interestPlanLinked = girviLinks.count { it.interestPlanId != null },
                totalPayments = paymentLinks.size,
                paymentModeLinked = paymentLinks.count { it.paymentModeId != null },
            ),
        )
    }

    private fun unique(entries: List<MasterEntry>, predicate: (MasterEntry) -> Boolean): String? {
        val matches = entries.filter(predicate)
        return matches.singleOrNull()?.id
    }

    private fun key(value: String): String = value.trim().replace(Regex("\\s+"), " ").lowercase(Locale.ROOT)
}
