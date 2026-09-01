package tw.pricecompare

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withTimeoutOrNull

class PriceCompareService(private val platforms: Map<PlatformId, BasePlatform>) {
    companion object {
        const val PLATFORM_TIMEOUT_MS = 3_000L
        val FAST_PLATFORMS = setOf(
            PlatformId.ETMALL, PlatformId.PCHOME, PlatformId.BUY123, PlatformId.YAHOO_AUCTION,
            PlatformId.YAHOO_SHOPPING, PlatformId.UNI_PROSPERITY, PlatformId.PXBOX,
            PlatformId.BOOKS, PlatformId.COSTCO
        )
    }

    suspend fun searchAll(request: SearchRequest): SearchResult = coroutineScope {
        val selected = platforms.filterKeys { request.mode == SearchMode.FULL || it in FAST_PLATFORMS }.values
        val products = selected.map { platform ->
            async { withTimeoutOrNull(PLATFORM_TIMEOUT_MS) { platform.search(request) }.orEmpty() }
        }.awaitAll().flatten().sortedBy { it.price }
        SearchResult(request.query, products)
    }

    suspend fun cheapest(request: SearchRequest, topN: Int = 10): List<Product> =
        searchAll(request).products.take(topN)
}

