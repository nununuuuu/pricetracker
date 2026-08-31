package com.example.platform

import com.example.model.PlatformType
import com.example.model.ProductItem
import kotlinx.coroutines.delay
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import kotlin.random.Random

class YahooCenterAdapter(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build()
) : PlatformAdapter {

    override val platform: PlatformType = PlatformType.YAHOO_CENTER

    private var config = PlatformConfig(
        platform = PlatformType.YAHOO_CENTER,
        statusNote = "Yahoo 奇摩購物中心 官方旗艦與品牌館連線正常"
    )

    override fun getConfig(): PlatformConfig = config

    override suspend fun testConnection(): PlatformResult<Long> {
        val start = System.currentTimeMillis()
        delay(110)
        return PlatformResult.Success(System.currentTimeMillis() - start, System.currentTimeMillis() - start)
    }

    override suspend fun searchProducts(keyword: String, page: Int): PlatformResult<List<ProductItem>> {
        val start = System.currentTimeMillis()
        delay(140 + Random.nextLong(100))
        val sampleList = ShopeeAdapter.generateMarketSampleProducts(keyword, PlatformType.YAHOO_CENTER)
        return PlatformResult.Success(sampleList, System.currentTimeMillis() - start)
    }

    override suspend fun getProductDetails(productId: String): PlatformResult<ProductItem> {
        return PlatformResult.Success(
            ProductItem(
                id = productId,
                platform = PlatformType.YAHOO_CENTER,
                originalPlatformId = productId.removePrefix("yahoocenter_"),
                title = "Yahoo 購物中心官方直營",
                normalizedTitle = "Yahoo 奇摩購物中心",
                url = "https://tw.buy.yahoo.com",
                imageUrl = "",
                sellerName = "Yahoo 官方自營旗艦",
                currentPrice = 2990.0,
                originalPrice = 4500.0
            ),
            40
        )
    }
}
