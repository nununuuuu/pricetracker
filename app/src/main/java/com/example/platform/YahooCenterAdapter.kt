package com.example.platform

import com.example.model.PlatformType
import com.example.model.ProductItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit

/** Yahoo Shopping (購物中心), using its persisted GraphQL query and rendered-page fallback. */
class YahooCenterAdapter(private val client: OkHttpClient = OkHttpClient.Builder().connectTimeout(10, TimeUnit.SECONDS).readTimeout(15, TimeUnit.SECONDS).callTimeout(20, TimeUnit.SECONDS).build()) : PlatformAdapter {
    override val platform = PlatformType.YAHOO_CENTER
    private val config = PlatformConfig(platform, statusNote = "Yahoo 購物中心真實 GraphQL／HTML 搜尋；不使用樣本資料")
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
        if (keyword.isBlank()) return@withContext PlatformResult.Error("Yahoo 購物中心搜尋關鍵字不可為空", false)
        val start = System.currentTimeMillis()
        try {
            when (val graphql = fetchGraphql(keyword)) {
                is FetchResult.Success -> return@withContext PlatformResult.Success(parseHits(graphql.hits), System.currentTimeMillis() - start)
                is FetchResult.RateLimited -> return@withContext PlatformResult.RateLimited()
                is FetchResult.Failure -> Unit
            }
            when (val html = fetchHtml(keyword)) {
                is FetchResult.Success -> PlatformResult.Success(parseHits(html.hits), System.currentTimeMillis() - start)
                is FetchResult.RateLimited -> PlatformResult.RateLimited()
                is FetchResult.Failure -> PlatformResult.Error("Yahoo 購物中心 API 與 HTML fallback 均無法取得可解析資料")
            }
        } catch (e: Exception) { PlatformResult.Error("Yahoo 購物中心搜尋失敗: ${e.message ?: e.javaClass.simpleName}") }
    }

    override suspend fun getProductDetails(productId: String): PlatformResult<ProductItem> = PlatformResult.Error("Yahoo 購物中心商品詳細頁解析尚未實作", false)

    private fun fetchGraphql(keyword: String): FetchResult {
        val payload = JSONObject().apply {
            put("variables", JSONObject().apply {
                put("property", "shopping"); put("cid", "0"); put("clv", "0"); put("p", keyword); put("pg", "1"); put("psz", "60"); put("qt", "product"); put("sort", "price")
                put("isTestStoreIncluded", "0"); put("spaceId", 2092115029L); put("searchChain", "shopping_cb"); put("source", "pc")
            })
            put("extensions", JSONObject().put("persistedQuery", JSONObject().put("version", 1).put("sha256Hash", GRAPHQL_HASH)))
        }
        val request = Request.Builder().url(GRAPHQL_URL).header("Content-Type", "application/json").header("Origin", "https://tw.buy.yahoo.com").header("Referer", "https://tw.buy.yahoo.com/").header("User-Agent", config.userAgent)
            .post(payload.toString().toRequestBody("application/json; charset=utf-8".toMediaType())).build()
        client.newCall(request).execute().use { response ->
            if (response.code == 429) return FetchResult.RateLimited
            if (!response.isSuccessful) return FetchResult.Failure
            val hits = JSONObject(response.body?.string().orEmpty()).optJSONObject("data")?.optJSONObject("getUther")?.optJSONArray("hits") ?: return FetchResult.Failure
            return FetchResult.Success(hits)
        }
    }

    private fun fetchHtml(keyword: String): FetchResult {
        val query = URLEncoder.encode(keyword, StandardCharsets.UTF_8.toString())
        val request = Request.Builder().url("https://tw.buy.yahoo.com/search/product?p=$query&sort=price").header("Accept", "text/html,application/xhtml+xml").header("User-Agent", config.userAgent).build()
        client.newCall(request).execute().use { response ->
            if (response.code == 429) return FetchResult.RateLimited
            if (!response.isSuccessful) return FetchResult.Failure
            val html = response.body?.string().orEmpty(); val match = ISOREDUX_PATTERN.find(html) ?: return FetchResult.Failure
            val hits = JSONObject(match.groupValues[1]).optJSONObject("search")?.optJSONObject("ecsearch")?.optJSONArray("hits") ?: return FetchResult.Failure
            return FetchResult.Success(hits)
        }
    }

    internal fun parseHits(hits: JSONArray): List<ProductItem> = buildList {
        for (i in 0 until hits.length()) {
            val item = hits.optJSONObject(i) ?: continue; val id = item.optString("ec_productid").trim(); val title = item.optString("ec_title").trim()
            val price = item.opt("ec_price")?.toString()?.toDoubleOrNull() ?: 0.0; val url = item.optString("ec_item_url").trim()
            if (id.isBlank() || title.isBlank() || url.isBlank() || price <= 0.0) continue
            add(ProductItem("yahoocenter_$id", platform, id, title, normalizeTitle(title), if (url.startsWith("http")) url else "https://tw.buy.yahoo.com$url", "", "Yahoo 購物中心", 0.0, price, price, price))
        }
    }

    private fun normalizeTitle(value: String) = value.lowercase().replace(Regex("[【】\\[\\]()（）|｜]"), " ").replace(Regex("\\s+"), " ").trim()
    private sealed interface FetchResult { data class Success(val hits: JSONArray) : FetchResult; data object RateLimited : FetchResult; data object Failure : FetchResult }
    private companion object {
        const val GRAPHQL_URL = "https://graphql.ec.yahoo.com/graphql"
        const val GRAPHQL_HASH = "2a0c2518414ba006e0a42b5bc640a76bbb533e99a336d55027f6e3b4a796aafd"
        val ISOREDUX_PATTERN = Regex("<script id=\"isoredux-data\" type=\"mime/invalid\">(.*?)</script>", setOf(RegexOption.DOT_MATCHES_ALL))
    }
}
