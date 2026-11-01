package com.burhannexus.dukaandost.data.repository

import com.burhannexus.dukaandost.data.local.dao.CustomerDao
import com.burhannexus.dukaandost.data.local.dao.SupplierDao
import com.burhannexus.dukaandost.data.local.entity.CustomerEntity
import com.burhannexus.dukaandost.data.local.entity.SupplierEntity
import com.burhannexus.dukaandost.domain.model.Party
import com.burhannexus.dukaandost.domain.repository.PartyRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PartyRepositoryImpl @Inject constructor(
    private val supplierDao: SupplierDao,
    private val customerDao: CustomerDao
) : PartyRepository {

    override fun observeSuppliers(): Flow<List<Party>> =
        supplierDao.observeActive().map { list -> list.map { Party(it.id, it.name, it.phone) } }

    override fun observeCustomers(): Flow<List<Party>> =
        customerDao.observeActive().map { list -> list.map { Party(it.id, it.name, it.phone) } }

    override suspend fun addSupplier(name: String, phone: String): Long? {
        val clean = name.trim()
        if (clean.isEmpty() || clean.length > MAX_NAME) return null
        return supplierDao.insert(
            SupplierEntity(
                name = clean,
                phone = phone.trim().ifEmpty { null },
                address = null,
                openingBalanceMinor = 0,
                createdAt = System.currentTimeMillis()
            )
        )
    }

    override suspend fun addCustomer(name: String, phone: String): Long? {
        val clean = name.trim()
        if (clean.isEmpty() || clean.length > MAX_NAME) return null
        return customerDao.insert(
            CustomerEntity(
                name = clean,
                phone = phone.trim().ifEmpty { null },
                address = null,
                openingBalanceMinor = 0,
                createdAt = System.currentTimeMillis()
            )
        )
    }

    private companion object {
        const val MAX_NAME = 100
    }
}
