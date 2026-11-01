package com.burhannexus.dukaandost.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.burhannexus.dukaandost.data.local.CategoryWithCount
import com.burhannexus.dukaandost.data.local.ProductStats
import com.burhannexus.dukaandost.data.local.ProductWithCategory
import com.burhannexus.dukaandost.data.local.entity.CategoryEntity
import com.burhannexus.dukaandost.data.local.entity.ProductEntity
import com.burhannexus.dukaandost.data.local.entity.StockMovementEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Query(
        """SELECT c.id AS id, c.name AS name,
           (SELECT COUNT(*) FROM products p WHERE p.categoryId = c.id AND p.isActive = 1) AS productCount
           FROM categories c ORDER BY c.name COLLATE NOCASE"""
    )
    fun observeAll(): Flow<List<CategoryWithCount>>

    @Insert
    suspend fun insert(entity: CategoryEntity): Long

    @Query("UPDATE categories SET name = :name WHERE id = :id")
    suspend fun rename(id: Long, name: String): Int

    @Query("DELETE FROM categories WHERE id = :id")
    suspend fun delete(id: Long): Int
}

@Dao
interface ProductDao {
    @Query(
        """SELECT p.*, c.name AS categoryName FROM products p
           LEFT JOIN categories c ON c.id = p.categoryId
           WHERE p.isActive = 1
             AND (:categoryId IS NULL OR p.categoryId = :categoryId)
             AND (:query = ''
                  OR p.name LIKE '%' || :query || '%' ESCAPE '\'
                  OR p.sku LIKE '%' || :query || '%' ESCAPE '\'
                  OR p.barcode LIKE '%' || :query || '%' ESCAPE '\')"""
    )
    fun observeActive(categoryId: Long?, query: String): Flow<List<ProductWithCategory>>

    @Query(
        """SELECT p.*, c.name AS categoryName FROM products p
           LEFT JOIN categories c ON c.id = p.categoryId
           WHERE p.id = :id AND p.isActive = 1"""
    )
    fun observeById(id: Long): Flow<ProductWithCategory?>

    @Query(
        """SELECT p.*, c.name AS categoryName FROM products p
           LEFT JOIN categories c ON c.id = p.categoryId
           WHERE p.id = :id AND p.isActive = 1"""
    )
    suspend fun getById(id: Long): ProductWithCategory?

    @Query("SELECT * FROM products WHERE id = :id")
    suspend fun getEntity(id: Long): ProductEntity?

    @Insert
    suspend fun insert(entity: ProductEntity): Long

    @Update
    suspend fun update(entity: ProductEntity)

    @Query("UPDATE products SET isActive = 0, updatedAt = :now WHERE id = :id")
    suspend fun archive(id: Long, now: Long)

    @Query("SELECT COUNT(*) FROM products WHERE isActive = 1 AND id != :excludeId AND barcode = :barcode")
    suspend fun countBarcode(barcode: String, excludeId: Long): Int

    @Query("SELECT COUNT(*) FROM products WHERE isActive = 1 AND id != :excludeId AND sku = :sku")
    suspend fun countSku(sku: String, excludeId: Long): Int

    @Query(
        """SELECT COUNT(*) AS totalProducts,
           COALESCE(SUM(stockMilli), 0) AS totalStockMilli,
           COALESCE(SUM(CASE WHEN stockMilli > 0 AND stockMilli <= minStockMilli THEN 1 ELSE 0 END), 0) AS lowStock,
           COALESCE(SUM(CASE WHEN stockMilli <= 0 THEN 1 ELSE 0 END), 0) AS outOfStock
           FROM products WHERE isActive = 1"""
    )
    fun observeStats(): Flow<ProductStats>
}

@Dao
interface StockMovementDao {
    @Insert
    suspend fun insert(entity: StockMovementEntity): Long

    @Query("SELECT * FROM stock_movements WHERE productId = :productId ORDER BY createdAt DESC, id DESC")
    fun observeForProduct(productId: Long): Flow<List<StockMovementEntity>>
}
