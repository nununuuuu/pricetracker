package com.example.model

enum class SecondaryKeywordOperator { AND, OR }

data class SecondaryKeywordRule(
    val keyword: String,
    val operator: SecondaryKeywordOperator
)

data class KeywordMatchOptions(
    val ignoreCase: Boolean = true,
    val ignoreWhitespace: Boolean = false
)

/**
 * Secondary rules are stored in the existing Room string-list column to keep
 * existing monitors compatible. Old must words become AND; old any words become OR.
 */
object SecondaryKeywordRules {
    private const val AND_PREFIX = "__AND__:"
    private const val OR_PREFIX = "__OR__:"
    private const val OPTIONS_PREFIX = "__MATCH_OPTIONS__:"

    fun decode(mustWords: List<String>, anyWords: List<String>): List<SecondaryKeywordRule> =
        mustWords.mapNotNull { stored ->
            when {
                stored.startsWith(OPTIONS_PREFIX) -> null
                stored.startsWith(AND_PREFIX) -> SecondaryKeywordRule(stored.removePrefix(AND_PREFIX), SecondaryKeywordOperator.AND)
                stored.isNotBlank() -> SecondaryKeywordRule(stored, SecondaryKeywordOperator.AND)
                else -> null
            }
        } + anyWords.filter { it.isNotBlank() }.map { SecondaryKeywordRule(it, SecondaryKeywordOperator.OR) }

    fun matchOptions(mustWords: List<String>) = mustWords.firstOrNull { it.startsWith(OPTIONS_PREFIX) }
        ?.removePrefix(OPTIONS_PREFIX)?.split(":")?.let { KeywordMatchOptions(it.getOrNull(0) == "1", it.getOrNull(1) == "1") }
        ?: KeywordMatchOptions()

    fun encode(rules: List<SecondaryKeywordRule>, options: KeywordMatchOptions): List<String> =
        listOf("$OPTIONS_PREFIX${if (options.ignoreCase) 1 else 0}:${if (options.ignoreWhitespace) 1 else 0}") + rules.mapNotNull { rule ->
        rule.keyword.trim().takeIf { it.isNotBlank() }?.let { keyword ->
            (if (rule.operator == SecondaryKeywordOperator.AND) AND_PREFIX else OR_PREFIX) + keyword
        }
    }

    /** Every AND starts a required group; consecutive OR terms join the previous group. */
    fun groups(rules: List<SecondaryKeywordRule>): List<List<String>> {
        val groups = mutableListOf<MutableList<String>>()
        rules.forEach { rule ->
            if (rule.operator == SecondaryKeywordOperator.AND || groups.isEmpty()) groups += mutableListOf(rule.keyword)
            else groups.last() += rule.keyword
        }
        return groups
    }
}

data class MonitorRule(
    val id: Long = 0,
    val name: String,
    val searchKeyword: String,
    val isEnabled: Boolean = true,
    val matchMode: MatchMode = MatchMode.CONTAINS,
    val mustIncludeWords: List<String> = emptyList(),
    val anyIncludeWords: List<String> = emptyList(),
    val excludeKeywords: List<String> = emptyList(),
    val enabledPlatforms: List<PlatformType> = PlatformType.entries,
    val thresholdMode: PriceThresholdMode = PriceThresholdMode.BOTH_OR,
    val maxFixedPrice: Double? = null,
    val discountThresholdPercent: Double = 30.0, // e.g. 30% below median
    val minDealScore: Int = 75,
    val scanIntervalMinutes: Int = 15,
    val createdAt: Long = System.currentTimeMillis(),
    val lastScannedAt: Long? = null,
    val totalFoundCount: Int = 0,
    val anomalyCount: Int = 0,
    val targetUrl: String = "",
    val trackMode: String = "KEYWORD" // "KEYWORD" or "URL"
)

data class ProductItem(
    val id: String, // platform_originalId
    val platform: PlatformType,
    val originalPlatformId: String,
    val title: String,
    val normalizedTitle: String,
    val url: String,
    val imageUrl: String,
    val sellerName: String,
    val sellerRating: Double = 4.8,
    val currentPrice: Double,
    val originalPrice: Double,
    val offerPrice: Double = currentPrice,
    val status: String = "AVAILABLE", // AVAILABLE, OUT_OF_STOCK
    val isAvailable: Boolean = true,
    val isNewItem: Boolean = false,
    val clusterId: String = "",
    val firstDiscoveredAt: Long = System.currentTimeMillis(),
    val lastDiscoveredAt: Long = System.currentTimeMillis(),
    val matchConfidence: Double = 1.0,
    val matchedMonitorId: Long? = null
)

data class PriceHistoryRecord(
    val id: Long = 0,
    val productId: String,
    val platform: PlatformType,
    val price: Double,
    val timestamp: Long = System.currentTimeMillis(),
    val note: String = ""
)

data class AnomalyReport(
    val id: Long = 0,
    val productId: String,
    val monitorRuleId: Long? = null,
    val monitorName: String,
    val productTitle: String,
    val productUrl: String,
    val imageUrl: String,
    val platform: PlatformType,
    val sellerName: String,
    val currentPrice: Double,
    val referencePrice: Double, // Market median or 30d median
    val deviationPercent: Double, // e.g. -65.2%
    val dealScore: Int, // 0 - 100
    val confidenceScore: Int, // 0 - 100
    val anomalyType: AnomalyType,
    val reasons: List<String>,
    val isFakeLowSuspected: Boolean = false,
    val isNotified: Boolean = false,
    val notifiedAt: Long? = null,
    val lastNotifiedPrice: Double? = null,
    val lastNotifiedDealScore: Int? = null,
    val isStarred: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

data class PlatformStatus(
    val platform: PlatformType,
    val isEnabled: Boolean = true,
    val isOnline: Boolean = false,
    val responseTimeMs: Long = 0,
    val lastSuccessScanAt: Long = 0,
    val lastError: String? = null,
    val requestCount: Int = 0,
    val errorCount: Int = 0,
    val rateLimitRpm: Int = 60,
    val limitationsNote: String = "尚未測試"
)

data class NotificationLog(
    val id: Long = 0,
    val reportId: Long,
    val productTitle: String,
    val platform: PlatformType,
    val currentPrice: Double,
    val referencePrice: Double,
    val discountPercent: Double,
    val dealScore: Int,
    val anomalyType: AnomalyType,
    val purchaseUrl: String,
    val notifiedAt: Long = System.currentTimeMillis()
)

data class MarketStats(
    val clusterId: String,
    val currentLowest: Double,
    val currentHighest: Double,
    val medianPrice: Double,
    val averagePrice: Double,
    val avg7Day: Double,
    val avg30Day: Double,
    val avg90Day: Double,
    val sampleCount: Int,
    val platformPrices: Map<PlatformType, Double>
)
