package com.burhannexus.dukaandost.domain.repository

import com.burhannexus.dukaandost.domain.model.Product
import com.burhannexus.dukaandost.domain.model.ProductInput
import com.burhannexus.dukaandost.domain.model.SaveResult
import com.burhannexus.dukaandost.domain.model.StockMovement
import kotlinx.coroutines.flow.Flow

interface ProductRepository {
    fun observeProducts(categoryId: Long?, query: String): Flow<List<Product>>
    fun observeProduct(id: Long): Flow<Product?>
    suspend fun getProduct(id: Long): Product?
    fun observeMovements(productId: Long): Flow<List<StockMovement>>

    /** Creates or updates a product and its stock history in one database transaction. */
    suspend fun save(input: ProductInput): SaveResult

    /**
     * "Delete": the product is archived (hidden from lists). Its stock history and any
     * future invoices that reference it are kept, so no business data is lost.
     */
    suspend fun archive(id: Long)
}
