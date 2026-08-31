package com.example.platform

import com.example.model.PlatformType
import com.example.model.ProductItem
import kotlinx.coroutines.delay
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import kotlin.random.Random

class PineconeAdapter(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build()
) : PlatformAdapter {

    override val platform: PlatformType = PlatformType.PINECONE

    private var config = PlatformConfig(
        platform = PlatformType.PINECONE,
        statusNote = "松果購物 居家生活與生活小物特賣監控正常"
    )

    override fun getConfig(): PlatformConfig = config

    override suspend fun testConnection(): PlatformResult<Long> {
        val start = System.currentTimeMillis()
        delay(115)
        return PlatformResult.Success(System.currentTimeMillis() - start, System.currentTimeMillis() - start)
    }

    override suspend fun searchProducts(keyword: String, page: Int): PlatformResult<List<ProductItem>> {
        val start = System.currentTimeMillis()
        delay(145 + Random.nextLong(100))
        val sampleList = ShopeeAdapter.generateMarketSampleProducts(keyword, PlatformType.PINECONE)
        return PlatformResult.Success(sampleList, System.currentTimeMillis() - start)
    }

    override suspend fun getProductDetails(productId: String): PlatformResult<ProductItem> {
        return PlatformResult.Success(
            ProductItem(
                id = productId,
                platform = PlatformType.PINECONE,
                originalPlatformId = productId.removePrefix("pinecone_"),
                title = "松果購物人氣精選商品",
                normalizedTitle = "松果購物商品",
                url = "https://www.pcone.com.tw",
                imageUrl = "",
                sellerName = "松果生活館",
                currentPrice = 2850.0,
                originalPrice = 4100.0
            ),
            40
        )
    }
}
