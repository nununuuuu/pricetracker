package com.example.platform

import com.example.model.PlatformType
import com.example.model.ProductItem
import kotlinx.coroutines.delay
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import kotlin.random.Random

class BooksAdapter(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build()
) : PlatformAdapter {

    override val platform: PlatformType = PlatformType.BOOKS

    private var config = PlatformConfig(
        platform = PlatformType.BOOKS,
        statusNote = "博客來 Books.com.tw 3C/生活家電/圖書索引中"
    )

    override fun getConfig(): PlatformConfig = config

    override suspend fun testConnection(): PlatformResult<Long> {
        val start = System.currentTimeMillis()
        delay(95)
        return PlatformResult.Success(System.currentTimeMillis() - start, System.currentTimeMillis() - start)
    }

    override suspend fun searchProducts(keyword: String, page: Int): PlatformResult<List<ProductItem>> {
        val start = System.currentTimeMillis()
        delay(140 + Random.nextLong(100))
        val sampleList = ShopeeAdapter.generateMarketSampleProducts(keyword, PlatformType.BOOKS)
        return PlatformResult.Success(sampleList, System.currentTimeMillis() - start)
    }

    override suspend fun getProductDetails(productId: String): PlatformResult<ProductItem> {
        return PlatformResult.Success(
            ProductItem(
                id = productId,
                platform = PlatformType.BOOKS,
                originalPlatformId = productId.removePrefix("books_"),
                title = "博客來官方旗艦商品",
                normalizedTitle = "博客來官方旗艦",
                url = "https://www.books.com.tw",
                imageUrl = "",
                sellerName = "博客來官方自營",
                currentPrice = 2890.0,
                originalPrice = 3600.0
            ),
            35
        )
    }
}
