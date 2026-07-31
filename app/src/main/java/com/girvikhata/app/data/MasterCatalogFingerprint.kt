package com.girvikhata.app.data

import com.girvikhata.app.domain.MasterCatalog
import java.security.MessageDigest

object MasterCatalogFingerprint {
    fun sha256(catalog: MasterCatalog): String {
        val canonical = buildString {
            append("master-catalog-v1\n")
            catalog.entries.forEachIndexed { index, entry ->
                append(index).append('|')
                append(escape(entry.id)).append('|')
                append(entry.kind.name).append('|')
                append(escape(entry.name)).append('|')
                append(entry.active).append('|')
                append(escape(entry.categoryName)).append('|')
                append(entry.rateBasisPoints).append('\n')
            }
        }
        return MessageDigest.getInstance("SHA-256")
            .digest(canonical.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
    }

    private fun escape(value: String): String = buildString(value.length) {
        value.forEach { character ->
            when (character) {
                '\\' -> append("\\\\")
                '|' -> append("\\|")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                else -> append(character)
            }
        }
    }
}
