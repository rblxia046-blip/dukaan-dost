package com.burhannexus.dukaandost.domain.repository

import com.burhannexus.dukaandost.domain.model.Category
import com.burhannexus.dukaandost.domain.model.CategoryResult
import kotlinx.coroutines.flow.Flow

interface CategoryRepository {
    fun observeAll(): Flow<List<Category>>
    suspend fun add(name: String): CategoryResult
    suspend fun rename(id: Long, name: String): CategoryResult

    /** Products in the category are kept and become uncategorised. */
    suspend fun delete(id: Long): CategoryResult
}
