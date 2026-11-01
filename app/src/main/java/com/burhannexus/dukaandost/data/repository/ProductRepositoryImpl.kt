package com.burhannexus.dukaandost.data.repository

import androidx.room.withTransaction
import com.burhannexus.dukaandost.core.Minor
import com.burhannexus.dukaandost.data.local.AppDatabase
import com.burhannexus.dukaandost.data.local.dao.ProductDao
import com.burhannexus.dukaandost.data.local.dao.StockMovementDao
import com.burhannexus.dukaandost.data.local.entity.ProductEntity
import com.burhannexus.dukaandost.data.local.entity.StockMovementEntity
import com.burhannexus.dukaandost.data.local.toDomain
import com.burhannexus.dukaandost.domain.model.Product
import com.burhannexus.dukaandost.domain.model.ProductInput
import com.burhannexus.dukaandost.domain.model.SaveResult
import com.burhannexus.dukaandost.domain.model.StockMovement
import com.burhannexus.dukaandost.domain.model.StockMovementType
import com.burhannexus.dukaandost.domain.repository.ProductRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProductRepositoryImpl @Inject constructor(
    private val db: AppDatabase,
    private val productDao: ProductDao,
    private val movementDao: StockMovementDao
) : ProductRepository {

    override fun observeProducts(categoryId: Long?, query: String): Flow<List<Product>> =
        productDao.observeActive(categoryId, escapeLike(query.trim()))
            .map { list -> list.map { it.toDomain() } }

    override fun observeProduct(id: Long): Flow<Product?> =
        productDao.observeById(id).map { it?.toDomain() }

    override suspend fun getProduct(id: Long): Product? = productDao.getById(id)?.toDomain()

    override fun observeMovements(productId: Long): Flow<List<StockMovement>> =
        movementDao.observeForProduct(productId).map { list -> list.map { it.toDomain() } }

    override suspend fun save(input: ProductInput): SaveResult = db.withTransaction {
        val sku = input.sku?.trim()?.takeIf { it.isNotEmpty() }
        val barcode = input.barcode?.trim()?.takeIf { it.isNotEmpty() }

        if (sku != null && productDao.countSku(sku, input.id) > 0) {
            SaveResult.DuplicateSku
        } else if (barcode != null && productDao.countBarcode(barcode, input.id) > 0) {
            SaveResult.DuplicateBarcode
        } else {
            val now = System.currentTimeMillis()
            val newStockMilli = Minor.quantityToMilli(input.stock)

            if (input.id == 0L) {
                val id = productDao.insert(
                    ProductEntity(
                        name = input.name.trim(),
                        categoryId = input.categoryId,
                        description = input.description.trim(),
                        sku = sku,
                        barcode = barcode,
                        purchasePriceMinor = Minor.moneyToMinor(input.purchasePrice),
                        salePriceMinor = Minor.moneyToMinor(input.salePrice),
                        stockMilli = newStockMilli,
                        minStockMilli = Minor.quantityToMilli(input.minStock),
                        unit = input.unit,
                        expiryEpochDay = input.expiryDate?.toEpochDay(),
                        createdAt = now,
                        updatedAt = now
                    )
                )
                if (newStockMilli != 0L) {
                    movementDao.insert(
                        StockMovementEntity(
                            productId = id,
                            type = StockMovementType.OPENING.name,
                            quantityChangeMilli = newStockMilli,
                            balanceAfterMilli = newStockMilli,
                            createdAt = now
                        )
                    )
                }
                SaveResult.Success(id)
            } else {
                val existing = productDao.getEntity(input.id)
                if (existing == null || !existing.isActive) {
                    SaveResult.NotFound
                } else {
                    productDao.update(
                        existing.copy(
                            name = input.name.trim(),
                            categoryId = input.categoryId,
                            description = input.description.trim(),
                            sku = sku,
                            barcode = barcode,
                            purchasePriceMinor = Minor.moneyToMinor(input.purchasePrice),
                            salePriceMinor = Minor.moneyToMinor(input.salePrice),
                            stockMilli = newStockMilli,
                            minStockMilli = Minor.quantityToMilli(input.minStock),
                            unit = input.unit,
                            expiryEpochDay = input.expiryDate?.toEpochDay(),
                            updatedAt = now
                        )
                    )
                    val delta = newStockMilli - existing.stockMilli
                    if (delta != 0L) {
                        movementDao.insert(
                            StockMovementEntity(
                                productId = existing.id,
                                type = StockMovementType.ADJUSTMENT.name,
                                quantityChangeMilli = delta,
                                balanceAfterMilli = newStockMilli,
                                createdAt = now
                            )
                        )
                    }
                    SaveResult.Success(existing.id)
                }
            }
        }
    }

    override suspend fun archive(id: Long) {
        productDao.archive(id, System.currentTimeMillis())
    }

    private fun escapeLike(text: String): String =
        text.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")
}
