package com.burhannexus.dukaandost.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.burhannexus.dukaandost.data.local.ReturnTotalsRow
import com.burhannexus.dukaandost.data.local.ReturnedItemRow
import com.burhannexus.dukaandost.data.local.entity.PurchaseReturnEntity
import com.burhannexus.dukaandost.data.local.entity.PurchaseReturnItemEntity
import com.burhannexus.dukaandost.data.local.entity.SaleReturnEntity
import com.burhannexus.dukaandost.data.local.entity.SaleReturnItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SaleReturnDao {
    @Insert
    suspend fun insert(entity: SaleReturnEntity): Long

    @Insert
    suspend fun insertItem(entity: SaleReturnItemEntity): Long

    @Query("SELECT COALESCE(MAX(number), 0) FROM sale_returns")
    suspend fun maxNumber(): Long

    @Query("SELECT * FROM sale_returns WHERE saleId = :saleId ORDER BY createdAt DESC, id DESC")
    fun observeForSale(saleId: Long): Flow<List<SaleReturnEntity>>

    @Query(
        """SELECT ri.saleItemId AS itemId, COALESCE(SUM(ri.quantityMilli), 0) AS quantityMilli,
           COALESCE(SUM(ri.valueMinor), 0) AS valueMinor
           FROM sale_return_items ri JOIN sale_returns r ON r.id = ri.returnId
           WHERE r.saleId = :saleId GROUP BY ri.saleItemId"""
    )
    fun observeReturnedByItem(saleId: Long): Flow<List<ReturnedItemRow>>

    @Query(
        """SELECT ri.saleItemId AS itemId, COALESCE(SUM(ri.quantityMilli), 0) AS quantityMilli,
           COALESCE(SUM(ri.valueMinor), 0) AS valueMinor
           FROM sale_return_items ri JOIN sale_returns r ON r.id = ri.returnId
           WHERE r.saleId = :saleId GROUP BY ri.saleItemId"""
    )
    suspend fun getReturnedByItem(saleId: Long): List<ReturnedItemRow>

    @Query(
        """SELECT COALESCE(SUM(valueMinor), 0) AS valueMinor, COALESCE(SUM(refundMinor), 0) AS refundMinor
           FROM sale_returns WHERE saleId = :saleId"""
    )
    suspend fun totalsForSale(saleId: Long): ReturnTotalsRow

    @Query("SELECT COALESCE(SUM(valueMinor), 0) FROM sale_returns WHERE createdAt >= :start AND createdAt < :end")
    fun observeValue(start: Long, end: Long): Flow<Long>

    @Query("SELECT COALESCE(SUM(valueMinor - cogsMinor), 0) FROM sale_returns WHERE createdAt >= :start AND createdAt < :end")
    fun observeProfitImpact(start: Long, end: Long): Flow<Long>

    @Query("SELECT COALESCE(SUM(valueMinor), 0) FROM sale_returns")
    fun observeValueAll(): Flow<Long>

    @Query("SELECT COALESCE(SUM(refundMinor), 0) FROM sale_returns")
    fun observeRefundAll(): Flow<Long>
}

@Dao
interface PurchaseReturnDao {
    @Insert
    suspend fun insert(entity: PurchaseReturnEntity): Long

    @Insert
    suspend fun insertItem(entity: PurchaseReturnItemEntity): Long

    @Query("SELECT COALESCE(MAX(number), 0) FROM purchase_returns")
    suspend fun maxNumber(): Long

    @Query("SELECT * FROM purchase_returns WHERE purchaseId = :purchaseId ORDER BY createdAt DESC, id DESC")
    fun observeForPurchase(purchaseId: Long): Flow<List<PurchaseReturnEntity>>

    @Query(
        """SELECT ri.purchaseItemId AS itemId, COALESCE(SUM(ri.quantityMilli), 0) AS quantityMilli,
           COALESCE(SUM(ri.valueMinor), 0) AS valueMinor
           FROM purchase_return_items ri JOIN purchase_returns r ON r.id = ri.returnId
           WHERE r.purchaseId = :purchaseId GROUP BY ri.purchaseItemId"""
    )
    fun observeReturnedByItem(purchaseId: Long): Flow<List<ReturnedItemRow>>

    @Query(
        """SELECT ri.purchaseItemId AS itemId, COALESCE(SUM(ri.quantityMilli), 0) AS quantityMilli,
           COALESCE(SUM(ri.valueMinor), 0) AS valueMinor
           FROM purchase_return_items ri JOIN purchase_returns r ON r.id = ri.returnId
           WHERE r.purchaseId = :purchaseId GROUP BY ri.purchaseItemId"""
    )
    suspend fun getReturnedByItem(purchaseId: Long): List<ReturnedItemRow>

    @Query(
        """SELECT COALESCE(SUM(valueMinor), 0) AS valueMinor, COALESCE(SUM(refundMinor), 0) AS refundMinor
           FROM purchase_returns WHERE purchaseId = :purchaseId"""
    )
    suspend fun totalsForPurchase(purchaseId: Long): ReturnTotalsRow

    @Query("SELECT COALESCE(SUM(valueMinor), 0) FROM purchase_returns")
    fun observeValueAll(): Flow<Long>

    @Query("SELECT COALESCE(SUM(refundMinor), 0) FROM purchase_returns")
    fun observeRefundAll(): Flow<Long>
}
