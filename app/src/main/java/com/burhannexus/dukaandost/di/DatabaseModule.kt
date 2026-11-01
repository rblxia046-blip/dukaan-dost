package com.burhannexus.dukaandost.di

import android.content.Context
import androidx.room.Room
import com.burhannexus.dukaandost.data.local.AppDatabase
import com.burhannexus.dukaandost.data.local.dao.CategoryDao
import com.burhannexus.dukaandost.data.local.dao.ProductDao
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
}
