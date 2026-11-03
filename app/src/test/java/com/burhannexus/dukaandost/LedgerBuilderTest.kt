package com.burhannexus.dukaandost

import com.burhannexus.dukaandost.domain.calc.LedgerBuilder
import com.burhannexus.dukaandost.domain.calc.LedgerEvent
import com.burhannexus.dukaandost.domain.model.LedgerEntryKind
import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Test

class LedgerBuilderTest {
    private fun d(v: String) = BigDecimal(v)

    @Test fun runningBalanceFollowsBillsReturnsAndPayments() {
        val entries = LedgerBuilder.build(
            opening = d("100"), openingAt = 0L,
            events = listOf(
                LedgerEvent(10, LedgerEntryKind.INVOICE, "S-000001", d("300"), d("0")),
                LedgerEvent(10, LedgerEntryKind.PAID_WITH_INVOICE, "S-000001", d("0"), d("100")),
                LedgerEvent(20, LedgerEntryKind.RETURN, "SR-000001", d("0"), d("50")),
                LedgerEvent(30, LedgerEntryKind.PAYMENT, "RV-000001", d("0"), d("40"))
            )
        )
        assertEquals(listOf(d("100"), d("400"), d("300"), d("250"), d("210")), entries.map { it.balance })
        assertEquals(LedgerEntryKind.OPENING, entries.first().kind)
    }

    @Test fun zeroOpeningBalanceIsNotListed() {
        val entries = LedgerBuilder.build(d("0"), 0L, listOf(LedgerEvent(1, LedgerEntryKind.INVOICE, "S-1", d("50"), d("0"))))
        assertEquals(1, entries.size)
        assertEquals(d("50"), entries[0].balance)
    }

    @Test fun eventsAreSortedByTimeButSameTimeKeepsOrder() {
        val entries = LedgerBuilder.build(
            d("0"), 0L,
            listOf(
                LedgerEvent(30, LedgerEntryKind.PAYMENT, "RV-1", d("0"), d("10")),
                LedgerEvent(10, LedgerEntryKind.INVOICE, "S-1", d("100"), d("0")),
                LedgerEvent(10, LedgerEntryKind.PAID_WITH_INVOICE, "S-1", d("0"), d("20"))
            )
        )
        assertEquals(listOf("S-1", "S-1", "RV-1"), entries.map { it.reference })
        assertEquals(d("70"), entries.last().balance)
    }

    @Test fun refundRaisesTheBalanceAgain() {
        // Fully paid sale 100, returned 100 and cash handed back: customer owes nothing.
        val entries = LedgerBuilder.build(
            d("0"), 0L,
            listOf(
                LedgerEvent(1, LedgerEntryKind.INVOICE, "S-1", d("100"), d("0")),
                LedgerEvent(1, LedgerEntryKind.PAID_WITH_INVOICE, "S-1", d("0"), d("100")),
                LedgerEvent(2, LedgerEntryKind.RETURN, "SR-1", d("0"), d("100")),
                LedgerEvent(2, LedgerEntryKind.REFUND, "SR-1", d("100"), d("0"))
            )
        )
        assertEquals(d("0"), entries.last().balance)
    }
}
