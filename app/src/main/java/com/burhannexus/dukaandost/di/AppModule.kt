package com.burhannexus.dukaandost.di

import com.burhannexus.dukaandost.data.repository.EmptyDashboardRepository
import com.burhannexus.dukaandost.domain.repository.DashboardRepository
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
    abstract fun bindDashboardRepository(impl: EmptyDashboardRepository): DashboardRepository
}
