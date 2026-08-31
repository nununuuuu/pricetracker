package com.example.engine

import com.example.model.AnomalyReport
import com.example.model.NotificationLog

object NotificationEngine {

    private const val NOTIFICATION_COOLDOWN_MS = 6 * 3600 * 1000L // 6 hours

    /**
     * Determines whether to send a new notification for an anomaly report.
     * Prevents spamming for unchanged prices, but allows re-notifying if:
     * 1. Never notified before
     * 2. Price dropped even lower (e.g. NT$3,299 -> NT$2,499)
     * 3. Deal Score increased by >= 10 points
     * 4. Cooldown expired (6 hours) and deal is still active
     */
    fun shouldSendNotification(
        report: AnomalyReport,
        lastReport: AnomalyReport?
    ): Boolean {
        if (report.isFakeLowSuspected) return false
        if (report.dealScore < 60) return false

        if (lastReport == null || !lastReport.isNotified) {
            return true
        }

        val lastNotifiedPrice = lastReport.lastNotifiedPrice ?: lastReport.currentPrice
        val priceDroppedFurther = report.currentPrice < lastNotifiedPrice * 0.95 // at least 5% further drop

        val lastScore = lastReport.lastNotifiedDealScore ?: lastReport.dealScore
        val scoreJumped = report.dealScore >= lastScore + 10

        val now = System.currentTimeMillis()
        val lastNotifiedAt = lastReport.notifiedAt ?: 0L
        val cooldownPassed = (now - lastNotifiedAt) > NOTIFICATION_COOLDOWN_MS

        return priceDroppedFurther || scoreJumped || (cooldownPassed && report.dealScore >= 75)
    }

    fun buildNotificationLog(report: AnomalyReport): NotificationLog {
        return NotificationLog(
            reportId = report.id,
            productTitle = report.productTitle,
            platform = report.platform,
            currentPrice = report.currentPrice,
            referencePrice = report.referencePrice,
            discountPercent = report.deviationPercent,
            dealScore = report.dealScore,
            anomalyType = report.anomalyType,
            purchaseUrl = report.productUrl,
            notifiedAt = System.currentTimeMillis()
        )
    }
}
