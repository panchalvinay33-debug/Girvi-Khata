package com.girvikhata.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class DefaultCategoryFingerprintRegressionTest {
    @Test
    fun freshDefaultsHaveStableFingerprintAcrossLoads() {
        val first = AppSnapshot.defaults()
        val second = AppSnapshot.defaults()

        // Older builds generated different UUIDs for these categories on every fresh load.
        assertNotEquals(first.categories.map { it.id }, second.categories.map { it.id })
        assertEquals(
            RelationalShadowFingerprint.sha256(first),
            RelationalShadowFingerprint.sha256(second),
        )
    }

    @Test
    fun customCategoryIdentityStillParticipatesInFingerprint() {
        val first = AppSnapshot(
            categories = listOf(CategoryRecord(id = "custom-a", name = "Vehicle")),
        )
        val second = AppSnapshot(
            categories = listOf(CategoryRecord(id = "custom-b", name = "Vehicle")),
        )

        assertNotEquals(
            RelationalShadowFingerprint.sha256(first),
            RelationalShadowFingerprint.sha256(second),
        )
    }

    @Test
    fun builtInCategoryStateStillChangesFingerprint() {
        val first = AppSnapshot(
            categories = listOf(CategoryRecord(id = "random-a", name = "Jewellery", active = true)),
        )
        val second = AppSnapshot(
            categories = listOf(CategoryRecord(id = "random-b", name = "Jewellery", active = false)),
        )

        assertNotEquals(
            RelationalShadowFingerprint.sha256(first),
            RelationalShadowFingerprint.sha256(second),
        )
    }
}
