package com.burhannexus.dukaandost.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import com.burhannexus.dukaandost.data.local.dao.CategoryDao
import com.burhannexus.dukaandost.data.local.dao.CustomerDao
import com.burhannexus.dukaandost.data.local.dao.ProductDao
import com.burhannexus.dukaandost.data.local.dao.PurchaseReturnDao
import com.burhannexus.dukaandost.data.local.dao.SaleReturnDao
import com.burhannexus.dukaandost.data.local.dao.PurchaseDao
import com.burhannexus.dukaandost.data.local.dao.SaleDao
import com.burhannexus.dukaandost.data.local.dao.StockMovementDao
import com.burhannexus.dukaandost.data.local.dao.SupplierDao
import com.burhannexus.dukaandost.data.local.entity.CategoryEntity
import com.burhannexus.dukaandost.data.local.entity.CustomerEntity
import com.burhannexus.dukaandost.data.local.entity.ProductEntity
import com.burhannexus.dukaandost.data.local.entity.PurchaseEntity
import com.burhannexus.dukaandost.data.local.entity.PurchaseItemEntity
import com.burhannexus.dukaandost.data.local.entity.PurchaseReturnEntity
import com.burhannexus.dukaandost.data.local.entity.PurchaseReturnItemEntity
import com.burhannexus.dukaandost.data.local.entity.SaleReturnEntity
import com.burhannexus.dukaandost.data.local.entity.SaleReturnItemEntity
import com.burhannexus.dukaandost.data.local.entity.SaleEntity
import com.burhannexus.dukaandost.data.local.entity.SaleItemEntity
import com.burhannexus.dukaandost.data.local.entity.StockMovementEntity
import com.burhannexus.dukaandost.data.local.entity.SupplierEntity

/**
 * Local, offline database. Every installation has its own independent copy.
 *
 * Schema changes must bump [version] and add a [Migration] to [MIGRATIONS].
 * Destructive migration is deliberately NOT used: user data must never be wiped silently.
 */
@Database(
    entities = [
        CategoryEntity::class, ProductEntity::class, StockMovementEntity::class,
        SupplierEntity::class, CustomerEntity::class,
        PurchaseEntity::class, PurchaseItemEntity::class,
        SaleEntity::class, SaleItemEntity::class,
        SaleReturnEntity::class, SaleReturnItemEntity::class,
        PurchaseReturnEntity::class, PurchaseReturnItemEntity::class
    ],
    version = 3,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun categoryDao(): CategoryDao
    abstract fun productDao(): ProductDao
    abstract fun stockMovementDao(): StockMovementDao
    abstract fun supplierDao(): SupplierDao
    abstract fun customerDao(): CustomerDao
    abstract fun purchaseDao(): PurchaseDao
    abstract fun saleDao(): SaleDao
    abstract fun saleReturnDao(): SaleReturnDao
    abstract fun purchaseReturnDao(): PurchaseReturnDao

    companion object {
        val MIGRATIONS: Array<Migration> = arrayOf(MIGRATION_1_2, MIGRATION_2_3)
    }
}
