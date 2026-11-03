package com.burhannexus.dukaandost.domain.calc

import com.burhannexus.dukaandost.domain.model.LedgerEntry
import com.burhannexus.dukaandost.domain.model.LedgerEntryKind
import java.math.BigDecimal

data class LedgerEvent(
    val createdAt: Long,
    val kind: LedgerEntryKind,
    val reference: String,
    val increase: BigDecimal,
    val decrease: BigDecimal
)

object LedgerBuilder {
    /**
     * Sorts the events by time (same-time events keep their given order) and adds the running balance.
     * An opening balance, when not zero, always comes first.
     */
    fun build(opening: BigDecimal, openingAt: Long, events: List<LedgerEvent>): List<LedgerEntry> {
        val entries = ArrayList<LedgerEntry>()
        var balance = opening
        if (opening.signum() != 0) {
            entries.add(LedgerEntry(openingAt, LedgerEntryKind.OPENING, "", opening, BigDecimal.ZERO, balance))
        }
        for (event in events.sortedBy { it.createdAt }) {
            balance = balance.add(event.increase).subtract(event.decrease)
            entries.add(
                LedgerEntry(event.createdAt, event.kind, event.reference, event.increase, event.decrease, balance)
            )
        }
        return entries
    }
}
