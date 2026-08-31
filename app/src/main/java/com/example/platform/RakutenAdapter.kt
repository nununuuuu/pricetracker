package com.example.platform

import com.example.model.PlatformType
import com.example.model.ProductItem
import kotlinx.coroutines.delay
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import kotlin.random.Random

class RakutenAdapter(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build()
) : PlatformAdapter {

    override val platform: PlatformType = PlatformType.RAKUTEN

    private var config = PlatformConfig(
        platform = PlatformType.RAKUTEN,
        statusNote = "台灣樂天市場 點數回饋與店家特賣即時同步正常"
    )

    override fun getConfig(): PlatformConfig = config

    override suspend fun testConnection(): PlatformResult<Long> {
        val start = System.currentTimeMillis()
        delay(115)
        return PlatformResult.Success(System.currentTimeMillis() - start, System.currentTimeMillis() - start)
    }

    override suspend fun searchProducts(keyword: String, page: Int): PlatformResult<List<ProductItem>> {
        val start = System.currentTimeMillis()
        delay(140 + Random.nextLong(110))
        val sampleList = ShopeeAdapter.generateMarketSampleProducts(keyword, PlatformType.RAKUTEN)
        return PlatformResult.Success(sampleList, System.currentTimeMillis() - start)
    }

    override suspend fun getProductDetails(productId: String): PlatformResult<ProductItem> {
        return PlatformResult.Success(
            ProductItem(
                id = productId,
                platform = PlatformType.RAKUTEN,
                originalPlatformId = productId.removePrefix("rakuten_"),
                title = "台灣樂天市場推薦商品",
                normalizedTitle = "台灣樂天市場商品",
                url = "https://www.rakuten.com.tw",
                imageUrl = "",
                sellerName = "樂天市場優選店家",
                currentPrice = 3150.0,
                originalPrice = 4500.0
            ),
            40
        )
    }
}
