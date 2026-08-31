package com.example.data.repository

import com.example.data.local.AppDatabase
import com.example.data.local.entity.*
import com.example.engine.*
import com.example.model.*
import com.example.platform.PlatformManager
import com.example.platform.PlatformResult
import com.example.platform.UrlParserHelper
import androidx.room.withTransaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class MonitoringRepository(
    private val database: AppDatabase,
    val platformManager: PlatformManager = PlatformManager()
) {
    /** Removes only identifiers created by the old demo seeder, never real listings. */
    suspend fun removeLegacyDemoData() = withContext(Dispatchers.IO) {
        val ids = listOf(
            "shopee_npw_001", "coupang_990p_001", "pchome_rtx5070_glitch",
            "costco_airpods_001", "momo_xm5_001", "books_kindle_001"
        )
        database.withTransaction {
            anomalyDao.deleteAnomaliesByProductIds(ids)
            historyDao.deleteHistoryByProductIds(ids)
            productDao.deleteProductsByIds(ids)
        }
    }
    private val monitorDao = database.monitorRuleDao()
    private val productDao = database.productDao()
    private val historyDao = database.priceHistoryDao()
    private val anomalyDao = database.anomalyReportDao()
    private val logDao = database.notificationLogDao()
    private val platformDao = database.platformStatusDao()

    val allMonitors: Flow<List<MonitorRule>> = monitorDao.getAllRules().map { list ->
        list.map { it.toDomainModel() }
    }

    val allAnomalies: Flow<List<AnomalyReport>> = anomalyDao.getAllAnomalies().map { list ->
        list.map { it.toDomainModel() }
    }

    val verifiedDeals: Flow<List<AnomalyReport>> = anomalyDao.getVerifiedDeals().map { list ->
        list.map { it.toDomainModel() }
    }

    val allProducts: Flow<List<ProductItem>> = productDao.getAllProducts().map { list ->
        list.map { it.toDomainModel() }
    }

    val notificationLogs: Flow<List<NotificationLog>> = logDao.getAllLogs().map { list ->
        list.map { it.toDomainModel() }
    }

    val totalProductCount: Flow<Int> = productDao.getProductCount()

    fun getTodayAnomalyCount(since: Long): Flow<Int> = anomalyDao.getTodayAnomalyCount(since)
    fun getTodayNotificationCount(since: Long): Flow<Int> = logDao.getTodayNotificationCount(since)

    fun getProductPriceHistory(productId: String): Flow<List<PriceHistoryRecord>> =
        historyDao.getHistoryForProduct(productId).map { list ->
            list.map { it.toDomainModel() }
        }

    suspend fun getProductPriceHistorySync(productId: String): List<PriceHistoryRecord> = withContext(Dispatchers.IO) {
        historyDao.getHistoryForProductSync(productId).map { it.toDomainModel() }
    }

    suspend fun getProductById(id: String): ProductItem? = withContext(Dispatchers.IO) {
        productDao.getProductById(id)?.toDomainModel()
    }

    suspend fun getAnomalyById(id: Long): AnomalyReport? = withContext(Dispatchers.IO) {
        anomalyDao.getAnomalyById(id)?.toDomainModel()
    }

    suspend fun toggleMonitor(id: Long, isEnabled: Boolean) = withContext(Dispatchers.IO) {
        monitorDao.setRuleEnabled(id, isEnabled)
    }

    suspend fun saveMonitor(rule: MonitorRule): Long = withContext(Dispatchers.IO) {
        monitorDao.insertRule(rule.toEntity())
    }

    /** Resolves a pasted URL before it is saved, so IDs never become monitor titles. */
    suspend fun resolveUrlRule(rule: MonitorRule): PlatformResult<MonitorRule> = withContext(Dispatchers.IO) {
        if (rule.trackMode != "URL") return@withContext PlatformResult.Success(rule, 0)
        val parsed = UrlParserHelper.parseProductUrl(rule.targetUrl)
        val platform = parsed.platform ?: return@withContext PlatformResult.Error("不支援的商品網址", false)
        val productId = parsed.productId ?: return@withContext PlatformResult.Error("此網址尚未能擷取商品編號", false)
        val adapter = platformManager.getAdapter(platform)
            ?: return@withContext PlatformResult.Error("${platform.displayName} 尚未支援網址解析", false)
        when (val result = adapter.getProductDetails(productId)) {
            is PlatformResult.Success -> {
                val isAutoFilled = rule.name.isBlank() || rule.name == parsed.suggestedName
                val isAutoKeyword = rule.searchKeyword.isBlank() || rule.searchKeyword == parsed.suggestedKeyword
                PlatformResult.Success(
                    rule.copy(
                        name = if (isAutoFilled) result.data.title else rule.name,
                        searchKeyword = if (isAutoKeyword) result.data.title else rule.searchKeyword
                    ),
                    result.latencyMs
                )
            }
            is PlatformResult.Error -> result
            is PlatformResult.RateLimited -> result
        }
    }

    suspend fun deleteMonitor(id: Long) = withContext(Dispatchers.IO) {
        monitorDao.deleteRuleById(id)
    }

    suspend fun toggleStarAnomaly(id: Long, isStarred: Boolean) = withContext(Dispatchers.IO) {
        anomalyDao.setStarred(id, isStarred)
    }

    suspend fun deleteAnomaly(id: Long) = withContext(Dispatchers.IO) {
        anomalyDao.deleteAnomalyById(id)
    }

    /**
     * Executes scan across all active monitors and all enabled platforms.
     */
    suspend fun executeScanForAllActiveMonitors(universalExclusions: List<String> = emptyList()): List<AnomalyReport> = withContext(Dispatchers.IO) {
        val activeRules = monitorDao.getActiveRules().map { it.toDomainModel() }
        val newAnomalies = mutableListOf<AnomalyReport>()

        for (rule in activeRules) {
            val anomalies = executeScanForMonitor(rule, universalExclusions)
            newAnomalies.addAll(anomalies)
        }
        newAnomalies
    }

    /**
     * Scans for a single monitor across configured platforms.
     * Prioritizes actual price anomaly detection (below historical low or below market reference).
     */
    suspend fun executeScanForMonitor(rule: MonitorRule, universalExclusions: List<String> = emptyList()): List<AnomalyReport> = withContext(Dispatchers.IO) {
        // The URL tells us the source platform, not the set of stores to compare.
        // Invalid/unknown URLs must not fall back to Shopee or create a monitor scan.
        if (rule.trackMode == "URL" && rule.targetUrl.isNotBlank() && !UrlParserHelper.parseProductUrl(rule.targetUrl).isValidUrl) {
            return@withContext emptyList()
        }
        val platformsToQuery = rule.enabledPlatforms
        val rawProducts = platformManager.searchAcrossPlatforms(rule.searchKeyword.ifBlank { rule.name }, platformsToQuery)
        val discoveredAnomalies = mutableListOf<AnomalyReport>()
        var foundCount = 0
        var anomalyCount = 0

        for (rawProd in rawProducts) {
            // 1. Keyword & Match Mode Filtering
            val matchRes = ProductMatcher.matchProduct(
                productTitle = rawProd.title,
                searchKeyword = rule.searchKeyword,
                matchMode = rule.matchMode,
                mustIncludeWords = rule.mustIncludeWords,
                anyIncludeWords = rule.anyIncludeWords,
                excludeKeywords = rule.excludeKeywords,
                ignoreCase = SecondaryKeywordRules.matchOptions(rule.mustIncludeWords).ignoreCase,
                ignoreWhitespace = SecondaryKeywordRules.matchOptions(rule.mustIncludeWords).ignoreWhitespace
            )

            if (!matchRes.isMatched) {
                continue
            }

            foundCount++

            // 2. False Price Check
            val falsePriceCheck = FalsePriceFilter.checkFalsePrice(
                title = rawProd.title,
                monitorExclusions = rule.excludeKeywords,
                universalExclusions = universalExclusions
            )

            val matchedProd = rawProd.copy(
                clusterId = matchRes.clusterId,
                normalizedTitle = matchRes.normalizedTitle,
                matchConfidence = matchRes.confidenceScore,
                matchedMonitorId = rule.id
            )

            // 3. Save / Update Product
            productDao.insertOrUpdateProduct(matchedProd.toEntity())

            // 4. Save Price History
            historyDao.insertHistory(
                PriceHistoryEntity(
                    productId = matchedProd.id,
                    platform = matchedProd.platform,
                    price = matchedProd.currentPrice,
                    timestamp = System.currentTimeMillis(),
                    note = "監控掃描更新"
                )
            )

            // 5. Gather Market Stats
            val history = historyDao.getHistoryForProductSync(matchedProd.id).map { it.toDomainModel() }
            val clusterProducts = productDao.getProductsByCluster(matchedProd.clusterId).map { it.toDomainModel() }
            val marketStats = MarketPriceCalculator.calculateMarketStats(
                clusterId = matchedProd.clusterId,
                currentProducts = clusterProducts.ifEmpty { listOf(matchedProd) },
                historyRecords = history
            )

            // 6. Evaluate Anomaly (Strict anomaly detection: price drop relative to history or market price)
            val evaluation = AnomalyDetectionEngine.evaluateProduct(
                product = matchedProd,
                rule = rule,
                marketStats = marketStats,
                isFakeLow = falsePriceCheck.isSuspected,
                fakeLowDetails = falsePriceCheck
            )

            if (evaluation.isAnomaly && !evaluation.isFakeLowSuspected) {
                anomalyCount++
                val reportEntity = AnomalyReportEntity(
                    productId = matchedProd.id,
                    monitorRuleId = rule.id,
                    monitorName = rule.name,
                    productTitle = matchedProd.title,
                    productUrl = matchedProd.url,
                    imageUrl = matchedProd.imageUrl,
                    platform = matchedProd.platform,
                    sellerName = matchedProd.sellerName,
                    currentPrice = matchedProd.currentPrice,
                    referencePrice = evaluation.referencePrice,
                    deviationPercent = evaluation.deviationPercent,
                    dealScore = evaluation.dealScore,
                    confidenceScore = evaluation.confidenceScore,
                    anomalyType = evaluation.anomalyType,
                    reasons = evaluation.reasons,
                    isFakeLowSuspected = false,
                    isNotified = false,
                    createdAt = System.currentTimeMillis()
                )

                val reportId = anomalyDao.insertAnomaly(reportEntity)
                val domainReport = reportEntity.copy(id = reportId).toDomainModel()

                // 7. Check Notification Deduplication
                val lastAnomaly = anomalyDao.getLatestAnomalyByProduct(matchedProd.id)?.toDomainModel()
                if (evaluation.shouldNotify && NotificationEngine.shouldSendNotification(domainReport, lastAnomaly)) {
                    val notifLog = NotificationEngine.buildNotificationLog(domainReport)
                    logDao.insertLog(notifLog.toEntity())
                    anomalyDao.markNotified(reportId, System.currentTimeMillis(), domainReport.currentPrice, domainReport.dealScore)
                }

                discoveredAnomalies.add(domainReport)
            }
        }

        // Update rule stats
        monitorDao.updateScanStats(rule.id, System.currentTimeMillis(), foundCount, anomalyCount)
        discoveredAnomalies
    }

    /**
     * Seeds initial rules, initial price history, and anomaly alerts for immediate rich out-of-the-box demo
     */
    suspend fun seedInitialDataIfEmpty() = withContext(Dispatchers.IO) {
        val existing = monitorDao.getActiveRules()
        if (existing.isNotEmpty()) return@withContext

        val now = System.currentTimeMillis()

        // Rule 1: SteelSeries Nova Pro Wireless
        val rule1 = MonitorRuleEntity(
            id = 1,
            name = "SteelSeries Nova Pro Wireless",
            searchKeyword = "Nova Pro Wireless",
            isEnabled = true,
            matchMode = MatchMode.CONTAINS,
            mustIncludeWords = listOf("Nova", "Pro", "Wireless"),
            anyIncludeWords = emptyList(),
            excludeKeywords = listOf("耳罩", "耳機架", "保護套", "線材", "空盒", "零件", "故障", "訂金", "租借"),
            enabledPlatforms = listOf(PlatformType.SHOPEE, PlatformType.COUPANG, PlatformType.PCHOME, PlatformType.MOMO),
            thresholdMode = PriceThresholdMode.BOTH_OR,
            maxFixedPrice = 5000.0,
            discountThresholdPercent = 30.0,
            minDealScore = 75,
            scanIntervalMinutes = 15,
            createdAt = now - 86400000 * 3,
            lastScannedAt = now - 600000,
            totalFoundCount = 8,
            anomalyCount = 2
        )

        // Rule 2: Samsung 990 Pro 2TB
        val rule2 = MonitorRuleEntity(
            id = 2,
            name = "Samsung 990 Pro 2TB SSD",
            searchKeyword = "Samsung 990 Pro 2TB",
            isEnabled = true,
            matchMode = MatchMode.CONTAINS,
            mustIncludeWords = listOf("990", "Pro", "2TB"),
            anyIncludeWords = emptyList(),
            excludeKeywords = listOf("散熱片", "外接盒", "傳輸線", "保護套", "模型", "空盒"),
            enabledPlatforms = listOf(PlatformType.SHOPEE, PlatformType.COUPANG, PlatformType.PCHOME, PlatformType.MOMO),
            thresholdMode = PriceThresholdMode.PERCENTAGE,
            maxFixedPrice = 3000.0,
            discountThresholdPercent = 35.0,
            minDealScore = 70,
            scanIntervalMinutes = 30,
            createdAt = now - 86400000 * 2,
            lastScannedAt = now - 1200000,
            totalFoundCount = 12,
            anomalyCount = 1
        )

        // Rule 3: RTX 5070 Ti
        val rule3 = MonitorRuleEntity(
            id = 3,
            name = "RTX 5070 Ti 顯示卡",
            searchKeyword = "RTX 5070 Ti",
            isEnabled = true,
            matchMode = MatchMode.CONTAINS,
            mustIncludeWords = listOf("5070", "Ti"),
            anyIncludeWords = emptyList(),
            excludeKeywords = listOf("顯卡支架", "風扇零件", "空盒", "散熱貼", "排線", "模型"),
            enabledPlatforms = listOf(PlatformType.SHOPEE, PlatformType.COUPANG, PlatformType.PCHOME, PlatformType.MOMO),
            thresholdMode = PriceThresholdMode.FIXED_PRICE,
            maxFixedPrice = 25000.0,
            discountThresholdPercent = 25.0,
            minDealScore = 80,
            scanIntervalMinutes = 10,
            createdAt = now - 86400000,
            lastScannedAt = now - 300000,
            totalFoundCount = 6,
            anomalyCount = 1
        )

        monitorDao.insertRule(rule1)
        monitorDao.insertRule(rule2)
        monitorDao.insertRule(rule3)

        // Seed Sample Products
        val p1 = ProductEntity(
            id = "shopee_npw_001",
            platform = PlatformType.SHOPEE,
            originalPlatformId = "npw_001",
            title = "【極速出清】SteelSeries 賽睿 Arctis Nova Pro Wireless 無線雙模旗艦電競耳機 (全新未拆 台灣公司貨)",
            normalizedTitle = "SteelSeries Arctis Nova Pro Wireless",
            url = PlatformType.SHOPEE.getSearchUrl("SteelSeries Arctis Nova Pro Wireless"),
            imageUrl = "https://images.unsplash.com/photo-1546435770-a3e426bf472b?w=400",
            sellerName = "電競瘋旗艦館 (蝦皮商城)",
            sellerRating = 4.9,
            currentPrice = 3299.0,
            originalPrice = 9490.0,
            offerPrice = 3299.0,
            status = "AVAILABLE",
            isAvailable = true,
            isNewItem = true,
            clusterId = "cluster_nova_pro_wireless",
            firstDiscoveredAt = now - 3600000 * 2,
            lastDiscoveredAt = now,
            matchConfidence = 0.96,
            matchedMonitorId = 1
        )

        val p2 = ProductEntity(
            id = "coupang_990p_001",
            platform = PlatformType.COUPANG,
            originalPlatformId = "990p_001",
            title = "SAMSUNG 三星 990 PRO 2TB PCIe 4.0 NVMe M.2 固態硬碟 (讀7450M/寫6900M)",
            normalizedTitle = "SAMSUNG 990 PRO 2TB PCIe 4.0 NVMe M.2 SSD",
            url = PlatformType.COUPANG.getSearchUrl("Samsung 990 PRO 2TB"),
            imageUrl = "https://images.unsplash.com/photo-1597872200969-2b65d56bd16b?w=400",
            sellerName = "酷澎台灣直送旗艦",
            sellerRating = 4.9,
            currentPrice = 1899.0,
            originalPrice = 5699.0,
            offerPrice = 1899.0,
            status = "AVAILABLE",
            isAvailable = true,
            isNewItem = true,
            clusterId = "cluster_samsung_990_pro_2tb",
            firstDiscoveredAt = now - 1800000,
            lastDiscoveredAt = now,
            matchConfidence = 0.98,
            matchedMonitorId = 2
        )

        val p3 = ProductEntity(
            id = "pchome_rtx5070_glitch",
            platform = PlatformType.PCHOME,
            originalPlatformId = "rtx5070_glitch",
            title = "【限時閃購標錯價疑雲】MSI 微星 RTX 5070 Ti Gaming X Slim 16G 顯卡",
            normalizedTitle = "MSI RTX 5070 Ti Gaming X Slim 16G",
            url = PlatformType.PCHOME.getSearchUrl("MSI RTX 5070 Ti Gaming X"),
            imageUrl = "https://images.unsplash.com/photo-1591799264318-7e6ef8ddb7ea?w=400",
            sellerName = "PChome 24h 微星專區",
            sellerRating = 4.8,
            currentPrice = 14999.0,
            originalPrice = 28900.0,
            offerPrice = 14999.0,
            status = "AVAILABLE",
            isAvailable = true,
            isNewItem = true,
            clusterId = "cluster_rtx_5070_ti",
            firstDiscoveredAt = now - 900000,
            lastDiscoveredAt = now,
            matchConfidence = 0.95,
            matchedMonitorId = 3
        )

        productDao.insertAll(listOf(p1, p2, p3))

        // Price History points (past 90 days)
        val histories = listOf(
            PriceHistoryEntity(productId = "shopee_npw_001", platform = PlatformType.SHOPEE, price = 9490.0, timestamp = now - 86400000 * 80),
            PriceHistoryEntity(productId = "shopee_npw_001", platform = PlatformType.SHOPEE, price = 9290.0, timestamp = now - 86400000 * 30),
            PriceHistoryEntity(productId = "shopee_npw_001", platform = PlatformType.SHOPEE, price = 8990.0, timestamp = now - 86400000 * 7),
            PriceHistoryEntity(productId = "shopee_npw_001", platform = PlatformType.SHOPEE, price = 3299.0, timestamp = now - 3600000 * 2),
            // Coupang SSD
            PriceHistoryEntity(productId = "coupang_990p_001", platform = PlatformType.COUPANG, price = 5699.0, timestamp = now - 86400000 * 60),
            PriceHistoryEntity(productId = "coupang_990p_001", platform = PlatformType.COUPANG, price = 5299.0, timestamp = now - 86400000 * 14),
            PriceHistoryEntity(productId = "coupang_990p_001", platform = PlatformType.COUPANG, price = 1899.0, timestamp = now - 1800000)
        )
        historyDao.insertAll(histories)

        // Seed Anomalies
        val a1 = AnomalyReportEntity(
            id = 1,
            productId = "shopee_npw_001",
            monitorRuleId = 1,
            monitorName = "SteelSeries Nova Pro Wireless",
            productTitle = "【極速出清】SteelSeries 賽睿 Arctis Nova Pro Wireless 無線雙模旗艦電競耳機 (全新未拆 台灣公司貨)",
            productUrl = PlatformType.SHOPEE.getSearchUrl("SteelSeries Arctis Nova Pro Wireless"),
            imageUrl = "https://images.unsplash.com/photo-1546435770-a3e426bf472b?w=400",
            platform = PlatformType.SHOPEE,
            sellerName = "電競瘋旗艦館 (蝦皮商城)",
            currentPrice = 3299.0,
            referencePrice = 9490.0,
            deviationPercent = -65.2,
            dealScore = 96,
            confidenceScore = 94,
            anomalyType = AnomalyType.SUSPECTED_GLITCH_PRICE,
            reasons = listOf(
                "較跨平台中位價 NT$9,490 偏離 -65.2%",
                "30 日常態價格 NT$9,290，創下新低",
                "電競瘋旗艦館 官方正品公司貨 (評分 4.9 ★)",
                "剛上架新商品，疑似標錯價或極端清倉"
            ),
            isFakeLowSuspected = false,
            isNotified = true,
            notifiedAt = now - 3600000 * 2,
            lastNotifiedPrice = 3299.0,
            lastNotifiedDealScore = 96,
            isStarred = true,
            createdAt = now - 3600000 * 2
        )

        val a2 = AnomalyReportEntity(
            id = 2,
            productId = "coupang_990p_001",
            monitorRuleId = 2,
            monitorName = "Samsung 990 Pro 2TB SSD",
            productTitle = "SAMSUNG 三星 990 PRO 2TB PCIe 4.0 NVMe M.2 固態硬碟 (讀7450M/寫6900M)",
            productUrl = PlatformType.COUPANG.getSearchUrl("Samsung 990 PRO 2TB"),
            imageUrl = "https://images.unsplash.com/photo-1597872200969-2b65d56bd16b?w=400",
            platform = PlatformType.COUPANG,
            sellerName = "酷澎台灣直送旗艦",
            currentPrice = 1899.0,
            referencePrice = 5699.0,
            deviationPercent = -66.7,
            dealScore = 95,
            confidenceScore = 96,
            anomalyType = AnomalyType.CLEARANCE_PRICE,
            reasons = listOf(
                "較跨平台中位價 NT$5,699 偏離 -66.7%",
                "酷澎直營火箭速配限時破盤",
                "具備 12 筆充足歷史樣本比對"
            ),
            isFakeLowSuspected = false,
            isNotified = true,
            notifiedAt = now - 1800000,
            lastNotifiedPrice = 1899.0,
            lastNotifiedDealScore = 95,
            createdAt = now - 1800000
        )

        val a3 = AnomalyReportEntity(
            id = 3,
            productId = "pchome_rtx5070_glitch",
            monitorRuleId = 3,
            monitorName = "RTX 5070 Ti 顯示卡",
            productTitle = "【限時閃購標錯價疑雲】MSI 微星 RTX 5070 Ti Gaming X Slim 16G 顯卡",
            productUrl = PlatformType.PCHOME.getSearchUrl("MSI RTX 5070 Ti Gaming X"),
            imageUrl = "https://images.unsplash.com/photo-1591799264318-7e6ef8ddb7ea?w=400",
            platform = PlatformType.PCHOME,
            sellerName = "PChome 24h 微星專區",
            currentPrice = 14999.0,
            referencePrice = 28900.0,
            deviationPercent = -48.1,
            dealScore = 92,
            confidenceScore = 90,
            anomalyType = AnomalyType.SUSPECTED_GLITCH_PRICE,
            reasons = listOf(
                "低於設定之最高預算 NT$25,000",
                "較市價 NT$28,900 下修 -48.1%",
                "PChome 24h 購物快速到貨"
            ),
            isFakeLowSuspected = false,
            isNotified = true,
            notifiedAt = now - 900000,
            lastNotifiedPrice = 14999.0,
            lastNotifiedDealScore = 92,
            createdAt = now - 900000
        )

        val a4 = AnomalyReportEntity(
            id = 4,
            productId = "costco_airpods_001",
            monitorRuleId = null,
            monitorName = "全網雷達自動偵測",
            productTitle = "Apple AirPods Pro 2 代 主動式降噪無線耳機 (Type-C MagSafe 充電盒)",
            productUrl = PlatformType.COSTCO.getSearchUrl("Apple AirPods Pro 2"),
            imageUrl = "https://images.unsplash.com/photo-1600294037681-c80b4cb5b434?w=400",
            platform = PlatformType.COSTCO,
            sellerName = "Costco 好市多線上旗艦",
            currentPrice = 4299.0,
            referencePrice = 7490.0,
            deviationPercent = -42.6,
            dealScore = 94,
            confidenceScore = 95,
            anomalyType = AnomalyType.CLEARANCE_PRICE,
            reasons = listOf(
                "全網雷達偵測：較市場公定價 NT$7,490 降幅 -42.6%",
                "Costco 線上破盤黑鑽特惠",
                "公司貨附原廠完整保固"
            ),
            isFakeLowSuspected = false,
            isNotified = false,
            createdAt = now - 7200000
        )

        val a5 = AnomalyReportEntity(
            id = 5,
            productId = "momo_xm5_001",
            monitorRuleId = null,
            monitorName = "全網雷達自動偵測",
            productTitle = "SONY 索尼 WH-1000XM5 旗艦無線降噪耳罩式耳機 (黑/銀/夜幕藍)",
            productUrl = PlatformType.MOMO.getSearchUrl("Sony WH 1000XM5"),
            imageUrl = "https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=400",
            platform = PlatformType.MOMO,
            sellerName = "momo 購物官方旗艦館",
            currentPrice = 6499.0,
            referencePrice = 11900.0,
            deviationPercent = -45.4,
            dealScore = 93,
            confidenceScore = 93,
            anomalyType = AnomalyType.CLEARANCE_PRICE,
            reasons = listOf(
                "全網雷達偵測：較市價 NT$11,900 下殺 -45.4%",
                "momo 品牌旗艦狂歡節閃購",
                "台灣索尼原廠正品"
            ),
            isFakeLowSuspected = false,
            isNotified = false,
            createdAt = now - 5400000
        )

        val a6 = AnomalyReportEntity(
            id = 6,
            productId = "books_kindle_001",
            monitorRuleId = null,
            monitorName = "全網雷達自動偵測",
            productTitle = "Amazon Kindle Paperwhite 5 (16GB) 6.8 吋防水平板電子書閱讀器",
            productUrl = PlatformType.BOOKS.getSearchUrl("Kindle Paperwhite 16GB"),
            imageUrl = "https://images.unsplash.com/photo-1544716278-ca5e3f4abd8c?w=400",
            platform = PlatformType.BOOKS,
            sellerName = "博客來 數位科技館",
            currentPrice = 2490.0,
            referencePrice = 4990.0,
            deviationPercent = -50.1,
            dealScore = 91,
            confidenceScore = 90,
            anomalyType = AnomalyType.SUSPECTED_GLITCH_PRICE,
            reasons = listOf(
                "全網雷達偵測：低於台灣代理中位市價 -50.1%",
                "博客來書香狂購週限時出清",
                "高解析度電子墨水屏"
            ),
            isFakeLowSuspected = false,
            isNotified = false,
            createdAt = now - 3600000
        )

        anomalyDao.insertAnomaly(a1)
        anomalyDao.insertAnomaly(a2)
        anomalyDao.insertAnomaly(a3)
        anomalyDao.insertAnomaly(a4)
        anomalyDao.insertAnomaly(a5)
        anomalyDao.insertAnomaly(a6)

        // Seed Notification Logs
        logDao.insertLog(
            NotificationLogEntity(
                reportId = 1,
                productTitle = "【極速出清】SteelSeries 賽睿 Arctis Nova Pro Wireless 無線雙模旗艦電競耳機",
                platform = PlatformType.SHOPEE,
                currentPrice = 3299.0,
                referencePrice = 9490.0,
                discountPercent = -65.2,
                dealScore = 96,
                anomalyType = AnomalyType.SUSPECTED_GLITCH_PRICE,
                purchaseUrl = PlatformType.SHOPEE.getSearchUrl("SteelSeries Arctis Nova Pro Wireless"),
                notifiedAt = now - 3600000 * 2
            )
        )
        logDao.insertLog(
            NotificationLogEntity(
                reportId = 2,
                productTitle = "SAMSUNG 三星 990 PRO 2TB PCIe 4.0 NVMe M.2 固態硬碟",
                platform = PlatformType.COUPANG,
                currentPrice = 1899.0,
                referencePrice = 5699.0,
                discountPercent = -66.7,
                dealScore = 95,
                anomalyType = AnomalyType.CLEARANCE_PRICE,
                purchaseUrl = PlatformType.COUPANG.getSearchUrl("Samsung 990 PRO 2TB"),
                notifiedAt = now - 1800000
            )
        )
    }
}

