package com.burhannexus.dukaandost.data.repository

import com.burhannexus.dukaandost.core.Minor
import com.burhannexus.dukaandost.data.local.dao.CustomerDao
import com.burhannexus.dukaandost.data.local.dao.CustomerPaymentDao
import com.burhannexus.dukaandost.data.local.dao.ProductDao
import com.burhannexus.dukaandost.data.local.dao.PurchaseDao
import com.burhannexus.dukaandost.data.local.dao.PurchaseReturnDao
import com.burhannexus.dukaandost.data.local.dao.SaleDao
import com.burhannexus.dukaandost.data.local.dao.SaleReturnDao
import com.burhannexus.dukaandost.data.local.dao.SupplierDao
import com.burhannexus.dukaandost.data.local.dao.SupplierPaymentDao
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
 *  - Sales and Profit are net of sales returns. Receivables/payables are net of returns and refunds.
 *  - Cash balance = received from sales - refunded to customers - paid for purchases + refunds from suppliers
 *    + payments received from customers - payments made to suppliers (expenses and opening cash come later).
 *  - Receivables and payables also include opening balances and are reduced by payments on account.
 */
@Singleton
class RoomDashboardRepository @Inject constructor(
    private val productDao: ProductDao,
    private val saleDao: SaleDao,
    private val purchaseDao: PurchaseDao,
    private val saleReturnDao: SaleReturnDao,
    private val purchaseReturnDao: PurchaseReturnDao,
    private val customerDao: CustomerDao,
    private val supplierDao: SupplierDao,
    private val customerPaymentDao: CustomerPaymentDao,
    private val supplierPaymentDao: SupplierPaymentDao
) : DashboardRepository {

    override fun observeSummary(range: DateRange): Flow<DashboardSummary> {
        val zone = ZoneId.systemDefault()
        val start = range.start.atStartOfDay(zone).toInstant().toEpochMilli()
        val end = range.end.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()

        val money = combine(
            listOf(
                saleDao.observeSalesTotal(start, end),            // 0
                saleReturnDao.observeValue(start, end),           // 1
                saleDao.observeProfit(start, end),                // 2
                saleReturnDao.observeProfitImpact(start, end),    // 3
                saleDao.observeReceivable(),                      // 4
                saleReturnDao.observeValueAll(),                  // 5
                saleReturnDao.observeRefundAll(),                 // 6
                purchaseDao.observePayable(),                     // 7
                purchaseReturnDao.observeValueAll(),              // 8
                purchaseReturnDao.observeRefundAll(),             // 9
                saleDao.observePaidTotal(),                       // 10
                purchaseDao.observePaidTotal(),                   // 11
                customerDao.observeOpeningTotal(),                // 12
                supplierDao.observeOpeningTotal(),                // 13
                customerPaymentDao.observeTotal(),                // 14
                supplierPaymentDao.observeTotal()                 // 15
            )
        ) { it }

        return combine(
            productDao.observeStats(),
            money,
            saleDao.observeRecent(RECENT),
            purchaseDao.observeRecent(RECENT)
        ) { stats, m, recentSales, recentPurchases ->
            DashboardSummary.EMPTY.copy(
                sales = Minor.minorToMoney(m[0] - m[1]),
                profit = Minor.minorToMoney(m[2] - m[3]),
                totalProducts = stats.totalProducts,
                totalStockQuantity = Minor.milliToQuantity(stats.totalStockMilli),
                lowStockProducts = stats.lowStock,
                outOfStockProducts = stats.outOfStock,
                customerReceivables = Minor.minorToMoney(m[4] - m[5] + m[6] + m[12] - m[14]),
                supplierPayables = Minor.minorToMoney(m[7] - m[8] + m[9] + m[13] - m[15]),
                cashBalance = Minor.minorToMoney(m[10] - m[6] - m[11] + m[9] + m[14] - m[15]),
                recentSales = recentSales.map { it.toSummary() },
                recentPurchases = recentPurchases.map { it.toSummary() }
            )
        }
    }

    private companion object {
        const val RECENT = 5
    }
}
