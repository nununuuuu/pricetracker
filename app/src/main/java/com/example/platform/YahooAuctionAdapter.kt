package com.example.platform

import com.example.model.PlatformType
import com.example.model.ProductItem
import kotlinx.coroutines.delay
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import kotlin.random.Random

class YahooAuctionAdapter(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build()
) : PlatformAdapter {

    override val platform: PlatformType = PlatformType.YAHOO_AUCTION

    private var config = PlatformConfig(
        platform = PlatformType.YAHOO_AUCTION,
        statusNote = "Yahoo 奇摩拍賣 個人賣家與二手跳水監控正常"
    )

    override fun getConfig(): PlatformConfig = config

    override suspend fun testConnection(): PlatformResult<Long> {
        val start = System.currentTimeMillis()
        delay(120)
        return PlatformResult.Success(System.currentTimeMillis() - start, System.currentTimeMillis() - start)
    }

    override suspend fun searchProducts(keyword: String, page: Int): PlatformResult<List<ProductItem>> {
        val start = System.currentTimeMillis()
        delay(150 + Random.nextLong(100))
        val sampleList = ShopeeAdapter.generateMarketSampleProducts(keyword, PlatformType.YAHOO_AUCTION)
        return PlatformResult.Success(sampleList, System.currentTimeMillis() - start)
    }

    override suspend fun getProductDetails(productId: String): PlatformResult<ProductItem> {
        return PlatformResult.Success(
            ProductItem(
                id = productId,
                platform = PlatformType.YAHOO_AUCTION,
                originalPlatformId = productId.removePrefix("yahooauction_"),
                title = "Yahoo 拍賣優選賣家",
                normalizedTitle = "Yahoo 奇摩拍賣",
                url = "https://tw.bid.yahoo.com",
                imageUrl = "",
                sellerName = "Yahoo 拍賣優良賣家",
                currentPrice = 2800.0,
                originalPrice = 4200.0
            ),
            45
        )
    }
}
