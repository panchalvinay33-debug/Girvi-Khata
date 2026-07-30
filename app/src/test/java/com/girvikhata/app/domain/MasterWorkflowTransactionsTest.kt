package com.girvikhata.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class MasterWorkflowTransactionsTest {
    @Test fun multipleItemsNormalizeAndPreserveOrder() {
        val items = MasterWorkflowTransactions.validateItems(
            listOf(
                MasterItemDraft(" Gold ", " Ring ", 2, "10.50", "0.50", " Locker A "),
                MasterItemDraft("Silver", "Chain", 1, "20", "", "Locker B"),
            ),
        )
        assertEquals(2, items.size)
        assertEquals("Gold", items[0].categoryName)
        assertEquals("Ring", items[0].itemName)
        assertEquals("10.5", items[0].grossWeightGrams)
        assertEquals("0.5", items[0].deductionWeightGrams)
        assertEquals("Locker A", items[0].description)
        assertEquals("Chain", items[1].itemName)
    }

    @Test fun duplicateCategoryAndItemIsRejectedCaseInsensitively() {
        assertThrows(IllegalArgumentException::class.java) {
            MasterWorkflowTransactions.validateItems(
                listOf(
                    MasterItemDraft("Gold", "Ring", 1, "10", "0", ""),
                    MasterItemDraft(" gold ", " RING ", 1, "5", "0", ""),
                ),
            )
        }
    }

    @Test fun deductionCannotExceedGross() {
        assertThrows(IllegalArgumentException::class.java) {
            MasterWorkflowTransactions.validateItems(
                listOf(MasterItemDraft("Gold", "Ring", 1, "5", "6", "")),
            )
        }
    }

    @Test fun customSplitMustMatchEnteredAmountAndDue() {
        val split = MasterWorkflowTransactions.validateCustomPayment(
            CustomPaymentDraft(7_000, 2_000, 1_000),
            AccountBalance(10_000, 5_000, 1_000),
            10_000,
        )
        assertEquals(7_000, split.principalPaise)
        assertEquals(2_000, split.interestPaise)
        assertEquals(1_000, split.chargesPaise)
    }

    @Test fun customSplitMismatchIsRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            MasterWorkflowTransactions.validateCustomPayment(
                CustomPaymentDraft(7_000, 2_000, 0),
                AccountBalance(10_000, 5_000, 1_000),
                10_000,
            )
        }
    }

    @Test fun customSplitCannotExceedComponentDue() {
        assertThrows(IllegalArgumentException::class.java) {
            MasterWorkflowTransactions.validateCustomPayment(
                CustomPaymentDraft(11_000, 0, 0),
                AccountBalance(10_000, 5_000, 1_000),
                11_000,
            )
        }
    }
}
