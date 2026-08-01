package com.girvikhata.app.backup

import com.girvikhata.app.data.AppSnapshot
import com.girvikhata.app.data.CustomerRecord
import com.girvikhata.app.data.GirviRecord
import com.girvikhata.app.domain.MasterCatalog
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PortableRecoveryPipelineTest {
    @Test
    fun `recovery key restores business masters and portable photos exactly`() {
        val customer = CustomerRecord(id = "customer-1", name = "Ravi", mobile = "9999999999", createdAt = 1L)
        val girvi = GirviRecord(
            id = "girvi-1",
            girviNumber = "GK-0001",
            customerId = customer.id,
            customerName = customer.name,
            categoryName = "Jewellery",
            itemName = "Ring",
            weightGrams = "5.0",
            principalPaise = 50_000L,
            monthlyRateBasisPoints = 200,
            createdAt = 2L,
        )
        val snapshot = AppSnapshot(
            customers = listOf(customer),
            categories = AppSnapshot.defaults().categories,
            girvis = listOf(girvi),
        )
        val masters = MasterCatalog()
        val media = linkedMapOf(
            "customer-customer-1" to byteArrayOf(1, 2, 3, 4),
            "item-item-1" to byteArrayOf(9, 8, 7, 6, 5),
        )
        val recoveryKey = RecoveryKeyStore.generate()

        val portablePayload = PortableAppBundleCodec.encodePortable(snapshot, masters, media)
        val encryptedPackage = PortableBackupCrypto.encrypt(
            portablePayload,
            recoveryKey.toCharArray(),
            snapshot.schemaVersion,
            createdAt = 123456789L,
        )
        val decryptedPackage = PortableBackupCrypto.decrypt(encryptedPackage, recoveryKey.toCharArray())
        val decoded = PortableAppBundleCodec.decode(decryptedPackage.payload)

        // Portable codecs canonicalize legacy Girvi item fields into explicit item records. Compare the
        // canonical portable representation instead of raw data-class shape so the test validates the
        // actual new-device recovery contract without rejecting equivalent legacy normalization.
        assertArrayEquals(
            SnapshotPortableCodec.encode(snapshot),
            SnapshotPortableCodec.encode(decoded.snapshot),
        )
        assertEquals(masters, decoded.masterCatalog)
        assertTrue(decoded.hasPortableMedia)
        assertEquals(media.keys, decoded.portableMedia.keys)
        media.forEach { (id, bytes) -> assertArrayEquals(bytes, decoded.portableMedia.getValue(id)) }
        assertEquals(123456789L, decryptedPackage.createdAt)
        assertTrue(decryptedPackage.payload.contentEquals(portablePayload))
    }
}
