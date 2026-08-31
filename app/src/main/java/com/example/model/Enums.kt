package com.example.model

enum class PlatformType(
    val displayName: String,
    val brandColorHex: Long,
    val iconSymbol: String,
    val baseUrl: String
) {
    SHOPEE("蝦皮購物", 0xFFEE4D2D, "🛍️", "https://shopee.tw"),
    MOMO("momo 購物網", 0xFFE91E63, "🍑", "https://www.momoshop.com.tw"),
    PCHOME("PChome 24h", 0xFFE53935, "📦", "https://24h.pchome.com.tw"),
    COUPANG("酷澎 Coupang", 0xFF1E88E5, "🚀", "https://www.tw.coupang.com"),
    ETMALL("東森 ETMall", 0xFFE65100, "🛒", "https://www.etmall.com.tw"),
    RAKUTEN("台灣樂天市場", 0xFFBF0000, "🔴", "https://www.rakuten.com.tw"),
    YAHOO_CENTER("Yahoo購物中心", 0xFF7B1FA2, "🟣", "https://tw.buy.yahoo.com"),
    YAHOO_AUCTION("Yahoo拍賣", 0xFF6A1B9A, "🔨", "https://tw.bid.yahoo.com"),
    COSTCO("Costco 好市多", 0xFF005CAB, "🏷️", "https://www.costco.com.tw"),
    PXGO("全聯全電商", 0xFF004EA2, "🥬", "https://shop.pxmart.com.tw"),
    CARREFOUR("家樂福/萬家福", 0xFF0284C7, "🏪", "https://online.carrefour.com.tw"),
    BOOKS("博客來", 0xFF0D9488, "📚", "https://www.books.com.tw"),
    RUTEN("露天市集", 0xFFF57C00, "🎪", "https://www.ruten.com.tw"),
    BUY123("生活市集", 0xFFFA541C, "🏠", "https://www.buy123.com.tw"),
    PINECONE("松果購物", 0xFF8B572A, "🌰", "https://www.pcone.com.tw");

    fun getSearchUrl(keyword: String): String {
        val clean = keyword.trim()
        val encoded = try {
            java.net.URLEncoder.encode(clean, "UTF-8")
        } catch (e: Exception) {
            clean
        }
        return when (this) {
            SHOPEE -> "https://shopee.tw/search?keyword=$encoded"
            MOMO -> "https://www.momoshop.com.tw/search/searchShop.jsp?keyword=$encoded"
            PCHOME -> "https://24h.pchome.com.tw/search/?q=$encoded"
            COUPANG -> "https://www.tw.coupang.com/np/search?q=$encoded"
            ETMALL -> "https://www.etmall.com.tw/Search?keyword=$encoded"
            RAKUTEN -> "https://www.rakuten.com.tw/search/$encoded"
            YAHOO_CENTER -> "https://tw.buy.yahoo.com/search/product?p=$encoded"
            YAHOO_AUCTION -> "https://tw.bid.yahoo.com/search/auction/product?p=$encoded"
            COSTCO -> "https://www.costco.com.tw/search?text=$encoded"
            PXGO -> "https://shop.pxmart.com.tw/search?keyword=$encoded"
            CARREFOUR -> "https://online.carrefour.com.tw/zh/search?q=$encoded"
            BOOKS -> "https://search.books.com.tw/search/query/key/$encoded"
            RUTEN -> "https://www.ruten.com.tw/find/?q=$encoded"
            BUY123 -> "https://www.buy123.com.tw/search?q=$encoded"
            PINECONE -> "https://www.pcone.com.tw/search?q=$encoded"
        }
    }
}

enum class MatchMode(val label: String) {
    CONTAINS("包含模式 (Contains)"),
    EXACT("完全符合 (Exact)")
}

enum class PriceThresholdMode(val label: String) {
    PERCENTAGE("低於市場中位價 (%)"),
    FIXED_PRICE("固定最高入手價 (NT$)"),
    BOTH_OR("任一符合 (門檻% 或 固定價)"),
    BOTH_AND("兩者皆符合 (門檻% 且 固定價)")
}

enum class AnomalyType(
    val title: String,
    val emoji: String,
    val colorHex: Long,
    val description: String
) {
    SUSPECTED_GLITCH_PRICE("疑似標錯價", "🚨", 0xFFEF4444, "價格異常遠低於市場中位價 (>50% 偏離)"),
    CLEARANCE_PRICE("清倉／大特賣", "🔥", 0xFFF97316, "深度折扣，疑似店家出清庫存"),
    FLASH_SALE("限時特價", "⚡", 0xFFF59E0B, "短時間價格大幅下修"),
    HISTORIC_LOW("新歷史低價", "📉", 0xFF10B981, "低於過去90天紀錄之最低價格"),
    CROSS_PLATFORM_ANOMALY("跨平台異常價", "🌐", 0xFF6366F1, "單一平台價格顯著低於其他各家平台中位價"),
    GOOD_PRICE("一般好價", "🟡", 0xFF3B82F6, "價格優於平均價，符合合理優惠區間"),
    INSUFFICIENT_DATA("資料不足", "⚪", 0xFF9CA3AF, "歷史樣本數不足，持續收集中"),
    SUSPECTED_FAKE_LOW_PRICE("疑似假低價／配件", "⚠️", 0xFF6B7280, "標題或特徵疑似配件、空盒、展示品或零件")
}

enum class DealScoreLevel(
    val label: String,
    val badgeEmoji: String,
    val colorHex: Long,
    val minScore: Int,
    val maxScore: Int
) {
    EXTREME_ANOMALY("極端異常", "🚨", 0xFFEF4444, 90, 100),
    STRONG_DEAL("強烈低價", "🔥", 0xFFF97316, 75, 89),
    GOOD_PRICE("好價", "🟡", 0xFFF59E0B, 60, 74),
    NORMAL("正常價格", "⚪", 0xFF64748B, 0, 59);

    companion object {
        fun fromScore(score: Int): DealScoreLevel {
            return when {
                score >= 90 -> EXTREME_ANOMALY
                score >= 75 -> STRONG_DEAL
                score >= 60 -> GOOD_PRICE
                else -> NORMAL
            }
        }
    }
}
