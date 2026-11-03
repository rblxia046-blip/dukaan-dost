package com.burhannexus.dukaandost.data.repository

import androidx.room.withTransaction
import com.burhannexus.dukaandost.core.InvoiceNumbers
import com.burhannexus.dukaandost.core.Minor
import com.burhannexus.dukaandost.data.local.AppDatabase
import com.burhannexus.dukaandost.data.local.PartyAccountRow
import com.burhannexus.dukaandost.data.local.dao.CustomerDao
import com.burhannexus.dukaandost.data.local.dao.CustomerPaymentDao
import com.burhannexus.dukaandost.data.local.dao.PurchaseDao
import com.burhannexus.dukaandost.data.local.dao.PurchaseReturnDao
import com.burhannexus.dukaandost.data.local.dao.SaleDao
import com.burhannexus.dukaandost.data.local.dao.SaleReturnDao
import com.burhannexus.dukaandost.data.local.dao.SupplierDao
import com.burhannexus.dukaandost.data.local.dao.SupplierPaymentDao
import com.burhannexus.dukaandost.data.local.entity.CustomerEntity
import com.burhannexus.dukaandost.data.local.entity.CustomerPaymentEntity
import com.burhannexus.dukaandost.data.local.entity.SupplierEntity
import com.burhannexus.dukaandost.data.local.entity.SupplierPaymentEntity
import com.burhannexus.dukaandost.domain.calc.LedgerBuilder
import com.burhannexus.dukaandost.domain.calc.LedgerEvent
import com.burhannexus.dukaandost.domain.model.Ledger
import com.burhannexus.dukaandost.domain.model.LedgerEntryKind
import com.burhannexus.dukaandost.domain.model.Party
import com.burhannexus.dukaandost.domain.model.PartyAccount
import com.burhannexus.dukaandost.domain.model.PartyInput
import com.burhannexus.dukaandost.domain.model.PartyKind
import com.burhannexus.dukaandost.domain.model.PartyPaymentResult
import com.burhannexus.dukaandost.domain.model.PartySaveResult
import com.burhannexus.dukaandost.domain.model.PaymentMethod
import com.burhannexus.dukaandost.domain.repository.PartyRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.math.BigDecimal
import java.math.RoundingMode
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PartyRepositoryImpl @Inject constructor(
    private val db: AppDatabase,
    private val supplierDao: SupplierDao,
    private val customerDao: CustomerDao,
    private val saleDao: SaleDao,
    private val saleReturnDao: SaleReturnDao,
    private val purchaseDao: PurchaseDao,
    private val purchaseReturnDao: PurchaseReturnDao,
    private val customerPaymentDao: CustomerPaymentDao,
    private val supplierPaymentDao: SupplierPaymentDao
) : PartyRepository {

    // ------------------------------------------------------------ quick lists / quick add
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

    // ------------------------------------------------------------ accounts
    override fun observeAccounts(kind: PartyKind): Flow<List<PartyAccount>> = when (kind) {
        PartyKind.CUSTOMER -> customerDao.observeAccounts()
        PartyKind.SUPPLIER -> supplierDao.observeAccounts()
    }.map { rows -> rows.map { it.toAccount() } }

    override suspend fun getAccount(kind: PartyKind, id: Long): PartyAccount? = when (kind) {
        PartyKind.CUSTOMER -> customerDao.observeAccount(id).first()
        PartyKind.SUPPLIER -> supplierDao.observeAccount(id).first()
    }?.toAccount()

    override suspend fun saveParty(kind: PartyKind, input: PartyInput): PartySaveResult {
        val name = input.name.trim()
        if (name.isEmpty()) return PartySaveResult.EMPTY_NAME
        if (name.length > MAX_NAME || input.address.trim().length > MAX_ADDRESS || input.phone.trim().length > MAX_PHONE) {
            return PartySaveResult.TOO_LONG
        }
        if (input.openingBalance.signum() < 0 || input.openingBalance > Minor.MAX_MONEY) {
            return PartySaveResult.INVALID_AMOUNT
        }
        val phone = input.phone.trim().ifEmpty { null }
        val address = input.address.trim().ifEmpty { null }
        val openingMinor = Minor.moneyToMinor(input.openingBalance)
        val now = System.currentTimeMillis()

        return db.withTransaction {
            when (kind) {
                PartyKind.CUSTOMER -> {
                    if (input.id == 0L) {
                        customerDao.insert(CustomerEntity(name = name, phone = phone, address = address, openingBalanceMinor = openingMinor, createdAt = now))
                        PartySaveResult.SUCCESS
                    } else {
                        val existing = customerDao.getById(input.id)
                        if (existing == null || !existing.isActive) {
                            PartySaveResult.NOT_FOUND
                        } else if (existing.openingBalanceMinor != openingMinor && customerDao.transactionCount(existing.id) > 0) {
                            // Changing the starting balance after transactions would silently rewrite history.
                            PartySaveResult.OPENING_LOCKED
                        } else {
                            customerDao.update(existing.copy(name = name, phone = phone, address = address, openingBalanceMinor = openingMinor))
                            PartySaveResult.SUCCESS
                        }
                    }
                }
                PartyKind.SUPPLIER -> {
                    if (input.id == 0L) {
                        supplierDao.insert(SupplierEntity(name = name, phone = phone, address = address, openingBalanceMinor = openingMinor, createdAt = now))
                        PartySaveResult.SUCCESS
                    } else {
                        val existing = supplierDao.getById(input.id)
                        if (existing == null || !existing.isActive) {
                            PartySaveResult.NOT_FOUND
                        } else if (existing.openingBalanceMinor != openingMinor && supplierDao.transactionCount(existing.id) > 0) {
                            PartySaveResult.OPENING_LOCKED
                        } else {
                            supplierDao.update(existing.copy(name = name, phone = phone, address = address, openingBalanceMinor = openingMinor))
                            PartySaveResult.SUCCESS
                        }
                    }
                }
            }
        }
    }

    // ------------------------------------------------------------ ledger
    override fun observeLedger(kind: PartyKind, id: Long): Flow<Ledger?> = when (kind) {
        PartyKind.CUSTOMER -> combine(
            customerDao.observeAccount(id),
            saleDao.observeForCustomer(id),
            saleReturnDao.observeForCustomer(id),
            customerPaymentDao.observeForCustomer(id)
        ) { row, sales, returns, payments ->
            row?.let {
                val events = ArrayList<LedgerEvent>()
                sales.forEach { s ->
                    val ref = InvoiceNumbers.sale(s.number)
                    events.add(LedgerEvent(s.createdAt, LedgerEntryKind.INVOICE, ref, Minor.minorToMoney(s.totalMinor), BigDecimal.ZERO))
                    if (s.paidMinor > 0) {
                        events.add(LedgerEvent(s.createdAt, LedgerEntryKind.PAID_WITH_INVOICE, ref, BigDecimal.ZERO, Minor.minorToMoney(s.paidMinor)))
                    }
                }
                returns.forEach { r ->
                    val ref = InvoiceNumbers.saleReturn(r.number)
                    events.add(LedgerEvent(r.createdAt, LedgerEntryKind.RETURN, ref, BigDecimal.ZERO, Minor.minorToMoney(r.valueMinor)))
                    if (r.refundMinor > 0) {
                        events.add(LedgerEvent(r.createdAt, LedgerEntryKind.REFUND, ref, Minor.minorToMoney(r.refundMinor), BigDecimal.ZERO))
                    }
                }
                payments.forEach { p ->
                    events.add(
                        LedgerEvent(p.createdAt, LedgerEntryKind.PAYMENT, InvoiceNumbers.customerPayment(p.number), BigDecimal.ZERO, Minor.minorToMoney(p.amountMinor))
                    )
                }
                val account = it.toAccount()
                Ledger(PartyKind.CUSTOMER, account, LedgerBuilder.build(account.openingBalance, 0L, events))
            }
        }

        PartyKind.SUPPLIER -> combine(
            supplierDao.observeAccount(id),
            purchaseDao.observeForSupplier(id),
            purchaseReturnDao.observeForSupplier(id),
            supplierPaymentDao.observeForSupplier(id)
        ) { row, purchases, returns, payments ->
            row?.let {
                val events = ArrayList<LedgerEvent>()
                purchases.forEach { p ->
                    val ref = InvoiceNumbers.purchase(p.number)
                    events.add(LedgerEvent(p.createdAt, LedgerEntryKind.INVOICE, ref, Minor.minorToMoney(p.totalMinor), BigDecimal.ZERO))
                    if (p.paidMinor > 0) {
                        events.add(LedgerEvent(p.createdAt, LedgerEntryKind.PAID_WITH_INVOICE, ref, BigDecimal.ZERO, Minor.minorToMoney(p.paidMinor)))
                    }
                }
                returns.forEach { r ->
                    val ref = InvoiceNumbers.purchaseReturn(r.number)
                    events.add(LedgerEvent(r.createdAt, LedgerEntryKind.RETURN, ref, BigDecimal.ZERO, Minor.minorToMoney(r.valueMinor)))
                    if (r.refundMinor > 0) {
                        events.add(LedgerEvent(r.createdAt, LedgerEntryKind.REFUND, ref, Minor.minorToMoney(r.refundMinor), BigDecimal.ZERO))
                    }
                }
                payments.forEach { p ->
                    events.add(
                        LedgerEvent(p.createdAt, LedgerEntryKind.PAYMENT, InvoiceNumbers.supplierPayment(p.number), BigDecimal.ZERO, Minor.minorToMoney(p.amountMinor))
                    )
                }
                val account = it.toAccount()
                Ledger(PartyKind.SUPPLIER, account, LedgerBuilder.build(account.openingBalance, 0L, events))
            }
        }
    }

    // ------------------------------------------------------------ payments
    override suspend fun recordPayment(
        kind: PartyKind,
        partyId: Long,
        amount: BigDecimal,
        method: PaymentMethod,
        note: String
    ): PartyPaymentResult {
        val value = amount.setScale(2, RoundingMode.HALF_UP)
        if (value.signum() <= 0 || value > Minor.MAX_MONEY) return PartyPaymentResult.InvalidAmount
        val minor = Minor.moneyToMinor(value)
        val now = System.currentTimeMillis()

        return db.withTransaction {
            when (kind) {
                PartyKind.CUSTOMER -> {
                    val party = customerDao.getById(partyId)
                    if (party == null || !party.isActive) {
                        PartyPaymentResult.NotFound
                    } else {
                        val number = customerPaymentDao.maxNumber() + 1
                        customerPaymentDao.insert(
                            CustomerPaymentEntity(
                                number = number, customerId = partyId, amountMinor = minor,
                                paymentMethod = method.name, note = note.trim(), createdAt = now
                            )
                        )
                        PartyPaymentResult.Success(number)
                    }
                }
                PartyKind.SUPPLIER -> {
                    val party = supplierDao.getById(partyId)
                    if (party == null || !party.isActive) {
                        PartyPaymentResult.NotFound
                    } else {
                        val number = supplierPaymentDao.maxNumber() + 1
                        supplierPaymentDao.insert(
                            SupplierPaymentEntity(
                                number = number, supplierId = partyId, amountMinor = minor,
                                paymentMethod = method.name, note = note.trim(), createdAt = now
                            )
                        )
                        PartyPaymentResult.Success(number)
                    }
                }
            }
        }
    }

    private fun PartyAccountRow.toAccount() = PartyAccount(
        id = id,
        name = name,
        phone = phone,
        address = address,
        openingBalance = Minor.minorToMoney(openingBalanceMinor),
        balance = Minor.minorToMoney(balanceMinor)
    )

    private companion object {
        const val MAX_NAME = 100
        const val MAX_ADDRESS = 200
        const val MAX_PHONE = 30
    }
}
