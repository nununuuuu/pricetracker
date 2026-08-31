package com.example.ui.viewmodel

import com.example.engine.FalsePriceFilter
import com.example.model.*

enum class AppTab(val title: String, val iconName: String) {
    DASHBOARD("總覽儀表板", "Dashboard"),
    DEALS("撿漏特惠", "Deals"),
    MONITORS("監控管理", "Monitors"),
    HISTORY("歷史比價", "History"),
    SETTINGS("系統設定", "Settings")
}

enum class DealSortOption(val label: String) {
    DEAL_SCORE_DESC("撿漏分數最高 (Deal Score)"),
    DISCOUNT_DESC("偏離折數最多 (%)"),
    PRICE_ASC("價格由低到高 (NT$)"),
    NEWEST("最新發現時間")
}

enum class DealFilterLevel(val label: String) {
    ALL("全部特惠"),
    EXTREME_ONLY("🚨 極端異常 (90+)"),
    STRONG_ONLY("🔥 強烈低價 (75+)"),
    GOOD_ONLY("🟡 好價 (60+)"),
    STARRED("⭐ 我的收藏")
}

enum class ChartTimeRange(val label: String, val days: Int) {
    HOURS_24("24小時", 1),
    DAYS_7("7天", 7),
    DAYS_30("30天", 30),
    DAYS_90("90天", 90)
}

data class ScannerStatus(
    val isScanning: Boolean = false,
    val currentPlatform: String = "",
    val progressPercent: Float = 0f,
    val lastScanSummary: String = ""
)

data class DealHunterUiState(
    val currentTab: AppTab = AppTab.DASHBOARD,
    val monitors: List<MonitorRule> = emptyList(),
    val allDeals: List<AnomalyReport> = emptyList(),
    val filteredDeals: List<AnomalyReport> = emptyList(),
    val products: List<ProductItem> = emptyList(),
    val notificationLogs: List<NotificationLog> = emptyList(),
    val platformStatuses: Map<PlatformType, PlatformStatus> = emptyMap(),
    val totalScannedCount: Int = 0,
    val todayAnomalyCount: Int = 0,
    val todayNotificationCount: Int = 0,
    val scannerStatus: ScannerStatus = ScannerStatus(),
    val selectedFilterLevel: DealFilterLevel = DealFilterLevel.ALL,
    val selectedSortOption: DealSortOption = DealSortOption.DEAL_SCORE_DESC,
    val selectedPlatformFilter: PlatformType? = null,
    val searchQuery: String = "",
    val selectedProductDetail: ProductDetailState? = null,
    val editingMonitor: MonitorRule? = null,
    val isCreateEditSheetOpen: Boolean = false,
    val minNotificationScore: Int = 75,
    val autoScanIntervalMinutes: Int = 15,
    val webhookUrl: String = "https://discord.com/api/webhooks/example",
    val isDiscordEnabled: Boolean = true,
    val isWebPushEnabled: Boolean = true,
    val universalDictionary: List<String> = FalsePriceFilter.DEFAULT_UNIVERSAL_DICTIONARY,
    val bannerMessage: String? = null
)

data class ProductDetailState(
    val product: ProductItem,
    val anomalyReport: AnomalyReport?,
    val priceHistory: List<PriceHistoryRecord>,
    val marketStats: MarketStats,
    val crossPlatformPrices: List<ProductItem>,
    val selectedChartRange: ChartTimeRange = ChartTimeRange.DAYS_30
)
