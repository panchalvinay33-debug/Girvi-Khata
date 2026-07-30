package com.girvikhata.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DataSafetyJournalTest {
    @Test
    fun sha256_isDeterministicAndContentSensitive() {
        val first = DataSafetyJournal.sha256("girvi-khata".toByteArray())
        val second = DataSafetyJournal.sha256("girvi-khata".toByteArray())
        val changed = DataSafetyJournal.sha256("girvi-khata-2".toByteArray())
        assertEquals(64, first.length)
        assertEquals(first, second)
        assertNotEquals(first, changed)
    }

    @Test
    fun backupIsDueWhenNeverCreated() {
        assertTrue(DataSafetyStatus(lastVerifiedBackupAt = 0L).backupDue)
    }

    @Test
    fun backupIsDueAfterFiveCommittedChanges() {
        assertTrue(
            DataSafetyStatus(
                lastVerifiedBackupAt = System.currentTimeMillis(),
                changesSinceBackup = 5,
            ).backupDue,
        )
    }

    @Test
    fun recentBackupWithFewChangesIsCurrent() {
        assertFalse(
            DataSafetyStatus(
                lastVerifiedBackupAt = System.currentTimeMillis(),
                changesSinceBackup = 2,
                journalValid = true,
            ).backupDue,
        )
    }

    @Test
    fun oldBackupIsDueEvenWithoutManyChanges() {
        val eightDaysAgo = System.currentTimeMillis() - 8L * 24 * 60 * 60 * 1000
        assertTrue(DataSafetyStatus(lastVerifiedBackupAt = eightDaysAgo, changesSinceBackup = 0).backupDue)
    }
}
