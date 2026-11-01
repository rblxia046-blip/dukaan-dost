package com.burhannexus.dukaandost.ui.products

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.burhannexus.dukaandost.domain.model.Category
import com.burhannexus.dukaandost.domain.model.Product
import com.burhannexus.dukaandost.domain.model.SaveResult
import com.burhannexus.dukaandost.domain.repository.CategoryRepository
import com.burhannexus.dukaandost.domain.repository.ProductRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class LoadState { LOADING, READY, NOT_FOUND }

@HiltViewModel
class ProductEditViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val productRepository: ProductRepository,
    categoryRepository: CategoryRepository
) : ViewModel() {

    private val productId: Long = savedStateHandle.get<Long>("productId") ?: 0L
    val isNew: Boolean = productId == 0L

    private val _form = MutableStateFlow(ProductForm())
    val form: StateFlow<ProductForm> = _form.asStateFlow()

    private val _loadState = MutableStateFlow(if (isNew) LoadState.READY else LoadState.LOADING)
    val loadState: StateFlow<LoadState> = _loadState.asStateFlow()

    val categories: StateFlow<List<Category>> = categoryRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        if (!isNew) {
            viewModelScope.launch {
                val product = productRepository.getProduct(productId)
                if (product == null) {
                    _loadState.value = LoadState.NOT_FOUND
                } else {
                    _form.value = product.toForm()
                    _loadState.value = LoadState.READY
                }
            }
        }
    }

    /** Edits the form; the error of [field] (if any) is cleared. */
    fun edit(field: FormField? = null, block: ProductForm.() -> ProductForm) {
        _form.update { current ->
            val updated = current.block()
            if (field == null) updated else updated.copy(errors = updated.errors - field)
        }
    }

    fun save(onSaved: () -> Unit) {
        val current = _form.value
        if (current.saving) return // prevents duplicate submissions

        when (val outcome = ProductValidator.validate(productId, current)) {
            is ValidationOutcome.Invalid -> _form.update { it.copy(errors = outcome.errors) }
            is ValidationOutcome.Valid -> {
                _form.update { it.copy(saving = true, errors = emptyMap()) }
                viewModelScope.launch {
                    try {
                        when (val result = productRepository.save(outcome.input)) {
                            is SaveResult.Success -> onSaved()
                            SaveResult.DuplicateSku -> fail(FormField.SKU, FormError.DUPLICATE_SKU)
                            SaveResult.DuplicateBarcode -> fail(FormField.BARCODE, FormError.DUPLICATE_BARCODE)
                            SaveResult.NotFound -> {
                                _form.update { it.copy(saving = false) }
                                _loadState.value = LoadState.NOT_FOUND
                            }
                        }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        fail(FormField.GENERAL, FormError.SAVE_FAILED)
                    }
                }
            }
        }
    }

    private fun fail(field: FormField, error: FormError) {
        _form.update { it.copy(saving = false, errors = mapOf(field to error)) }
    }

    private fun Product.toForm() = ProductForm(
        name = name,
        categoryId = categoryId,
        description = description,
        sku = sku.orEmpty(),
        barcode = barcode.orEmpty(),
        purchasePrice = purchasePrice.toPlainString(),
        salePrice = salePrice.toPlainString(),
        stock = stock.stripTrailingZeros().toPlainString(),
        minStock = minStock.stripTrailingZeros().toPlainString(),
        unit = unit,
        expiryDate = expiryDate
    )
}
