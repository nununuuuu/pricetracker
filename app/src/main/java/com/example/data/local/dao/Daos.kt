package com.example.data.local.dao

import androidx.room.*
import com.example.data.local.entity.*
import com.example.model.PlatformType
import kotlinx.coroutines.flow.Flow

@Dao
interface MonitorRuleDao {
    @Query("SELECT * FROM monitor_rules ORDER BY createdAt DESC")
    fun getAllRules(): Flow<List<MonitorRuleEntity>>

    @Query("SELECT * FROM monitor_rules WHERE isEnabled = 1")
    suspend fun getActiveRules(): List<MonitorRuleEntity>

    @Query("SELECT * FROM monitor_rules WHERE id = :id")
    suspend fun getRuleById(id: Long): MonitorRuleEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRule(rule: MonitorRuleEntity): Long

    @Update
    suspend fun updateRule(rule: MonitorRuleEntity)

    @Query("UPDATE monitor_rules SET isEnabled = :isEnabled WHERE id = :id")
    suspend fun setRuleEnabled(id: Long, isEnabled: Boolean)

    @Query("UPDATE monitor_rules SET lastScannedAt = :scannedAt, totalFoundCount = :foundCount, anomalyCount = :anomalyCount WHERE id = :id")
    suspend fun updateScanStats(id: Long, scannedAt: Long, foundCount: Int, anomalyCount: Int)

    @Delete
    suspend fun deleteRule(rule: MonitorRuleEntity)

    @Query("DELETE FROM monitor_rules WHERE id = :id")
    suspend fun deleteRuleById(id: Long)
}

@Dao
interface ProductDao {
    @Query("SELECT * FROM products ORDER BY lastDiscoveredAt DESC")
    fun getAllProducts(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE clusterId = :clusterId")
    suspend fun getProductsByCluster(clusterId: String): List<ProductEntity>

    @Query("SELECT * FROM products WHERE matchedMonitorId = :monitorId ORDER BY lastDiscoveredAt DESC")
    fun getProductsByMonitor(monitorId: Long): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE id = :id")
    suspend fun getProductById(id: String): ProductEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateProduct(product: ProductEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(products: List<ProductEntity>)

    @Query("SELECT COUNT(*) FROM products")
    fun getProductCount(): Flow<Int>

    @Query("DELETE FROM products WHERE id IN (:ids)")
    suspend fun deleteProductsByIds(ids: List<String>)

    @Query("SELECT id FROM products WHERE originalPlatformId IN (:originalIds)")
    suspend fun getProductIdsByOriginalPlatformIds(originalIds: List<String>): List<String>

    @Query("DELETE FROM products")
    suspend fun deleteAllProducts()
}

@Dao
interface PriceHistoryDao {
    @Query("SELECT * FROM price_history WHERE productId = :productId ORDER BY timestamp ASC")
    fun getHistoryForProduct(productId: String): Flow<List<PriceHistoryEntity>>

    @Query("SELECT * FROM price_history WHERE productId = :productId ORDER BY timestamp ASC")
    suspend fun getHistoryForProductSync(productId: String): List<PriceHistoryEntity>

    @Query("SELECT * FROM price_history WHERE timestamp >= :sinceTimestamp")
    suspend fun getHistorySince(sinceTimestamp: Long): List<PriceHistoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(record: PriceHistoryEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(records: List<PriceHistoryEntity>)

    @Query("DELETE FROM price_history WHERE productId IN (:productIds)")
    suspend fun deleteHistoryByProductIds(productIds: List<String>)

    @Query("DELETE FROM price_history")
    suspend fun deleteAllHistory()
}

@Dao
interface AnomalyReportDao {
    @Query("SELECT * FROM anomaly_reports ORDER BY dealScore DESC, createdAt DESC")
    fun getAllAnomalies(): Flow<List<AnomalyReportEntity>>

    @Query("SELECT * FROM anomaly_reports WHERE isFakeLowSuspected = 0 ORDER BY dealScore DESC, createdAt DESC")
    fun getVerifiedDeals(): Flow<List<AnomalyReportEntity>>

    @Query("SELECT * FROM anomaly_reports WHERE id = :id")
    suspend fun getAnomalyById(id: Long): AnomalyReportEntity?

    @Query("SELECT * FROM anomaly_reports WHERE productId = :productId ORDER BY createdAt DESC LIMIT 1")
    suspend fun getLatestAnomalyByProduct(productId: String): AnomalyReportEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnomaly(report: AnomalyReportEntity): Long

    @Update
    suspend fun updateAnomaly(report: AnomalyReportEntity)

    @Query("UPDATE anomaly_reports SET isStarred = :isStarred WHERE id = :id")
    suspend fun setStarred(id: Long, isStarred: Boolean)

    @Query("UPDATE anomaly_reports SET isNotified = 1, notifiedAt = :notifiedAt, lastNotifiedPrice = :price, lastNotifiedDealScore = :dealScore WHERE id = :id")
    suspend fun markNotified(id: Long, notifiedAt: Long, price: Double, dealScore: Int)

    @Query("SELECT COUNT(*) FROM anomaly_reports WHERE createdAt >= :sinceTimestamp")
    fun getTodayAnomalyCount(sinceTimestamp: Long): Flow<Int>

    @Query("DELETE FROM anomaly_reports WHERE id = :id")
    suspend fun deleteAnomalyById(id: Long)

    @Query("DELETE FROM anomaly_reports WHERE productId IN (:productIds)")
    suspend fun deleteAnomaliesByProductIds(productIds: List<String>)

    @Query("DELETE FROM anomaly_reports")
    suspend fun deleteAllAnomalies()
}

@Dao
interface NotificationLogDao {
    @Query("SELECT * FROM notification_logs ORDER BY notifiedAt DESC")
    fun getAllLogs(): Flow<List<NotificationLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: NotificationLogEntity): Long

    @Query("SELECT COUNT(*) FROM notification_logs WHERE notifiedAt >= :sinceTimestamp")
    fun getTodayNotificationCount(sinceTimestamp: Long): Flow<Int>
}

@Dao
interface PlatformStatusDao {
    @Query("SELECT * FROM platform_statuses")
    fun getAllStatuses(): Flow<List<PlatformStatusEntity>>

    @Query("SELECT * FROM platform_statuses WHERE platform = :platform")
    suspend fun getStatusByPlatform(platform: PlatformType): PlatformStatusEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateStatus(status: PlatformStatusEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(statuses: List<PlatformStatusEntity>)
}
