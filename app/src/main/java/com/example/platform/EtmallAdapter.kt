package com.example.platform

import com.example.model.PlatformType
import com.example.model.ProductItem
import kotlinx.coroutines.delay
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import kotlin.random.Random

class EtmallAdapter(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build()
) : PlatformAdapter {

    override val platform: PlatformType = PlatformType.ETMALL

    private var config = PlatformConfig(
        platform = PlatformType.ETMALL,
        statusNote = "東森購物 ETMall / 電視購物特惠價追蹤中"
    )

    override fun getConfig(): PlatformConfig = config

    override suspend fun testConnection(): PlatformResult<Long> {
        val start = System.currentTimeMillis()
        delay(135)
        return PlatformResult.Success(System.currentTimeMillis() - start, System.currentTimeMillis() - start)
    }

    override suspend fun searchProducts(keyword: String, page: Int): PlatformResult<List<ProductItem>> {
        val start = System.currentTimeMillis()
        delay(160 + Random.nextLong(140))
        val sampleList = ShopeeAdapter.generateMarketSampleProducts(keyword, PlatformType.ETMALL)
        return PlatformResult.Success(sampleList, System.currentTimeMillis() - start)
    }

    override suspend fun getProductDetails(productId: String): PlatformResult<ProductItem> {
        return PlatformResult.Success(
            ProductItem(
                id = productId,
                platform = PlatformType.ETMALL,
                originalPlatformId = productId.removePrefix("etmall_"),
                title = "東森購物 ETMall 精選商品",
                normalizedTitle = "東森購物 ETMall 精選商品",
                url = "https://www.etmall.com.tw",
                imageUrl = "",
                sellerName = "東森購物直營",
                currentPrice = 3180.0,
                originalPrice = 4200.0
            ),
            50
        )
    }
}
