package com.example.platform

import com.example.model.PlatformType
import com.example.model.ProductItem
import kotlinx.coroutines.delay
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import kotlin.random.Random

class CoupangAdapter(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build()
) : PlatformAdapter {

    override val platform: PlatformType = PlatformType.COUPANG

    private var config = PlatformConfig(
        platform = PlatformType.COUPANG,
        statusNote = "酷澎火箭速配 / 跨境直購即時價格追蹤中"
    )

    override fun getConfig(): PlatformConfig = config

    override suspend fun testConnection(): PlatformResult<Long> {
        val start = System.currentTimeMillis()
        delay(140)
        return PlatformResult.Success(System.currentTimeMillis() - start, System.currentTimeMillis() - start)
    }

    override suspend fun searchProducts(keyword: String, page: Int): PlatformResult<List<ProductItem>> {
        val start = System.currentTimeMillis()
        delay(180 + Random.nextLong(150))
        val sampleList = ShopeeAdapter.generateMarketSampleProducts(keyword, PlatformType.COUPANG)
        return PlatformResult.Success(sampleList, System.currentTimeMillis() - start)
    }

    override suspend fun getProductDetails(productId: String): PlatformResult<ProductItem> {
        return PlatformResult.Success(
            ProductItem(
                id = productId,
                platform = PlatformType.COUPANG,
                originalPlatformId = productId.removePrefix("coupang_"),
                title = "酷澎直營商品",
                normalizedTitle = "酷澎直營商品",
                url = "https://www.tw.coupang.com",
                imageUrl = "",
                sellerName = "Coupang 酷澎直營",
                currentPrice = 1899.0,
                originalPrice = 5699.0
            ),
            45
        )
    }
}
