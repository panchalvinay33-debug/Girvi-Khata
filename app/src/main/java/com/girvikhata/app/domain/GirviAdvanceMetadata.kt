package com.girvikhata.app.domain

import java.nio.charset.StandardCharsets
import java.util.Base64

/**
 * Backward-compatible Alpha 25C bridge for additional advances.
 *
 * The current encrypted snapshot schema has no dedicated advances array. Until the schema-v4
 * migration lands, immutable advance events are stored inside the already-encrypted GirviRecord
 * releaseNote using a versioned metadata envelope. User-facing release notes remain intact via
 * [strip]. This keeps old snapshots readable and makes every advance date/rule reproducible.
 */
object GirviAdvanceMetadata {
    private const val START = "[[GIRVI_ADVANCES:"
    private const val END = "]]"
    private const val VERSION = "GKADV1"

    data class Advance(
        val id: String,
        val amountPaise: Long,
        val createdAt: Long,
        val terms: InterestTerms,
        val note: String = "",
    ) {
        init {
            require(id.isNotBlank()) { "Advance id required" }
            require(amountPaise > 0L) { "Advance amount must be positive" }
            require(createdAt > 0L) { "Advance date required" }
        }
    }

    fun read(value: String?): List<Advance> {
        if (value.isNullOrBlank()) return emptyList()
        val start = value.indexOf(START)
        if (start < 0) return emptyList()
        val payloadStart = start + START.length
        val end = value.indexOf(END, payloadStart)
        if (end < 0) return emptyList()
        return decode(value.substring(payloadStart, end))
    }

    fun attach(userNote: String?, advances: List<Advance>): String {
        val clean = strip(userNote).trim()
        if (advances.isEmpty()) return clean
        val metadata = START + encode(advances) + END
        return if (clean.isBlank()) metadata else "$clean\n$metadata"
    }

    fun append(userNote: String?, advance: Advance): String {
        val existing = read(userNote)
        require(existing.none { it.id == advance.id }) { "Duplicate advance id" }
        return attach(userNote, existing + advance)
    }

    fun strip(value: String?): String {
        if (value.isNullOrBlank()) return ""
        var result = value
        while (true) {
            val start = result.indexOf(START)
            if (start < 0) break
            val end = result.indexOf(END, start + START.length)
            if (end < 0) break
            result = result.removeRange(start, end + END.length)
        }
        return result.trim()
    }

    private fun encode(advances: List<Advance>): String = buildString {
        append(VERSION)
        advances.sortedWith(compareBy<Advance> { it.createdAt }.thenBy { it.id }).forEach { advance ->
            append('|')
            append(b64(advance.id)).append(',')
            append(advance.amountPaise).append(',')
            append(advance.createdAt).append(',')
            append(b64(InterestTermsCodec.encode(advance.terms))).append(',')
            append(b64(advance.note))
        }
    }

    private fun decode(payload: String): List<Advance> {
        val parts = payload.split('|')
        if (parts.firstOrNull() != VERSION) return emptyList()
        return parts.drop(1).mapNotNull { row ->
            runCatching {
                val columns = row.split(',', limit = 5)
                require(columns.size == 5)
                Advance(
                    id = unb64(columns[0]),
                    amountPaise = columns[1].toLong(),
                    createdAt = columns[2].toLong(),
                    terms = InterestTermsCodec.decode(unb64(columns[3])) ?: error("Invalid interest terms"),
                    note = unb64(columns[4]),
                )
            }.getOrNull()
        }
    }

    private fun b64(value: String): String = Base64.getUrlEncoder().withoutPadding()
        .encodeToString(value.toByteArray(StandardCharsets.UTF_8))

    private fun unb64(value: String): String = String(
        Base64.getUrlDecoder().decode(value),
        StandardCharsets.UTF_8,
    )
}
