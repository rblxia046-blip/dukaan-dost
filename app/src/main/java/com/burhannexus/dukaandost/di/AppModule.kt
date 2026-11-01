package com.burhannexus.dukaandost.di

import com.burhannexus.dukaandost.data.repository.CategoryRepositoryImpl
import com.burhannexus.dukaandost.data.repository.ProductRepositoryImpl
import com.burhannexus.dukaandost.data.repository.RoomDashboardRepository
import com.burhannexus.dukaandost.domain.repository.CategoryRepository
import com.burhannexus.dukaandost.domain.repository.DashboardRepository
import com.burhannexus.dukaandost.domain.repository.ProductRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {
    @Binds
    @Singleton
    abstract fun bindDashboardRepository(impl: RoomDashboardRepository): DashboardRepository

    @Binds
    @Singleton
    abstract fun bindProductRepository(impl: ProductRepositoryImpl): ProductRepository

    @Binds
    @Singleton
    abstract fun bindCategoryRepository(impl: CategoryRepositoryImpl): CategoryRepository
}
