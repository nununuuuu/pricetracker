package com.example.platform

import com.example.model.PlatformType
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

data class ParsedUrlInfo(
    /** Source platform only; it must never silently become a comparison platform. */
    val platform: PlatformType?,
    val suggestedName: String,
    val suggestedKeyword: String,
    val cleanUrl: String,
    val isValidUrl: Boolean,
    val productId: String? = null
)

object UrlParserHelper {

    fun parseProductUrl(rawUrl: String): ParsedUrlInfo {
        val trimmed = rawUrl.trim()
        if (trimmed.isBlank() || !trimmed.startsWith("http", ignoreCase = true)) {
            return ParsedUrlInfo(
                platform = null,
                suggestedName = "",
                suggestedKeyword = "",
                cleanUrl = trimmed,
                isValidUrl = false
            )
        }

        val lower = trimmed.lowercase()
        val platform = when {
            lower.contains("shopee.tw") || lower.contains("xiapi") -> PlatformType.SHOPEE
            lower.contains("momoshop.com.tw") || lower.contains("momo.dm") -> PlatformType.MOMO
            lower.contains("24h.pchome.com.tw") || lower.contains("pchome.com.tw") -> PlatformType.PCHOME
            lower.contains("coupang.com") -> PlatformType.COUPANG
            lower.contains("etmall.com.tw") -> PlatformType.ETMALL
            lower.contains("rakuten.com.tw") -> PlatformType.RAKUTEN
            lower.contains("tw.bid.yahoo.com") -> PlatformType.YAHOO_AUCTION
            lower.contains("buy.yahoo.com") || lower.contains("tw.buy.yahoo.com") -> PlatformType.YAHOO_CENTER
            lower.contains("costco.com.tw") -> PlatformType.COSTCO
            lower.contains("pxmart.com.tw") || lower.contains("pxgo") -> PlatformType.PXGO
            lower.contains("carrefour.com.tw") -> PlatformType.CARREFOUR
            lower.contains("books.com.tw") -> PlatformType.BOOKS
            lower.contains("ruten.com.tw") -> PlatformType.RUTEN
            lower.contains("buy123.com.tw") -> PlatformType.BUY123
            lower.contains("pcone.com.tw") -> PlatformType.PINECONE
            else -> null
        }

        val decodedUrl = try {
            URLDecoder.decode(trimmed, StandardCharsets.UTF_8.name())
        } catch (e: Exception) {
            trimmed
        }

        var extractedTitle = ""

        // Extract from common query params
        val queryIndex = decodedUrl.indexOf('?')
        if (queryIndex != -1) {
            val queryString = decodedUrl.substring(queryIndex + 1)
            val params = queryString.split("&")
            for (param in params) {
                val parts = param.split("=", limit = 2)
                if (parts.size == 2) {
                    val key = parts[0].lowercase()
                    val value = parts[1].replace("+", " ").trim()
                    if (key in listOf("keyword", "q", "p", "query", "key", "title", "goodsname", "search", "name") && value.isNotBlank()) {
                        extractedTitle = value
                        break
                    }
                }
            }
        }

        // A URL path often contains only an SKU (for example PChome's DSAJBU-...)
        // and is never a product title. The resolver fetches the real title later.
        val finalName = extractedTitle
        val finalKeyword = extractedTitle
        val productId = when (platform) {
            PlatformType.PCHOME -> Regex("/prod/([^/?#]+)", RegexOption.IGNORE_CASE).find(trimmed)?.groupValues?.getOrNull(1)
            PlatformType.MOMO -> Regex("[?&]i_code=([^&#]+)", RegexOption.IGNORE_CASE).find(trimmed)?.groupValues?.getOrNull(1)
            else -> null
        }

        return ParsedUrlInfo(
            platform = platform,
            suggestedName = finalName,
            suggestedKeyword = finalKeyword,
            cleanUrl = trimmed,
            isValidUrl = platform != null,
            productId = productId
        )
    }
}
