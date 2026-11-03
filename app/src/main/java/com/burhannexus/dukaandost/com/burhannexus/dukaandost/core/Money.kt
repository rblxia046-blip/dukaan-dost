package com.burhannexus.dukaandost.core

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

/** Money display helper. All monetary values in the app are BigDecimal, never Float/Double. */
object Money {
    private const val SYMBOL = "Rs."

    fun format(amount: BigDecimal): String {
        // Locale.US symbols keep Latin digits and separators readable in English and Urdu layouts.
        val df = DecimalFormat("#,##0.00", DecimalFormatSymbols(Locale.US))
        df.roundingMode = RoundingMode.HALF_UP
        return "$SYMBOL ${df.format(amount)}"
    }

    fun formatQuantity(quantity: BigDecimal): String {
        if (quantity.signum() == 0) return "0"
        return quantity.stripTrailingZeros().toPlainString()
    }
}
