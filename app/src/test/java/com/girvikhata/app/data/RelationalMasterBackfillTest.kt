package com.girvikhata.app.data

import com.girvikhata.app.domain.MasterCatalog
import com.girvikhata.app.domain.MasterEntry
import com.girvikhata.app.domain.MasterKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RelationalMasterBackfillTest {
    @Test fun `resolves item unit locker plan and payment mode IDs`() {
        val catalog = MasterCatalog(listOf(
            MasterEntry(id = "item", kind = MasterKind.ITEM, name = "Ring", categoryName = "Gold"),
            MasterEntry(id = "unit", kind = MasterKind.UNIT, name = "Gram"),
            MasterEntry(id = "locker", kind = MasterKind.LOCKER, name = "Locker A"),
            MasterEntry(id = "plan", kind = MasterKind.INTEREST_PLAN, name = "Monthly 2%", rateBasisPoints = 200),
            MasterEntry(id = "mode", kind = MasterKind.PAYMENT_MODE, name = "UPI"),
        ))
        val customer = CustomerRecord(id = "c", name = "Customer")
        val item = GirviItemRecord(id = "i", categoryName = "gold", itemName = "RING", description = "Unit: gram • Locker: locker a")
        val payment = PaymentRecord(id = "p", receiptNumber = "R1", amountPaise = 100, principalPaise = 100, interestPaise = 0, mode = "upi")
        val girvi = GirviRecord(id = "g", girviNumber = "G1", customerId = "c", customerName = "Customer", categoryName = "Gold", itemName = "Ring", weightGrams = "1", principalPaise = 1000, monthlyRateBasisPoints = 200, items = listOf(item), payments = listOf(payment))

        val plan = RelationalMasterBackfill.plan(AppSnapshot(customers = listOf(customer), girvis = listOf(girvi)), catalog)
        assertEquals(ItemMasterLink("i", "item", "unit", "locker"), plan.items.single())
        assertEquals(GirviMasterLink("g", "plan"), plan.girvis.single())
        assertEquals(PaymentMasterLink("p", "mode"), plan.payments.single())
        assertTrue(plan.coverage.complete)
    }

    @Test fun `ambiguous same-rate plans remain unresolved`() {
        val catalog = MasterCatalog(listOf(
            MasterEntry(id = "a", kind = MasterKind.INTEREST_PLAN, name = "A", rateBasisPoints = 200),
            MasterEntry(id = "b", kind = MasterKind.INTEREST_PLAN, name = "B", rateBasisPoints = 200),
        ))
        val customer = CustomerRecord(id = "c", name = "Customer")
        val girvi = GirviRecord(id = "g", girviNumber = "G1", customerId = "c", customerName = "Customer", categoryName = "Gold", itemName = "Ring", weightGrams = "1", principalPaise = 1000, monthlyRateBasisPoints = 200)
        assertNull(RelationalMasterBackfill.plan(AppSnapshot(customers = listOf(customer), girvis = listOf(girvi)), catalog).girvis.single().interestPlanId)
    }

    @Test fun `ambiguous payment modes remain unresolved`() {
        val catalog = MasterCatalog(listOf(
            MasterEntry(id = "a", kind = MasterKind.PAYMENT_MODE, name = "UPI"),
            MasterEntry(id = "b", kind = MasterKind.PAYMENT_MODE, name = "upi"),
        ))
        val customer = CustomerRecord(id = "c", name = "Customer")
        val payment = PaymentRecord(id = "p", receiptNumber = "R1", amountPaise = 100, principalPaise = 100, interestPaise = 0, mode = "UPI")
        val girvi = GirviRecord(id = "g", girviNumber = "G1", customerId = "c", customerName = "Customer", categoryName = "Gold", itemName = "Ring", weightGrams = "1", principalPaise = 1000, monthlyRateBasisPoints = 200, payments = listOf(payment))
        assertNull(RelationalMasterBackfill.plan(AppSnapshot(customers = listOf(customer), girvis = listOf(girvi)), catalog).payments.single().paymentModeId)
    }

    @Test fun `plan order is deterministic`() {
        val catalog = MasterCatalog(listOf(
            MasterEntry(id = "u", kind = MasterKind.UNIT, name = "Gram"),
            MasterEntry(id = "l", kind = MasterKind.LOCKER, name = "Locker"),
        ))
        val one = GirviRecord(id = "b", girviNumber = "G2", customerId = "c", customerName = "C", categoryName = "X", itemName = "B", weightGrams = "1", principalPaise = 100, monthlyRateBasisPoints = 0)
        val two = GirviRecord(id = "a", girviNumber = "G1", customerId = "c", customerName = "C", categoryName = "X", itemName = "A", weightGrams = "1", principalPaise = 100, monthlyRateBasisPoints = 0)
        val customer = CustomerRecord(id = "c", name = "C")
        val a = RelationalMasterBackfill.plan(AppSnapshot(customers = listOf(customer), girvis = listOf(one, two)), catalog)
        val b = RelationalMasterBackfill.plan(AppSnapshot(customers = listOf(customer), girvis = listOf(two, one)), catalog)
        assertEquals(a, b)
    }
}
