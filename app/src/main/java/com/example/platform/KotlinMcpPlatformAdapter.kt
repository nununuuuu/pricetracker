package com.example.platform

import com.example.model.PlatformType
import com.example.model.ProductItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import tw.pricecompare.BasePlatform
import tw.pricecompare.SearchRequest

/** Bridges the standalone Kotlin port of mcp-taiwan-price-compare into Android. */
class KotlinMcpPlatformAdapter(
    override val platform: PlatformType,
    private val delegate: BasePlatform
) : PlatformAdapter {
    private val config = PlatformConfig(
        platform = platform,
        statusNote = "Kotlin 版 Taiwan Price Compare 解析器（真實資料來源）"
    )

    override fun getConfig() = config

    override suspend fun searchProducts(keyword: String, page: Int): PlatformResult<List<ProductItem>> = withContext(Dispatchers.IO) {
        if (keyword.isBlank()) return@withContext PlatformResult.Error("搜尋關鍵字不可為空", false)
        val startedAt = System.currentTimeMillis()
        try {
            val products = delegate.search(SearchRequest(query = keyword, maxPerPlatform = 100)).map { product ->
                ProductItem(
                    id = "${platform.name.lowercase()}_${product.id}",
                    platform = platform,
                    originalPlatformId = product.id,
                    title = product.name,
                    normalizedTitle = product.name.lowercase().replace(Regex("\\s+"), " ").trim(),
                    url = product.url,
                    imageUrl = "",
                    sellerName = platform.displayName,
                    currentPrice = product.price.toDouble(),
                    originalPrice = product.price.toDouble(),
                    offerPrice = product.price.toDouble()
                )
            }
            PlatformResult.Success(products, System.currentTimeMillis() - startedAt)
        } catch (error: Exception) {
            PlatformResult.Error("${platform.displayName} 搜尋失敗：${error.message ?: error.javaClass.simpleName}")
        }
    }

    override suspend fun getProductDetails(productId: String): PlatformResult<ProductItem> =
        PlatformResult.Error("${platform.displayName} 尚未提供商品詳情端點", false)

    override suspend fun testConnection(): PlatformResult<Long> {
        val startedAt = System.currentTimeMillis()
        return when (val result = searchProducts("手機")) {
            is PlatformResult.Success -> PlatformResult.Success(System.currentTimeMillis() - startedAt, System.currentTimeMillis() - startedAt)
            is PlatformResult.Error -> result
            is PlatformResult.RateLimited -> result
        }
    }
}
