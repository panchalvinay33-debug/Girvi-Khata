package com.girvikhata.app.domain

object GirviInterestMetadata {
    private const val START = "[[GIRVI_INTEREST:"
    private const val END = "]]"

    fun attach(userDescription: String, terms: InterestTerms): String {
        val clean = strip(userDescription).trim()
        val metadata = START + InterestTermsCodec.encode(terms) + END
        return if (clean.isBlank()) metadata else "$clean\n$metadata"
    }

    fun read(description: String?): InterestTerms? {
        if (description.isNullOrBlank()) return null
        val start = description.indexOf(START)
        if (start < 0) return null
        val payloadStart = start + START.length
        val end = description.indexOf(END, payloadStart)
        if (end < 0) return null
        return InterestTermsCodec.decode(description.substring(payloadStart, end))
    }

    fun strip(description: String?): String {
        if (description.isNullOrBlank()) return ""
        var value = description
        while (true) {
            val start = value.indexOf(START)
            if (start < 0) break
            val end = value.indexOf(END, start + START.length)
            if (end < 0) break
            value = value.removeRange(start, end + END.length)
        }
        return value.trim()
    }
}
