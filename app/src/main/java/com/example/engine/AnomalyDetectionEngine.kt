package com.example.engine

import com.example.model.*
import kotlin.math.abs
import kotlin.math.roundToInt

data class AnomalyEvaluation(
    val isAnomaly: Boolean,
    val deviationPercent: Double,
    val referencePrice: Double,
    val dealScore: Int,
    val confidenceScore: Int,
    val anomalyType: AnomalyType,
    val reasons: List<String>,
    val isFakeLowSuspected: Boolean,
    val shouldNotify: Boolean
)

object AnomalyDetectionEngine {

    fun evaluateProduct(
        product: ProductItem,
        rule: MonitorRule,
        marketStats: MarketStats,
        isFakeLow: Boolean,
        fakeLowDetails: FalsePriceCheckResult
    ): AnomalyEvaluation {
        val currentPrice = product.currentPrice
        val refPrice = if (marketStats.medianPrice > 0) marketStats.medianPrice else product.originalPrice
        val deviationPercent = if (refPrice > 0) {
            ((currentPrice - refPrice) / refPrice) * 100.0
        } else {
            0.0
        }

        val reasons = mutableListOf<String>()

        // 1. Check False Low Price
        if (isFakeLow) {
            reasons.add(fakeLowDetails.reason)
            return AnomalyEvaluation(
                isAnomaly = false,
                deviationPercent = deviationPercent,
                referencePrice = refPrice,
                dealScore = 25,
                confidenceScore = 30,
                anomalyType = AnomalyType.SUSPECTED_FAKE_LOW_PRICE,
                reasons = reasons,
                isFakeLowSuspected = true,
                shouldNotify = false
            )
        }

        // 2. Base Deal Score computation
        var baseScore = 40.0

        // Price deviation contribution (up to 45 points)
        val dropPercent = -deviationPercent // positive when price dropped
        if (dropPercent > 0) {
            when {
                dropPercent >= 60.0 -> baseScore += 45.0 // e.g. -60% -> +45
                dropPercent >= 45.0 -> baseScore += 38.0
                dropPercent >= 30.0 -> baseScore += 28.0
                dropPercent >= 20.0 -> baseScore += 18.0
                dropPercent >= 10.0 -> baseScore += 10.0
                else -> baseScore += dropPercent * 0.8
            }
            reasons.add("較跨平台中位價 (NT$${refPrice.toInt()}) 便宜 ${dropPercent.roundToInt()}%")
        } else {
            baseScore -= abs(deviationPercent) * 0.5
            reasons.add("目前價格高於或等於參考行情")
        }

        // 3. Historical context contribution (up to 15 points)
        if (marketStats.avg30Day > 0 && currentPrice < marketStats.avg30Day * 0.8) {
            baseScore += 10.0
            reasons.add("顯著低於 30 日常態價格 (NT$${marketStats.avg30Day.toInt()})")
        }
        if (marketStats.currentLowest > 0 && currentPrice <= marketStats.currentLowest) {
            baseScore += 5.0
            reasons.add("刷新追蹤歷史最低價 (NT$${currentPrice.toInt()})")
        }

        // 4. New product surge bonus
        if (product.isNewItem && dropPercent >= 30.0) {
            baseScore += 5.0
            reasons.add("剛上架新商品，具高度時效性")
        }

        // 5. Confidence Score computation (0 - 100)
        var confidence = 70.0
        if (marketStats.sampleCount >= 8) {
            confidence += 15.0
            reasons.add("具備充足比價樣本 (${marketStats.sampleCount} 筆)")
        } else if (marketStats.sampleCount >= 3) {
            confidence += 8.0
        } else {
            confidence -= 20.0
            reasons.add("市場價格樣本偏少，信心度校正")
        }

        if (product.sellerRating >= 4.8) {
            confidence += 10.0
            reasons.add("高評分優良賣家 (${product.sellerRating} ★)")
        }

        val finalConfidence = confidence.coerceIn(20.0, 98.0).roundToInt()
        val finalDealScore = (baseScore * (finalConfidence / 100.0 * 0.4 + 0.6)).coerceIn(0.0, 100.0).roundToInt()

        // 6. Anomaly classification
        val anomalyType = when {
            dropPercent >= 55.0 -> AnomalyType.SUSPECTED_GLITCH_PRICE
            dropPercent >= 38.0 -> AnomalyType.CLEARANCE_PRICE
            dropPercent >= 25.0 && product.isNewItem -> AnomalyType.FLASH_SALE
            currentPrice <= marketStats.currentLowest && marketStats.sampleCount > 3 -> AnomalyType.HISTORIC_LOW
            dropPercent >= 20.0 && marketStats.platformPrices.size >= 2 -> AnomalyType.CROSS_PLATFORM_ANOMALY
            dropPercent >= 15.0 -> AnomalyType.GOOD_PRICE
            marketStats.sampleCount < 2 -> AnomalyType.INSUFFICIENT_DATA
            else -> AnomalyType.GOOD_PRICE
        }

        // 7. Check trigger conditions
        val matchesFixedPrice = rule.maxFixedPrice != null && currentPrice <= rule.maxFixedPrice
        val matchesPercentage = dropPercent >= rule.discountThresholdPercent

        val isTriggered = when (rule.thresholdMode) {
            PriceThresholdMode.PERCENTAGE -> matchesPercentage
            PriceThresholdMode.FIXED_PRICE -> matchesFixedPrice
            PriceThresholdMode.BOTH_OR -> matchesPercentage || matchesFixedPrice
            PriceThresholdMode.BOTH_AND -> matchesPercentage && matchesFixedPrice
        }

        val isAnomaly = (finalDealScore >= 60 && isTriggered) || finalDealScore >= 85
        val shouldNotify = isAnomaly && finalDealScore >= rule.minDealScore

        return AnomalyEvaluation(
            isAnomaly = isAnomaly,
            deviationPercent = deviationPercent,
            referencePrice = refPrice,
            dealScore = finalDealScore,
            confidenceScore = finalConfidence,
            anomalyType = anomalyType,
            reasons = reasons,
            isFakeLowSuspected = false,
            shouldNotify = shouldNotify
        )
    }
}
