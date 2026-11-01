package com.burhannexus.dukaandost.domain.model

import java.math.BigDecimal

/** Dashboard figures for a date range. Every value must come from database records. */
data class DashboardSummary(
    val sales: BigDecimal,
    val profit: BigDecimal,
    val totalProducts: Int,
    val totalStockQuantity: BigDecimal,
    val lowStockProducts: Int,
    val outOfStockProducts: Int,
    val customerReceivables: BigDecimal,
    val supplierPayables: BigDecimal,
    val expenses: BigDecimal,
    val cashBalance: BigDecimal
) {
    companion object {
        /** Genuinely empty state: no records exist yet. Not sample data. */
        val EMPTY = DashboardSummary(
            sales = BigDecimal.ZERO,
            profit = BigDecimal.ZERO,
            totalProducts = 0,
            totalStockQuantity = BigDecimal.ZERO,
            lowStockProducts = 0,
            outOfStockProducts = 0,
            customerReceivables = BigDecimal.ZERO,
            supplierPayables = BigDecimal.ZERO,
            expenses = BigDecimal.ZERO,
            cashBalance = BigDecimal.ZERO
        )
    }
}
