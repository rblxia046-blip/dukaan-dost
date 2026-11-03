package com.burhannexus.dukaandost.domain.model

import com.burhannexus.dukaandost.domain.calc.ReturnCalculator
import java.math.BigDecimal

data class ReturnableLine(
    val itemId: Long,
    val name: String,
    val soldQuantity: BigDecimal,
    val returnedQuantity: BigDecimal,
    /** Whole-line value after the invoice discount share (landed value for purchases). */
    val netLineValue: BigDecimal,
    val returnedValue: BigDecimal
) {
    val returnable: BigDecimal get() = soldQuantity.subtract(returnedQuantity)

    fun valueFor(quantity: BigDecimal): BigDecimal =
        ReturnCalculator.returnValue(netLineValue, soldQuantity, quantity, returnedQuantity, returnedValue)
}

data class ReturnableInvoice(
    val kind: InvoiceKind,
    val invoiceId: Long,
    val number: Long,
    val partyName: String?,
    val lines: List<ReturnableLine>,
    val total: BigDecimal,
    val paid: BigDecimal,
    val returnedValue: BigDecimal,
    val refundedAmount: BigDecimal
)

data class ReturnLineInput(val itemId: Long, val quantity: BigDecimal)

data class ReturnInput(
    val invoiceId: Long,
    val lines: List<ReturnLineInput>,
    val method: PaymentMethod
)

sealed interface ReturnResult {
    data class Success(val id: Long, val number: Long, val value: BigDecimal, val refund: BigDecimal) : ReturnResult
    object NothingSelected : ReturnResult
    object InvalidQuantity : ReturnResult
    object NotFound : ReturnResult
    data class NotEnoughStock(val productName: String) : ReturnResult
}
