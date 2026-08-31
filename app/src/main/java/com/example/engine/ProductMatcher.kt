package com.example.engine

import java.util.Locale

data class MatchResult(
    val isMatched: Boolean,
    val confidenceScore: Double, // 0.0 ~ 1.0
    val clusterId: String,
    val normalizedTitle: String,
    val rejectionReason: String? = null
)

object ProductMatcher {

    private val STOPWORDS = setOf(
        "台灣公司貨", "公司貨", "正品", "原廠", "現貨", "免運", "快速出貨", "特賣", "下殺",
        "熱銷", "推薦", "福利品", "拆封", "全新", "限時", "官方", "旗艦", "直送", "專賣",
        "保固", "一年保", "兩年保", "五年保", "含稅", "開發票"
    )

    fun normalizeTitle(title: String): String {
        var clean = title.lowercase(Locale.ROOT)
        // Remove brackets and common promo text
        clean = clean.replace(Regex("【.*?】|\\[.*?\\]|\\(.*?\\)|（.*?）"), " ")
        STOPWORDS.forEach { word ->
            clean = clean.replace(word.lowercase(Locale.ROOT), " ")
        }
        // Replace special characters with spaces
        clean = clean.replace(Regex("[^a-z0-9\\u4e00-\\u9fa5]"), " ")
        return clean.replace(Regex("\\s+"), " ").trim()
    }

    fun matchProduct(
        productTitle: String,
        searchKeyword: String,
        mustIncludeWords: List<String>,
        anyIncludeWords: List<String>,
        excludeKeywords: List<String>
    ): MatchResult {
        val lowerTitle = productTitle.lowercase(Locale.ROOT)
        val normTitle = normalizeTitle(productTitle)

        // 1. Exclude keywords check
        excludeKeywords.forEach { excl ->
            if (excl.isNotBlank() && lowerTitle.contains(excl.lowercase(Locale.ROOT).trim())) {
                return MatchResult(
                    isMatched = false,
                    confidenceScore = 0.0,
                    clusterId = "",
                    normalizedTitle = normTitle,
                    rejectionReason = "命中排除關鍵字: $excl"
                )
            }
        }

        // 2. Must include check
        val missingMustWords = mustIncludeWords.filter { must ->
            must.isNotBlank() && !lowerTitle.contains(must.lowercase(Locale.ROOT).trim())
        }
        if (missingMustWords.isNotEmpty()) {
            return MatchResult(
                isMatched = false,
                confidenceScore = 0.0,
                clusterId = "",
                normalizedTitle = normTitle,
                rejectionReason = "缺少必備關鍵字: [${missingMustWords.joinToString(", ")}]"
            )
        }

        // 3. Any include check (if specified)
        val validAnyWords = anyIncludeWords.filter { it.isNotBlank() }
        if (validAnyWords.isNotEmpty()) {
            val hasAny = validAnyWords.any { anyWord ->
                lowerTitle.contains(anyWord.lowercase(Locale.ROOT).trim())
            }
            if (!hasAny) {
                return MatchResult(
                    isMatched = false,
                    confidenceScore = 0.0,
                    clusterId = "",
                    normalizedTitle = normTitle,
                    rejectionReason = "未包含任一指定關鍵字"
                )
            }
        }

        // 4. Calculate token overlap similarity and confidence
        val searchTokens = normalizeTitle(searchKeyword).split(" ").filter { it.length >= 2 }
        val titleTokens = normTitle.split(" ").filter { it.length >= 2 }.toSet()

        var matchedTokensCount = 0
        searchTokens.forEach { token ->
            if (titleTokens.contains(token) || lowerTitle.contains(token)) {
                matchedTokensCount++
            }
        }

        val tokenCoverage = if (searchTokens.isNotEmpty()) {
            matchedTokensCount.toDouble() / searchTokens.size.toDouble()
        } else {
            1.0
        }

        // Confidence calculation (0.6 ~ 0.98)
        val confidence = (0.5 + tokenCoverage * 0.45).coerceIn(0.1, 0.98)

        // Cluster ID generation based on essential alphanumeric tokens
        val clusterTokens = searchTokens.sorted().joinToString("_")
        val clusterId = if (clusterTokens.isNotBlank()) "cluster_$clusterTokens" else "cluster_default"

        return MatchResult(
            isMatched = tokenCoverage >= 0.6,
            confidenceScore = confidence,
            clusterId = clusterId,
            normalizedTitle = normTitle,
            rejectionReason = if (tokenCoverage < 0.6) "關鍵字覆蓋率不足 (${(tokenCoverage * 100).toInt()}%)" else null
        )
    }
}
