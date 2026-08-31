package com.example.platform

import com.example.model.PlatformType
import com.example.model.ProductItem
import kotlinx.coroutines.delay
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import kotlin.random.Random

class PxgoAdapter(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build()
) : PlatformAdapter {

    override val platform: PlatformType = PlatformType.PXGO

    private var config = PlatformConfig(
        platform = PlatformType.PXGO,
        statusNote = "全聯全電商 (PXGo!小時達/分批取) 促銷連線正常"
    )

    override fun getConfig(): PlatformConfig = config

    override suspend fun testConnection(): PlatformResult<Long> {
        val start = System.currentTimeMillis()
        delay(105)
        return PlatformResult.Success(System.currentTimeMillis() - start, System.currentTimeMillis() - start)
    }

    override suspend fun searchProducts(keyword: String, page: Int): PlatformResult<List<ProductItem>> {
        val start = System.currentTimeMillis()
        delay(140 + Random.nextLong(110))
        val sampleList = ShopeeAdapter.generateMarketSampleProducts(keyword, PlatformType.PXGO)
        return PlatformResult.Success(sampleList, System.currentTimeMillis() - start)
    }

    override suspend fun getProductDetails(productId: String): PlatformResult<ProductItem> {
        return PlatformResult.Success(
            ProductItem(
                id = productId,
                platform = PlatformType.PXGO,
                originalPlatformId = productId.removePrefix("pxgo_"),
                title = "全聯全電商熱銷特惠",
                normalizedTitle = "全聯線上商品",
                url = "https://shop.pxmart.com.tw",
                imageUrl = "",
                sellerName = "全聯福利中心",
                currentPrice = 2899.0,
                originalPrice = 3699.0
            ),
            40
        )
    }
}
