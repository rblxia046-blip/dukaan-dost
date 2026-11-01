package com.burhannexus.dukaandost.domain.repository

import com.burhannexus.dukaandost.domain.model.InvoiceDetail
import com.burhannexus.dukaandost.domain.model.InvoiceSummary
import com.burhannexus.dukaandost.domain.model.Party
import com.burhannexus.dukaandost.domain.model.PurchaseInput
import com.burhannexus.dukaandost.domain.model.PurchaseResult
import com.burhannexus.dukaandost.domain.model.SaleInput
import com.burhannexus.dukaandost.domain.model.SaleResult
import kotlinx.coroutines.flow.Flow

interface PartyRepository {
    fun observeSuppliers(): Flow<List<Party>>
    fun observeCustomers(): Flow<List<Party>>

    /** Quick-add used while billing. Returns the new id, or null when the name is blank or too long. */
    suspend fun addSupplier(name: String, phone: String): Long?
    suspend fun addCustomer(name: String, phone: String): Long?
}

interface PurchaseRepository {
    fun observeHistory(): Flow<List<InvoiceSummary>>
    fun observe(id: Long): Flow<InvoiceDetail?>

    /** Saves the invoice, raises stock, updates average cost and writes stock history in ONE transaction. */
    suspend fun save(input: PurchaseInput): PurchaseResult
}

interface SalesRepository {
    fun observeHistory(): Flow<List<InvoiceSummary>>
    fun observe(id: Long): Flow<InvoiceDetail?>

    /** Saves the invoice, lowers stock and writes stock history in ONE transaction. Idempotent per clientToken. */
    suspend fun save(input: SaleInput): SaleResult
}
