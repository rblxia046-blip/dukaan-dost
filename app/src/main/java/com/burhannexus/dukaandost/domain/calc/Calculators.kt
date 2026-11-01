package com.burhannexus.dukaandost.domain.calc

import java.math.BigDecimal
import java.math.RoundingMode

private val ZERO = BigDecimal.ZERO

private fun BigDecimal.money(): BigDecimal = setScale(2, RoundingMode.HALF_UP)

// ------------------------------------------------------------------ Sales
data class SaleCalcLine(val quantity: BigDecimal, val unitPrice: BigDecimal, val discount: BigDecimal)

data class SaleTotals(
    val lineTotals: List<BigDecimal>,
    val subtotal: BigDecimal,
    val total: BigDecimal
)

object SaleCalculator {
    /** Returns null when any figure is invalid (negative, zero quantity, discount above amount). */
    fun calculate(lines: List<SaleCalcLine>, invoiceDiscount: BigDecimal): SaleTotals? {
        if (lines.isEmpty() || invoiceDiscount.signum() < 0) return null
        val lineTotals = ArrayList<BigDecimal>()
        for (line in lines) {
            if (line.quantity.signum() <= 0 || line.unitPrice.signum() < 0 || line.discount.signum() < 0) return null
            val gross = line.quantity.multiply(line.unitPrice).money()
            if (line.discount > gross) return null
            lineTotals.add(gross.subtract(line.discount).money())
        }
        val subtotal = lineTotals.fold(ZERO) { a, b -> a.add(b) }.money()
        if (invoiceDiscount > subtotal) return null
        return SaleTotals(lineTotals, subtotal, subtotal.subtract(invoiceDiscount).money())
    }
}

// -------------------------------------------------------------- Purchases
data class PurchaseCalcLine(val quantity: BigDecimal, val unitCost: BigDecimal, val discount: BigDecimal)

data class PurchaseTotals(
    val lineTotals: List<BigDecimal>,
    /** Line total plus its share of (extra expenses - invoice discount). Sums exactly to [total]. */
    val landedTotals: List<BigDecimal>,
    val subtotal: BigDecimal,
    val total: BigDecimal
)

object PurchaseCalculator {
    fun calculate(
        lines: List<PurchaseCalcLine>,
        invoiceDiscount: BigDecimal,
        extraExpenses: BigDecimal
    ): PurchaseTotals? {
        if (lines.isEmpty() || invoiceDiscount.signum() < 0 || extraExpenses.signum() < 0) return null
        val lineTotals = ArrayList<BigDecimal>()
        for (line in lines) {
            if (line.quantity.signum() <= 0 || line.unitCost.signum() < 0 || line.discount.signum() < 0) return null
            val gross = line.quantity.multiply(line.unitCost).money()
            if (line.discount > gross) return null
            lineTotals.add(gross.subtract(line.discount).money())
        }
        val subtotal = lineTotals.fold(ZERO) { a, b -> a.add(b) }.money()
        if (invoiceDiscount > subtotal) return null

        val adjustment = extraExpenses.subtract(invoiceDiscount)
        val landed = ArrayList<BigDecimal>()
        var allocated = ZERO
        for (i in lineTotals.indices) {
            val share = if (i == lineTotals.lastIndex) {
                adjustment.subtract(allocated)
            } else if (subtotal.signum() == 0) {
                ZERO
            } else {
                adjustment.multiply(lineTotals[i]).divide(subtotal, 2, RoundingMode.HALF_UP)
            }
            if (i != lineTotals.lastIndex) allocated = allocated.add(share)
            landed.add(lineTotals[i].add(share).money())
        }
        return PurchaseTotals(lineTotals, landed, subtotal, subtotal.add(adjustment).money())
    }
}

/**
 * Inventory costing method: WEIGHTED AVERAGE COST.
 * On every purchase the product's cost becomes
 *   (existing stock x current cost + landed cost of new goods) / (existing stock + new quantity).
 * "Landed" = line amount plus its share of extra purchase expenses, minus its share of invoice discount.
 * Negative or zero existing stock counts as no value. Sales use the cost at the moment of sale (COGS).
 */
object WeightedAverage {
    fun newCost(
        oldStock: BigDecimal,
        oldCost: BigDecimal,
        addedQuantity: BigDecimal,
        addedLandedTotal: BigDecimal
    ): BigDecimal {
        val base = if (oldStock.signum() > 0) oldStock else ZERO
        val denominator = base.add(addedQuantity)
        val totalValue = base.multiply(oldCost).add(addedLandedTotal)
        return totalValue.divide(denominator, 2, RoundingMode.HALF_UP)
    }

    /** Cost of goods sold for a quantity at the current average cost. */
    fun costOfGoods(quantity: BigDecimal, unitCost: BigDecimal): BigDecimal = quantity.multiply(unitCost).money()
}
