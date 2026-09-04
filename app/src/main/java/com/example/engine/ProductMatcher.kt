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
        val comparisonTitle = if (ignoreWhitespace) normTitle else lowerTitle
        val normalizedSearch = normalizeTitle(searchKeyword, ignoreCase)
        val normalizedSearchTokens = normalizedSearch
            .split(" ")
            .filter { it.length >= 2 }
        val modelTokens = normalizedSearchTokens.filter(::isModelIdentifier).distinct()

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

        // A broad category or a single chipset/model-family is useful for
        // browsing, but it is not enough to claim two listings are the same
        // product.  Reject it from price comparison rather than calculating a
        // misleading "market lowest" price from incompatible variants.
        if (normalizedSearchTokens.size < 2) {
            return MatchResult(
                isMatched = false,
                confidenceScore = 0.0,
                clusterId = "",
                normalizedTitle = normTitle,
                rejectionReason = "比價關鍵字不足以識別同一型號；請至少輸入兩組型號／規格代碼"
            )
        }

        val missingModelTokens = modelTokens.filterNot { comparisonTitle.contains(it) }
        if (missingModelTokens.isNotEmpty()) {
            return MatchResult(
                isMatched = false,
                confidenceScore = 0.0,
                clusterId = "",
                normalizedTitle = normTitle,
                rejectionReason = "商品未完整符合型號／規格：${missingModelTokens.joinToString()}"
            )
        }

        // 4. Calculate token overlap similarity and confidence
        val searchTokens = if (ignoreWhitespace) {
            listOf(normalizedSearch.replace(" ", "")).filter(String::isNotBlank)
        } else {
            normalizedSearchTokens
        }
        val titleTokens = normTitle.split(" ").filter { it.length >= 2 }.toSet()

        var matchedTokensCount = 0
        searchTokens.forEach { token ->
            if (titleTokens.contains(token) || comparisonTitle.contains(token)) {
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

        // The full, strictly matched query signature identifies the comparison
        // cluster.  A product that only shares a category or model family was
        // rejected above and can never join this cluster.
        val clusterTokens = normalizedSearchTokens.sorted().joinToString("_")
        val clusterId = if (clusterTokens.isNotBlank()) "cluster_$clusterTokens" else "cluster_default"

        return MatchResult(
            isMatched = tokenCoverage >= 1.0,
            confidenceScore = confidence,
            clusterId = clusterId,
            normalizedTitle = normTitle,
            rejectionReason = if (tokenCoverage < 1.0) "商品未完整符合搜尋關鍵字 (${(tokenCoverage * 100).toInt()}%)" else null
        )
    }

    /** A model identifier contains letters and numbers, or is a long numeric SKU. */
    private fun isModelIdentifier(token: String): Boolean {
        val compact = token.replace(Regex("[^a-zA-Z0-9]"), "")
        val hasLetter = compact.any(Char::isLetter)
        val hasDigit = compact.any(Char::isDigit)
        return (compact.length >= 3 && hasLetter && hasDigit) ||
            (compact.length >= 4 && compact.all(Char::isDigit))
    }
}
