package com.example.engine

import com.example.model.MatchMode
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProductMatcherTest {

    @Test
    fun `rejects a broad single model family for cross-store price comparison`() {
        val result = ProductMatcher.matchProduct(
            productTitle = "技嘉 B860M GAMING WIFI6 主機板",
            searchKeyword = "B860M",
            matchMode = MatchMode.CONTAINS,
            mustIncludeWords = emptyList(),
            anyIncludeWords = emptyList(),
            excludeKeywords = emptyList()
        )

        assertFalse(result.isMatched)
        assertTrue(result.rejectionReason!!.contains("不足以識別"))
    }

    @Test
    fun `accepts only an exact model identity with all model tokens present`() {
        val matched = ProductMatcher.matchProduct(
            productTitle = "MSI DSAJBU-A900J02SS 電競主機板",
            searchKeyword = "DSAJBU A900J02SS",
            matchMode = MatchMode.CONTAINS,
            mustIncludeWords = emptyList(),
            anyIncludeWords = emptyList(),
            excludeKeywords = emptyList(),
            ignoreWhitespace = true
        )
        val mismatched = ProductMatcher.matchProduct(
            productTitle = "MSI DSAJBU-A900J02TT 電競主機板",
            searchKeyword = "DSAJBU A900J02SS",
            matchMode = MatchMode.CONTAINS,
            mustIncludeWords = emptyList(),
            anyIncludeWords = emptyList(),
            excludeKeywords = emptyList(),
            ignoreWhitespace = true
        )

        assertTrue(matched.isMatched)
        assertFalse(mismatched.isMatched)
    }
}
