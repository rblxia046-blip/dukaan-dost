package com.burhannexus.dukaandost.domain.model

import java.math.BigDecimal

enum class PaymentMethod { CASH, BANK, WALLET }

/** A supplier or customer. Full management screens and ledgers arrive in Phase 4. */
data class Party(val id: Long, val name: String, val phone: String?)

enum class InvoiceKind { PURCHASE, SALE }

data class InvoiceLine(
    val name: String,
    val quantity: BigDecimal,
    val unitPrice: BigDecimal,
    val discount: BigDecimal,
    val total: BigDecimal
)

data class InvoiceSummary(
    val id: Long,
    val number: Long,
    val createdAt: Long,
    val partyName: String?,
    val total: BigDecimal,
    val paid: BigDecimal,
    val returned: BigDecimal = BigDecimal.ZERO,
    val refunded: BigDecimal = BigDecimal.ZERO
) {
    /** Still owed after returns: total - returned goods - (money paid - money handed back). */
    val remaining: BigDecimal get() = total.subtract(returned).subtract(paid.subtract(refunded))
}

data class ReturnSummary(
    val id: Long,
    val number: Long,
    val createdAt: Long,
    val value: BigDecimal,
    val refund: BigDecimal
)

data class InvoiceDetail(
    val id: Long,
    val kind: InvoiceKind,
    val number: Long,
    val createdAt: Long,
    val partyName: String?,
    val lines: List<InvoiceLine>,
    val subtotal: BigDecimal,
    val invoiceDiscount: BigDecimal,
    val extraExpenses: BigDecimal,
    val total: BigDecimal,
    val paid: BigDecimal,
    val paymentMethod: PaymentMethod,
    val note: String,
    val returns: List<ReturnSummary> = emptyList()
) {
    val returnedValue: BigDecimal get() = returns.fold(BigDecimal.ZERO) { a, r -> a.add(r.value) }
    val refundedAmount: BigDecimal get() = returns.fold(BigDecimal.ZERO) { a, r -> a.add(r.refund) }
    val remaining: BigDecimal get() = total.subtract(returnedValue).subtract(paid.subtract(refundedAmount))
}

// ---- Purchases ----
data class PurchaseLineInput(
    val productId: Long,
    val quantity: BigDecimal,
    val unitCost: BigDecimal,
    val discount: BigDecimal
)

data class PurchaseInput(
    val supplierId: Long?,
    val lines: List<PurchaseLineInput>,
    val invoiceDiscount: BigDecimal,
    val extraExpenses: BigDecimal,
    val paid: BigDecimal,
    val method: PaymentMethod,
    val note: String
)

sealed interface PurchaseResult {
    data class Success(val id: Long, val number: Long) : PurchaseResult
    object EmptyCart : PurchaseResult
    object InvalidAmount : PurchaseResult
    object SupplierRequired : PurchaseResult
    object ProductMissing : PurchaseResult
}

// ---- Sales ----
data class SaleLineInput(
    val productId: Long,
    val quantity: BigDecimal,
    val unitPrice: BigDecimal,
    val discount: BigDecimal
)

data class SaleInput(
    /** Unique per checkout attempt; saving the same token twice never creates a second sale. */
    val clientToken: String,
    val customerId: Long?,
    val lines: List<SaleLineInput>,
    val invoiceDiscount: BigDecimal,
    val paid: BigDecimal,
    val method: PaymentMethod,
    val note: String
)

sealed interface SaleResult {
    data class Success(val id: Long, val number: Long) : SaleResult
    data class InsufficientStock(val productName: String, val available: BigDecimal) : SaleResult
    object EmptyCart : SaleResult
    object InvalidAmount : SaleResult
    object CustomerRequired : SaleResult
    object ProductMissing : SaleResult
}
