package com.burhannexus.dukaandost

import com.burhannexus.dukaandost.domain.calc.ReturnCalculator
import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Test

class ReturnCalculatorTest {
    private fun d(v: String) = BigDecimal(v)

    @Test fun netLineValueRemovesInvoiceDiscountShare() =
        assertEquals(d("126.00"), ReturnCalculator.netLineValue(d("140.00"), d("140.00"), d("14.00")))

    @Test fun partialThenRemainingReturnAddsUpExactly() {
        val net = d("126.00")
        val first = ReturnCalculator.returnValue(net, d("4"), d("1"), d("0"), d("0"))
        assertEquals(d("31.50"), first)
        val rest = ReturnCalculator.returnValue(net, d("4"), d("3"), d("1"), first)
        assertEquals(d("94.50"), rest)
    }

    @Test fun threeSingleUnitReturnsNeverExceedLineValue() {
        val net = d("100.00")
        val a = ReturnCalculator.returnValue(net, d("3"), d("1"), d("0"), d("0"))
        val b = ReturnCalculator.returnValue(net, d("3"), d("1"), d("1"), a)
        val c = ReturnCalculator.returnValue(net, d("3"), d("1"), d("2"), a.add(b))
        assertEquals(d("33.33"), a)
        assertEquals(d("33.33"), b)
        assertEquals(d("33.34"), c)
        assertEquals(d("100.00"), a.add(b).add(c))
    }

    @Test fun returnFirstCancelsUnpaidBalance() {
        // Bill 300, paid 100: owes 200. Returning 50 leaves 150 owed, nothing to refund.
        assertEquals(d("0.00"), ReturnCalculator.refund(d("300"), d("100"), d("0"), d("0"), d("50")))
    }

    @Test fun refundIsOnlyTheOverpayment() {
        // Bill 300, paid 100. Returning 250 leaves a bill of 50, so 50 of the 100 paid goes back.
        assertEquals(d("50.00"), ReturnCalculator.refund(d("300"), d("100"), d("0"), d("0"), d("250")))
    }

    @Test fun fullyPaidReturnRefundsFullValue() =
        assertEquals(d("100.00"), ReturnCalculator.refund(d("300"), d("300"), d("0"), d("0"), d("100")))

    @Test fun secondReturnConsidersEarlierRefund() =
        assertEquals(d("50.00"), ReturnCalculator.refund(d("300"), d("300"), d("100"), d("100"), d("50")))

    @Test fun returningEverythingOnCreditRefundsNothing() =
        assertEquals(d("0.00"), ReturnCalculator.refund(d("300"), d("0"), d("0"), d("0"), d("300")))
}
