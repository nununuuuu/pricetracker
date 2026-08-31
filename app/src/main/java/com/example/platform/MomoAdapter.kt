package com.example.platform

import com.example.model.PlatformType
import com.example.model.ProductItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit

class MomoAdapter(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .callTimeout(20, TimeUnit.SECONDS)
        .build()
) : PlatformAdapter {

    override val platform: PlatformType = PlatformType.MOMO

    private val apiUrl = "https://apisearch.momoshop.com.tw/momoSearchCloud/moec/textSearch"
    private val productUrl = "https://www.momoshop.com.tw/goods/GoodsDetail.jsp?i_code="

    private var config = PlatformConfig(
        platform = PlatformType.MOMO,
        statusNote = "momo 真實搜尋 API；不再使用 sample fallback"
    )

    override fun getConfig(): PlatformConfig = config

    override suspend fun testConnection(): PlatformResult<Long> {
        val start = System.currentTimeMillis()
        return when (val result = searchProducts("手機", 1)) {
            is PlatformResult.Success -> PlatformResult.Success(
                System.currentTimeMillis() - start,
                System.currentTimeMillis() - start
            )
            is PlatformResult.Error -> PlatformResult.Error(result.message, result.cause)
        }
    }

    override suspend fun searchProducts(keyword: String, page: Int): PlatformResult<List<ProductItem>> = withContext(Dispatchers.IO) {
        val start = System.currentTimeMillis()
        if (keyword.isBlank()) return@withContext PlatformResult.Error("momo 搜尋關鍵字不可為空")

        try {
            val payload = JSONObject().apply {
                put("host", "ecmobile")
                put("flag", "searchEngine")
                put("data", JSONObject().apply {
                    put("maxPage", 30)
                    put("cateLevel", -1)
                    put("serviceCode", "MT01")
                    put("platform", 16)
                    put("has3P", "Y")
                    put("NAM", "N")
                    put("china", "N")
                    put("cp", "N")
                    put("first", "N")
                    put("freeze", "N")
                    put("prefere", "N")
                    put("stockYN", "N")
                    put("superstore", "N")
                    put("threeHours", "N")
                    put("tomorrow", "N")
                    put("tvshop", "N")
                    put("video", "N")
                    put("cycle", "N")
                    put("cod", "N")
                    put("superstorePay", "N")
                    put("moCoinFeedback", "N")
                    put("superstoreFree", "N")
                    put("discount", "N")
                    put("isBrandSeriesPage", false)
                    put("isShowAdShop", false)
                    put("curRecommendedWordsCnt", 0)
                    put("searchValue", keyword)
                    put("curPage", page.coerceAtLeast(1))
                })
            }

            val request = Request.Builder()
                .url(apiUrl)
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .header("Origin", "https://m.momoshop.com.tw")
                .header("Referer", "https://m.momoshop.com.tw/")
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/142.0 Mobile Safari/537.36")
                .post(payload.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext PlatformResult.Error("momo HTTP ${response.code}")
                }

                val body = response.body?.string().orEmpty()
                if (body.isBlank()) return@withContext PlatformResult.Error("momo 回傳空內容")

                val root = JSONObject(body)
                if (!root.optBoolean("success", false)) {
                    return@withContext PlatformResult.Error("momo API 回報搜尋失敗")
                }

                val goods = root.optJSONObject("rtnSearchData")?.optJSONArray("goodsInfoList")
                    ?: return@withContext PlatformResult.Success(emptyList(), System.currentTimeMillis() - start)

                val products = buildList {
                    for (i in 0 until goods.length()) {
                        val item = goods.optJSONObject(i) ?: continue
                        val code = item.optString("goodsCode").trim()
                        val title = item.optString("goodsName").trim()
                        val price = parsePrice(item.opt("SALE_PRICE"))
                        if (code.isBlank() || title.isBlank() || price <= 0.0) continue

                        add(
                            ProductItem(
                                id = "momo_$code",
                                platform = PlatformType.MOMO,
                                originalPlatformId = code,
                                title = title,
                                normalizedTitle = normalizeTitle(title),
                                url = productUrl + URLEncoder.encode(code, StandardCharsets.UTF_8.toString()),
                                imageUrl = item.optString("imgUrl", item.optString("goodsImgUrl", "")),
                                sellerName = item.optString("sellerName", "momo"),
                                sellerRating = 0.0,
                                currentPrice = price,
                                originalPrice = parsePrice(item.opt("MARKET_PRICE")).takeIf { it >= price } ?: price,
                                offerPrice = price
                            )
                        )
                    }
                }

                PlatformResult.Success(products, System.currentTimeMillis() - start)
            }
        } catch (e: Exception) {
            PlatformResult.Error("momo 搜尋失敗: ${e.message ?: e.javaClass.simpleName}", e)
        }
    }

    override suspend fun getProductDetails(productId: String): PlatformResult<ProductItem> {
        return PlatformResult.Error("momo 商品詳細頁解析尚未實作；請使用 searchProducts 的真實商品資料")
    }

    private fun parsePrice(value: Any?): Double {
        return when (value) {
            is Number -> value.toDouble()
            is String -> value.replace(Regex("[^0-9.]"), "").toDoubleOrNull() ?: 0.0
            else -> 0.0
        }
    }

    private fun normalizeTitle(value: String): String = value
        .lowercase()
        .replace(Regex("[【】\\[\\]()（）|｜]"), " ")
        .replace(Regex("\\s+"), " ")
        .trim()
}
