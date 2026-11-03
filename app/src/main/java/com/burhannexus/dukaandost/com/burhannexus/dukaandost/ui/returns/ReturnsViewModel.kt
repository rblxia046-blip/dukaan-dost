package com.burhannexus.dukaandost.ui.returns

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.burhannexus.dukaandost.domain.model.InvoiceKind
import com.burhannexus.dukaandost.domain.model.InvoiceSummary
import com.burhannexus.dukaandost.domain.model.ReturnListItem
import com.burhannexus.dukaandost.domain.repository.PurchaseRepository
import com.burhannexus.dukaandost.domain.repository.SalesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ReturnsViewModel @Inject constructor(
    private val sales: SalesRepository,
    private val purchases: PurchaseRepository
) : ViewModel() {

    private val kind = MutableStateFlow(InvoiceKind.SALE)

    val returns: StateFlow<List<ReturnListItem>> = kind
        .flatMapLatest { if (it == InvoiceKind.SALE) sales.observeReturns() else purchases.observeReturns() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Invoices that a new return can be made against. */
    val invoices: StateFlow<List<InvoiceSummary>> = kind
        .flatMapLatest { if (it == InvoiceKind.SALE) sales.observeHistory() else purchases.observeHistory() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setKind(value: InvoiceKind) {
        kind.value = value
    }
}