// Extension converters
fun MonitorRuleEntity.toDomainModel() = MonitorRule(
    id = id,
    name = name,
    searchKeyword = searchKeyword,
    isEnabled = isEnabled,
    matchMode = matchMode,
    mustIncludeWords = mustIncludeWords,
    anyIncludeWords = anyIncludeWords,
    excludeKeywords = excludeKeywords,
    enabledPlatforms = enabledPlatforms.ifEmpty { PlatformType.entries },
    thresholdMode = thresholdMode,
    maxFixedPrice = maxFixedPrice,
    discountThresholdPercent = discountThresholdPercent,
    minDealScore = minDealScore,
    scanIntervalMinutes = scanIntervalMinutes,
    createdAt = createdAt,
    lastScannedAt = lastScannedAt,
    totalFoundCount = totalFoundCount,
    anomalyCount = anomalyCount,
    targetUrl = targetUrl,
    trackMode = trackMode
)

fun MonitorRule.toEntity() = MonitorRuleEntity(
    id = id,
    name = name,
    searchKeyword = searchKeyword,
    isEnabled = isEnabled,
    matchMode = matchMode,
    mustIncludeWords = mustIncludeWords,
    anyIncludeWords = anyIncludeWords,
    excludeKeywords = excludeKeywords,
    enabledPlatforms = enabledPlatforms,
    thresholdMode = thresholdMode,
    maxFixedPrice = maxFixedPrice,
    discountThresholdPercent = discountThresholdPercent,
    minDealScore = minDealScore,
    scanIntervalMinutes = scanIntervalMinutes,
    createdAt = createdAt,
    lastScannedAt = lastScannedAt,
    totalFoundCount = totalFoundCount,
    anomalyCount = anomalyCount,
    targetUrl = targetUrl,
    trackMode = trackMode
)

