package com.burhannexus.dukaandost.domain.model

import java.math.BigDecimal

enum class PartyKind { CUSTOMER, SUPPLIER }

/**
 * A customer or supplier with the money position.
 * balance > 0: customer owes us / we owe the supplier. balance < 0: advance paid by them / to them.
 */
data class PartyAccount(
    val id: Long,
    val name: String,
    val phone: String?,
    val address: String?,
    val openingBalance: BigDecimal,
    val balance: BigDecimal
)

data class PartyInput(
    /** 0 = new. */
    val id: Long,
    val name: String,
    val phone: String,
    val address: String,
    val openingBalance: BigDecimal
)

enum class PartySaveResult { SUCCESS, EMPTY_NAME, TOO_LONG, INVALID_AMOUNT, NOT_FOUND, OPENING_LOCKED }

sealed interface PartyPaymentResult {
    data class Success(val number: Long) : PartyPaymentResult
    object InvalidAmount : PartyPaymentResult
    object NotFound : PartyPaymentResult
}

enum class LedgerEntryKind { OPENING, INVOICE, PAID_WITH_INVOICE, RETURN, REFUND, PAYMENT }

/**
 * One line of a ledger. [increase] raises the outstanding balance (a bill, a cash refund),
 * [decrease] lowers it (a payment, returned goods).
 */
data class LedgerEntry(
    val createdAt: Long,
    val kind: LedgerEntryKind,
    val reference: String,
    val increase: BigDecimal,
    val decrease: BigDecimal,
    val balance: BigDecimal
)

data class Ledger(val kind: PartyKind, val account: PartyAccount, val entries: List<LedgerEntry>)

data class ReturnListItem(
    val id: Long,
    val number: Long,
    val createdAt: Long,
    val invoiceId: Long,
    val invoiceNumber: Long,
    val partyName: String?,
    val value: BigDecimal,
    val refund: BigDecimal
)
