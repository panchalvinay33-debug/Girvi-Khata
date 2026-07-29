package com.girvikhata.app.backup

import com.girvikhata.app.data.AppSnapshot
import com.girvikhata.app.data.CategoryRecord
import com.girvikhata.app.data.CustomerRecord
import com.girvikhata.app.data.GirviItemRecord
import com.girvikhata.app.data.GirviRecord
import com.girvikhata.app.data.PaymentRecord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class SnapshotPortableCodecTest {
    @Test
    fun `inspection reports complete snapshot counts`() {
        val snapshot = AppSnapshot(
            schemaVersion = 3,
            customers = listOf(CustomerRecord(id = "c1", name = "Vinay", mobile = "9999999999")),
            categories = listOf(CategoryRecord(id = "cat1", name = "Gold")),
            girvis = listOf(
                GirviRecord(
                    id = "g1",
                    girviNumber = "GK-1",
                    customerId = "c1",
                    customerName = "Vinay",
                    categoryName = "Gold",
                    itemName = "Ring",
                    weightGrams = "10",
                    principalPaise = 300_000,
                    monthlyRateBasisPoints = 300,
                    items = listOf(GirviItemRecord(id = "i1", categoryName = "Gold", itemName = "Ring", grossWeightGrams = "10")),
                    payments = listOf(
                        PaymentRecord(
                            id = "p1",
                            receiptNumber = "PAY-1",
                            amountPaise = 10_000,
                            principalPaise = 10_000,
                            interestPaise = 0,
                        ),
                    ),
                ),
            ),
        )
        val payload = SnapshotPortableCodec.encode(snapshot)
        val inspection = SnapshotPortableCodec.inspect(payload)
        assertEquals(3, inspection.schemaVersion)
        assertEquals(1, inspection.customerCount)
        assertEquals(1, inspection.categoryCount)
        assertEquals(1, inspection.girviCount)
        assertEquals(1, inspection.paymentEntryCount)
    }

    @Test
    fun `portable payload encrypts and decrypts without loss`() {
        val snapshot = AppSnapshot(customers = listOf(CustomerRecord(name = "Test Customer")))
        val payload = SnapshotPortableCodec.encode(snapshot)
        val encrypted = PortableBackupCrypto.encrypt(payload, "StrongBackup123".toCharArray(), snapshot.schemaVersion)
        val restored = PortableBackupCrypto.decrypt(encrypted, "StrongBackup123".toCharArray())
        assertEquals(payload.toList(), restored.payload.toList())
        assertEquals(snapshot.schemaVersion, restored.schemaVersion)
        assertNotEquals(payload.toList(), encrypted.toList())
    }
}
