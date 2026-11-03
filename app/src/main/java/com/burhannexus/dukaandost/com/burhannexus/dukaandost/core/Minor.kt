package com.burhannexus.dukaandost.core

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Conversion between BigDecimal and the integers stored in the database.
 * Money is stored as paisa (2 decimals), quantities as thousandths (3 decimals).
 * No Float/Double is ever used for business values.
 */
object Minor {
    val MAX_MONEY = BigDecimal("999999999.99")
    val MAX_QUANTITY = BigDecimal("999999999.999")

    fun moneyToMinor(value: BigDecimal): Long =
        value.setScale(2, RoundingMode.HALF_UP).unscaledValue().longValueExact()

    fun minorToMoney(minor: Long): BigDecimal = BigDecimal.valueOf(minor, 2)

    fun quantityToMilli(value: BigDecimal): Long =
        value.setScale(3, RoundingMode.HALF_UP).unscaledValue().longValueExact()

    fun milliToQuantity(milli: Long): BigDecimal = BigDecimal.valueOf(milli, 3)
}
