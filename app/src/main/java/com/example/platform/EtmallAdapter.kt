package com.example.platform

import com.example.model.PlatformType
import com.example.model.ProductItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit

/** Real ETMall search API adapter. */
class EtmallAdapter(private val client: OkHttpClient = OkHttpClient.Builder().connectTimeout(10, TimeUnit.SECONDS).readTimeout(15, TimeUnit.SECONDS).callTimeout(20, TimeUnit.SECONDS).build()) : PlatformAdapter {
    override val platform = PlatformType.ETMALL
    private val config = PlatformConfig(platform, statusNote = "東森購物真實搜尋 API；不使用樣本資料")
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
        if (keyword.isBlank()) return@withContext PlatformResult.Error("ETMall 搜尋關鍵字不可為空", false)
        val start = System.currentTimeMillis()
        try {
            val query = URLEncoder.encode(keyword, StandardCharsets.UTF_8.toString()); val index = page.coerceAtLeast(1) - 1
            val request = Request.Builder().url("https://www.etmall.com.tw/Search/Get?Keyword=$query&SortType=4&PageSize=40&PageIndex=$index").header("Accept", "application/json").header("Referer", "https://www.etmall.com.tw/").header("User-Agent", config.userAgent).build()
            client.newCall(request).execute().use { response ->
                if (response.code == 429) return@withContext PlatformResult.RateLimited()
                if (!response.isSuccessful) return@withContext PlatformResult.Error("ETMall HTTP ${response.code}", response.code >= 500, response.code)
                val body = response.body?.string().orEmpty(); if (body.isBlank()) return@withContext PlatformResult.Error("ETMall 回傳空內容")
                val parsed = try { parseSearchJson(body) } catch (_: Exception) { return@withContext PlatformResult.Error("ETMall 回傳格式無法解析（PARSE）", false) }
                PlatformResult.Success(parsed, System.currentTimeMillis() - start)
            }
        } catch (e: Exception) { PlatformResult.Error("ETMall 搜尋失敗: ${e.message ?: e.javaClass.simpleName}") }
    }
    override suspend fun getProductDetails(productId: String): PlatformResult<ProductItem> = PlatformResult.Error("ETMall 商品詳細頁解析尚未實作", false)
    internal fun parseSearchJson(json: String): List<ProductItem> {
        val products = JSONObject(json).optJSONObject("SearchProductResult")?.optJSONArray("products") ?: return emptyList()
        return buildList { for (i in 0 until products.length()) {
            val item = products.optJSONObject(i) ?: continue; val id = item.opt("id")?.toString()?.trim().orEmpty(); val title = item.optString("title").trim()
            val price = item.optString("finalPrice").replace(Regex("[^0-9.]"), "").toDoubleOrNull() ?: 0.0; if (id.isBlank() || title.isBlank() || price <= 0.0) continue
            val link = item.optString("pageLink").trim(); val url = if (link.startsWith("http")) link else if (link.isNotBlank()) "https://www.etmall.com.tw$link" else "https://www.etmall.com.tw/i/$id"
            add(ProductItem("etmall_$id", platform, id, title, normalizeTitle(title), url, "", "ETMall", 0.0, price, price, price))
        }}
    }
    private fun normalizeTitle(value: String) = value.lowercase().replace(Regex("[【】\\[\\]()（）|｜]"), " ").replace(Regex("\\s+"), " ").trim()
}
