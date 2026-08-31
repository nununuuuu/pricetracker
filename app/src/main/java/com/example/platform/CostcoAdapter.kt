package com.example.platform

import com.example.model.PlatformType
import com.example.model.ProductItem
import kotlinx.coroutines.delay
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import kotlin.random.Random

class CostcoAdapter(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build()
) : PlatformAdapter {

    override val platform: PlatformType = PlatformType.COSTCO

    private var config = PlatformConfig(
        platform = PlatformType.COSTCO,
        statusNote = "Costco 好市多線上購物 會員專屬破盤價與黑五優惠連線正常"
    )

    override fun getConfig(): PlatformConfig = config

    override suspend fun testConnection(): PlatformResult<Long> {
        val start = System.currentTimeMillis()
        delay(125)
        return PlatformResult.Success(System.currentTimeMillis() - start, System.currentTimeMillis() - start)
    }

    override suspend fun searchProducts(keyword: String, page: Int): PlatformResult<List<ProductItem>> {
        val start = System.currentTimeMillis()
        delay(160 + Random.nextLong(100))
        val sampleList = ShopeeAdapter.generateMarketSampleProducts(keyword, PlatformType.COSTCO)
        return PlatformResult.Success(sampleList, System.currentTimeMillis() - start)
    }

    override suspend fun getProductDetails(productId: String): PlatformResult<ProductItem> {
        return PlatformResult.Success(
            ProductItem(
                id = productId,
                platform = PlatformType.COSTCO,
                originalPlatformId = productId.removePrefix("costco_"),
                title = "Costco 好市多會員獨享特價",
                normalizedTitle = "Costco 好市多線上商品",
                url = "https://www.costco.com.tw",
                imageUrl = "",
                sellerName = "Costco 好市多官方",
                currentPrice = 3399.0,
                originalPrice = 4299.0
            ),
            50
        )
    }
}
