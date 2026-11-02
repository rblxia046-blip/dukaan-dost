package com.burhannexus.dukaandost.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "sale_returns",
    foreignKeys = [
        ForeignKey(
            entity = SaleEntity::class,
            parentColumns = ["id"],
            childColumns = ["saleId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [Index("saleId"), Index(value = ["number"], unique = true), Index("createdAt")]
)
data class SaleReturnEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val number: Long,
    val saleId: Long,
    val createdAt: Long,
    /** Value of the goods returned (after the invoice discount share). */
    val valueMinor: Long,
    /** Cash actually handed back to the customer. */
    val refundMinor: Long,
    /** Cost of the returned goods (original cost snapshot). */
    val cogsMinor: Long,
    val paymentMethod: String,
    val note: String
)

@Entity(
    tableName = "sale_return_items",
    foreignKeys = [
        ForeignKey(
            entity = SaleReturnEntity::class,
            parentColumns = ["id"],
            childColumns = ["returnId"],
            onDelete = ForeignKey.RESTRICT
        ),
        ForeignKey(
            entity = SaleItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["saleItemId"],
            onDelete = ForeignKey.RESTRICT
        ),
        ForeignKey(
            entity = ProductEntity::class,
            parentColumns = ["id"],
            childColumns = ["productId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [Index("returnId"), Index("saleItemId"), Index("productId")]
)
data class SaleReturnItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val returnId: Long,
    val saleItemId: Long,
    val productId: Long,
    val quantityMilli: Long,
    val valueMinor: Long
)

@Entity(
    tableName = "purchase_returns",
    foreignKeys = [
        ForeignKey(
            entity = PurchaseEntity::class,
            parentColumns = ["id"],
            childColumns = ["purchaseId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [Index("purchaseId"), Index(value = ["number"], unique = true), Index("createdAt")]
)
data class PurchaseReturnEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val number: Long,
    val purchaseId: Long,
    val createdAt: Long,
    /** Landed value of the goods sent back to the supplier. */
    val valueMinor: Long,
    /** Cash received back from the supplier. */
    val refundMinor: Long,
    val paymentMethod: String,
    val note: String
)

@Entity(
    tableName = "purchase_return_items",
    foreignKeys = [
        ForeignKey(
            entity = PurchaseReturnEntity::class,
            parentColumns = ["id"],
            childColumns = ["returnId"],
            onDelete = ForeignKey.RESTRICT
        ),
        ForeignKey(
            entity = PurchaseItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["purchaseItemId"],
            onDelete = ForeignKey.RESTRICT
        ),
        ForeignKey(
            entity = ProductEntity::class,
            parentColumns = ["id"],
            childColumns = ["productId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [Index("returnId"), Index("purchaseItemId"), Index("productId")]
)
data class PurchaseReturnItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val returnId: Long,
    val purchaseItemId: Long,
    val productId: Long,
    val quantityMilli: Long,
    val valueMinor: Long
)
