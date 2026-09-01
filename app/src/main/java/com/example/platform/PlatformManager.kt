package com.example.platform

import com.example.model.PlatformStatus
import com.example.model.PlatformType
import com.example.model.ProductItem
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class PlatformManager(
    private val adapters: Map<PlatformType, PlatformAdapter> = mapOf(
        PlatformType.SHOPEE to ShopeeAdapter(),
        PlatformType.MOMO to MomoAdapter(),
        PlatformType.PCHOME to PChomeAdapter(),
        PlatformType.COUPANG to CoupangAdapter(),
        PlatformType.ETMALL to EtmallAdapter(),
        PlatformType.RAKUTEN to KotlinMcpPlatformAdapter(PlatformType.RAKUTEN, tw.pricecompare.RakutenPlatform()),
        PlatformType.YAHOO_CENTER to YahooCenterAdapter(),
        PlatformType.YAHOO_AUCTION to KotlinMcpPlatformAdapter(PlatformType.YAHOO_AUCTION, tw.pricecompare.YahooAuctionPlatform()),
        PlatformType.COSTCO to KotlinMcpPlatformAdapter(PlatformType.COSTCO, tw.pricecompare.CostcoPlatform()),
        PlatformType.PXGO to KotlinMcpPlatformAdapter(PlatformType.PXGO, tw.pricecompare.PxboxPlatform()),
        PlatformType.CARREFOUR to KotlinMcpPlatformAdapter(PlatformType.CARREFOUR, tw.pricecompare.UniProsperityPlatform()),
        PlatformType.BOOKS to KotlinMcpPlatformAdapter(PlatformType.BOOKS, tw.pricecompare.BooksPlatform()),
        PlatformType.RUTEN to KotlinMcpPlatformAdapter(PlatformType.RUTEN, tw.pricecompare.RutenPlatform()),
        PlatformType.BUY123 to KotlinMcpPlatformAdapter(PlatformType.BUY123, tw.pricecompare.Buy123Platform()),
        PlatformType.PINECONE to KotlinMcpPlatformAdapter(PlatformType.PINECONE, tw.pricecompare.PineconePlatform())
    )
) {
    private val _platformStatuses = MutableStateFlow<Map<PlatformType, PlatformStatus>>(
        PlatformType.entries.associateWith { platform ->
            val adapter = adapters[platform]
            val config = adapter?.getConfig()
            PlatformStatus(
                platform = platform,
                isEnabled = config?.isLiveSupported == true,
                // No request has been made yet.  Never present fabricated health
                // information as a successful platform connection.
                isOnline = false,
                responseTimeMs = 0,
                lastSuccessScanAt = 0,
                limitationsNote = config?.statusNote ?: "尚未支援"
            )
        }
    )
    val platformStatuses: StateFlow<Map<PlatformType, PlatformStatus>> = _platformStatuses.asStateFlow()

    fun getAdapter(platform: PlatformType): PlatformAdapter? = adapters[platform]

    suspend fun searchAcrossPlatforms(
        keyword: String,
        targetPlatforms: List<PlatformType> = PlatformType.entries
    ): List<ProductItem> = coroutineScope {
        val activePlatforms = targetPlatforms.filter { platform ->
            _platformStatuses.value[platform]?.isEnabled == true && adapters[platform]?.getConfig()?.isLiveSupported == true
        }

        val deferredResults = activePlatforms.map { platform ->
            async {
                val adapter = adapters[platform] ?: return@async emptyList<ProductItem>()
                val start = System.currentTimeMillis()
                try {
                    when (val result = adapter.searchProducts(keyword)) {
                        is PlatformResult.Success -> {
                            updateStatusSuccess(platform, result.latencyMs)
                            result.data
                        }
                        is PlatformResult.Error -> {
                            updateStatusError(platform, result.message)
                            emptyList()
                        }
                        is PlatformResult.RateLimited -> {
                            updateStatusError(platform, "Rate limited (${result.retryAfterSeconds}s)")
                            emptyList()
                        }
                    }
                } catch (e: Exception) {
                    updateStatusError(platform, e.localizedMessage ?: "Unknown network error")
                    emptyList()
                }
            }
        }

        deferredResults.awaitAll().flatten()
    }

    suspend fun runDiagnostics(): Map<PlatformType, Long> = coroutineScope {
        val latencies = mutableMapOf<PlatformType, Long>()
        PlatformType.entries.forEach { platform ->
            val adapter = adapters[platform]
            if (adapter != null) {
                when (val res = adapter.testConnection()) {
                    is PlatformResult.Success -> {
                        latencies[platform] = res.data
                        updateStatusSuccess(platform, res.data)
                    }
                    is PlatformResult.Error -> {
                        updateStatusError(platform, res.message)
                    }
                    is PlatformResult.RateLimited -> {
                        updateStatusError(platform, "限流中")
                    }
                }
            }
        }
        latencies
    }

    fun setPlatformEnabled(platform: PlatformType, isEnabled: Boolean) {
        val current = _platformStatuses.value.toMutableMap()
        val existing = current[platform]
        if (existing != null) {
            current[platform] = existing.copy(isEnabled = isEnabled)
            _platformStatuses.value = current
        }
    }

    private fun updateStatusSuccess(platform: PlatformType, latencyMs: Long) {
        val current = _platformStatuses.value.toMutableMap()
        val existing = current[platform]
        if (existing != null) {
            current[platform] = existing.copy(
                isOnline = true,
                responseTimeMs = latencyMs,
                lastSuccessScanAt = System.currentTimeMillis(),
                requestCount = existing.requestCount + 1,
                lastError = null
            )
            _platformStatuses.value = current
        }
    }

    private fun updateStatusError(platform: PlatformType, error: String) {
        val current = _platformStatuses.value.toMutableMap()
        val existing = current[platform]
        if (existing != null) {
            current[platform] = existing.copy(
                isOnline = false,
                lastError = error,
                errorCount = existing.errorCount + 1,
                requestCount = existing.requestCount + 1
            )
            _platformStatuses.value = current
        }
    }
}
