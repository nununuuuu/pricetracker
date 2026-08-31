package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.PriceHistoryRecord
import com.example.ui.theme.*
import com.example.ui.viewmodel.ChartTimeRange
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun PriceHistoryChart(
    historyRecords: List<PriceHistoryRecord>,
    medianPrice: Double,
    selectedRange: ChartTimeRange,
    onRangeChange: (ChartTimeRange) -> Unit,
    modifier: Modifier = Modifier
) {
    val formatter = NumberFormat.getCurrencyInstance(Locale.TAIWAN).apply {
        maximumFractionDigits = 0
    }
    val dateFormat = SimpleDateFormat("MM/dd", Locale.TAIWAN)
    var selectedIndex by remember { mutableStateOf<Int?>(null) }

    // Filter records by selected range
    val now = System.currentTimeMillis()
    val cutoff = now - (selectedRange.days.toLong() * 86400000L)
    val filteredRecords = historyRecords.filter { it.timestamp >= cutoff }.ifEmpty { historyRecords }

    val prices = filteredRecords.map { it.price }
    val minPrice = (prices.minOrNull() ?: 1000.0).coerceAtLeast(100.0)
    val maxPrice = ((prices + listOf(medianPrice)).maxOrNull() ?: 10000.0).coerceAtLeast(minPrice + 500.0)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(HighDensitySurface)
            .border(1.dp, HighDensityBorderLight, RoundedCornerShape(16.dp))
            .padding(14.dp)
    ) {
        // Range Switcher Tabs
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "📈 價格歷史趨勢",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = SlateTextPrimary,
                fontSize = 14.sp
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                ChartTimeRange.entries.forEach { range ->
                    val isSelected = range == selectedRange
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                if (isSelected) PrimaryBlueLight
                                else HighDensitySurfaceElevated
                            )
                            .border(
                                1.dp,
                                if (isSelected) PrimaryBlueContainer else HighDensityBorderLight,
                                RoundedCornerShape(6.dp)
                            )
                            .clickable { onRangeChange(range) }
                            .padding(horizontal = 7.dp, vertical = 3.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = range.label,
                            fontSize = 10.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) PrimaryBlue else SlateTextSecondary
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Selected Point Info or Stats Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (selectedIndex != null && selectedIndex!! in filteredRecords.indices) {
                val pt = filteredRecords[selectedIndex!!]
                Text(
                    text = "${dateFormat.format(Date(pt.timestamp))}: ${formatter.format(pt.price)} (${pt.platform.displayName.split(" ").first()})",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = AnomalyGlitchRed,
                    fontSize = 11.5.sp
                )
            } else {
                Text(
                    text = "市場中位基準: ${formatter.format(medianPrice)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = SlateTextSecondary,
                    fontSize = 10.5.sp
                )
            }

            Text(
                text = "歷史低價: ${formatter.format(minPrice)}",
                style = MaterialTheme.typography.bodySmall,
                color = AnomalyHistoricGreen,
                fontWeight = FontWeight.Bold,
                fontSize = 10.5.sp
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Interactive Canvas Chart
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(filteredRecords) {
                        detectTapGestures { offset ->
                            if (filteredRecords.isNotEmpty()) {
                                val stepX = size.width / (filteredRecords.size - 1).coerceAtLeast(1)
                                val tappedIdx = (offset.x / stepX).toInt().coerceIn(0, filteredRecords.lastIndex)
                                selectedIndex = tappedIdx
                            }
                        }
                    }
            ) {
                val w = size.width
                val h = size.height
                val paddingBottom = 20.dp.toPx()
                val paddingTop = 10.dp.toPx()
                val drawH = h - paddingBottom - paddingTop

                if (filteredRecords.isEmpty()) return@Canvas

                // 1. Draw Market Median Baseline (Dotted line)
                if (medianPrice in minPrice..maxPrice) {
                    val medianNorm = ((maxPrice - medianPrice) / (maxPrice - minPrice)).toFloat()
                    val medianY = paddingTop + medianNorm * drawH
                    drawLine(
                        color = SlateTextMuted.copy(alpha = 0.35f),
                        start = Offset(0f, medianY),
                        end = Offset(w, medianY),
                        strokeWidth = 1.5.dp.toPx()
                    )
                }

                // 2. Draw Price Polyline
                val points = filteredRecords.mapIndexed { idx, record ->
                    val normX = if (filteredRecords.size > 1) idx.toFloat() / (filteredRecords.size - 1) else 0.5f
                    val normY = ((maxPrice - record.price) / (maxPrice - minPrice)).toFloat()
                    Offset(normX * w, paddingTop + normY * drawH)
                }

                val linePath = Path()
                points.forEachIndexed { i, pt ->
                    if (i == 0) linePath.moveTo(pt.x, pt.y) else linePath.lineTo(pt.x, pt.y)
                }

                drawPath(
                    path = linePath,
                    color = PrimaryBlue,
                    style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                )

                // 3. Draw Points
                points.forEachIndexed { idx, pt ->
                    val isCurrentSelected = idx == selectedIndex
                    drawCircle(
                        color = if (isCurrentSelected) AnomalyGlitchRed else PrimaryBlue,
                        radius = if (isCurrentSelected) 5.dp.toPx() else 3.dp.toPx(),
                        center = pt
                    )
                    drawCircle(
                        color = Color.White,
                        radius = if (isCurrentSelected) 2.5.dp.toPx() else 1.2.dp.toPx(),
                        center = pt
                    )
                }
            }
        }
    }
}