fun ProductEntity.toDomainModel() = ProductItem(
    id = id,
    platform = platform,
    originalPlatformId = originalPlatformId,
    title = title,
    normalizedTitle = normalizedTitle,
    url = url,
    imageUrl = imageUrl,
    sellerName = sellerName,
    sellerRating = sellerRating,
    currentPrice = currentPrice,
    originalPrice = originalPrice,
    offerPrice = offerPrice,
    status = status,
    isAvailable = isAvailable,
    isNewItem = isNewItem,
    clusterId = clusterId,
    firstDiscoveredAt = firstDiscoveredAt,
    lastDiscoveredAt = lastDiscoveredAt,
    matchConfidence = matchConfidence,
    matchedMonitorId = matchedMonitorId
)

fun ProductItem.toEntity() = ProductEntity(
    id = id,
    platform = platform,
    originalPlatformId = originalPlatformId,
    title = title,
    normalizedTitle = normalizedTitle,
    url = url,
    imageUrl = imageUrl,
    sellerName = sellerName,
    sellerRating = sellerRating,
    currentPrice = currentPrice,
    originalPrice = originalPrice,
    offerPrice = offerPrice,
    status = status,
    isAvailable = isAvailable,
    isNewItem = isNewItem,
    clusterId = clusterId,
    firstDiscoveredAt = firstDiscoveredAt,
    lastDiscoveredAt = lastDiscoveredAt,
    matchConfidence = matchConfidence,
    matchedMonitorId = matchedMonitorId
)

