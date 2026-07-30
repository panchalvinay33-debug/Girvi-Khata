package com.girvikhata.app.data

data class RelationalShadowDelta(
    val deleteCustomerIds: Set<String>,
    val upsertCustomers: List<CustomerRecord>,
    val deleteCategoryIds: Set<String>,
    val upsertCategories: List<CategoryRecord>,
    val deleteGirviIds: Set<String>,
    val upsertGirvis: List<GirviRecord>,
) {
    val changedRows: Int
        get() = deleteCustomerIds.size + upsertCustomers.size +
            deleteCategoryIds.size + upsertCategories.size +
            deleteGirviIds.size + upsertGirvis.sumOf { 1 + RelationalShadowFingerprint.stableItems(it).size + it.payments.size }

    val isEmpty: Boolean
        get() = changedRows == 0
}

object RelationalShadowDeltaPlanner {
    fun plan(current: AppSnapshot, target: AppSnapshot): RelationalShadowDelta {
        val currentCustomers = current.customers.associateBy { it.id }
        val targetCustomers = target.customers.associateBy { it.id }
        val currentCategories = current.categories.associateBy { it.id }
        val targetCategories = target.categories.associateBy { it.id }
        val currentGirvis = current.girvis.associateBy { it.id }
        val targetGirvis = target.girvis.associateBy { it.id }

        val changedGirvis = target.girvis.filter { targetGirvi ->
            val existing = currentGirvis[targetGirvi.id] ?: return@filter true
            girviSemanticKey(existing) != girviSemanticKey(targetGirvi)
        }

        return RelationalShadowDelta(
            deleteCustomerIds = currentCustomers.keys - targetCustomers.keys,
            upsertCustomers = target.customers.filter { currentCustomers[it.id] != it },
            deleteCategoryIds = currentCategories.keys - targetCategories.keys,
            upsertCategories = target.categories.filter { currentCategories[it.id] != it },
            deleteGirviIds = currentGirvis.keys - targetGirvis.keys,
            upsertGirvis = changedGirvis,
        )
    }

    private fun girviSemanticKey(girvi: GirviRecord): List<Any?> = listOf(
        girvi.id,
        girvi.girviNumber,
        girvi.customerId,
        girvi.customerName,
        girvi.principalPaise,
        girvi.monthlyRateBasisPoints,
        girvi.createdAt,
        girvi.status,
        girvi.manualInterestAdjustmentPaise,
        girvi.releasedAt,
        girvi.releaseNote,
        RelationalShadowFingerprint.stableItems(girvi),
        girvi.payments,
    )
}

data class RelationalCutoverEvidence(
    val consecutiveHealthySyncs: Int,
    val lastHealthyAt: Long?,
    val lastFailureAt: Long?,
    val stressDatasetVerified: Boolean,
    val rollbackSimulationVerified: Boolean,
    val ownerPhysicalTestApproved: Boolean,
)

object RelationalCutoverPolicy {
    const val MIN_CONSECUTIVE_HEALTHY_SYNCS = 25

    fun blockers(evidence: RelationalCutoverEvidence): List<String> = buildList {
        if (evidence.consecutiveHealthySyncs < MIN_CONSECUTIVE_HEALTHY_SYNCS) {
            add("At least $MIN_CONSECUTIVE_HEALTHY_SYNCS consecutive healthy syncs required")
        }
        if (evidence.lastHealthyAt == null) add("No verified relational sync yet")
        if (evidence.lastFailureAt != null && (evidence.lastHealthyAt == null || evidence.lastFailureAt > evidence.lastHealthyAt)) {
            add("Latest relational attempt failed")
        }
        if (!evidence.stressDatasetVerified) add("Large-dataset stress test pending")
        if (!evidence.rollbackSimulationVerified) add("Rollback simulation pending")
        if (!evidence.ownerPhysicalTestApproved) add("Owner physical-device approval pending")
    }

    fun eligible(evidence: RelationalCutoverEvidence): Boolean = blockers(evidence).isEmpty()
}
