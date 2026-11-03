package com.burhannexus.dukaandost.ui.parties

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.burhannexus.dukaandost.R
import com.burhannexus.dukaandost.core.Minor
import com.burhannexus.dukaandost.data.pdf.ReceiptService
import com.burhannexus.dukaandost.data.pdf.StatementLabels
import com.burhannexus.dukaandost.data.pdf.StatementRow
import com.burhannexus.dukaandost.domain.model.BusinessProfile
import com.burhannexus.dukaandost.domain.model.Ledger
import com.burhannexus.dukaandost.domain.model.PartyKind
import com.burhannexus.dukaandost.domain.model.PartyPaymentResult
import com.burhannexus.dukaandost.domain.model.PaymentMethod
import com.burhannexus.dukaandost.domain.repository.BusinessProfileRepository
import com.burhannexus.dukaandost.domain.repository.PartyRepository
import com.burhannexus.dukaandost.ui.products.ProductValidator
import com.burhannexus.dukaandost.ui.trade.UiText
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

sealed interface LedgerState {
    object Loading : LedgerState
    object Missing : LedgerState
    data class Found(val ledger: Ledger) : LedgerState
}

@HiltViewModel
class LedgerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: PartyRepository,
    private val receipts: ReceiptService,
    profiles: BusinessProfileRepository
) : ViewModel() {

    val kind: PartyKind = if (savedStateHandle.get<String>("kind") == "supplier") PartyKind.SUPPLIER else PartyKind.CUSTOMER
    private val partyId: Long = savedStateHandle.get<Long>("partyId") ?: 0L

    val state: StateFlow<LedgerState> = repository.observeLedger(kind, partyId)
        .map<Ledger?, LedgerState> { if (it == null) LedgerState.Missing else LedgerState.Found(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LedgerState.Loading)

    val profile: StateFlow<BusinessProfile?> = profiles.profile
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Calls [onResult] with an error message, or null when the payment was saved. */
    fun recordPayment(amountText: String, method: PaymentMethod, note: String, onResult: (UiText?) -> Unit) {
        val amount = ProductValidator.parseDecimal(amountText, 2, Minor.MAX_MONEY)
        if (amount == null || amount.signum() <= 0) {
            onResult(UiText(R.string.err_invalid_number))
            return
        }
        viewModelScope.launch {
            val result = try {
                repository.recordPayment(kind, partyId, amount, method, note)
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                null
            }
            onResult(
                when (result) {
                    is PartyPaymentResult.Success -> null
                    PartyPaymentResult.InvalidAmount -> UiText(R.string.err_invalid_number)
                    PartyPaymentResult.NotFound -> UiText(R.string.party_not_found)
                    null -> UiText(R.string.trade_err_save_failed)
                }
            )
        }
    }

    suspend fun createStatement(rows: List<StatementRow>, labels: StatementLabels): File =
        receipts.createStatementPdf(rows, labels)
}
