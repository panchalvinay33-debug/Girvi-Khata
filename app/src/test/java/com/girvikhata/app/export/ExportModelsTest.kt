package com.girvikhata.app.export

import com.girvikhata.app.data.AppSnapshot
import com.girvikhata.app.data.CustomerRecord
import com.girvikhata.app.data.GirviRecord
import com.girvikhata.app.data.PaymentRecord
import com.girvikhata.app.domain.CollectionRow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExportModelsTest {
    @Test fun receiptIncludesAccountingBreakup() {
        val payment = PaymentRecord(
            receiptNumber = "PAY-1", amountPaise = 12_000,
            principalPaise = 10_000, interestPaise = 2_000, mode = "UPI",
        )
        val girvi = GirviRecord(
            girviNumber = "GK-1", customerId = "c1", customerName = "Sita",
            categoryName = "Gold", itemName = "Ring", weightGrams = "5",
            principalPaise = 100_000, monthlyRateBasisPoints = 200,
        )
        val text = ReceiptTextBuilder.paymentReceipt(girvi, payment)
        assertTrue(text.contains("PAY-1"))
        assertTrue(text.contains("Principal"))
        assertTrue(text.contains("UPI"))
    }

    @Test fun csvEscapesCustomerCommaAndQuotes() {
        val csv = CsvExportBuilder.collections(listOf(
            CollectionRow("PAY-1", "GK-1", "Panchal, \"Vinay\"", 100, 100, 0, 0, "CASH", 1),
        ))
        assertTrue(csv.contains("\"Panchal, \"\"Vinay\"\"\""))
    }

    @Test fun backupManifestHasStableHashAndCounts() {
        val customer = CustomerRecord(name = "A")
        val snapshot = AppSnapshot(customers = listOf(customer))
        val first = BackupExportDescriptor.manifest(snapshot, "encrypted".toByteArray(), now = 10)
        val second = BackupExportDescriptor.manifest(snapshot, "encrypted".toByteArray(), now = 10)
        assertEquals(first, second)
        assertEquals(64, first.payloadSha256.length)
        assertEquals(1, first.customerCount)
    }
}
