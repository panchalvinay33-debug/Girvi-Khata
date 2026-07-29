package com.girvikhata.app.backup

import com.girvikhata.app.data.AppSnapshot
import com.girvikhata.app.data.CategoryRecord
import com.girvikhata.app.data.CustomerRecord
import com.girvikhata.app.data.GirviItemRecord
import com.girvikhata.app.data.GirviRecord
import com.girvikhata.app.data.PaymentRecord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class SnapshotPortableCodecTest {
    private fun completeSnapshot() = AppSnapshot(
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
                status = "RELEASED",
                releasedAt = 1_700_000_100_000,
                releaseNote = "Settled",
                manualInterestAdjustmentPaise = -500,
                items = listOf(GirviItemRecord(id = "i1", categoryName = "Gold", itemName = "Ring", grossWeightGrams = "10", description = "Hallmark")),
                payments = listOf(
                    PaymentRecord(
                        id = "p1",
                        receiptNumber = "PAY-1",
                        amountPaise = 10_000,
                        principalPaise = 9_000,
                        interestPaise = 1_000,
                        note = "Part payment",
                        createdAt = 1_700_000_050_000,
                    ),
                ),
                createdAt = 1_700_000_000_000,
            ),
        ),
    )

    @Test
    fun `inspection reports complete snapshot counts`() {
        val inspection = SnapshotPortableCodec.inspect(SnapshotPortableCodec.encode(completeSnapshot()))
        assertEquals(3, inspection.schemaVersion)
        assertEquals(1, inspection.customerCount)
        assertEquals(1, inspection.categoryCount)
        assertEquals(1, inspection.girviCount)
        assertEquals(1, inspection.paymentEntryCount)
    }

    @Test
    fun `decode round trip preserves accounting and release metadata`() {
        val source = completeSnapshot()
        val decoded = SnapshotPortableCodec.decode(SnapshotPortableCodec.encode(source))
        assertEquals(source, decoded)
        assertEquals("Settled", decoded.girvis.single().releaseNote)
        assertEquals(-500, decoded.girvis.single().manualInterestAdjustmentPaise)
        assertEquals(1_000, decoded.girvis.single().payments.single().interestPaise)
    }

    @Test
    fun `portable payload encrypts decrypts and decodes without loss`() {
        val snapshot = completeSnapshot()
        val payload = SnapshotPortableCodec.encode(snapshot)
        val encrypted = PortableBackupCrypto.encrypt(payload, "StrongBackup123".toCharArray(), snapshot.schemaVersion)
        val restored = PortableBackupCrypto.decrypt(encrypted, "StrongBackup123".toCharArray())
        assertEquals(snapshot, SnapshotPortableCodec.decode(restored.payload))
        assertEquals(snapshot.schemaVersion, restored.schemaVersion)
        assertNotEquals(payload.toList(), encrypted.toList())
    }

    @Test
    fun `decode rejects girvi linked to missing customer`() {
        val invalid = String(SnapshotPortableCodec.encode(completeSnapshot())).replace("\"customerId\":\"c1\"", "\"customerId\":\"missing\"")
        assertThrows(IllegalArgumentException::class.java) {
            SnapshotPortableCodec.decode(invalid.toByteArray())
        }
    }

    @Test
    fun `decode rejects unsupported future schema`() {
        val invalid = String(SnapshotPortableCodec.encode(completeSnapshot())).replace("\"schemaVersion\":3", "\"schemaVersion\":99")
        assertThrows(IllegalArgumentException::class.java) {
            SnapshotPortableCodec.decode(invalid.toByteArray())
        }
    }

    @Test
    fun `decode rejects duplicate girvi numbers`() {
        val source = completeSnapshot()
        val duplicate = source.copy(girvis = source.girvis + source.girvis.single().copy(id = "g2"))
        assertThrows(IllegalArgumentException::class.java) {
            SnapshotPortableCodec.decode(SnapshotPortableCodec.encode(duplicate))
        }
    }
}