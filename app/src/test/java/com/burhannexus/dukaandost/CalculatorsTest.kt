package com.burhannexus.dukaandost

import com.burhannexus.dukaandost.domain.calc.PurchaseCalcLine
import com.burhannexus.dukaandost.domain.calc.PurchaseCalculator
import com.burhannexus.dukaandost.domain.calc.SaleCalcLine
import com.burhannexus.dukaandost.domain.calc.SaleCalculator
import com.burhannexus.dukaandost.domain.calc.WeightedAverage
import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class CalculatorsTest {
    private fun d(v: String) = BigDecimal(v)

    // ---- Sales
    @Test fun saleTotalsWithItemAndInvoiceDiscount() {
        val totals = SaleCalculator.calculate(listOf(SaleCalcLine(d("3"), d("50.00"), d("10"))), d("5"))!!
        assertEquals(d("140.00"), totals.lineTotals[0])
        assertEquals(d("140.00"), totals.subtotal)
        assertEquals(d("135.00"), totals.total)
    }

    @Test fun saleRoundsLineAmountHalfUp() {
        val totals = SaleCalculator.calculate(listOf(SaleCalcLine(d("0.5"), d("0.25"), d("0"))), d("0"))!!
        assertEquals(d("0.13"), totals.lineTotals[0])
    }

    @Test fun saleRejectsInvalidFigures() {
        assertNull(SaleCalculator.calculate(emptyList(), d("0")))
        assertNull(SaleCalculator.calculate(listOf(SaleCalcLine(d("0"), d("10"), d("0"))), d("0")))
        assertNull(SaleCalculator.calculate(listOf(SaleCalcLine(d("1"), d("10"), d("11"))), d("0")))
        assertNull(SaleCalculator.calculate(listOf(SaleCalcLine(d("1"), d("10"), d("0"))), d("10.01")))
        assertNull(SaleCalculator.calculate(listOf(SaleCalcLine(d("1"), d("10"), d("0"))), d("-1")))
        assertNotNull(SaleCalculator.calculate(listOf(SaleCalcLine(d("1"), d("10"), d("0"))), d("10")))
    }

    // ---- Purchases
    @Test fun purchaseSharesExtraExpensesAndDiscountAcrossLines() {
        val totals = PurchaseCalculator.calculate(
            listOf(
                PurchaseCalcLine(d("10"), d("5.00"), d("0")),
                PurchaseCalcLine(d("5"), d("10.00"), d("0"))
            ),
            invoiceDiscount = d("10"),
            extraExpenses = d("20")
        )!!
        assertEquals(d("100.00"), totals.subtotal)
        assertEquals(d("110.00"), totals.total)
        assertEquals(d("55.00"), totals.landedTotals[0])
        assertEquals(d("55.00"), totals.landedTotals[1])
    }

    @Test fun purchaseLandedTotalsAlwaysSumToTotal() {
        val totals = PurchaseCalculator.calculate(
            listOf(
                PurchaseCalcLine(d("1"), d("33.33"), d("0")),
                PurchaseCalcLine(d("1"), d("33.33"), d("0")),
                PurchaseCalcLine(d("1"), d("33.34"), d("0"))
            ),
            invoiceDiscount = d("0"),
            extraExpenses = d("10")
        )!!
        assertEquals(totals.total, totals.landedTotals.fold(BigDecimal.ZERO) { a, b -> a.add(b) })
    }

    @Test fun purchaseRejectsInvalidFigures() {
        assertNull(PurchaseCalculator.calculate(emptyList(), d("0"), d("0")))
        assertNull(PurchaseCalculator.calculate(listOf(PurchaseCalcLine(d("1"), d("10"), d("0"))), d("11"), d("0")))
        assertNull(PurchaseCalculator.calculate(listOf(PurchaseCalcLine(d("1"), d("10"), d("0"))), d("0"), d("-1")))
    }

    // ---- Weighted average cost
    @Test fun averageCostBlendsOldAndNewStock() =
        assertEquals(d("7.50"), WeightedAverage.newCost(d("10"), d("5.00"), d("10"), d("100.00")))

    @Test fun averageCostWithNoStockIsNewCost() =
        assertEquals(d("10.00"), WeightedAverage.newCost(d("0"), d("99.00"), d("10"), d("100.00")))

    @Test fun negativeStockCountsAsNoValue() =
        assertEquals(d("10.00"), WeightedAverage.newCost(d("-3"), d("99.00"), d("10"), d("100.00")))

    @Test fun costOfGoodsRoundsToPaisa() =
        assertEquals(d("3.75"), WeightedAverage.costOfGoods(d("2.5"), d("1.50")))
}
