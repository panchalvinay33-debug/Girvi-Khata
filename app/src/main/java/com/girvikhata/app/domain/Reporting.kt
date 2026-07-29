package com.girvikhata.app.domain

import com.girvikhata.app.data.AppSnapshot
import com.girvikhata.app.data.GirviRecord
import com.girvikhata.app.data.PaymentRecord
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId


enum class GirviStatusFilter { ALL, ACTIVE, RELEASED }

data class DateRange(val fromInclusive: Long, val toInclusive: Long) {
    init { require(fromInclusive <= toInclusive) }
    fun contains(timestamp: Long): Boolean = timestamp in fromInclusive..toInclusive
}

object ReportDateRanges {
    fun localDaysEndingToday(days: Long, now: Long = System.currentTimeMillis(), zoneId: ZoneId = ZoneId.systemDefault()): DateRange {
        require(days > 0) { "Days must be positive" }
        val today = Instant.ofEpochMilli(now).atZone(zoneId).toLocalDate()
        return localDates(today.minusDays(days - 1), today, zoneId)
    }

    fun localDates(from: LocalDate, to: LocalDate, zoneId: ZoneId = ZoneId.systemDefault()): DateRange {
        require(!from.isAfter(to)) { "From date cannot be after to date" }
        val start = from.atStartOfDay(zoneId).toInstant().toEpochMilli()
        val endExclusive = to.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
        return DateRange(start, endExclusive - 1)
    }

    fun allTime(): DateRange = DateRange(0, Long.MAX_VALUE)
}

data class CustomerLedgerSummary(
    val customerId: String,
    val customerName: String,
    val totalGirvi: Int,
    val activeGirvi: Int,
    val releasedGirvi: Int,
    val originalPrincipalPaise: Long,
    val effectiveReceivedPaise: Long,
    val outstandingPrincipalPaise: Long,
    val outstandingInterestPaise: Long,
    val totalOutstandingPaise: Long,
)

data class PortfolioSummary(
    val totalCustomers: Int,
    val totalGirvi: Int,
    val activeGirvi: Int,
    val releasedGirvi: Int,
    val originalPrincipalPaise: Long,
    val effectiveReceivedPaise: Long,
    val outstandingPrincipalPaise: Long,
    val outstandingInterestPaise: Long,
    val totalOutstandingPaise: Long,
)

data class CollectionRow(
    val receiptNumber: String,
    val girviNumber: String,
    val customerName: String,
    val amountPaise: Long,
    val principalPaise: Long,
    val interestPaise: Long,
    val chargesPaise: Long,
    val mode: String,
    val createdAt: Long,
)

object EffectiveLedger {
    fun payments(girvi: GirviRecord): List<PaymentRecord> {
        val reversed = girvi.payments.filter { it.isReversal }.mapNotNull { it.reversedPaymentId }.toSet()
        return girvi.payments.filter { !it.isReversal && it.id !in reversed }
    }

    fun receivedPaise(girvi: GirviRecord): Long = payments(girvi).sumOf { it.amountPaise }
}

object ReportingEngine {
    fun filterGirvi(snapshot: AppSnapshot, status: GirviStatusFilter, query: String = ""): List<GirviRecord> {
        val clean = query.trim()
        return snapshot.girvis.asSequence()
            .filter {
                when (status) {
                    GirviStatusFilter.ALL -> true
                    GirviStatusFilter.ACTIVE -> it.status == "ACTIVE"
                    GirviStatusFilter.RELEASED -> it.status == "RELEASED"
                }
            }
            .filter {
                clean.isBlank() || it.girviNumber.contains(clean, true) ||
                    it.customerName.contains(clean, true) ||
                    it.effectiveItems.any { item -> item.itemName.contains(clean, true) || item.categoryName.contains(clean, true) }
            }
            .sortedByDescending { it.createdAt }
            .toList()
    }

    fun customerLedger(snapshot: AppSnapshot, customerId: String, settlementMonths: Int): CustomerLedgerSummary {
        require(settlementMonths in 0..120) { "Settlement months must be between 0 and 120" }
        val customer = snapshot.customers.firstOrNull { it.id == customerId }
        val girvis = snapshot.girvis.filter { it.customerId == customerId }
        val views = girvis.map { GirviSettlementUseCase.settlementView(it, settlementMonths) }
        return CustomerLedgerSummary(
            customerId = customerId,
            customerName = customer?.name ?: girvis.firstOrNull()?.customerName.orEmpty(),
            totalGirvi = girvis.size,
            activeGirvi = girvis.count { it.status == "ACTIVE" },
            releasedGirvi = girvis.count { it.status == "RELEASED" },
            originalPrincipalPaise = girvis.sumOf { it.principalPaise },
            effectiveReceivedPaise = girvis.sumOf(EffectiveLedger::receivedPaise),
            outstandingPrincipalPaise = views.sumOf { it.principalDuePaise },
            outstandingInterestPaise = views.sumOf { it.interestDuePaise },
            totalOutstandingPaise = views.sumOf { it.totalDuePaise },
        )
    }

    fun portfolio(snapshot: AppSnapshot, settlementMonths: Int): PortfolioSummary {
        require(settlementMonths in 0..120) { "Settlement months must be between 0 and 120" }
        val views = snapshot.girvis.map { GirviSettlementUseCase.settlementView(it, settlementMonths) }
        return PortfolioSummary(
            totalCustomers = snapshot.customers.size,
            totalGirvi = snapshot.girvis.size,
            activeGirvi = snapshot.girvis.count { it.status == "ACTIVE" },
            releasedGirvi = snapshot.girvis.count { it.status == "RELEASED" },
            originalPrincipalPaise = snapshot.girvis.sumOf { it.principalPaise },
            effectiveReceivedPaise = snapshot.girvis.sumOf(EffectiveLedger::receivedPaise),
            outstandingPrincipalPaise = views.sumOf { it.principalDuePaise },
            outstandingInterestPaise = views.sumOf { it.interestDuePaise },
            totalOutstandingPaise = views.sumOf { it.totalDuePaise },
        )
    }

    fun outstandingCustomers(snapshot: AppSnapshot, settlementMonths: Int): List<CustomerLedgerSummary> =
        snapshot.customers.map { customerLedger(snapshot, it.id, settlementMonths) }
            .filter { it.totalOutstandingPaise > 0 }
            .sortedByDescending { it.totalOutstandingPaise }

    fun collections(snapshot: AppSnapshot, range: DateRange): List<CollectionRow> = snapshot.girvis.flatMap { girvi ->
        EffectiveLedger.payments(girvi)
            .filter { range.contains(it.createdAt) }
            .map {
                CollectionRow(
                    receiptNumber = it.receiptNumber,
                    girviNumber = girvi.girviNumber,
                    customerName = girvi.customerName,
                    amountPaise = it.amountPaise,
                    principalPaise = it.principalPaise,
                    interestPaise = it.interestPaise,
                    chargesPaise = it.chargesPaise,
                    mode = it.mode,
                    createdAt = it.createdAt,
                )
            }
    }.sortedByDescending { it.createdAt }
}
