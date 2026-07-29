package com.girvikhata.app.export

import com.girvikhata.app.data.AppSnapshot
import com.girvikhata.app.data.GirviRecord
import com.girvikhata.app.data.PaymentRecord
import com.girvikhata.app.domain.CollectionRow
import com.girvikhata.app.domain.EffectiveLedger
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ExportManifest(
    val schemaVersion: Int,
    val createdAt: Long,
    val customerCount: Int,
    val girviCount: Int,
    val paymentEntryCount: Int,
    val payloadSha256: String,
)

object ReceiptTextBuilder {
    fun paymentReceipt(girvi: GirviRecord, payment: PaymentRecord): String = buildString {
        appendLine("GIRVI KHATA - PAYMENT RECEIPT")
        appendLine("Receipt: ${payment.receiptNumber}")
        appendLine("Girvi: ${girvi.girviNumber}")
        appendLine("Customer: ${girvi.customerName}")
        appendLine("Date: ${formatDate(payment.createdAt)}")
        appendLine("Mode: ${payment.mode}")
        appendLine("Amount: ${money(payment.amountPaise)}")
        appendLine("Principal: ${money(payment.principalPaise)}")
        appendLine("Interest: ${money(payment.interestPaise)}")
        appendLine("Charges: ${money(payment.chargesPaise)}")
        if (payment.note.isNotBlank()) appendLine("Note: ${payment.note}")
        append("This is a digitally generated testing receipt.")
    }

    fun customerStatement(customerName: String, girvis: List<GirviRecord>): String = buildString {
        appendLine("GIRVI KHATA - CUSTOMER STATEMENT")
        appendLine("Customer: $customerName")
        appendLine("Generated: ${formatDate(System.currentTimeMillis())}")
        appendLine()
        girvis.sortedByDescending { it.createdAt }.forEach { girvi ->
            appendLine("${girvi.girviNumber} | ${girvi.status} | Principal ${money(girvi.principalPaise)}")
            EffectiveLedger.payments(girvi).forEach { payment ->
                appendLine("  ${payment.receiptNumber} | ${formatDate(payment.createdAt)} | ${money(payment.amountPaise)} | ${payment.mode}")
            }
        }
    }

    private fun formatDate(value: Long): String = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale("en", "IN")).format(Date(value))
    private fun money(paise: Long): String = "₹%,.2f".format(Locale("en", "IN"), paise / 100.0)
}

object CsvExportBuilder {
    fun collections(rows: List<CollectionRow>): String = buildString {
        appendLine("receipt_number,girvi_number,customer,amount,principal,interest,charges,mode,created_at")
        rows.forEach { row ->
            appendLine(listOf(
                row.receiptNumber,
                row.girviNumber,
                csv(row.customerName),
                row.amountPaise,
                row.principalPaise,
                row.interestPaise,
                row.chargesPaise,
                row.mode,
                row.createdAt,
            ).joinToString(","))
        }
    }

    private fun csv(value: String): String = "\"${value.replace("\"", "\"\"")}\""
}

object BackupExportDescriptor {
    fun manifest(snapshot: AppSnapshot, encryptedPayload: ByteArray, now: Long = System.currentTimeMillis()): ExportManifest =
        ExportManifest(
            schemaVersion = snapshot.schemaVersion,
            createdAt = now,
            customerCount = snapshot.customers.size,
            girviCount = snapshot.girvis.size,
            paymentEntryCount = snapshot.girvis.sumOf { it.payments.size },
            payloadSha256 = sha256(encryptedPayload),
        )

    fun sha256(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256")
        .digest(bytes)
        .joinToString("") { "%02x".format(it) }
}
