package com.burhannexus.dukaandost.ui.trade

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.burhannexus.dukaandost.domain.model.InvoiceDetail
import com.burhannexus.dukaandost.domain.model.InvoiceSummary
import com.burhannexus.dukaandost.domain.repository.PurchaseRepository
import com.burhannexus.dukaandost.domain.repository.SalesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

sealed interface InvoiceState {
    object Loading : InvoiceState
    object Missing : InvoiceState
    data class Found(val invoice: InvoiceDetail) : InvoiceState
}

private fun InvoiceDetail?.toState(): InvoiceState =
    if (this == null) InvoiceState.Missing else InvoiceState.Found(this)

@HiltViewModel
class SaleInvoiceViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    repository: SalesRepository
) : ViewModel() {
    private val id: Long = savedStateHandle.get<Long>("invoiceId") ?: 0L
    val state: StateFlow<InvoiceState> = repository.observe(id)
        .map { it.toState() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), InvoiceState.Loading)
}

@HiltViewModel
class PurchaseInvoiceViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    repository: PurchaseRepository
) : ViewModel() {
    private val id: Long = savedStateHandle.get<Long>("invoiceId") ?: 0L
    val state: StateFlow<InvoiceState> = repository.observe(id)
        .map { it.toState() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), InvoiceState.Loading)
}

@HiltViewModel
class SalesHistoryViewModel @Inject constructor(
    repository: SalesRepository
) : ViewModel() {
    val invoices: StateFlow<List<InvoiceSummary>> = repository.observeHistory()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}

@HiltViewModel
class PurchaseHistoryViewModel @Inject constructor(
    repository: PurchaseRepository
) : ViewModel() {
    val invoices: StateFlow<List<InvoiceSummary>> = repository.observeHistory()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}
