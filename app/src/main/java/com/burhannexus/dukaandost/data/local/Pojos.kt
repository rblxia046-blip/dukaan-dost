package com.burhannexus.dukaandost.data.local

import androidx.room.Embedded
import com.burhannexus.dukaandost.data.local.entity.ProductEntity

data class ProductWithCategory(
    @Embedded val product: ProductEntity,
    val categoryName: String?
)

data class ProductStats(
    val totalProducts: Int,
    val totalStockMilli: Long,
    val lowStock: Int,
    val outOfStock: Int
)

data class CategoryWithCount(
    val id: Long,
    val name: String,
    val productCount: Int
)

data class InvoiceRow(
    val id: Long,
    val number: Long,
    val createdAt: Long,
    val totalMinor: Long,
    val paidMinor: Long,
    val partyName: String?,
    val returnedMinor: Long,
    val refundedMinor: Long
)

data class PurchaseWithSupplier(
    @Embedded val purchase: com.burhannexus.dukaandost.data.local.entity.PurchaseEntity,
    val partyName: String?
)

data class PurchaseItemRow(
    @Embedded val item: com.burhannexus.dukaandost.data.local.entity.PurchaseItemEntity,
    val productName: String
)

data class SaleWithCustomer(
    @Embedded val sale: com.burhannexus.dukaandost.data.local.entity.SaleEntity,
    val partyName: String?
)

data class ReturnedItemRow(
    val itemId: Long,
    val quantityMilli: Long,
    val valueMinor: Long
)

data class ReturnTotalsRow(
    val valueMinor: Long,
    val refundMinor: Long
)
