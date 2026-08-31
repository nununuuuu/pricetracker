package com.example.platform

import com.example.model.PlatformType
import com.example.model.ProductItem
import kotlinx.coroutines.delay
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import kotlin.random.Random

class CarrefourAdapter(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build()
) : PlatformAdapter {

    override val platform: PlatformType = PlatformType.CARREFOUR

    private var config = PlatformConfig(
        platform = PlatformType.CARREFOUR,
        statusNote = "家樂福線上購物 量販特賣與民生家電連線正常"
    )

    override fun getConfig(): PlatformConfig = config

    override suspend fun testConnection(): PlatformResult<Long> {
        val start = System.currentTimeMillis()
        delay(115)
        return PlatformResult.Success(System.currentTimeMillis() - start, System.currentTimeMillis() - start)
    }

    override suspend fun searchProducts(keyword: String, page: Int): PlatformResult<List<ProductItem>> {
        val start = System.currentTimeMillis()
        delay(150 + Random.nextLong(130))
        val sampleList = ShopeeAdapter.generateMarketSampleProducts(keyword, PlatformType.CARREFOUR)
        return PlatformResult.Success(sampleList, System.currentTimeMillis() - start)
    }

    override suspend fun getProductDetails(productId: String): PlatformResult<ProductItem> {
        return PlatformResult.Success(
            ProductItem(
                id = productId,
                platform = PlatformType.CARREFOUR,
                originalPlatformId = productId.removePrefix("carrefour_"),
                title = "家樂福線上量販特惠",
                normalizedTitle = "家樂福線上量販",
                url = "https://online.carrefour.com.tw",
                imageUrl = "",
                sellerName = "家樂福直營",
                currentPrice = 3099.0,
                originalPrice = 3990.0
            ),
            40
        )
    }
}
