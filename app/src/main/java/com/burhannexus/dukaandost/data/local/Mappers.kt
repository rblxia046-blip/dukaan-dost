package com.burhannexus.dukaandost.data.local

import com.burhannexus.dukaandost.core.Minor
import com.burhannexus.dukaandost.data.local.entity.StockMovementEntity
import com.burhannexus.dukaandost.domain.model.Category
import com.burhannexus.dukaandost.domain.model.Product
import com.burhannexus.dukaandost.domain.model.StockMovement
import com.burhannexus.dukaandost.domain.model.StockMovementType
import java.time.LocalDate

fun ProductWithCategory.toDomain(): Product = Product(
    id = product.id,
    name = product.name,
    categoryId = product.categoryId,
    categoryName = categoryName,
    description = product.description,
    sku = product.sku,
    barcode = product.barcode,
    purchasePrice = Minor.minorToMoney(product.purchasePriceMinor),
    salePrice = Minor.minorToMoney(product.salePriceMinor),
    stock = Minor.milliToQuantity(product.stockMilli),
    minStock = Minor.milliToQuantity(product.minStockMilli),
    unit = product.unit,
    expiryDate = product.expiryEpochDay?.let { LocalDate.ofEpochDay(it) }
)

fun CategoryWithCount.toDomain(): Category = Category(id, name, productCount)

fun StockMovementEntity.toDomain(): StockMovement = StockMovement(
    id = id,
    productId = productId,
    type = runCatching { StockMovementType.valueOf(type) }.getOrDefault(StockMovementType.ADJUSTMENT),
    quantityChange = Minor.milliToQuantity(quantityChangeMilli),
    balanceAfter = Minor.milliToQuantity(balanceAfterMilli),
    note = note,
    createdAt = createdAt
)

fun InvoiceRow.toSummary(): com.burhannexus.dukaandost.domain.model.InvoiceSummary =
    com.burhannexus.dukaandost.domain.model.InvoiceSummary(
        id = id,
        number = number,
        createdAt = createdAt,
        partyName = partyName,
        total = Minor.minorToMoney(totalMinor),
        paid = Minor.minorToMoney(paidMinor)
    )

fun paymentMethodOf(value: String): com.burhannexus.dukaandost.domain.model.PaymentMethod =
    runCatching { com.burhannexus.dukaandost.domain.model.PaymentMethod.valueOf(value) }
        .getOrDefault(com.burhannexus.dukaandost.domain.model.PaymentMethod.CASH)
