package com.burhannexus.dukaandost.ui.trade

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.burhannexus.dukaandost.R
import com.burhannexus.dukaandost.domain.model.InvoiceKind
import com.burhannexus.dukaandost.domain.model.PaymentMethod
import com.burhannexus.dukaandost.domain.model.ReturnInput
import com.burhannexus.dukaandost.domain.model.ReturnLineInput
import com.burhannexus.dukaandost.domain.model.ReturnResult
import com.burhannexus.dukaandost.domain.model.ReturnableInvoice
import com.burhannexus.dukaandost.domain.repository.PurchaseRepository
import com.burhannexus.dukaandost.domain.repository.SalesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ReturnUiState(
    val loading: Boolean = true,
    val invoice: ReturnableInvoice? = null,
    val quantities: Map<Long, String> = emptyMap(),
    val method: PaymentMethod = PaymentMethod.CASH,
    val saving: Boolean = false,
    val message: UiText? = null,
    val done: ReturnResult.Success? = null
)

/** One view model for both kinds: route argument "kind" is "sale" or "purchase". */
@HiltViewModel
class ReturnViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val sales: SalesRepository,
    private val purchases: PurchaseRepository
) : ViewModel() {

    private val invoiceId: Long = savedStateHandle.get<Long>("invoiceId") ?: 0L
    val isSale: Boolean = savedStateHandle.get<String>("kind") != "purchase"

    private val _state = MutableStateFlow(ReturnUiState())
    val state: StateFlow<ReturnUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val source = if (isSale) sales.observeReturnable(invoiceId) else purchases.observeReturnable(invoiceId)
            source.collect { invoice -> _state.update { it.copy(loading = false, invoice = invoice) } }
        }
    }

    fun setQuantity(itemId: Long, text: String) {
        _state.update { it.copy(quantities = it.quantities + (itemId to text), message = null) }
    }

    fun setMethod(method: PaymentMethod) {
        _state.update { it.copy(method = method) }
    }

    fun dismissDone() {
        _state.update { it.copy(done = null) }
    }

    fun save() {
        val s = _state.value
        val invoice = s.invoice ?: return
        if (s.saving) return
        val evaluation = evaluateReturn(invoice, s.quantities)
        if (evaluation.hasError) {
            _state.update { it.copy(message = UiText(R.string.return_err_quantity)) }
            return
        }
        if (!evaluation.hasSelection) {
            _state.update { it.copy(message = UiText(R.string.return_err_nothing)) }
            return
        }
        _state.update { it.copy(saving = true, message = null) }
        val input = ReturnInput(
            invoiceId = invoiceId,
            lines = evaluation.lines
                .filter { it.quantity.signum() > 0 }
                .map { ReturnLineInput(it.line.itemId, it.quantity) },
            method = s.method
        )
        viewModelScope.launch {
            try {
                val result = if (isSale) sales.saveReturn(input) else purchases.saveReturn(input)
                when (result) {
                    is ReturnResult.Success -> _state.update {
                        it.copy(saving = false, quantities = emptyMap(), done = result)
                    }
                    ReturnResult.NothingSelected -> fail(UiText(R.string.return_err_nothing))
                    ReturnResult.InvalidQuantity -> fail(UiText(R.string.return_err_quantity))
                    ReturnResult.NotFound -> fail(UiText(R.string.return_err_not_found))
                    is ReturnResult.NotEnoughStock -> fail(UiText(R.string.return_err_stock, listOf(result.productName)))
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                fail(UiText(R.string.trade_err_save_failed))
            }
        }
    }

    private fun fail(message: UiText) {
        _state.update { it.copy(saving = false, message = message) }
    }

    val kind: InvoiceKind get() = if (isSale) InvoiceKind.SALE else InvoiceKind.PURCHASE
}
