package com.girvikhata.app.domain

import com.girvikhata.app.data.AppSnapshot
import com.girvikhata.app.data.CustomerRecord
import com.girvikhata.app.data.GirviRecord

/** Customer profile operations kept independent from Compose so they remain fully testable. */
object CustomerAccountOperations {
    data class Profile(
        val customer: CustomerRecord,
        val girvis: List<GirviRecord>,
        val activeCount: Int,
        val releasedCount: Int,
        val effectiveReceivedPaise: Long,
        val totalOutstandingPaise: Long,
    )

    fun profile(snapshot: AppSnapshot, customerId: String, settlementMonths: Int): Profile {
        require(settlementMonths in 0..120) { "Settlement months must be between 0 and 120" }
        val customer = snapshot.customers.firstOrNull { it.id == customerId }
            ?: throw IllegalArgumentException("Customer not found")
        val girvis = snapshot.girvis.filter { it.customerId == customerId }.sortedByDescending { it.createdAt }
        return Profile(
            customer = customer,
            girvis = girvis,
            activeCount = girvis.count { it.status == "ACTIVE" },
            releasedCount = girvis.count { it.status == "RELEASED" },
            effectiveReceivedPaise = girvis.sumOf(EffectiveLedger::receivedPaise),
            totalOutstandingPaise = girvis.sumOf { GirviSettlementUseCase.settlementView(it, settlementMonths).totalDuePaise },
        )
    }

    fun updateCustomer(
        snapshot: AppSnapshot,
        customerId: String,
        name: String,
        mobile: String,
        address: String,
    ): AppSnapshot {
        val cleanName = name.trim().replace(Regex("\\s+"), " ")
        val cleanMobile = mobile.filter(Char::isDigit)
        val cleanAddress = address.trim().replace(Regex("\\s+"), " ")
        require(cleanName.length >= 2) { "Customer name is required" }
        require(cleanMobile.isEmpty() || cleanMobile.length in 10..15) { "Mobile number must contain 10 to 15 digits" }
        require(snapshot.customers.any { it.id == customerId }) { "Customer not found" }
        require(snapshot.customers.none { it.id != customerId && cleanMobile.isNotEmpty() && it.mobile.filter(Char::isDigit) == cleanMobile }) {
            "Another customer already uses this mobile number"
        }

        val customers = snapshot.customers.map {
            if (it.id == customerId) it.copy(name = cleanName, mobile = cleanMobile, address = cleanAddress) else it
        }
        val girvis = snapshot.girvis.map {
            if (it.customerId == customerId) it.copy(customerName = cleanName) else it
        }
        return snapshot.copy(customers = customers, girvis = girvis)
    }

    fun canDelete(snapshot: AppSnapshot, customerId: String): Boolean =
        snapshot.girvis.none { it.customerId == customerId }

    fun deleteUnusedCustomer(snapshot: AppSnapshot, customerId: String): AppSnapshot {
        require(canDelete(snapshot, customerId)) { "Customer has girvi history and cannot be deleted" }
        require(snapshot.customers.any { it.id == customerId }) { "Customer not found" }
        return snapshot.copy(customers = snapshot.customers.filterNot { it.id == customerId })
    }
}
