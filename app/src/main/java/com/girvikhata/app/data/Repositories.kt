package com.girvikhata.app.data

import com.girvikhata.app.domain.Category
import com.girvikhata.app.domain.Customer
import com.girvikhata.app.domain.GirviAccount
import com.girvikhata.app.domain.InterestPlan
import com.girvikhata.app.domain.ItemMaster
import com.girvikhata.app.domain.Payment
import kotlinx.coroutines.flow.Flow

interface CustomerRepository {
    fun observeAll(): Flow<List<Customer>>
    fun search(query: String): Flow<List<Customer>>
    suspend fun get(id: String): Customer?
    suspend fun save(customer: Customer)
}

interface MasterDataRepository {
    fun observeCategories(): Flow<List<Category>>
    fun observeItems(categoryId: String): Flow<List<ItemMaster>>
    fun observeInterestPlans(): Flow<List<InterestPlan>>
    suspend fun saveCategory(category: Category)
    suspend fun saveItem(item: ItemMaster)
    suspend fun saveInterestPlan(plan: InterestPlan)
    suspend fun deactivateCategory(id: String)
    suspend fun deactivateItem(id: String)
}

interface GirviRepository {
    fun observeActive(): Flow<List<GirviAccount>>
    fun observeByCustomer(customerId: String): Flow<List<GirviAccount>>
    fun search(query: String): Flow<List<GirviAccount>>
    suspend fun get(id: String): GirviAccount?
    suspend fun save(account: GirviAccount)
    suspend fun update(account: GirviAccount)
}

interface PaymentRepository {
    fun observeForGirvi(girviId: String): Flow<List<Payment>>
    suspend fun post(payment: Payment)
    suspend fun reverse(original: Payment, reversal: Payment)
}

interface AuditRepository {
    suspend fun record(event: AuditEvent)
}

data class AuditEvent(
    val action: String,
    val entityType: String,
    val entityId: String,
    val atEpochMillis: Long,
    val safeMetadata: Map<String, String> = emptyMap(),
)
