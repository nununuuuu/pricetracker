package com.example.platform

import android.text.Html
import com.example.model.PlatformType
import com.example.model.ProductItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit

/** Real server-rendered Coupang Taiwan search adapter. */
class CoupangAdapter(private val client: OkHttpClient = OkHttpClient.Builder().connectTimeout(10, TimeUnit.SECONDS).readTimeout(15, TimeUnit.SECONDS).callTimeout(20, TimeUnit.SECONDS).build()) : PlatformAdapter {
    override val platform = PlatformType.COUPANG
    private val config = PlatformConfig(platform, statusNote = "酷澎真實搜尋頁；HTML 解析失敗時回報錯誤，不使用樣本資料")
    override fun getConfig() = config

    override suspend fun testConnection(): PlatformResult<Long> {
        val start = System.currentTimeMillis()
        return when (val result = searchProducts("手機")) {
            is PlatformResult.Success -> PlatformResult.Success(System.currentTimeMillis() - start, System.currentTimeMillis() - start)
            is PlatformResult.Error -> result
            is PlatformResult.RateLimited -> result
        }
    }

    override suspend fun searchProducts(keyword: String, page: Int): PlatformResult<List<ProductItem>> = withContext(Dispatchers.IO) {
        if (keyword.isBlank()) return@withContext PlatformResult.Error("Coupang 搜尋關鍵字不可為空", false)
        val start = System.currentTimeMillis()
        try {
            val query = URLEncoder.encode(keyword, StandardCharsets.UTF_8.toString())
            val request = Request.Builder().url("https://www.tw.coupang.com/np/search?q=$query&sorter=salePriceAsc&listSize=60")
                .header("Accept", "text/html,application/xhtml+xml").header("User-Agent", config.userAgent).build()
            client.newCall(request).execute().use { response ->
                if (response.code == 429) return@withContext PlatformResult.RateLimited()
                if (!response.isSuccessful) return@withContext PlatformResult.Error("Coupang HTTP ${response.code}", response.code >= 500, response.code)
                val body = response.body?.string().orEmpty()
                if (body.isBlank()) return@withContext PlatformResult.Error("Coupang 回傳空內容")
                if (!body.contains(PRODUCT_DELIMITER)) return@withContext PlatformResult.Error("Coupang 搜尋頁格式無法辨識（PARSE）", false)
                PlatformResult.Success(parseSearchHtml(body), System.currentTimeMillis() - start)
            }
        } catch (e: Exception) { PlatformResult.Error("Coupang 搜尋失敗: ${e.message ?: e.javaClass.simpleName}") }
    }

    override suspend fun getProductDetails(productId: String): PlatformResult<ProductItem> = PlatformResult.Error("Coupang 商品詳細頁解析尚未實作", false)

    internal fun parseSearchHtml(html: String): List<ProductItem> = buildList {
        html.split(PRODUCT_DELIMITER).drop(1).forEach { block ->
            val vendorItemId = ID_PATTERN.find(block)?.groupValues?.get(1) ?: return@forEach
            val link = LINK_PATTERN.find(block) ?: return@forEach
            val title = NAME_PATTERN.find(block)?.groupValues?.get(1)?.let(::decodeHtml)?.trim().orEmpty()
            val price = PRICE_PATTERN.find(block)?.groupValues?.get(1)?.replace(",", "")?.toDoubleOrNull() ?: return@forEach
            if (title.isBlank() || price <= 0.0) return@forEach
            val path = decodeHtml(link.groupValues[1]); val itemId = link.groupValues[2]
            add(ProductItem("coupang_$vendorItemId", platform, vendorItemId, title, normalizeTitle(title), "https://www.tw.coupang.com$path?itemId=$itemId&vendorItemId=$vendorItemId", "", "Coupang", 0.0, price, price, price))
        }
    }

    private fun decodeHtml(value: String) = Html.fromHtml(value, Html.FROM_HTML_MODE_LEGACY).toString()
    private fun normalizeTitle(value: String) = value.lowercase().replace(Regex("[【】\\[\\]()（）|｜]"), " ").replace(Regex("\\s+"), " ").trim()
    private companion object {
        const val PRODUCT_DELIMITER = "<li class=\"ProductUnit_productUnit__"
        val ID_PATTERN = Regex("data-id=\"(\\d+)\"")
        val LINK_PATTERN = Regex("href=\"(/products/[^\"?]+)\\?[^\"]*itemId=(\\d+)")
        val NAME_PATTERN = Regex("<div class=\"ProductUnit_productNameV2__[^\"]*\">([^<]+)</div>")
        val PRICE_PATTERN = Regex("<span translate=\"no\">\\$([\\d,]+)</span>")
    }
}
