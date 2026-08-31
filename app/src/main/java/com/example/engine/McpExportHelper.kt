package com.example.engine

import com.example.model.AnomalyReport
import com.example.model.MarketStats
import com.example.model.ProductItem
import org.json.JSONArray
import org.json.JSONObject

object McpExportHelper {

    /**
     * Formats products and anomalies into an MCP-compliant JSON schema (Model Context Protocol format)
     * for LLMs like Claude, Gemini, ChatGPT.
     */
    fun formatToMcpJson(
        query: String,
        products: List<ProductItem>,
        stats: MarketStats?,
        anomalies: List<AnomalyReport>
    ): String {
        val root = JSONObject()
        root.put("tool", "taiwan_price_compare")
        root.put("query", query)
        root.put("timestamp", System.currentTimeMillis())

        val statsObj = JSONObject()
        if (stats != null) {
            statsObj.put("median_price", stats.medianPrice)
            statsObj.put("average_price", stats.averagePrice)
            statsObj.put("min_price", stats.currentLowest)
            statsObj.put("max_price", stats.currentHighest)
            statsObj.put("historical_low_90d", stats.avg90Day)
            statsObj.put("sample_count", stats.sampleCount)
        }
        root.put("market_statistics", statsObj)

        val platformsArray = JSONArray()
        products.sortedBy { it.currentPrice }.forEach { p ->
            val pObj = JSONObject()
            pObj.put("platform", p.platform.displayName)
            pObj.put("platform_code", p.platform.name.lowercase())
            pObj.put("title", p.title)
            pObj.put("current_price_ntd", p.currentPrice)
            pObj.put("original_price_ntd", p.originalPrice)
            pObj.put("seller", p.sellerName)
            pObj.put("seller_rating", p.sellerRating)
            pObj.put("url", p.url)
            pObj.put("is_lowest_price", p.currentPrice == stats?.currentLowest)
            platformsArray.put(pObj)
        }
        root.put("platform_listings", platformsArray)

        val anomalyArray = JSONArray()
        anomalies.forEach { a ->
            val aObj = JSONObject()
            aObj.put("platform", a.platform.displayName)
            aObj.put("deal_score", a.dealScore)
            aObj.put("deviation_percent", a.deviationPercent)
            aObj.put("anomaly_type", a.anomalyType.title)
            aObj.put("reasons", JSONArray(a.reasons))
            anomalyArray.put(aObj)
        }
        root.put("detected_anomalies", anomalyArray)

        return root.toString(2)
    }

    /**
     * Formats price compare result into AI Prompt-ready Markdown text.
     */
    fun formatToMarkdownSummary(
        query: String,
        products: List<ProductItem>,
        stats: MarketStats?
    ): String {
        val sb = StringBuilder()
        sb.appendLine("## 🇹🇼 台灣電商即時比價報告 (MCP Price Compare)")
        sb.appendLine("**搜尋商品**: `$query`")
        if (stats != null) {
            sb.appendLine("- **全網市場中位價**: NT$ ${String.format("%,.0f", stats.medianPrice)}")
            sb.appendLine("- **90天常態均價**: NT$ ${String.format("%,.0f", stats.avg90Day)}")
            sb.appendLine("- **今日最低價**: NT$ ${String.format("%,.0f", stats.currentLowest)} (價差 ${String.format("%.1f", (1 - stats.currentLowest / stats.medianPrice) * 100)}%)")
        }
        sb.appendLine()
        sb.appendLine("| 平台 | 商品標題 | 售價 (NT$) | 賣家/規格 | 連結 |")
        sb.appendLine("|---|---|---|---|---|")
        products.sortedBy { it.currentPrice }.forEach { p ->
            val isMin = stats != null && p.currentPrice == stats.currentLowest
            val priceTag = if (isMin) "**NT$ ${String.format("%,.0f", p.currentPrice)} 👑**" else "NT$ ${String.format("%,.0f", p.currentPrice)}"
            sb.appendLine("| ${p.platform.iconSymbol} ${p.platform.displayName.split(" ").first()} | ${p.title.take(30)}... | $priceTag | ${p.sellerName} | [前往購買](${p.url}) |")
        }
        return sb.toString()
    }
}