fun PriceHistoryEntity.toDomainModel() = PriceHistoryRecord(
    id = id,
    productId = productId,
    platform = platform,
    price = price,
    timestamp = timestamp,
    note = note
)

fun AnomalyReportEntity.toDomainModel() = AnomalyReport(
    id = id,
    productId = productId,
    monitorRuleId = monitorRuleId,
    monitorName = monitorName,
    productTitle = productTitle,
    productUrl = productUrl,
    imageUrl = imageUrl,
    platform = platform,
    sellerName = sellerName,
    currentPrice = currentPrice,
    referencePrice = referencePrice,
    deviationPercent = deviationPercent,
    dealScore = dealScore,
    confidenceScore = confidenceScore,
    anomalyType = anomalyType,
    reasons = reasons,
    isFakeLowSuspected = isFakeLowSuspected,
    isNotified = isNotified,
    notifiedAt = notifiedAt,
    lastNotifiedPrice = lastNotifiedPrice,
    lastNotifiedDealScore = lastNotifiedDealScore,
    isStarred = isStarred,
    createdAt = createdAt
)

fun NotificationLogEntity.toDomainModel() = NotificationLog(
    id = id,
    reportId = reportId,
    productTitle = productTitle,
    platform = platform,
    currentPrice = currentPrice,
    referencePrice = referencePrice,
    discountPercent = discountPercent,
    dealScore = dealScore,
    anomalyType = anomalyType,
    purchaseUrl = purchaseUrl,
    notifiedAt = notifiedAt
)

fun NotificationLog.toEntity() = NotificationLogEntity(
    id = id,
    reportId = reportId,
    productTitle = productTitle,
    platform = platform,
    currentPrice = currentPrice,
    referencePrice = referencePrice,
    discountPercent = discountPercent,
    dealScore = dealScore,
    anomalyType = anomalyType,
    purchaseUrl = purchaseUrl,
    notifiedAt = notifiedAt
)
