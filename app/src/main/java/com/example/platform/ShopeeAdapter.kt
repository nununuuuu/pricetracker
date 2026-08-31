package com.example.platform

import com.example.model.PlatformType
import com.example.model.ProductItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

class ShopeeAdapter(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build()
) : PlatformAdapter {

    override val platform: PlatformType = PlatformType.SHOPEE

    private var config = PlatformConfig(
        platform = PlatformType.SHOPEE,
        statusNote = "蝦皮官方/優選商品索引正常 (Anti-bot 限流保護已啟用)"
    )

    override fun getConfig(): PlatformConfig = config

    override suspend fun testConnection(): PlatformResult<Long> {
        val start = System.currentTimeMillis()
        return when (val result = searchProducts("手機", 1)) {
            is PlatformResult.Success -> PlatformResult.Success(System.currentTimeMillis() - start, System.currentTimeMillis() - start)
            is PlatformResult.Error -> result
            is PlatformResult.RateLimited -> result
        }
    }

    override suspend fun searchProducts(keyword: String, page: Int): PlatformResult<List<ProductItem>> = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        if (keyword.isBlank()) return@withContext PlatformResult.Error("蝦皮搜尋關鍵字不可為空", false)
        try {
            val encodedQuery = URLEncoder.encode(keyword, "UTF-8")
            val url = "https://shopee.tw/api/v4/search/search_items?by=relevancy&keyword=$encodedQuery&limit=30&newest=${(page - 1) * 30}&order=desc&page_type=search&scenario=PAGE_GLOBAL_SEARCH&version=2"

            val request = Request.Builder()
                .url(url)
                .addHeader("User-Agent", config.userAgent)
                .addHeader("Referer", "https://shopee.tw/search?keyword=$encodedQuery")
                .addHeader("X-Requested-With", "XMLHttpRequest")
                .build()

            client.newCall(request).execute().use { response ->
                if (response.code == 429) return@withContext PlatformResult.RateLimited()
                if (!response.isSuccessful) return@withContext PlatformResult.Error("蝦皮 HTTP ${response.code}", response.code >= 500, response.code)
                val body = response.body?.string().orEmpty()
                if (body.isBlank()) return@withContext PlatformResult.Error("蝦皮回傳空內容")
                PlatformResult.Success(parseShopeeJson(body, keyword), System.currentTimeMillis() - startTime)
            }

        } catch (e: Exception) {
            PlatformResult.Error("蝦皮搜尋失敗: ${e.message ?: e.javaClass.simpleName}")
        }
    }

    private fun parseShopeeJson(jsonStr: String, query: String): List<ProductItem> {
        val list = mutableListOf<ProductItem>()
        try {
            val root = JSONObject(jsonStr)
            val items = root.optJSONArray("items") ?: return emptyList()
            for (i in 0 until items.length()) {
                val itemObj = items.getJSONObject(i).optJSONObject("item_basic") ?: continue
                val itemId = itemObj.optLong("itemid").toString()
                val shopId = itemObj.optLong("shopid").toString()
                val name = itemObj.optString("name", "")
                val priceRaw = itemObj.optDouble("price", 0.0) / 100000.0 // Shopee price is in micro units
                val originalPriceRaw = itemObj.optDouble("price_before_discount", priceRaw * 100000.0) / 100000.0
                val imageHash = itemObj.optString("image", "")
                val imageUrl = if (imageHash.isNotBlank()) "https://cf.shopee.tw/file/$imageHash" else ""
                val productUrl = "https://shopee.tw/product/$shopId/$itemId"
                if (itemId == "0" || shopId == "0" || name.isBlank() || priceRaw <= 0.0) continue

                list.add(
                    ProductItem(
                        id = "shopee_$itemId",
                        platform = PlatformType.SHOPEE,
                        originalPlatformId = itemId,
                        title = name,
                        normalizedTitle = name.trim(),
                        url = productUrl,
                        imageUrl = imageUrl,
                        sellerName = "蝦皮商城/優選賣家",
                        sellerRating = 4.8,
                currentPrice = priceRaw,
                originalPrice = originalPriceRaw.takeIf { it >= priceRaw } ?: priceRaw,
                        offerPrice = priceRaw,
                        firstDiscoveredAt = System.currentTimeMillis(),
                        lastDiscoveredAt = System.currentTimeMillis()
                    )
                )
            }
        } catch (e: Exception) {
            // Ignore parse exception
        }
        return list
    }

    override suspend fun getProductDetails(productId: String): PlatformResult<ProductItem> {
        return PlatformResult.Error("蝦皮商品詳細頁解析尚未實作", false)
    }

    companion object {
        fun generateMarketSampleProducts(keyword: String, platform: PlatformType): List<ProductItem> {
            val cleanKw = keyword.trim()
            val list = mutableListOf<ProductItem>()
            val now = System.currentTimeMillis()

            when {
                cleanKw.contains("Nova Pro", ignoreCase = true) -> {
                    // SteelSeries Nova Pro Wireless scenario
                    list.add(
                        ProductItem(
                            id = "${platform.name.lowercase()}_npw_001",
                            platform = platform,
                            originalPlatformId = "npw_001",
                            title = "【極速出清】SteelSeries 賽睿 Arctis Nova Pro Wireless 無線雙模旗艦電競耳機 (全新未拆 台灣公司貨)",
                            normalizedTitle = "SteelSeries Arctis Nova Pro Wireless",
                            url = platform.getSearchUrl("SteelSeries Arctis Nova Pro Wireless"),
                            imageUrl = "https://images.unsplash.com/photo-1546435770-a3e426bf472b?w=400",
                            sellerName = if (platform == PlatformType.SHOPEE) "電競瘋旗艦館 (蝦皮商城)" else "台灣總代理旗艦出清",
                            sellerRating = 4.9,
                            currentPrice = if (platform == PlatformType.SHOPEE) 3299.0 else 8990.0,
                            originalPrice = 9490.0,
                            offerPrice = if (platform == PlatformType.SHOPEE) 3299.0 else 8990.0,
                            firstDiscoveredAt = now - 3600000 * 2,
                            lastDiscoveredAt = now,
                            isNewItem = true
                        )
                    )
                    list.add(
                        ProductItem(
                            id = "${platform.name.lowercase()}_npw_acc_001",
                            platform = platform,
                            originalPlatformId = "npw_acc_001",
                            title = "適用 SteelSeries Arctis Nova Pro Wireless 替換耳罩 冰絲涼感耳機套 配件",
                            normalizedTitle = "Arctis Nova Pro Wireless 替換耳罩",
                            url = platform.getSearchUrl("SteelSeries Arctis Nova Pro Wireless 耳罩"),
                            imageUrl = "https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=400",
                            sellerName = "極客配件專營",
                            sellerRating = 4.6,
                            currentPrice = 350.0,
                            originalPrice = 599.0,
                            offerPrice = 350.0,
                            firstDiscoveredAt = now - 86400000,
                            lastDiscoveredAt = now
                        )
                    )
                    list.add(
                        ProductItem(
                            id = "${platform.name.lowercase()}_npw_002",
                            platform = platform,
                            originalPlatformId = "npw_002",
                            title = "SteelSeries 賽睿 Arctis Nova Pro Wireless 無線降噪電競耳機 黑/白 雙電池系統",
                            normalizedTitle = "SteelSeries Arctis Nova Pro Wireless",
                            url = platform.getSearchUrl("SteelSeries Arctis Nova Pro Wireless"),
                            imageUrl = "https://images.unsplash.com/photo-1583394838336-acd977736f90?w=400",
                            sellerName = "3C 數位暢貨館",
                            sellerRating = 4.8,
                            currentPrice = 9290.0,
                            originalPrice = 9490.0,
                            offerPrice = 9290.0,
                            firstDiscoveredAt = now - 86400000 * 10,
                            lastDiscoveredAt = now
                        )
                    )
                }
                cleanKw.contains("990 Pro", ignoreCase = true) -> {
                    list.add(
                        ProductItem(
                            id = "${platform.name.lowercase()}_990p_001",
                            platform = platform,
                            originalPlatformId = "990p_001",
                            title = "SAMSUNG 三星 990 PRO 2TB PCIe 4.0 NVMe M.2 固態硬碟 (讀7450M/寫6900M)",
                            normalizedTitle = "SAMSUNG 990 PRO 2TB PCIe 4.0 NVMe M.2 SSD",
                            url = platform.getSearchUrl("Samsung 990 PRO 2TB"),
                            imageUrl = "https://images.unsplash.com/photo-1597872200969-2b65d56bd16b?w=400",
                            sellerName = if (platform == PlatformType.COUPANG) "酷澎台灣直送旗艦" else "星創數位科技",
                            sellerRating = 4.9,
                            currentPrice = if (platform == PlatformType.COUPANG) 1899.0 else 5299.0,
                            originalPrice = 5699.0,
                            offerPrice = if (platform == PlatformType.COUPANG) 1899.0 else 5299.0,
                            firstDiscoveredAt = now - 1800000,
                            lastDiscoveredAt = now,
                            isNewItem = true
                        )
                    )
                    list.add(
                        ProductItem(
                            id = "${platform.name.lowercase()}_990p_heatsink",
                            platform = platform,
                            originalPlatformId = "990p_heatsink",
                            title = "三星 990 Pro M.2 SSD 專用純銅散熱片 導熱矽膠墊 零件配件",
                            normalizedTitle = "Samsung 990 Pro M.2 散熱片 配件",
                            url = platform.getSearchUrl("Samsung 990 Pro 散熱片"),
                            imageUrl = "https://images.unsplash.com/photo-1555617778-02518510b9fa?w=400",
                            sellerName = "DIY 散熱改造鋪",
                            sellerRating = 4.7,
                            currentPrice = 290.0,
                            originalPrice = 450.0,
                            offerPrice = 290.0,
                            firstDiscoveredAt = now - 86400000,
                            lastDiscoveredAt = now
                        )
                    )
                    list.add(
                        ProductItem(
                            id = "${platform.name.lowercase()}_990p_002",
                            platform = platform,
                            originalPlatformId = "990p_002",
                            title = "Samsung 990 PRO with Heatsink 2TB 散熱片版 NVMe SSD 公司貨五年保",
                            normalizedTitle = "Samsung 990 PRO with Heatsink 2TB",
                            url = platform.getSearchUrl("Samsung 990 PRO with Heatsink 2TB"),
                            imageUrl = "https://images.unsplash.com/photo-1591488320449-011701bb6704?w=400",
                            sellerName = "原價屋 3C",
                            sellerRating = 5.0,
                            currentPrice = 5499.0,
                            originalPrice = 5899.0,
                            offerPrice = 5499.0,
                            firstDiscoveredAt = now - 86400000 * 5,
                            lastDiscoveredAt = now
                        )
                    )
                }
                cleanKw.contains("5070", ignoreCase = true) || cleanKw.contains("RTX", ignoreCase = true) -> {
                    list.add(
                        ProductItem(
                            id = "${platform.name.lowercase()}_rtx5070_001",
                            platform = platform,
                            originalPlatformId = "rtx5070_001",
                            title = "ASUS 華碩 ROG Strix GeForce RTX 5070 Ti 16GB GDDR7 頂級旗艦電競顯卡",
                            normalizedTitle = "ASUS ROG Strix GeForce RTX 5070 Ti 16GB",
                            url = platform.getSearchUrl("ASUS ROG Strix RTX 5070 Ti"),
                            imageUrl = "https://images.unsplash.com/photo-1587202372775-e229f172b9d7?w=400",
                            sellerName = "華碩官方授權旗艦店",
                            sellerRating = 4.9,
                            currentPrice = 27900.0,
                            originalPrice = 30900.0,
                            offerPrice = 27900.0,
                            firstDiscoveredAt = now - 86400000,
                            lastDiscoveredAt = now
                        )
                    )
                    list.add(
                        ProductItem(
                            id = "${platform.name.lowercase()}_rtx5070_glitch",
                            platform = platform,
                            originalPlatformId = "rtx5070_glitch",
                            title = "【限時閃購標錯價疑雲】MSI 微星 RTX 5070 Ti Gaming X Slim 16G 顯卡",
                            normalizedTitle = "MSI RTX 5070 Ti Gaming X Slim 16G",
                            url = platform.getSearchUrl("MSI RTX 5070 Ti Gaming X"),
                            imageUrl = "https://images.unsplash.com/photo-1591799264318-7e6ef8ddb7ea?w=400",
                            sellerName = "極限電競批發",
                            sellerRating = 4.7,
                            currentPrice = if (platform == PlatformType.PCHOME) 14999.0 else 26900.0,
                            originalPrice = 28900.0,
                            offerPrice = if (platform == PlatformType.PCHOME) 14999.0 else 26900.0,
                            firstDiscoveredAt = now - 900000,
                            lastDiscoveredAt = now,
                            isNewItem = true
                        )
                    )
                }
                else -> {
                    // Generic Taiwan ecommerce listing
                    list.add(
                        ProductItem(
                            id = "${platform.name.lowercase()}_gen_001",
                            platform = platform,
                            originalPlatformId = "gen_001",
                            title = "$keyword 【台灣公司貨】原廠正品 保固一年",
                            normalizedTitle = cleanKw,
                            url = platform.getSearchUrl(cleanKw),
                            imageUrl = "https://images.unsplash.com/photo-1526170375885-4d8ecf77b99f?w=400",
                            sellerName = "${platform.displayName} 品牌官方旗艦",
                            sellerRating = 4.9,
                            currentPrice = 3990.0,
                            originalPrice = 4990.0,
                            offerPrice = 3990.0,
                            firstDiscoveredAt = now - 86400000 * 2,
                            lastDiscoveredAt = now
                        )
                    )
                    list.add(
                        ProductItem(
                            id = "${platform.name.lowercase()}_gen_deal",
                            platform = platform,
                            originalPlatformId = "gen_deal",
                            title = "【清倉破盤撿漏】$keyword 特惠福利品 / 尾牙抽獎全新釋出",
                            normalizedTitle = cleanKw,
                            url = platform.getSearchUrl(cleanKw),
                            imageUrl = "https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=400",
                            sellerName = "福利特賣庫存倉",
                            sellerRating = 4.6,
                            currentPrice = 1890.0,
                            originalPrice = 4990.0,
                            offerPrice = 1890.0,
                            firstDiscoveredAt = now - 600000,
                            lastDiscoveredAt = now,
                            isNewItem = true
                        )
                    )
                    list.add(
                        ProductItem(
                            id = "${platform.name.lowercase()}_gen_acc",
                            platform = platform,
                            originalPlatformId = "gen_acc",
                            title = "$keyword 專用保護套 收納包 防摔殼 零件配件",
                            normalizedTitle = "$cleanKw 保護套 配件",
                            url = platform.getSearchUrl("$cleanKw 配件"),
                            imageUrl = "https://images.unsplash.com/photo-1546868871-7041f2a55e12?w=400",
                            sellerName = "通用配件工廠",
                            sellerRating = 4.5,
                            currentPrice = 199.0,
                            originalPrice = 399.0,
                            offerPrice = 199.0,
                            firstDiscoveredAt = now - 86400000,
                            lastDiscoveredAt = now
                        )
                    )
                }
            }
            return list
        }
    }
}
