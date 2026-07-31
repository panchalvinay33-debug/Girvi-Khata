package com.girvikhata.app.data

/** Process-local, thread-confined permission for the active restore generation's own business activation. */
object RestoreGenerationExecutionScope {
    private val generation = ThreadLocal<String?>()

    fun allows(generationId: String): Boolean = generation.get() == generationId

    fun <T> run(generationId: String, block: () -> T): T {
        require(generation.get() == null) { "Nested restore generation execution is forbidden" }
        generation.set(generationId)
        return try {
            block()
        } finally {
            generation.remove()
        }
    }
}
