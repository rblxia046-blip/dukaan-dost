package com.burhannexus.dukaandost.di

import android.content.Context
import androidx.room.Room
import com.burhannexus.dukaandost.data.local.AppDatabase
import com.burhannexus.dukaandost.data.local.dao.CategoryDao
import com.burhannexus.dukaandost.data.local.dao.CustomerDao
import com.burhannexus.dukaandost.data.local.dao.ProductDao
import com.burhannexus.dukaandost.data.local.dao.PurchaseDao
import com.burhannexus.dukaandost.data.local.dao.PurchaseReturnDao
import com.burhannexus.dukaandost.data.local.dao.SaleDao
import com.burhannexus.dukaandost.data.local.dao.SaleReturnDao
import com.burhannexus.dukaandost.data.local.dao.SupplierDao
import com.burhannexus.dukaandost.data.local.dao.StockMovementDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "dukaan_dost.db")
            .addMigrations(*AppDatabase.MIGRATIONS)
            .build()

    @Provides
    fun provideCategoryDao(db: AppDatabase): CategoryDao = db.categoryDao()

    @Provides
    fun provideProductDao(db: AppDatabase): ProductDao = db.productDao()

    @Provides
    fun provideStockMovementDao(db: AppDatabase): StockMovementDao = db.stockMovementDao()

    @Provides
    fun provideSupplierDao(db: AppDatabase): SupplierDao = db.supplierDao()

    @Provides
    fun provideCustomerDao(db: AppDatabase): CustomerDao = db.customerDao()

    @Provides
    fun providePurchaseDao(db: AppDatabase): PurchaseDao = db.purchaseDao()

    @Provides
    fun provideSaleDao(db: AppDatabase): SaleDao = db.saleDao()

    @Provides
    fun provideSaleReturnDao(db: AppDatabase): SaleReturnDao = db.saleReturnDao()

    @Provides
    fun providePurchaseReturnDao(db: AppDatabase): PurchaseReturnDao = db.purchaseReturnDao()
}
