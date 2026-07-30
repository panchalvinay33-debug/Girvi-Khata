package com.girvikhata.app.data

import java.security.MessageDigest

/** Stable semantic fingerprint used to prove that the relational shadow matches the encrypted snapshot. */
object RelationalShadowFingerprint {
    fun sha256(snapshot: AppSnapshot): String {
        val canonical = buildString {
            append("schema=").append(snapshot.schemaVersion).append('\n')
            snapshot.customers.sortedBy { it.id }.forEach { c ->
                append("C|").append(c.id).append('|').append(escape(c.name)).append('|')
                    .append(escape(c.mobile)).append('|').append(escape(c.address)).append('|')
                    .append(c.createdAt).append('\n')
            }
            snapshot.categories.sortedBy { it.id }.forEach { c ->
                append("K|").append(c.id).append('|').append(escape(c.name)).append('|')
                    .append(if (c.active) 1 else 0).append('\n')
            }
            snapshot.girvis.sortedBy { it.id }.forEach { g ->
                append("G|").append(g.id).append('|').append(escape(g.girviNumber)).append('|')
                    .append(g.customerId).append('|').append(escape(g.customerName)).append('|')
                    .append(g.principalPaise).append('|').append(g.monthlyRateBasisPoints).append('|')
                    .append(g.createdAt).append('|').append(g.status).append('|')
                    .append(g.manualInterestAdjustmentPaise).append('|').append(g.releasedAt ?: -1L).append('|')
                    .append(escape(g.releaseNote)).append('\n')
                g.effectiveItems.sortedBy { it.id }.forEach { i ->
                    append("I|").append(g.id).append('|').append(i.id).append('|')
                        .append(escape(i.categoryName)).append('|').append(escape(i.itemName)).append('|')
                        .append(i.quantity).append('|').append(escape(i.grossWeightGrams)).append('|')
                        .append(escape(i.deductionWeightGrams)).append('|').append(escape(i.description)).append('\n')
                }
                g.payments.sortedBy { it.id }.forEach { p ->
                    append("P|").append(g.id).append('|').append(p.id).append('|')
                        .append(escape(p.receiptNumber)).append('|').append(p.amountPaise).append('|')
                        .append(p.principalPaise).append('|').append(p.interestPaise).append('|')
                        .append(p.chargesPaise).append('|').append(escape(p.mode)).append('|')
                        .append(escape(p.note)).append('|').append(p.createdAt).append('|')
                        .append(if (p.isReversal) 1 else 0).append('|').append(p.reversedPaymentId.orEmpty()).append('\n')
                }
            }
        }
        return MessageDigest.getInstance("SHA-256")
            .digest(canonical.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
    }

    fun expectedCounts(snapshot: AppSnapshot) = RelationalShadowCounts(
        customers = snapshot.customers.size,
        categories = snapshot.categories.size,
        girvis = snapshot.girvis.size,
        items = snapshot.girvis.sumOf { it.effectiveItems.size },
        payments = snapshot.girvis.sumOf { it.payments.size },
    )

    private fun escape(value: String): String = value
        .replace("\\", "\\\\")
        .replace("|", "\\|")
        .replace("\n", "\\n")
}

data class RelationalShadowCounts(
    val customers: Int,
    val categories: Int,
    val girvis: Int,
    val items: Int,
    val payments: Int,
)
