package com.burhannexus.dukaandost.data.repository

import com.burhannexus.dukaandost.core.Minor
import com.burhannexus.dukaandost.data.local.dao.ProductDao
import com.burhannexus.dukaandost.domain.model.DashboardSummary
import com.burhannexus.dukaandost.domain.model.DateRange
import com.burhannexus.dukaandost.domain.repository.DashboardRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Phase 2: product figures come from the database. Sales, profit, receivables,
 * payables, expenses and cash stay at zero until Phases 3+ record real transactions,
 * because no such records exist yet. The date range is used by those later phases.
 */
@Singleton
class RoomDashboardRepository @Inject constructor(
    private val productDao: ProductDao
) : DashboardRepository {

    override fun observeSummary(range: DateRange): Flow<DashboardSummary> =
        productDao.observeStats().map { stats ->
            DashboardSummary.EMPTY.copy(
                totalProducts = stats.totalProducts,
                totalStockQuantity = Minor.milliToQuantity(stats.totalStockMilli),
                lowStockProducts = stats.lowStock,
                outOfStockProducts = stats.outOfStock
            )
        }
}
