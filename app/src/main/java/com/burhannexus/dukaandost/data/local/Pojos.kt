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
