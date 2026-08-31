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

class PChomeAdapter(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .callTimeout(20, TimeUnit.SECONDS)
        .build()
) : PlatformAdapter {

    override val platform: PlatformType = PlatformType.PCHOME

    private var config = PlatformConfig(
        platform = PlatformType.PCHOME,
        statusNote = "PChome 真實搜尋 API；排除加價購且不使用 sample fallback"
    )

    override fun getConfig(): PlatformConfig = config

    override suspend fun testConnection(): PlatformResult<Long> {
        val start = System.currentTimeMillis()
        return when (val result = searchProducts("手機", 1)) {
            is PlatformResult.Success -> PlatformResult.Success(System.currentTimeMillis() - start, System.currentTimeMillis() - start)
            is PlatformResult.Error -> PlatformResult.Error(
                message = result.message,
                isTransient = result.isTransient,
                statusCode = result.statusCode
            )
            is PlatformResult.RateLimited -> result
        }
    }

    override suspend fun searchProducts(keyword: String, page: Int): PlatformResult<List<ProductItem>> = withContext(Dispatchers.IO) {
        val start = System.currentTimeMillis()
        if (keyword.isBlank()) return@withContext PlatformResult.Error("PChome 搜尋關鍵字不可為空")

        try {
            val encodedQuery = URLEncoder.encode(keyword, StandardCharsets.UTF_8.toString())
            val url = "https://ecshweb.pchome.com.tw/search/v3.3/all/results?q=$encodedQuery&page=${page.coerceAtLeast(1)}&sort=prc/ac"
            val request = Request.Builder()
                .url(url)
                .header("Accept", "application/json")
                .header("User-Agent", config.userAgent)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext PlatformResult.Error("PChome HTTP ${response.code}")
                }
                val body = response.body?.string().orEmpty()
                if (body.isBlank()) return@withContext PlatformResult.Error("PChome 回傳空內容")

                val products = parsePChomeJson(body)
                PlatformResult.Success(products, System.currentTimeMillis() - start)
            }
        } catch (e: Exception) {
            PlatformResult.Error(
                message = "PChome 搜尋失敗: ${e.message ?: e.javaClass.simpleName}",
                isTransient = true
            )
        }
    }

    private fun parsePChomeJson(jsonStr: String): List<ProductItem> {
        val root = JSONObject(jsonStr)
        val prods = root.optJSONArray("prods") ?: return emptyList()
        return buildList {
            for (i in 0 until prods.length()) {
                val prod = prods.optJSONObject(i) ?: continue
                val id = prod.optString("Id").trim()
                val name = prod.optString("name").trim()
                val price = prod.optDouble("price", 0.0)
                if (id.isBlank() || name.isBlank() || price <= 0.0 || name.contains("【加價購】")) continue

                val picS = prod.optString("picS", "")
                val picUrl = when {
                    picS.isBlank() -> ""
                    picS.startsWith("http") -> picS
                    else -> "https://cs-a.ecimg.tw$picS"
                }
                val originPrice = prod.optDouble("originPrice", 0.0).takeIf { it >= price } ?: price

                add(
                    ProductItem(
                        id = "pchome_$id",
                        platform = PlatformType.PCHOME,
                        originalPlatformId = id,
                        title = name,
                        normalizedTitle = normalizeTitle(name),
                        url = "https://24h.pchome.com.tw/prod/$id",
                        imageUrl = picUrl,
                        sellerName = "PChome 24h",
                        sellerRating = 0.0,
                        currentPrice = price,
                        originalPrice = originPrice,
                        offerPrice = price
                    )
                )
            }
        }
    }

    override suspend fun getProductDetails(productId: String): PlatformResult<ProductItem> {
        return when (val result = searchProducts(productId)) {
            is PlatformResult.Success -> result.data.firstOrNull { it.originalPlatformId.equals(productId, true) }
                ?.let { PlatformResult.Success(it, result.latencyMs) }
                ?: PlatformResult.Error("PChome 找不到商品編號 $productId", false, 404)
            is PlatformResult.Error -> result
            is PlatformResult.RateLimited -> result
        }
    }

    private fun normalizeTitle(value: String): String = value
        .lowercase()
        .replace(Regex("[【】\\[\\]()（）|｜]"), " ")
        .replace(Regex("\\s+"), " ")
        .trim()
}
