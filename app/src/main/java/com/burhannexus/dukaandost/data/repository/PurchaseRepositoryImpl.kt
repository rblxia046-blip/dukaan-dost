package com.burhannexus.dukaandost.data.repository

import androidx.room.withTransaction
import com.burhannexus.dukaandost.core.InvoiceNumbers
import com.burhannexus.dukaandost.core.Minor
import com.burhannexus.dukaandost.data.local.AppDatabase
import com.burhannexus.dukaandost.data.local.dao.ProductDao
import com.burhannexus.dukaandost.data.local.dao.PurchaseDao
import com.burhannexus.dukaandost.data.local.dao.StockMovementDao
import com.burhannexus.dukaandost.data.local.entity.ProductEntity
import com.burhannexus.dukaandost.data.local.entity.PurchaseEntity
import com.burhannexus.dukaandost.data.local.entity.PurchaseItemEntity
import com.burhannexus.dukaandost.data.local.entity.StockMovementEntity
import com.burhannexus.dukaandost.data.local.paymentMethodOf
import com.burhannexus.dukaandost.data.local.toSummary
import com.burhannexus.dukaandost.domain.calc.PurchaseCalcLine
import com.burhannexus.dukaandost.domain.calc.PurchaseCalculator
import com.burhannexus.dukaandost.domain.calc.WeightedAverage
import com.burhannexus.dukaandost.domain.model.InvoiceDetail
import com.burhannexus.dukaandost.domain.model.InvoiceKind
import com.burhannexus.dukaandost.domain.model.InvoiceLine
import com.burhannexus.dukaandost.domain.model.InvoiceSummary
import com.burhannexus.dukaandost.domain.model.PurchaseInput
import com.burhannexus.dukaandost.domain.model.PurchaseResult
import com.burhannexus.dukaandost.domain.model.StockMovementType
import com.burhannexus.dukaandost.domain.repository.PurchaseRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.math.BigDecimal
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PurchaseRepositoryImpl @Inject constructor(
    private val db: AppDatabase,
    private val purchaseDao: PurchaseDao,
    private val productDao: ProductDao,
    private val movementDao: StockMovementDao
) : PurchaseRepository {

    override fun observeHistory(): Flow<List<InvoiceSummary>> =
        purchaseDao.observeRecent(HISTORY_LIMIT).map { rows -> rows.map { it.toSummary() } }

    override fun observe(id: Long): Flow<InvoiceDetail?> =
        combine(purchaseDao.observeById(id), purchaseDao.observeItems(id)) { head, items ->
            head?.let {
                val p = it.purchase
                InvoiceDetail(
                    id = p.id,
                    kind = InvoiceKind.PURCHASE,
                    number = p.number,
                    createdAt = p.createdAt,
                    partyName = it.partyName,
                    lines = items.map { row ->
                        InvoiceLine(
                            name = row.productName,
                            quantity = Minor.milliToQuantity(row.item.quantityMilli),
                            unitPrice = Minor.minorToMoney(row.item.unitCostMinor),
                            discount = Minor.minorToMoney(row.item.discountMinor),
                            total = Minor.minorToMoney(row.item.lineTotalMinor)
                        )
                    },
                    subtotal = Minor.minorToMoney(p.subtotalMinor),
                    invoiceDiscount = Minor.minorToMoney(p.discountMinor),
                    extraExpenses = Minor.minorToMoney(p.extraExpensesMinor),
                    total = Minor.minorToMoney(p.totalMinor),
                    paid = Minor.minorToMoney(p.paidMinor),
                    paymentMethod = paymentMethodOf(p.paymentMethod),
                    note = p.note
                )
            }
        }

    override suspend fun save(input: PurchaseInput): PurchaseResult = db.withTransaction {
        if (input.lines.isEmpty()) {
            PurchaseResult.EmptyCart
        } else if (input.lines.map { it.productId }.toSet().size != input.lines.size) {
            PurchaseResult.InvalidAmount
        } else {
            val totals = PurchaseCalculator.calculate(
                input.lines.map { PurchaseCalcLine(it.quantity, it.unitCost, it.discount) },
                input.invoiceDiscount,
                input.extraExpenses
            )
            if (totals == null || input.paid.signum() < 0 || input.paid > totals.total) {
                PurchaseResult.InvalidAmount
            } else if (input.paid < totals.total && input.supplierId == null) {
                PurchaseResult.SupplierRequired
            } else {
                saveValidated(input, totals)
            }
        }
    }

    private suspend fun saveValidated(
        input: PurchaseInput,
        totals: com.burhannexus.dukaandost.domain.calc.PurchaseTotals
    ): PurchaseResult {
        // Load every product first: if one is gone nothing is written.
        val products = ArrayList<ProductEntity>()
        for (line in input.lines) {
            val product = productDao.getEntity(line.productId)
            if (product == null || !product.isActive) return PurchaseResult.ProductMissing
            products.add(product)
        }

        val now = System.currentTimeMillis()
        val number = purchaseDao.maxNumber() + 1
        val purchaseId = purchaseDao.insert(
            PurchaseEntity(
                number = number,
                supplierId = input.supplierId,
                createdAt = now,
                subtotalMinor = Minor.moneyToMinor(totals.subtotal),
                discountMinor = Minor.moneyToMinor(input.invoiceDiscount),
                extraExpensesMinor = Minor.moneyToMinor(input.extraExpenses),
                totalMinor = Minor.moneyToMinor(totals.total),
                paidMinor = Minor.moneyToMinor(input.paid),
                paymentMethod = input.method.name,
                note = input.note.trim()
            )
        )

        input.lines.forEachIndexed { index, line ->
            val product = products[index]
            val qtyMilli = Minor.quantityToMilli(line.quantity)
            val oldStock = Minor.milliToQuantity(product.stockMilli)
            val oldCost = Minor.minorToMoney(product.purchasePriceMinor)
            val newCost = WeightedAverage.newCost(oldStock, oldCost, line.quantity, totals.landedTotals[index])
            val newStockMilli = product.stockMilli + qtyMilli

            purchaseDao.insertItem(
                PurchaseItemEntity(
                    purchaseId = purchaseId,
                    productId = product.id,
                    quantityMilli = qtyMilli,
                    unitCostMinor = Minor.moneyToMinor(line.unitCost),
                    discountMinor = Minor.moneyToMinor(line.discount),
                    lineTotalMinor = Minor.moneyToMinor(totals.lineTotals[index]),
                    landedTotalMinor = Minor.moneyToMinor(totals.landedTotals[index])
                )
            )
            productDao.update(
                product.copy(
                    stockMilli = newStockMilli,
                    purchasePriceMinor = Minor.moneyToMinor(newCost),
                    updatedAt = now
                )
            )
            movementDao.insert(
                StockMovementEntity(
                    productId = product.id,
                    type = StockMovementType.PURCHASE.name,
                    quantityChangeMilli = qtyMilli,
                    balanceAfterMilli = newStockMilli,
                    note = InvoiceNumbers.purchase(number),
                    createdAt = now
                )
            )
        }
        return PurchaseResult.Success(purchaseId, number)
    }

    private companion object {
        const val HISTORY_LIMIT = 200
    }
}
