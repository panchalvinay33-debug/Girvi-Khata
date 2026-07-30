package com.girvikhata.app.domain

import java.time.LocalDate
import java.time.format.DateTimeFormatter

class GirviNumberGenerator(
    private val prefix: String = "GK",
) {
    fun generate(date: LocalDate, sequence: Long): String {
        require(sequence in 1..999_999)
        val datePart = date.format(DateTimeFormatter.BASIC_ISO_DATE)
        return "$prefix-$datePart-${sequence.toString().padStart(6, '0')}"
    }
}
