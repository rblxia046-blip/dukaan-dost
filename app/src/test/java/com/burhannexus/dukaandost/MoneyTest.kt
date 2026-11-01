package com.burhannexus.dukaandost

import com.burhannexus.dukaandost.core.Money
import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Test

class MoneyTest {
    @Test fun formatsZero() = assertEquals("Rs. 0.00", Money.format(BigDecimal.ZERO))
    @Test fun formatsThousandsAndRounds() =
        assertEquals("Rs. 1,234.50", Money.format(BigDecimal("1234.5")))
    @Test fun roundsHalfUp() = assertEquals("Rs. 0.13", Money.format(BigDecimal("0.125")))
    @Test fun formatsQuantity() {
        assertEquals("0", Money.formatQuantity(BigDecimal.ZERO))
        assertEquals("12.5", Money.formatQuantity(BigDecimal("12.500")))
        assertEquals("100", Money.formatQuantity(BigDecimal("100.00")))
    }
}
