package com.burhannexus.dukaandost

import com.burhannexus.dukaandost.core.InvoiceNumbers
import com.burhannexus.dukaandost.domain.calc.InvoiceTextFormatter
import com.burhannexus.dukaandost.domain.model.InvoiceDetail
import com.burhannexus.dukaandost.domain.model.InvoiceKind
import com.burhannexus.dukaandost.domain.model.InvoiceLine
import com.burhannexus.dukaandost.domain.model.PaymentMethod
import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class InvoiceTextFormatterTest {
    @Test fun invoiceNumbersArePadded() {
        assertEquals("S-000012", InvoiceNumbers.sale(12))
        assertEquals("P-000001", InvoiceNumbers.purchase(1))
    }

    @Test fun textContainsKeyFigures() {
        val invoice = InvoiceDetail(
            id = 1, kind = InvoiceKind.SALE, number = 7, createdAt = 0L, partyName = "Ali",
            lines = listOf(InvoiceLine("Charger", BigDecimal("2"), BigDecimal("150.00"), BigDecimal.ZERO, BigDecimal("300.00"))),
            subtotal = BigDecimal("300.00"), invoiceDiscount = BigDecimal.ZERO, extraExpenses = BigDecimal.ZERO,
            total = BigDecimal("300.00"), paid = BigDecimal("100.00"),
            paymentMethod = PaymentMethod.CASH, note = ""
        )
        val text = InvoiceTextFormatter.format(invoice, "Sales invoice", "Customer")
        assertTrue(text.contains("S-000007"))
        assertTrue(text.contains("Ali"))
        assertTrue(text.contains("Charger"))
        assertTrue(text.contains("Rs. 300.00"))
        assertTrue(text.contains("Remaining: Rs. 200.00"))
    }
}
