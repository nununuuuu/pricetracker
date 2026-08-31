package com.example.platform

import com.example.model.PlatformType
import com.example.model.ProductItem

sealed class PlatformResult<out T> {
    data class Success<T>(val data: T, val latencyMs: Long) : PlatformResult<T>()
    data class Error(val message: String, val isTransient: Boolean = true, val statusCode: Int? = null) : PlatformResult<Nothing>()
    data class RateLimited(val retryAfterSeconds: Int = 30) : PlatformResult<Nothing>()
}

data class PlatformConfig(
    val platform: PlatformType,
    val isEnabled: Boolean = true,
    val requestTimeoutMs: Long = 10000,
    val maxRetries: Int = 3,
    val rateLimitRpm: Int = 60,
    val userAgent: String = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36",
    val isLiveSupported: Boolean = true,
    val statusNote: String = "正常運行"
)

interface PlatformAdapter {
    val platform: PlatformType
    fun getConfig(): PlatformConfig
    suspend fun searchProducts(keyword: String, page: Int = 1): PlatformResult<List<ProductItem>>
    suspend fun getProductDetails(productId: String): PlatformResult<ProductItem>
    suspend fun testConnection(): PlatformResult<Long> // returns response time in ms
}
