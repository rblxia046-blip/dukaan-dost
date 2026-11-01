package com.burhannexus.dukaandost.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "suppliers", indices = [Index("name")])
data class SupplierEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val phone: String?,
    val address: String?,
    val openingBalanceMinor: Long,
    val isActive: Boolean = true,
    val createdAt: Long
)

@Entity(tableName = "customers", indices = [Index("name")])
data class CustomerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val phone: String?,
    val address: String?,
    val openingBalanceMinor: Long,
    val isActive: Boolean = true,
    val createdAt: Long
)

@Entity(
    tableName = "purchases",
    foreignKeys = [
        ForeignKey(
            entity = SupplierEntity::class,
            parentColumns = ["id"],
            childColumns = ["supplierId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index("supplierId"),
        Index(value = ["number"], unique = true),
        Index("createdAt")
    ]
)
data class PurchaseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val number: Long,
    val supplierId: Long?,
    val createdAt: Long,
    val subtotalMinor: Long,
    val discountMinor: Long,
    val extraExpensesMinor: Long,
    val totalMinor: Long,
    val paidMinor: Long,
    val paymentMethod: String,
    val note: String
)

@Entity(
    tableName = "purchase_items",
    foreignKeys = [
        ForeignKey(
            entity = PurchaseEntity::class,
            parentColumns = ["id"],
            childColumns = ["purchaseId"],
            onDelete = ForeignKey.RESTRICT
        ),
        ForeignKey(
            entity = ProductEntity::class,
            parentColumns = ["id"],
            childColumns = ["productId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [Index("purchaseId"), Index("productId")]
)
data class PurchaseItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val purchaseId: Long,
    val productId: Long,
    val quantityMilli: Long,
    val unitCostMinor: Long,
    val discountMinor: Long,
    val lineTotalMinor: Long,
    /** Line total plus its share of extra expenses minus invoice discount (used for average cost). */
    val landedTotalMinor: Long
)

@Entity(
    tableName = "sales",
    foreignKeys = [
        ForeignKey(
            entity = CustomerEntity::class,
            parentColumns = ["id"],
            childColumns = ["customerId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index("customerId"),
        Index(value = ["number"], unique = true),
        Index(value = ["clientToken"], unique = true),
        Index("createdAt")
    ]
)
data class SaleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val number: Long,
    val clientToken: String,
    val customerId: Long?,
    val createdAt: Long,
    val subtotalMinor: Long,
    val discountMinor: Long,
    val totalMinor: Long,
    val paidMinor: Long,
    val paymentMethod: String,
    /** Cost of goods sold at the moment of sale (weighted average cost). */
    val cogsMinor: Long,
    val note: String
)

@Entity(
    tableName = "sale_items",
    foreignKeys = [
        ForeignKey(
            entity = SaleEntity::class,
            parentColumns = ["id"],
            childColumns = ["saleId"],
            onDelete = ForeignKey.RESTRICT
        ),
        ForeignKey(
            entity = ProductEntity::class,
            parentColumns = ["id"],
            childColumns = ["productId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [Index("saleId"), Index("productId")]
)
data class SaleItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val saleId: Long,
    val productId: Long,
    /** Name at the time of sale, so old invoices never change. */
    val productName: String,
    val quantityMilli: Long,
    val unitPriceMinor: Long,
    val unitCostMinor: Long,
    val discountMinor: Long,
    val lineTotalMinor: Long
)
