package com.burhannexus.dukaandost

import com.burhannexus.dukaandost.core.Minor
import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Test

class MinorTest {
    @Test fun moneyRoundTrip() {
        assertEquals(12550L, Minor.moneyToMinor(BigDecimal("125.50")))
        assertEquals(BigDecimal("125.50"), Minor.minorToMoney(12550L))
    }

    @Test fun moneyRoundsHalfUp() = assertEquals(13L, Minor.moneyToMinor(BigDecimal("0.125")))

    @Test fun quantityRoundTrip() {
        assertEquals(2500L, Minor.quantityToMilli(BigDecimal("2.5")))
        assertEquals(BigDecimal("2.500"), Minor.milliToQuantity(2500L))
    }

    @Test fun largestAllowedValuesFitInLong() {
        Minor.moneyToMinor(Minor.MAX_MONEY)
        Minor.quantityToMilli(Minor.MAX_QUANTITY)
    }
}
