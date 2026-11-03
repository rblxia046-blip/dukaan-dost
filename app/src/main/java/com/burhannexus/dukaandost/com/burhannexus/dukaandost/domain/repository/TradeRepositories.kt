package com.burhannexus.dukaandost.domain.repository

import com.burhannexus.dukaandost.domain.model.InvoiceDetail
import com.burhannexus.dukaandost.domain.model.InvoiceSummary
import com.burhannexus.dukaandost.domain.model.Ledger
import com.burhannexus.dukaandost.domain.model.PartyAccount
import com.burhannexus.dukaandost.domain.model.PartyInput
import com.burhannexus.dukaandost.domain.model.PartyKind
import com.burhannexus.dukaandost.domain.model.PartyPaymentResult
import com.burhannexus.dukaandost.domain.model.PartySaveResult
import com.burhannexus.dukaandost.domain.model.PaymentMethod
import com.burhannexus.dukaandost.domain.model.ReturnListItem
import com.burhannexus.dukaandost.domain.model.Party
import com.burhannexus.dukaandost.domain.model.ReturnInput
import com.burhannexus.dukaandost.domain.model.ReturnResult
import com.burhannexus.dukaandost.domain.model.ReturnableInvoice
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

    // ---- Phase 4: full management and ledgers ----
    fun observeAccounts(kind: PartyKind): Flow<List<PartyAccount>>
    suspend fun getAccount(kind: PartyKind, id: Long): PartyAccount?
    suspend fun saveParty(kind: PartyKind, input: PartyInput): PartySaveResult
    fun observeLedger(kind: PartyKind, id: Long): Flow<Ledger?>

    /** Money received from a customer / paid to a supplier on account. Reduces the outstanding balance. */
    suspend fun recordPayment(
        kind: PartyKind,
        partyId: Long,
        amount: java.math.BigDecimal,
        method: PaymentMethod,
        note: String
    ): PartyPaymentResult
}

interface PurchaseRepository {
    fun observeHistory(): Flow<List<InvoiceSummary>>
    fun observe(id: Long): Flow<InvoiceDetail?>

    /** Saves the invoice, raises stock, updates average cost and writes stock history in ONE transaction. */
    suspend fun save(input: PurchaseInput): PurchaseResult

    fun observeReturnable(purchaseId: Long): Flow<ReturnableInvoice?>

    /** Sends goods back to the supplier: stock falls, payable falls, any excess is refunded in cash. One transaction. */
    suspend fun saveReturn(input: ReturnInput): ReturnResult

    fun observeReturns(): Flow<List<ReturnListItem>>
}

interface SalesRepository {
    fun observeHistory(): Flow<List<InvoiceSummary>>
    fun observe(id: Long): Flow<InvoiceDetail?>

    /** Saves the invoice, lowers stock and writes stock history in ONE transaction. Idempotent per clientToken. */
    suspend fun save(input: SaleInput): SaleResult

    fun observeReturnable(saleId: Long): Flow<ReturnableInvoice?>

    /** Takes goods back from a customer: stock rises, receivable falls, any excess is refunded in cash. One transaction. */
    suspend fun saveReturn(input: ReturnInput): ReturnResult

    fun observeReturns(): Flow<List<ReturnListItem>>
}
