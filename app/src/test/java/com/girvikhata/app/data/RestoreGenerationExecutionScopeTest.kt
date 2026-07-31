package com.girvikhata.app.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RestoreGenerationExecutionScopeTest {
    @Test
    fun `scope permits only matching generation and clears after success`() {
        assertFalse(RestoreGenerationExecutionScope.allows("g1"))

        RestoreGenerationExecutionScope.run("g1") {
            assertTrue(RestoreGenerationExecutionScope.allows("g1"))
            assertFalse(RestoreGenerationExecutionScope.allows("g2"))
        }

        assertFalse(RestoreGenerationExecutionScope.allows("g1"))
    }

    @Test
    fun `scope clears after failure`() {
        runCatching {
            RestoreGenerationExecutionScope.run("g1") {
                error("boom")
            }
        }

        assertFalse(RestoreGenerationExecutionScope.allows("g1"))
    }

    @Test
    fun `nested restore generation scope is rejected`() {
        val failure = runCatching {
            RestoreGenerationExecutionScope.run("g1") {
                RestoreGenerationExecutionScope.run("g2") { Unit }
            }
        }.exceptionOrNull()

        assertTrue(failure is IllegalArgumentException)
        assertFalse(RestoreGenerationExecutionScope.allows("g1"))
        assertFalse(RestoreGenerationExecutionScope.allows("g2"))
    }
}
