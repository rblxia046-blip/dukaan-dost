package com.burhannexus.dukaandost.data.repository

import android.database.sqlite.SQLiteConstraintException
import com.burhannexus.dukaandost.data.local.dao.CategoryDao
import com.burhannexus.dukaandost.data.local.entity.CategoryEntity
import com.burhannexus.dukaandost.data.local.toDomain
import com.burhannexus.dukaandost.domain.model.Category
import com.burhannexus.dukaandost.domain.model.CategoryResult
import com.burhannexus.dukaandost.domain.repository.CategoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CategoryRepositoryImpl @Inject constructor(
    private val dao: CategoryDao
) : CategoryRepository {

    override fun observeAll(): Flow<List<Category>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun add(name: String): CategoryResult {
        val clean = name.trim()
        validate(clean)?.let { return it }
        return try {
            dao.insert(CategoryEntity(name = clean))
            CategoryResult.SUCCESS
        } catch (e: SQLiteConstraintException) {
            CategoryResult.DUPLICATE_NAME
        }
    }

    override suspend fun rename(id: Long, name: String): CategoryResult {
        val clean = name.trim()
        validate(clean)?.let { return it }
        return try {
            if (dao.rename(id, clean) > 0) CategoryResult.SUCCESS else CategoryResult.NOT_FOUND
        } catch (e: SQLiteConstraintException) {
            CategoryResult.DUPLICATE_NAME
        }
    }

    override suspend fun delete(id: Long): CategoryResult =
        if (dao.delete(id) > 0) CategoryResult.SUCCESS else CategoryResult.NOT_FOUND

    private fun validate(name: String): CategoryResult? = when {
        name.isEmpty() -> CategoryResult.EMPTY_NAME
        name.length > MAX_NAME -> CategoryResult.TOO_LONG
        else -> null
    }

    private companion object {
        const val MAX_NAME = 50
    }
}
