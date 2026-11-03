package com.burhannexus.dukaandost.data.repository

import androidx.room.withTransaction
import com.burhannexus.dukaandost.core.InvoiceNumbers
import com.burhannexus.dukaandost.core.Minor
import com.burhannexus.dukaandost.data.local.AppDatabase
import com.burhannexus.dukaandost.data.local.dao.ProductDao
import com.burhannexus.dukaandost.data.local.dao.SaleDao
import com.burhannexus.dukaandost.data.local.dao.SaleReturnDao
import com.burhannexus.dukaandost.data.local.dao.StockMovementDao
import com.burhannexus.dukaandost.data.local.entity.ProductEntity
import com.burhannexus.dukaandost.data.local.entity.SaleEntity
import com.burhannexus.dukaandost.data.local.entity.SaleItemEntity
import com.burhannexus.dukaandost.data.local.entity.SaleReturnEntity
import com.burhannexus.dukaandost.data.local.entity.SaleReturnItemEntity
import com.burhannexus.dukaandost.data.local.entity.StockMovementEntity
import com.burhannexus.dukaandost.data.local.paymentMethodOf
import com.burhannexus.dukaandost.data.local.toSummary
import com.burhannexus.dukaandost.domain.calc.ReturnCalculator
import com.burhannexus.dukaandost.domain.calc.SaleCalcLine
import com.burhannexus.dukaandost.domain.calc.SaleCalculator
import com.burhannexus.dukaandost.domain.calc.SaleTotals
import com.burhannexus.dukaandost.domain.calc.WeightedAverage
import com.burhannexus.dukaandost.domain.model.InvoiceDetail
import com.burhannexus.dukaandost.domain.model.InvoiceKind
import com.burhannexus.dukaandost.domain.model.InvoiceLine
import com.burhannexus.dukaandost.domain.model.InvoiceSummary
import com.burhannexus.dukaandost.domain.model.ReturnInput
import com.burhannexus.dukaandost.domain.model.ReturnResult
import com.burhannexus.dukaandost.domain.model.ReturnListItem
import com.burhannexus.dukaandost.domain.model.ReturnSummary
import com.burhannexus.dukaandost.domain.model.ReturnableInvoice
import com.burhannexus.dukaandost.domain.model.ReturnableLine
import com.burhannexus.dukaandost.domain.model.SaleInput
import com.burhannexus.dukaandost.domain.model.SaleResult
import com.burhannexus.dukaandost.domain.model.StockMovementType
import com.burhannexus.dukaandost.domain.repository.SalesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.math.BigDecimal
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SalesRepositoryImpl @Inject constructor(
    private val db: AppDatabase,
    private val saleDao: SaleDao,
    private val returnDao: SaleReturnDao,
    private val productDao: ProductDao,
    private val movementDao: StockMovementDao
) : SalesRepository {

    override fun observeHistory(): Flow<List<InvoiceSummary>> =
        saleDao.observeRecent(HISTORY_LIMIT).map { rows -> rows.map { it.toSummary() } }

    override fun observe(id: Long): Flow<InvoiceDetail?> =
        combine(saleDao.observeById(id), saleDao.observeItems(id), returnDao.observeForSale(id)) { head, items, returns ->
            head?.let {
                val s = it.sale
                InvoiceDetail(
                    id = s.id,
                    kind = InvoiceKind.SALE,
                    number = s.number,
                    createdAt = s.createdAt,
                    partyName = it.partyName,
                    lines = items.map { item ->
                        InvoiceLine(
                            name = item.productName,
                            quantity = Minor.milliToQuantity(item.quantityMilli),
                            unitPrice = Minor.minorToMoney(item.unitPriceMinor),
                            discount = Minor.minorToMoney(item.discountMinor),
                            total = Minor.minorToMoney(item.lineTotalMinor)
                        )
                    },
                    subtotal = Minor.minorToMoney(s.subtotalMinor),
                    invoiceDiscount = Minor.minorToMoney(s.discountMinor),
                    extraExpenses = BigDecimal.ZERO,
                    total = Minor.minorToMoney(s.totalMinor),
                    paid = Minor.minorToMoney(s.paidMinor),
                    paymentMethod = paymentMethodOf(s.paymentMethod),
                    note = s.note,
                    returns = returns.map { r ->
                        ReturnSummary(
                            id = r.id,
                            number = r.number,
                            createdAt = r.createdAt,
                            value = Minor.minorToMoney(r.valueMinor),
                            refund = Minor.minorToMoney(r.refundMinor)
                        )
                    }
                )
            }
        }

    override suspend fun save(input: SaleInput): SaleResult = db.withTransaction {
        val existing = saleDao.findByToken(input.clientToken)
        if (existing != null) {
            // Same checkout submitted twice (double tap, retry): return the original sale.
            SaleResult.Success(existing.id, existing.number)
        } else if (input.lines.isEmpty()) {
            SaleResult.EmptyCart
        } else if (input.lines.map { it.productId }.toSet().size != input.lines.size) {
            SaleResult.InvalidAmount
        } else {
            val totals = SaleCalculator.calculate(
                input.lines.map { SaleCalcLine(it.quantity, it.unitPrice, it.discount) },
                input.invoiceDiscount
            )
            if (totals == null || input.paid.signum() < 0 || input.paid > totals.total) {
                SaleResult.InvalidAmount
            } else if (input.paid < totals.total && input.customerId == null) {
                SaleResult.CustomerRequired
            } else {
                saveValidated(input, totals)
            }
        }
    }

    private suspend fun saveValidated(input: SaleInput, totals: SaleTotals): SaleResult {
        val products = ArrayList<ProductEntity>()
        for (line in input.lines) {
            val product = productDao.getEntity(line.productId)
            if (product == null || !product.isActive) return SaleResult.ProductMissing
            // Selling more than the available stock is not allowed (the opt-in setting comes with Settings).
            if (Minor.quantityToMilli(line.quantity) > product.stockMilli) {
                return SaleResult.InsufficientStock(product.name, Minor.milliToQuantity(product.stockMilli))
            }
            products.add(product)
        }

        // Cost of goods sold per line at the current weighted average cost.
        val lineCosts = input.lines.mapIndexed { index, line ->
            WeightedAverage.costOfGoods(line.quantity, Minor.minorToMoney(products[index].purchasePriceMinor))
        }
        val cogs = lineCosts.fold(BigDecimal.ZERO) { a, b -> a.add(b) }

        val now = System.currentTimeMillis()
        val number = saleDao.maxNumber() + 1
        val saleId = saleDao.insert(
            SaleEntity(
                number = number,
                clientToken = input.clientToken,
                customerId = input.customerId,
                createdAt = now,
                subtotalMinor = Minor.moneyToMinor(totals.subtotal),
                discountMinor = Minor.moneyToMinor(input.invoiceDiscount),
                totalMinor = Minor.moneyToMinor(totals.total),
                paidMinor = Minor.moneyToMinor(input.paid),
                paymentMethod = input.method.name,
                cogsMinor = Minor.moneyToMinor(cogs),
                note = input.note.trim()
            )
        )

        input.lines.forEachIndexed { index, line ->
            val product = products[index]
            val qtyMilli = Minor.quantityToMilli(line.quantity)
            val newStockMilli = product.stockMilli - qtyMilli

            saleDao.insertItem(
                SaleItemEntity(
                    saleId = saleId,
                    productId = product.id,
                    productName = product.name,
                    quantityMilli = qtyMilli,
                    unitPriceMinor = Minor.moneyToMinor(line.unitPrice),
                    unitCostMinor = product.purchasePriceMinor,
                    discountMinor = Minor.moneyToMinor(line.discount),
                    lineTotalMinor = Minor.moneyToMinor(totals.lineTotals[index])
                )
            )
            productDao.update(product.copy(stockMilli = newStockMilli, updatedAt = now))
            movementDao.insert(
                StockMovementEntity(
                    productId = product.id,
                    type = StockMovementType.SALE.name,
                    quantityChangeMilli = -qtyMilli,
                    balanceAfterMilli = newStockMilli,
                    note = InvoiceNumbers.sale(number),
                    createdAt = now
                )
            )
        }
        return SaleResult.Success(saleId, number)
    }

    override fun observeReturnable(saleId: Long): Flow<ReturnableInvoice?> =
        combine(
            saleDao.observeById(saleId),
            saleDao.observeItems(saleId),
            returnDao.observeReturnedByItem(saleId),
            returnDao.observeForSale(saleId)
        ) { head, items, returnedRows, returns ->
            head?.let {
                val sale = it.sale
                val returnedByItem = returnedRows.associateBy { row -> row.itemId }
                ReturnableInvoice(
                    kind = InvoiceKind.SALE,
                    invoiceId = sale.id,
                    number = sale.number,
                    partyName = it.partyName,
                    lines = items.map { item ->
                        val prev = returnedByItem[item.id]
                        ReturnableLine(
                            itemId = item.id,
                            name = item.productName,
                            soldQuantity = Minor.milliToQuantity(item.quantityMilli),
                            returnedQuantity = Minor.milliToQuantity(prev?.quantityMilli ?: 0L),
                            netLineValue = ReturnCalculator.netLineValue(
                                Minor.minorToMoney(item.lineTotalMinor),
                                Minor.minorToMoney(sale.subtotalMinor),
                                Minor.minorToMoney(sale.discountMinor)
                            ),
                            returnedValue = Minor.minorToMoney(prev?.valueMinor ?: 0L)
                        )
                    },
                    total = Minor.minorToMoney(sale.totalMinor),
                    paid = Minor.minorToMoney(sale.paidMinor),
                    returnedValue = Minor.minorToMoney(returns.sumOf { r -> r.valueMinor }),
                    refundedAmount = Minor.minorToMoney(returns.sumOf { r -> r.refundMinor })
                )
            }
        }

    override suspend fun saveReturn(input: ReturnInput): ReturnResult = db.withTransaction {
        val sale = saleDao.getSale(input.invoiceId)
        if (sale == null) {
            ReturnResult.NotFound
        } else if (input.lines.isEmpty()) {
            ReturnResult.NothingSelected
        } else if (input.lines.map { it.itemId }.toSet().size != input.lines.size) {
            ReturnResult.InvalidQuantity
        } else {
            saveReturnValidated(sale, input)
        }
    }

    private suspend fun saveReturnValidated(sale: SaleEntity, input: ReturnInput): ReturnResult {
        val items = saleDao.getItems(sale.id).associateBy { it.id }
        val returned = returnDao.getReturnedByItem(sale.id).associateBy { it.itemId }
        val totals = returnDao.totalsForSale(sale.id)
        val subtotal = Minor.minorToMoney(sale.subtotalMinor)
        val discount = Minor.minorToMoney(sale.discountMinor)

        val prepared = ArrayList<PreparedSaleReturnLine>()
        var value = BigDecimal.ZERO
        var cogs = BigDecimal.ZERO
        for (line in input.lines) {
            val item = items[line.itemId] ?: return ReturnResult.InvalidQuantity
            if (line.quantity.signum() <= 0) return ReturnResult.InvalidQuantity
            val sold = Minor.milliToQuantity(item.quantityMilli)
            val prev = returned[item.id]
            val prevQuantity = Minor.milliToQuantity(prev?.quantityMilli ?: 0L)
            val prevValue = Minor.minorToMoney(prev?.valueMinor ?: 0L)
            if (line.quantity > sold.subtract(prevQuantity)) return ReturnResult.InvalidQuantity

            val net = ReturnCalculator.netLineValue(Minor.minorToMoney(item.lineTotalMinor), subtotal, discount)
            val lineValue = ReturnCalculator.returnValue(net, sold, line.quantity, prevQuantity, prevValue)
            value = value.add(lineValue)
            cogs = cogs.add(WeightedAverage.costOfGoods(line.quantity, Minor.minorToMoney(item.unitCostMinor)))
            prepared.add(PreparedSaleReturnLine(item, Minor.quantityToMilli(line.quantity), lineValue))
        }

        val refund = ReturnCalculator.refund(
            total = Minor.minorToMoney(sale.totalMinor),
            paid = Minor.minorToMoney(sale.paidMinor),
            refundedBefore = Minor.minorToMoney(totals.refundMinor),
            returnedBefore = Minor.minorToMoney(totals.valueMinor),
            thisReturnValue = value
        )

        val now = System.currentTimeMillis()
        val number = returnDao.maxNumber() + 1
        val returnId = returnDao.insert(
            SaleReturnEntity(
                number = number,
                saleId = sale.id,
                createdAt = now,
                valueMinor = Minor.moneyToMinor(value),
                refundMinor = Minor.moneyToMinor(refund),
                cogsMinor = Minor.moneyToMinor(cogs),
                paymentMethod = input.method.name,
                note = ""
            )
        )
        for (p in prepared) {
            returnDao.insertItem(
                SaleReturnItemEntity(
                    returnId = returnId,
                    saleItemId = p.item.id,
                    productId = p.item.productId,
                    quantityMilli = p.quantityMilli,
                    valueMinor = Minor.moneyToMinor(p.value)
                )
            )
            val product = productDao.getEntity(p.item.productId) ?: continue
            val newStock = product.stockMilli + p.quantityMilli
            productDao.update(product.copy(stockMilli = newStock, updatedAt = now))
            movementDao.insert(
                StockMovementEntity(
                    productId = product.id,
                    type = StockMovementType.SALE_RETURN.name,
                    quantityChangeMilli = p.quantityMilli,
                    balanceAfterMilli = newStock,
                    note = InvoiceNumbers.saleReturn(number),
                    createdAt = now
                )
            )
        }
        return ReturnResult.Success(returnId, number, value, refund)
    }

    private class PreparedSaleReturnLine(val item: SaleItemEntity, val quantityMilli: Long, val value: BigDecimal)

    override fun observeReturns(): Flow<List<ReturnListItem>> =
        returnDao.observeAll(RETURN_LIST_LIMIT).map { rows ->
            rows.map { row ->
                ReturnListItem(
                    id = row.id,
                    number = row.number,
                    createdAt = row.createdAt,
                    invoiceId = row.invoiceId,
                    invoiceNumber = row.invoiceNumber,
                    partyName = row.partyName,
                    value = Minor.minorToMoney(row.valueMinor),
                    refund = Minor.minorToMoney(row.refundMinor)
                )
            }
        }

    private companion object {
        const val HISTORY_LIMIT = 200
        const val RETURN_LIST_LIMIT = 200
    }
}
