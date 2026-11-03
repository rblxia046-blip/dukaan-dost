package com.burhannexus.dukaandost.ui.trade

import com.burhannexus.dukaandost.core.Minor
import com.burhannexus.dukaandost.domain.calc.ReturnCalculator
import com.burhannexus.dukaandost.domain.model.ReturnableInvoice
import com.burhannexus.dukaandost.domain.model.ReturnableLine
import com.burhannexus.dukaandost.ui.products.ProductValidator
import java.math.BigDecimal

data class LineEvaluation(
    val line: ReturnableLine,
    val quantity: BigDecimal,
    val hasError: Boolean,
    val value: BigDecimal
)

data class ReturnEvaluation(
    val lines: List<LineEvaluation>,
    val totalValue: BigDecimal,
    /** Cash that changes hands because of this return. */
    val refund: BigDecimal,
    val hasError: Boolean,
    val hasSelection: Boolean
)

/** Pure calculation of what the typed quantities mean; shared by the screen and the save action. */
fun evaluateReturn(invoice: ReturnableInvoice, texts: Map<Long, String>): ReturnEvaluation {
    val lines = invoice.lines.map { line ->
        val text = texts[line.itemId].orEmpty().trim()
        if (text.isEmpty()) {
            LineEvaluation(line, BigDecimal.ZERO, false, BigDecimal.ZERO)
        } else {
            // The maximum allowed is what is still returnable, so too-large values are rejected.
            val quantity = ProductValidator.parseDecimal(text, 3, line.returnable.min(Minor.MAX_QUANTITY))
            when {
                quantity == null -> LineEvaluation(line, BigDecimal.ZERO, true, BigDecimal.ZERO)
                quantity.signum() == 0 -> LineEvaluation(line, BigDecimal.ZERO, false, BigDecimal.ZERO)
                else -> LineEvaluation(line, quantity, false, line.valueFor(quantity))
            }
        }
    }
    val totalValue = lines.fold(BigDecimal.ZERO) { acc, l -> acc.add(l.value) }
    val refund = ReturnCalculator.refund(
        total = invoice.total,
        paid = invoice.paid,
        refundedBefore = invoice.refundedAmount,
        returnedBefore = invoice.returnedValue,
        thisReturnValue = totalValue
    )
    return ReturnEvaluation(
        lines = lines,
        totalValue = totalValue,
        refund = refund,
        hasError = lines.any { it.hasError },
        hasSelection = lines.any { it.quantity.signum() > 0 }
    )
}
