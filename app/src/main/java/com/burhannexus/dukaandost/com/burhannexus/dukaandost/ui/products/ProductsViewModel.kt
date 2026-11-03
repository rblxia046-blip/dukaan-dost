package com.burhannexus.dukaandost.ui.products

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.burhannexus.dukaandost.domain.model.Category
import com.burhannexus.dukaandost.domain.model.CategoryResult
import com.burhannexus.dukaandost.domain.model.Product
import com.burhannexus.dukaandost.domain.repository.CategoryRepository
import com.burhannexus.dukaandost.domain.repository.ProductRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.math.BigDecimal
import javax.inject.Inject

enum class ProductSort { NAME, LOWEST_STOCK, HIGHEST_STOCK, SALE_PRICE_LOW, SALE_PRICE_HIGH }

data class ProductsUiState(
    val products: List<Product> = emptyList(),
    val categories: List<Category> = emptyList(),
    val query: String = "",
    val selectedCategoryId: Long? = null,
    val sort: ProductSort = ProductSort.NAME,
    /** Totals for the products currently shown. */
    val stockValue: BigDecimal = BigDecimal.ZERO,
    val expectedProfit: BigDecimal = BigDecimal.ZERO,
    val isLoading: Boolean = true
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ProductsViewModel @Inject constructor(
    private val productRepository: ProductRepository,
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    private val query = MutableStateFlow("")
    private val categoryFilter = MutableStateFlow<Long?>(null)
    private val sort = MutableStateFlow(ProductSort.NAME)

    private val products = combine(query, categoryFilter) { q, c -> q to c }
        .flatMapLatest { (q, c) -> productRepository.observeProducts(c, q) }

    val uiState: StateFlow<ProductsUiState> = combine(
        products,
        categoryRepository.observeAll(),
        query,
        categoryFilter,
        sort
    ) { list, categories, q, c, s ->
        val sorted = list.sortedWith(comparatorFor(s))
        ProductsUiState(
            products = sorted,
            categories = categories,
            query = q,
            selectedCategoryId = c,
            sort = s,
            stockValue = sorted.fold(BigDecimal.ZERO) { acc, p -> acc + p.stockValue },
            expectedProfit = sorted.fold(BigDecimal.ZERO) { acc, p -> acc + p.expectedProfit },
            isLoading = false
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProductsUiState())

    fun onQueryChange(value: String) {
        query.value = value
    }

    fun selectCategory(id: Long?) {
        categoryFilter.value = id
    }

    fun selectSort(value: ProductSort) {
        sort.value = value
    }

    fun addCategory(name: String, onResult: (CategoryResult) -> Unit) {
        viewModelScope.launch { onResult(categoryRepository.add(name)) }
    }

    fun renameCategory(id: Long, name: String, onResult: (CategoryResult) -> Unit) {
        viewModelScope.launch { onResult(categoryRepository.rename(id, name)) }
    }

    fun deleteCategory(id: Long) {
        viewModelScope.launch {
            if (categoryFilter.value == id) categoryFilter.value = null
            categoryRepository.delete(id)
        }
    }

    private fun comparatorFor(sort: ProductSort): Comparator<Product> {
        val byName = compareBy<Product, String>(String.CASE_INSENSITIVE_ORDER) { it.name }
        return when (sort) {
            ProductSort.NAME -> byName
            ProductSort.LOWEST_STOCK -> compareBy<Product> { it.stock }.then(byName)
            ProductSort.HIGHEST_STOCK -> compareByDescending<Product> { it.stock }.then(byName)
            ProductSort.SALE_PRICE_LOW -> compareBy<Product> { it.salePrice }.then(byName)
            ProductSort.SALE_PRICE_HIGH -> compareByDescending<Product> { it.salePrice }.then(byName)
        }
    }
}
