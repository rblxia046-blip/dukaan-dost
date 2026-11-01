package com.burhannexus.dukaandost.data.repository

import com.burhannexus.dukaandost.core.Minor
import com.burhannexus.dukaandost.data.local.dao.ProductDao
import com.burhannexus.dukaandost.data.local.dao.PurchaseDao
import com.burhannexus.dukaandost.data.local.dao.SaleDao
import com.burhannexus.dukaandost.data.local.toSummary
import com.burhannexus.dukaandost.domain.model.DashboardSummary
import com.burhannexus.dukaandost.domain.model.DateRange
import com.burhannexus.dukaandost.domain.repository.DashboardRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Every figure comes from database records.
 *  - Sales and Profit: for the selected date range. Profit = sales (after discounts) - cost of goods sold.
 *    Expenses are not recorded yet (a later phase), so Expenses is 0 and Profit is gross profit.
 *  - Receivables, payables, cash balance and stock: current totals (not date-filtered).
 *  - Cash balance = money received from sales - money paid for purchases (expenses/opening cash come later).
 */
@Singleton
class RoomDashboardRepository @Inject constructor(
    private val productDao: ProductDao,
    private val saleDao: SaleDao,
    private val purchaseDao: PurchaseDao
) : DashboardRepository {

    override fun observeSummary(range: DateRange): Flow<DashboardSummary> {
        val zone = ZoneId.systemDefault()
        val start = range.start.atStartOfDay(zone).toInstant().toEpochMilli()
        val end = range.end.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()

        val money = combine(
            listOf(
                saleDao.observeSalesTotal(start, end),
                saleDao.observeProfit(start, end),
                saleDao.observeReceivable(),
                purchaseDao.observePayable(),
                saleDao.observePaidTotal(),
                purchaseDao.observePaidTotal()
            )
        ) { it }

        return combine(
            productDao.observeStats(),
            money,
            saleDao.observeRecent(RECENT),
            purchaseDao.observeRecent(RECENT)
        ) { stats, m, recentSales, recentPurchases ->
            DashboardSummary.EMPTY.copy(
                sales = Minor.minorToMoney(m[0]),
                profit = Minor.minorToMoney(m[1]),
                totalProducts = stats.totalProducts,
                totalStockQuantity = Minor.milliToQuantity(stats.totalStockMilli),
                lowStockProducts = stats.lowStock,
                outOfStockProducts = stats.outOfStock,
                customerReceivables = Minor.minorToMoney(m[2]),
                supplierPayables = Minor.minorToMoney(m[3]),
                cashBalance = Minor.minorToMoney(m[4] - m[5]),
                recentSales = recentSales.map { it.toSummary() },
                recentPurchases = recentPurchases.map { it.toSummary() }
            )
        }
    }

    private companion object {
        const val RECENT = 5
    }
}
