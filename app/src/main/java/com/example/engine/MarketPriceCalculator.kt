package com.example.engine

import com.example.model.MarketStats
import com.example.model.PlatformType
import com.example.model.PriceHistoryRecord
import com.example.model.ProductItem

object MarketPriceCalculator {

    fun calculateMarketStats(
        clusterId: String,
        currentProducts: List<ProductItem>,
        historyRecords: List<PriceHistoryRecord>
    ): MarketStats {
        val validCurrentPrices = currentProducts
            .filter { it.currentPrice > 0 }
            .map { it.currentPrice }

        val allHistoryPrices = historyRecords
            .filter { it.price > 0 }
            .map { it.price }

        val combinedPrices = (validCurrentPrices + allHistoryPrices).filter { it > 0 }

        if (combinedPrices.isEmpty()) {
            return MarketStats(
                clusterId = clusterId,
                currentLowest = 0.0,
                currentHighest = 0.0,
                medianPrice = 0.0,
                averagePrice = 0.0,
                avg7Day = 0.0,
                avg30Day = 0.0,
                avg90Day = 0.0,
                sampleCount = 0,
                platformPrices = emptyMap()
            )
        }

        val sortedPrices = combinedPrices.sorted()
        val median = calculateMedian(sortedPrices)
        val average = sortedPrices.average()

        val now = System.currentTimeMillis()
        val day7Millis = 7L * 86400000L
        val day30Millis = 30L * 86400000L
        val day90Millis = 90L * 86400000L

        val p7d = historyRecords.filter { now - it.timestamp <= day7Millis && it.price > 0 }.map { it.price }
        val p30d = historyRecords.filter { now - it.timestamp <= day30Millis && it.price > 0 }.map { it.price }
        val p90d = historyRecords.filter { now - it.timestamp <= day90Millis && it.price > 0 }.map { it.price }

        val avg7d = if (p7d.isNotEmpty()) calculateMedian(p7d.sorted()) else median
        val avg30d = if (p30d.isNotEmpty()) calculateMedian(p30d.sorted()) else median
        val avg90d = if (p90d.isNotEmpty()) calculateMedian(p90d.sorted()) else median

        val platformMap = mutableMapOf<PlatformType, Double>()
        currentProducts.forEach { prod ->
            if (prod.currentPrice > 0) {
                val existing = platformMap[prod.platform]
                if (existing == null || prod.currentPrice < existing) {
                    platformMap[prod.platform] = prod.currentPrice
                }
            }
        }

        return MarketStats(
            clusterId = clusterId,
            currentLowest = sortedPrices.first(),
            currentHighest = sortedPrices.last(),
            medianPrice = median,
            averagePrice = average,
            avg7Day = avg7d,
            avg30Day = avg30d,
            avg90Day = avg90d,
            sampleCount = combinedPrices.size,
            platformPrices = platformMap
        )
    }

    private fun calculateMedian(sortedList: List<Double>): Double {
        if (sortedList.isEmpty()) return 0.0
        val size = sortedList.size
        return if (size % 2 == 1) {
            sortedList[size / 2]
        } else {
            (sortedList[size / 2 - 1] + sortedList[size / 2]) / 2.0
        }
    }
}
