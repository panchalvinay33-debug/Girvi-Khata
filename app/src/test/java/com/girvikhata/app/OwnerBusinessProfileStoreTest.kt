package com.girvikhata.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class OwnerBusinessProfileStoreTest {
    @Test
    fun normalize_trimsWhitespaceAndIndianPrefix() {
        val normalized = OwnerBusinessProfileStore.normalize(
            OwnerBusinessProfile(
                businessName = "  Shree   Shyam\nJewellers ",
                ownerName = "  Vinay   Panchal ",
                mobile = "+91 98765 43210",
                address = "  Main   Road\nPitol  ",
            ),
        )

        assertEquals("Shree Shyam Jewellers", normalized.businessName)
        assertEquals("Vinay Panchal", normalized.ownerName)
        assertEquals("9876543210", normalized.mobile)
        assertEquals("Main Road Pitol", normalized.address)
    }

    @Test
    fun normalize_rejectsPartialMobile() {
        assertThrows(IllegalArgumentException::class.java) {
            OwnerBusinessProfileStore.normalize(
                OwnerBusinessProfile("Shop", "Owner", "12345", ""),
            )
        }
    }

    @Test
    fun normalize_requiresBusinessAndOwner() {
        assertThrows(IllegalArgumentException::class.java) {
            OwnerBusinessProfileStore.normalize(OwnerBusinessProfile("", "Owner"))
        }
        assertThrows(IllegalArgumentException::class.java) {
            OwnerBusinessProfileStore.normalize(OwnerBusinessProfile("Shop", ""))
        }
    }

    @Test
    fun normalize_acceptsLeadingZeroIndianMobile() {
        assertEquals("9876543210", OwnerBusinessProfileStore.normalizeIndianMobile("09876543210"))
    }
}
