package com.example.platform

import com.example.model.PlatformType
import com.example.model.ProductItem
import kotlinx.coroutines.delay
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import kotlin.random.Random

class MomoAdapter(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build()
) : PlatformAdapter {

    override val platform: PlatformType = PlatformType.MOMO

    private var config = PlatformConfig(
        platform = PlatformType.MOMO,
        statusNote = "momo 購物網限時折價券與破盤行情監控正常"
    )

    override fun getConfig(): PlatformConfig = config

    override suspend fun testConnection(): PlatformResult<Long> {
        val start = System.currentTimeMillis()
        delay(110)
        return PlatformResult.Success(System.currentTimeMillis() - start, System.currentTimeMillis() - start)
    }

    override suspend fun searchProducts(keyword: String, page: Int): PlatformResult<List<ProductItem>> {
        val start = System.currentTimeMillis()
        delay(160 + Random.nextLong(140))
        val sampleList = ShopeeAdapter.generateMarketSampleProducts(keyword, PlatformType.MOMO)
        return PlatformResult.Success(sampleList, System.currentTimeMillis() - start)
    }

    override suspend fun getProductDetails(productId: String): PlatformResult<ProductItem> {
        return PlatformResult.Success(
            ProductItem(
                id = productId,
                platform = PlatformType.MOMO,
                originalPlatformId = productId.removePrefix("momo_"),
                title = "momo 購物商品",
                normalizedTitle = "momo 購物商品",
                url = "https://www.momoshop.com.tw",
                imageUrl = "",
                sellerName = "momo 品牌旗艦館",
                currentPrice = 9290.0,
                originalPrice = 9490.0
            ),
            50
        )
    }
}
