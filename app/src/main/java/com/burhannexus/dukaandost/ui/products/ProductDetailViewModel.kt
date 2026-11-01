package com.burhannexus.dukaandost.ui.products

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.burhannexus.dukaandost.domain.model.Product
import com.burhannexus.dukaandost.domain.model.StockMovement
import com.burhannexus.dukaandost.domain.repository.ProductRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface DetailState {
    object Loading : DetailState
    object Missing : DetailState
    data class Found(val product: Product) : DetailState
}

@HiltViewModel
class ProductDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: ProductRepository
) : ViewModel() {

    private val productId: Long = savedStateHandle.get<Long>("productId") ?: 0L

    val state: StateFlow<DetailState> = repository.observeProduct(productId)
        .map<Product?, DetailState> { if (it == null) DetailState.Missing else DetailState.Found(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DetailState.Loading)

    val movements: StateFlow<List<StockMovement>> = repository.observeMovements(productId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Archives the product. The screen closes itself when the product disappears. */
    fun archive() {
        viewModelScope.launch { repository.archive(productId) }
    }
}
