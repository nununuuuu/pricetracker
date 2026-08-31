package com.example.platform

import com.example.model.PlatformType
import com.example.model.ProductItem
import kotlinx.coroutines.delay
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import kotlin.random.Random

class Buy123Adapter(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build()
) : PlatformAdapter {

    override val platform: PlatformType = PlatformType.BUY123

    private var config = PlatformConfig(
        platform = PlatformType.BUY123,
        statusNote = "生活市集 今日強打爆品與限時閃購比價正常"
    )

    override fun getConfig(): PlatformConfig = config

    override suspend fun testConnection(): PlatformResult<Long> {
        val start = System.currentTimeMillis()
        delay(110)
        return PlatformResult.Success(System.currentTimeMillis() - start, System.currentTimeMillis() - start)
    }

    override suspend fun searchProducts(keyword: String, page: Int): PlatformResult<List<ProductItem>> {
        val start = System.currentTimeMillis()
        delay(140 + Random.nextLong(110))
        val sampleList = ShopeeAdapter.generateMarketSampleProducts(keyword, PlatformType.BUY123)
        return PlatformResult.Success(sampleList, System.currentTimeMillis() - start)
    }

    override suspend fun getProductDetails(productId: String): PlatformResult<ProductItem> {
        return PlatformResult.Success(
            ProductItem(
                id = productId,
                platform = PlatformType.BUY123,
                originalPlatformId = productId.removePrefix("buy123_"),
                title = "生活市集今日爆品特惠",
                normalizedTitle = "生活市集商品",
                url = "https://www.buy123.com.tw",
                imageUrl = "",
                sellerName = "生活市集直營嚴選",
                currentPrice = 2690.0,
                originalPrice = 3990.0
            ),
            40
        )
    }
}
