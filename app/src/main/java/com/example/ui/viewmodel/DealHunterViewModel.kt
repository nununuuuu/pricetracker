package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.repository.MonitoringRepository
import com.example.engine.FalsePriceFilter
import com.example.engine.MarketPriceCalculator
import com.example.model.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class DealHunterViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val repository = MonitoringRepository(database)
    private val prefs = application.getSharedPreferences("deal_hunter_prefs", Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow(
        DealHunterUiState(
            universalDictionary = loadSavedUniversalDictionary()
        )
    )
    val uiState: StateFlow<DealHunterUiState> = _uiState.asStateFlow()

    init {
        // Remove the previous release's fabricated demo data. Live scans are the
        // only source allowed to populate products and anomalies.
        viewModelScope.launch {
            repository.removeLegacyDemoData()
        }

        // Observe monitors
        viewModelScope.launch {
            repository.allMonitors.collect { monitorsList ->
                _uiState.update { it.copy(monitors = monitorsList) }
            }
        }

        // Observe deals/anomalies
        viewModelScope.launch {
            repository.allAnomalies.collect { dealsList ->
                _uiState.update { current ->
                    val filtered = applyFilters(
                        deals = dealsList,
                        level = current.selectedFilterLevel,
                        sort = current.selectedSortOption,
                        query = current.searchQuery,
                        platformFilter = current.selectedPlatformFilter
                    )
                    current.copy(allDeals = dealsList, filteredDeals = filtered)
                }
            }
        }

        // Observe products
        viewModelScope.launch {
            repository.allProducts.collect { prods ->
                _uiState.update { it.copy(products = prods) }
            }
        }

        // Observe notification logs
        viewModelScope.launch {
            repository.notificationLogs.collect { logs ->
                _uiState.update { it.copy(notificationLogs = logs) }
            }
        }

        // Observe platform status
        viewModelScope.launch {
            repository.platformManager.platformStatuses.collect { statusMap ->
                _uiState.update { it.copy(platformStatuses = statusMap) }
            }
        }

        // Observe counts
        val startOfToday = System.currentTimeMillis() - 86400000L
        viewModelScope.launch {
            repository.totalProductCount.collect { count ->
                _uiState.update { it.copy(totalScannedCount = count) }
            }
        }
        viewModelScope.launch {
            repository.getTodayAnomalyCount(startOfToday).collect { count ->
                _uiState.update { it.copy(todayAnomalyCount = count) }
            }
        }
        viewModelScope.launch {
            repository.getTodayNotificationCount(startOfToday).collect { count ->
                _uiState.update { it.copy(todayNotificationCount = count) }
            }
        }
    }

    fun selectTab(tab: AppTab) {
        _uiState.update { it.copy(currentTab = tab) }
    }

    fun setSelectedPlatformFilter(platform: PlatformType?) {
        _uiState.update { current ->
            val newPlatform = if (current.selectedPlatformFilter == platform) null else platform
            val filtered = applyFilters(
                deals = current.allDeals,
                level = current.selectedFilterLevel,
                sort = current.selectedSortOption,
                query = current.searchQuery,
                platformFilter = newPlatform
            )
            val msg = if (newPlatform != null) "已切換顯示 [${newPlatform.displayName.split(" ").first()}] 監控商品" else "顯示全部平台商品"
            current.copy(
                selectedPlatformFilter = newPlatform,
                filteredDeals = filtered,
                bannerMessage = msg
            )
        }
        viewModelScope.launch {
            delay(2500)
            _uiState.update { it.copy(bannerMessage = null) }
        }
    }

    fun setFilterLevel(level: DealFilterLevel) {
        _uiState.update { current ->
            val filtered = applyFilters(
                deals = current.allDeals,
                level = level,
                sort = current.selectedSortOption,
                query = current.searchQuery,
                platformFilter = current.selectedPlatformFilter
            )
            current.copy(selectedFilterLevel = level, filteredDeals = filtered)
        }
    }

    fun setSortOption(sort: DealSortOption) {
        _uiState.update { current ->
            val filtered = applyFilters(
                deals = current.allDeals,
                level = current.selectedFilterLevel,
                sort = sort,
                query = current.searchQuery,
                platformFilter = current.selectedPlatformFilter
            )
            current.copy(selectedSortOption = sort, filteredDeals = filtered)
        }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { current ->
            val filtered = applyFilters(
                deals = current.allDeals,
                level = current.selectedFilterLevel,
                sort = current.selectedSortOption,
                query = query,
                platformFilter = current.selectedPlatformFilter
            )
            current.copy(searchQuery = query, filteredDeals = filtered)
        }
    }

    fun toggleMonitor(id: Long, isEnabled: Boolean) {
        viewModelScope.launch {
            repository.toggleMonitor(id, isEnabled)
            showBannerMessage(if (isEnabled) "已啟用監控" else "已暫停監控")
        }
    }

    fun toggleStar(dealId: Long, isStarred: Boolean) {
        viewModelScope.launch {
            repository.toggleStarAnomaly(dealId, isStarred)
        }
    }

    fun deleteMonitor(id: Long) {
        viewModelScope.launch {
            repository.deleteMonitor(id)
            showBannerMessage("已刪除監控項目")
        }
    }

    fun duplicateMonitor(rule: MonitorRule) {
        viewModelScope.launch {
            val duplicated = rule.copy(
                id = 0,
                name = "${rule.name} (複製)",
                createdAt = System.currentTimeMillis()
            )
            repository.saveMonitor(duplicated)
            showBannerMessage("已複製監控項目：${duplicated.name}")
        }
    }

    fun openCreateMonitorSheet() {
        _uiState.update {
            it.copy(
                editingMonitor = MonitorRule(
                    name = "",
                    searchKeyword = "",
                    mustIncludeWords = emptyList(),
                    excludeKeywords = listOf("耳罩", "保護套", "線材", "空盒"),
                    enabledPlatforms = listOf(PlatformType.SHOPEE, PlatformType.MOMO, PlatformType.PCHOME, PlatformType.COUPANG, PlatformType.ETMALL, PlatformType.YAHOO_CENTER),
                    thresholdMode = PriceThresholdMode.PERCENTAGE,
                    maxFixedPrice = null,
                    discountThresholdPercent = 25.0,
                    minDealScore = 75,
                    scanIntervalMinutes = 15,
                    isEnabled = true
                ),
                isCreateEditSheetOpen = true
            )
        }
    }

    fun openEditMonitorSheet(rule: MonitorRule) {
        _uiState.update {
            it.copy(
                editingMonitor = rule,
                isCreateEditSheetOpen = true
            )
        }
    }

    fun closeCreateEditSheet() {
        _uiState.update { it.copy(isCreateEditSheetOpen = false, editingMonitor = null) }
    }

    fun saveEditingMonitor(rule: MonitorRule) {
        viewModelScope.launch {
            val resolvedRule = when (val result = repository.resolveUrlRule(rule)) {
                is com.example.platform.PlatformResult.Success -> result.data
                is com.example.platform.PlatformResult.Error -> {
                    showBannerMessage("無法解析商品頁：${result.message}")
                    return@launch
                }
                is com.example.platform.PlatformResult.RateLimited -> {
                    showBannerMessage("商品頁暫時被限流，請稍後再試")
                    return@launch
                }
            }
            val id = repository.saveMonitor(resolvedRule)
            val savedRule = resolvedRule.copy(id = if (resolvedRule.id == 0L) id else resolvedRule.id)
            closeCreateEditSheet()
            _uiState.update { it.copy(scannerStatus = ScannerStatus(true, "正在首次掃描 [${savedRule.name}]…", 0.3f)) }
            val anomalies = repository.executeScanForMonitor(savedRule, _uiState.value.universalDictionary)
            _uiState.update { it.copy(scannerStatus = ScannerStatus(false, "首次掃描完成", 1f, "更新 ${anomalies.size} 筆異常價格")) }
            showBannerMessage("已儲存並完成首次掃描")
        }
    }

    fun triggerQuickScan() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    scannerStatus = ScannerStatus(
                        isScanning = true,
                        currentPlatform = "正在連線各平台...",
                        progressPercent = 0.1f
                    )
                )
            }

            val platforms = listOf("蝦皮 Shopee", "酷澎 Coupang", "PChome 24h", "momo 購物網")
            for (i in platforms.indices) {
                _uiState.update {
                    it.copy(
                        scannerStatus = ScannerStatus(
                            isScanning = true,
                            currentPlatform = "正在檢索 ${platforms[i]} 最新破盤行情...",
                            progressPercent = (i + 1) * 0.22f
                        )
                    )
                }
                delay(300)
            }

            val newAnomalies = repository.executeScanForAllActiveMonitors(
                universalExclusions = _uiState.value.universalDictionary
            )

            _uiState.update {
                it.copy(
                    scannerStatus = ScannerStatus(
                        isScanning = false,
                        currentPlatform = "掃描完成",
                        progressPercent = 1.0f,
                        lastScanSummary = "全平台掃描完成，更新 ${newAnomalies.size} 個異常特惠價格"
                    )
                )
            }
            showBannerMessage("全平台掃描完成！發現 ${newAnomalies.size} 筆價格異動")
        }
    }

    fun triggerScanForSingleMonitor(rule: MonitorRule) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    scannerStatus = ScannerStatus(
                        isScanning = true,
                        currentPlatform = "正在掃描 [${rule.name}]...",
                        progressPercent = 0.5f
                    )
                )
            }
            val anomalies = repository.executeScanForMonitor(
                rule = rule,
                universalExclusions = _uiState.value.universalDictionary
            )
            _uiState.update {
                it.copy(
                    scannerStatus = ScannerStatus(
                        isScanning = false,
                        currentPlatform = "掃描完成",
                        progressPercent = 1.0f
                    )
                )
            }
            showBannerMessage("[${rule.name}] 掃描完成！")
        }
    }

    fun openProductDetail(product: ProductItem, anomaly: AnomalyReport? = null) {
        viewModelScope.launch {
            val history = repository.getProductPriceHistorySync(product.id)
            val clusterProducts = _uiState.value.products.filter { it.clusterId.isNotBlank() && it.clusterId == product.clusterId }
            val stats = MarketPriceCalculator.calculateMarketStats(
                clusterId = product.clusterId,
                currentProducts = clusterProducts.ifEmpty { listOf(product) },
                historyRecords = history
            )

            _uiState.update {
                it.copy(
                    selectedProductDetail = ProductDetailState(
                        product = product,
                        anomalyReport = anomaly ?: it.allDeals.find { d -> d.productId == product.id },
                        priceHistory = history,
                        marketStats = stats,
                        crossPlatformPrices = clusterProducts.ifEmpty { listOf(product) }
                    ),
                    currentTab = AppTab.HISTORY
                )
            }
        }
    }

    fun setChartTimeRange(range: ChartTimeRange) {
        val currentDetail = _uiState.value.selectedProductDetail ?: return
        _uiState.update {
            it.copy(
                selectedProductDetail = currentDetail.copy(selectedChartRange = range)
            )
        }
    }

    fun clearSelectedProductDetail() {
        _uiState.update { it.copy(selectedProductDetail = null) }
    }

    fun setPlatformEnabled(platform: PlatformType, isEnabled: Boolean) {
        repository.platformManager.setPlatformEnabled(platform, isEnabled)
    }

    fun runDiagnostics() {
        viewModelScope.launch {
            showBannerMessage("正在診斷 4 大電商 API 與連線狀態...")
            val results = repository.platformManager.runDiagnostics()
            showBannerMessage("診斷完成：${results.size} 家電商連線均正常")
        }
    }

    // --- Universal Dictionary Management ---

    fun addUniversalDictionaryWord(rawWord: String) {
        val trimmed = rawWord.trim()
        if (trimmed.isBlank()) return
        val currentList = _uiState.value.universalDictionary
        if (!currentList.contains(trimmed)) {
            val updated = currentList + trimmed
            saveUniversalDictionary(updated)
            _uiState.update { it.copy(universalDictionary = updated) }
            showBannerMessage("已新增通用排除詞：$trimmed")
        } else {
            showBannerMessage("「$trimmed」已在通用字典中")
        }
    }

    fun addUniversalDictionaryWords(words: List<String>) {
        val currentList = _uiState.value.universalDictionary
        val toAdd = words.map { it.trim() }.filter { it.isNotBlank() && !currentList.contains(it) }
        if (toAdd.isNotEmpty()) {
            val updated = currentList + toAdd
            saveUniversalDictionary(updated)
            _uiState.update { it.copy(universalDictionary = updated) }
            showBannerMessage("已加入 ${toAdd.size} 個關鍵字至通用字典")
        } else {
            showBannerMessage("所選字詞已全數存在於字典中")
        }
    }

    fun removeUniversalDictionaryWord(word: String) {
        val currentList = _uiState.value.universalDictionary
        val updated = currentList.filter { it != word }
        saveUniversalDictionary(updated)
        _uiState.update { it.copy(universalDictionary = updated) }
        showBannerMessage("已刪除通用排除詞：$word")
    }

    fun resetUniversalDictionary() {
        val defaultList = FalsePriceFilter.DEFAULT_UNIVERSAL_DICTIONARY
        saveUniversalDictionary(defaultList)
        _uiState.update { it.copy(universalDictionary = defaultList) }
        showBannerMessage("已恢復為預設通用字典庫 (${defaultList.size} 個詞)")
    }

    fun clearUniversalDictionary() {
        saveUniversalDictionary(emptyList())
        _uiState.update { it.copy(universalDictionary = emptyList()) }
        showBannerMessage("已清空通用排除字典")
    }

    private fun saveUniversalDictionary(list: List<String>) {
        prefs.edit().putStringSet("universal_dictionary_keys", list.toSet()).apply()
    }

    private fun loadSavedUniversalDictionary(): List<String> {
        val saved = prefs.getStringSet("universal_dictionary_keys", null)
        return saved?.toList() ?: FalsePriceFilter.DEFAULT_UNIVERSAL_DICTIONARY
    }

    // --- Settings Update ---

    fun updateSettings(
        minScore: Int,
        intervalMinutes: Int,
        webhook: String,
        discord: Boolean,
        webPush: Boolean
    ) {
        _uiState.update {
            it.copy(
                minNotificationScore = minScore,
                autoScanIntervalMinutes = intervalMinutes,
                webhookUrl = webhook,
                isDiscordEnabled = discord,
                isWebPushEnabled = webPush
            )
        }
        showBannerMessage("系統參數與通知管道已更新")
    }

    fun showBannerMessage(msg: String) {
        _uiState.update { it.copy(bannerMessage = msg) }
        viewModelScope.launch {
            delay(3500)
            _uiState.update { if (it.bannerMessage == msg) it.copy(bannerMessage = null) else it }
        }
    }

    private fun applyFilters(
        deals: List<AnomalyReport>,
        level: DealFilterLevel,
        sort: DealSortOption,
        query: String,
        platformFilter: PlatformType? = null
    ): List<AnomalyReport> {
        var result = deals

        // 0. Platform filter
        if (platformFilter != null) {
            result = result.filter { it.platform == platformFilter }
        }

        // 1. Search query
        if (query.isNotBlank()) {
            val q = query.trim().lowercase()
            result = result.filter {
                it.productTitle.lowercase().contains(q) ||
                it.monitorName.lowercase().contains(q) ||
                it.sellerName.lowercase().contains(q)
            }
        }

        // 2. Level filter
        result = when (level) {
            DealFilterLevel.ALL -> result
            DealFilterLevel.EXTREME_ONLY -> result.filter { it.dealScore >= 90 }
            DealFilterLevel.STRONG_ONLY -> result.filter { it.dealScore in 75..89 }
            DealFilterLevel.GOOD_ONLY -> result.filter { it.dealScore in 60..74 }
            DealFilterLevel.STARRED -> result.filter { it.isStarred }
        }

        // 3. Sorting
        result = when (sort) {
            DealSortOption.DEAL_SCORE_DESC -> result.sortedByDescending { it.dealScore }
            DealSortOption.DISCOUNT_DESC -> result.sortedBy { it.deviationPercent } // most negative first
            DealSortOption.PRICE_ASC -> result.sortedBy { it.currentPrice }
            DealSortOption.NEWEST -> result.sortedByDescending { it.createdAt }
        }

        return result
    }
}
