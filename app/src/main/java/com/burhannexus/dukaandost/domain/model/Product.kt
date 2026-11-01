package com.burhannexus.dukaandost.domain.model

import java.math.BigDecimal
import java.time.LocalDate

data class Product(
    val id: Long,
    val name: String,
    val categoryId: Long?,
    val categoryName: String?,
    val description: String,
    val sku: String?,
    val barcode: String?,
    val purchasePrice: BigDecimal,
    val salePrice: BigDecimal,
    val stock: BigDecimal,
    val minStock: BigDecimal,
    val unit: String,
    val expiryDate: LocalDate?
) {
    private val sellableStock: BigDecimal get() = stock.max(BigDecimal.ZERO)

    /** Stock valued at purchase price. */
    val stockValue: BigDecimal get() = sellableStock.multiply(purchasePrice)

    /** Profit if all current stock sold at the current sale price. */
    val expectedProfit: BigDecimal get() = sellableStock.multiply(salePrice.subtract(purchasePrice))

    val profitPerUnit: BigDecimal get() = salePrice.subtract(purchasePrice)
    val isOutOfStock: Boolean get() = stock.signum() <= 0
    val isLowStock: Boolean get() = stock.signum() > 0 && stock <= minStock
}

/** Used by Phase 2 (opening/adjustment); the other types are written by later phases. */
enum class StockMovementType { OPENING, ADJUSTMENT, PURCHASE, SALE, SALE_RETURN, PURCHASE_RETURN }

data class StockMovement(
    val id: Long,
    val productId: Long,
    val type: StockMovementType,
    val quantityChange: BigDecimal,
    val balanceAfter: BigDecimal,
    val note: String,
    val createdAt: Long
)

data class ProductInput(
    val id: Long,
    val name: String,
    val categoryId: Long?,
    val description: String,
    val sku: String?,
    val barcode: String?,
    val purchasePrice: BigDecimal,
    val salePrice: BigDecimal,
    val stock: BigDecimal,
    val minStock: BigDecimal,
    val unit: String,
    val expiryDate: LocalDate?
)

sealed interface SaveResult {
    data class Success(val id: Long) : SaveResult
    object DuplicateSku : SaveResult
    object DuplicateBarcode : SaveResult
    object NotFound : SaveResult
}
