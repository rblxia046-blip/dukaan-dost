package com.burhannexus.dukaandost.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import com.burhannexus.dukaandost.data.local.dao.CategoryDao
import com.burhannexus.dukaandost.data.local.dao.ProductDao
import com.burhannexus.dukaandost.data.local.dao.StockMovementDao
import com.burhannexus.dukaandost.data.local.entity.CategoryEntity
import com.burhannexus.dukaandost.data.local.entity.ProductEntity
import com.burhannexus.dukaandost.data.local.entity.StockMovementEntity

/**
 * Local, offline database. Every installation has its own independent copy.
 *
 * Schema changes must bump [version] and add a [Migration] to [MIGRATIONS].
 * Destructive migration is deliberately NOT used: user data must never be wiped silently.
 */
@Database(
    entities = [CategoryEntity::class, ProductEntity::class, StockMovementEntity::class],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun categoryDao(): CategoryDao
    abstract fun productDao(): ProductDao
    abstract fun stockMovementDao(): StockMovementDao

    companion object {
        val MIGRATIONS: Array<Migration> = emptyArray()
    }
}
