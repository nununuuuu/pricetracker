package com.example.engine

import com.example.model.MatchMode
import com.example.model.SecondaryKeywordOperator
import com.example.model.SecondaryKeywordRules

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

    fun normalizeTitle(title: String, ignoreCase: Boolean = true): String {
        var clean = if (ignoreCase) title.lowercase(Locale.ROOT) else title
        // Remove brackets and common promo text
        clean = clean.replace(Regex("【.*?】|\\[.*?\\]|\\(.*?\\)|（.*?）"), " ")
        STOPWORDS.forEach { word ->
            clean = clean.replace(word.lowercase(Locale.ROOT), " ")
        }
        // Replace special characters with spaces
        clean = clean.replace(Regex("[^a-zA-Z0-9\\u4e00-\\u9fa5]"), " ")
        return clean.replace(Regex("\\s+"), " ").trim()
    }

    fun matchProduct(
        productTitle: String,
        searchKeyword: String,
        matchMode: MatchMode = MatchMode.CONTAINS,
        mustIncludeWords: List<String>,
        anyIncludeWords: List<String>,
        excludeKeywords: List<String>,
        ignoreCase: Boolean = true,
        ignoreWhitespace: Boolean = false
    ): MatchResult {
        fun comparable(value: String): String {
            val cased = if (ignoreCase) value.lowercase(Locale.ROOT) else value
            return if (ignoreWhitespace) cased.replace(Regex("\\s+"), "") else cased
        }
        val lowerTitle = comparable(productTitle)
        val normTitle = normalizeTitle(productTitle, ignoreCase).let { if (ignoreWhitespace) it.replace(" ", "") else it }

        // 1. Exclude keywords check
        excludeKeywords.forEach { excl ->
            if (excl.isNotBlank() && lowerTitle.contains(comparable(excl.trim()))) {
                return MatchResult(
                    isMatched = false,
                    confidenceScore = 0.0,
                    clusterId = "",
                    normalizedTitle = normTitle,
                    rejectionReason = "命中排除關鍵字: $excl"
                )
            }
        }

        // 2. Ordered secondary rules form groups: groups are AND, terms inside
        // each group are OR. [A AND, B OR, C AND] means (A OR B) AND C.
        val secondaryRules = SecondaryKeywordRules.decode(mustIncludeWords, anyIncludeWords)
        val unmatchedGroups = SecondaryKeywordRules.groups(secondaryRules)
            .filter { group -> group.none { keyword -> lowerTitle.contains(comparable(keyword.trim())) } }
        if (unmatchedGroups.isNotEmpty()) {
            return MatchResult(
                isMatched = false,
                confidenceScore = 0.0,
                clusterId = "",
                normalizedTitle = normTitle,
                rejectionReason = "未符合關鍵字群組: [${unmatchedGroups.joinToString { it.joinToString(" OR ") }}]"
            )
        }

        // 3. EXACT is deliberately strict after the same normalisation used for
        // product titles; promotional text has already been removed above.
        val normalizedKeyword = normalizeTitle(searchKeyword, ignoreCase).let { if (ignoreWhitespace) it.replace(" ", "") else it }
        if (matchMode == MatchMode.EXACT) {
            val exact = normalizedKeyword.isNotBlank() && normTitle == normalizedKeyword
            return MatchResult(
                isMatched = exact,
                confidenceScore = if (exact) 0.98 else 0.0,
                clusterId = if (exact) "cluster_${normalizedKeyword.replace(" ", "_")}" else "",
                normalizedTitle = normTitle,
                rejectionReason = if (exact) null else "完全符合模式：商品標題與關鍵字不同"
            )
        }

        // 4. Calculate token overlap similarity and confidence
        val searchTokens = normalizeTitle(searchKeyword, ignoreCase).let { if (ignoreWhitespace) it.replace(" ", "") else it }.split(" ").filter { it.length >= 2 }
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
