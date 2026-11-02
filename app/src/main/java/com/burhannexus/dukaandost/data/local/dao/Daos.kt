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

@Dao
interface SupplierDao {
    @Query("SELECT * FROM suppliers WHERE isActive = 1 ORDER BY name COLLATE NOCASE")
    fun observeActive(): Flow<List<com.burhannexus.dukaandost.data.local.entity.SupplierEntity>>

    @Insert
    suspend fun insert(entity: com.burhannexus.dukaandost.data.local.entity.SupplierEntity): Long
}

@Dao
interface CustomerDao {
    @Query("SELECT * FROM customers WHERE isActive = 1 ORDER BY name COLLATE NOCASE")
    fun observeActive(): Flow<List<com.burhannexus.dukaandost.data.local.entity.CustomerEntity>>

    @Insert
    suspend fun insert(entity: com.burhannexus.dukaandost.data.local.entity.CustomerEntity): Long
}

@Dao
interface PurchaseDao {
    @Insert
    suspend fun insert(entity: com.burhannexus.dukaandost.data.local.entity.PurchaseEntity): Long

    @Insert
    suspend fun insertItem(entity: com.burhannexus.dukaandost.data.local.entity.PurchaseItemEntity): Long

    @Query("SELECT COALESCE(MAX(number), 0) FROM purchases")
    suspend fun maxNumber(): Long

    @Query("SELECT * FROM purchases WHERE id = :id")
    suspend fun getPurchase(id: Long): com.burhannexus.dukaandost.data.local.entity.PurchaseEntity?

    @Query("SELECT i.*, pr.name AS productName FROM purchase_items i JOIN products pr ON pr.id = i.productId WHERE i.purchaseId = :id ORDER BY i.id")
    suspend fun getItems(id: Long): List<com.burhannexus.dukaandost.data.local.PurchaseItemRow>

    @Query(
        """SELECT p.id AS id, p.number AS number, p.createdAt AS createdAt, p.totalMinor AS totalMinor,
           p.paidMinor AS paidMinor, s.name AS partyName,
           COALESCE((SELECT SUM(r.valueMinor) FROM purchase_returns r WHERE r.purchaseId = p.id), 0) AS returnedMinor,
           COALESCE((SELECT SUM(r.refundMinor) FROM purchase_returns r WHERE r.purchaseId = p.id), 0) AS refundedMinor
           FROM purchases p LEFT JOIN suppliers s ON s.id = p.supplierId
           ORDER BY p.createdAt DESC, p.id DESC LIMIT :limit"""
    )
    fun observeRecent(limit: Int): Flow<List<com.burhannexus.dukaandost.data.local.InvoiceRow>>

    @Query(
        """SELECT p.*, s.name AS partyName FROM purchases p
           LEFT JOIN suppliers s ON s.id = p.supplierId WHERE p.id = :id"""
    )
    fun observeById(id: Long): Flow<com.burhannexus.dukaandost.data.local.PurchaseWithSupplier?>

    @Query(
        """SELECT i.*, pr.name AS productName FROM purchase_items i
           JOIN products pr ON pr.id = i.productId WHERE i.purchaseId = :id ORDER BY i.id"""
    )
    fun observeItems(id: Long): Flow<List<com.burhannexus.dukaandost.data.local.PurchaseItemRow>>

    @Query("SELECT COALESCE(SUM(totalMinor - paidMinor), 0) FROM purchases")
    fun observePayable(): Flow<Long>

    @Query("SELECT COALESCE(SUM(paidMinor), 0) FROM purchases")
    fun observePaidTotal(): Flow<Long>
}

@Dao
interface SaleDao {
    @Insert
    suspend fun insert(entity: com.burhannexus.dukaandost.data.local.entity.SaleEntity): Long

    @Insert
    suspend fun insertItem(entity: com.burhannexus.dukaandost.data.local.entity.SaleItemEntity): Long

    @Query("SELECT COALESCE(MAX(number), 0) FROM sales")
    suspend fun maxNumber(): Long

    @Query("SELECT * FROM sales WHERE id = :id")
    suspend fun getSale(id: Long): com.burhannexus.dukaandost.data.local.entity.SaleEntity?

    @Query("SELECT * FROM sale_items WHERE saleId = :id ORDER BY id")
    suspend fun getItems(id: Long): List<com.burhannexus.dukaandost.data.local.entity.SaleItemEntity>

    @Query("SELECT * FROM sales WHERE clientToken = :token LIMIT 1")
    suspend fun findByToken(token: String): com.burhannexus.dukaandost.data.local.entity.SaleEntity?

    @Query(
        """SELECT s.id AS id, s.number AS number, s.createdAt AS createdAt, s.totalMinor AS totalMinor,
           s.paidMinor AS paidMinor, c.name AS partyName,
           COALESCE((SELECT SUM(r.valueMinor) FROM sale_returns r WHERE r.saleId = s.id), 0) AS returnedMinor,
           COALESCE((SELECT SUM(r.refundMinor) FROM sale_returns r WHERE r.saleId = s.id), 0) AS refundedMinor
           FROM sales s LEFT JOIN customers c ON c.id = s.customerId
           ORDER BY s.createdAt DESC, s.id DESC LIMIT :limit"""
    )
    fun observeRecent(limit: Int): Flow<List<com.burhannexus.dukaandost.data.local.InvoiceRow>>

    @Query(
        """SELECT s.*, c.name AS partyName FROM sales s
           LEFT JOIN customers c ON c.id = s.customerId WHERE s.id = :id"""
    )
    fun observeById(id: Long): Flow<com.burhannexus.dukaandost.data.local.SaleWithCustomer?>

    @Query("SELECT * FROM sale_items WHERE saleId = :id ORDER BY id")
    fun observeItems(id: Long): Flow<List<com.burhannexus.dukaandost.data.local.entity.SaleItemEntity>>

    @Query("SELECT COALESCE(SUM(totalMinor), 0) FROM sales WHERE createdAt >= :start AND createdAt < :end")
    fun observeSalesTotal(start: Long, end: Long): Flow<Long>

    /** Gross profit = sales total (after discounts) minus cost of goods sold. */
    @Query("SELECT COALESCE(SUM(totalMinor - cogsMinor), 0) FROM sales WHERE createdAt >= :start AND createdAt < :end")
    fun observeProfit(start: Long, end: Long): Flow<Long>

    @Query("SELECT COALESCE(SUM(totalMinor - paidMinor), 0) FROM sales")
    fun observeReceivable(): Flow<Long>

    @Query("SELECT COALESCE(SUM(paidMinor), 0) FROM sales")
    fun observePaidTotal(): Flow<Long>
}
