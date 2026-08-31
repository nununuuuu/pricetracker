package com.example.platform

import com.example.model.PlatformType
import com.example.model.ProductItem

/**
 * Explicitly represents a platform whose real adapter has not been implemented.
 * Returning an error is intentional: unavailable live data must never be replaced
 * with a plausible-looking price.
 */
class UnsupportedPlatformAdapter(
    override val platform: PlatformType,
    private val reason: String = "此平台尚未完成真實資料串接"
) : PlatformAdapter {
    override fun getConfig() = PlatformConfig(platform, isLiveSupported = false, statusNote = reason)
    override suspend fun searchProducts(keyword: String, page: Int): PlatformResult<List<ProductItem>> =
        PlatformResult.Error("${platform.displayName}：$reason", isTransient = false)
    override suspend fun getProductDetails(productId: String): PlatformResult<ProductItem> =
        PlatformResult.Error("${platform.displayName}：$reason", isTransient = false)
    override suspend fun testConnection(): PlatformResult<Long> =
        PlatformResult.Error("${platform.displayName}：$reason", isTransient = false)
}
