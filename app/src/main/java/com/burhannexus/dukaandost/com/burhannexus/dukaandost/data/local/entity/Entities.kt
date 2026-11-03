package com.burhannexus.dukaandost.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "categories", indices = [Index(value = ["name"], unique = true)])
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(collate = ColumnInfo.NOCASE) val name: String
)

@Entity(
    tableName = "products",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("categoryId"), Index("barcode"), Index("sku"), Index("name")]
)
data class ProductEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val categoryId: Long?,
    val description: String,
    val sku: String?,
    val barcode: String?,
    /** Paisa (2 decimals). */
    val purchasePriceMinor: Long,
    val salePriceMinor: Long,
    /** Thousandths of a unit (3 decimals). */
    val stockMilli: Long,
    val minStockMilli: Long,
    val unit: String,
    val expiryEpochDay: Long?,
    /** false = archived ("deleted" by the user); history is kept. */
    val isActive: Boolean = true,
    val createdAt: Long,
    val updatedAt: Long
)

@Entity(
    tableName = "stock_movements",
    foreignKeys = [
        ForeignKey(
            entity = ProductEntity::class,
            parentColumns = ["id"],
            childColumns = ["productId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [Index("productId")]
)
data class StockMovementEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val productId: Long,
    val type: String,
    val quantityChangeMilli: Long,
    val balanceAfterMilli: Long,
    val note: String = "",
    val createdAt: Long
)
