package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.MonitorRule
import com.example.model.PlatformType
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.DealHunterUiState
import com.example.ui.viewmodel.DealHunterViewModel
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    uiState: DealHunterUiState,
    viewModel: DealHunterViewModel,
    onNavigateToDeals: () -> Unit,
    onNavigateToMonitors: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activePlatform = uiState.selectedPlatformFilter
    val monitoredRules = if (activePlatform != null) {
        uiState.monitors.filter { it.enabledPlatforms.contains(activePlatform) }
    } else {
        uiState.monitors
    }

    // Calculate how many monitored items reached target price
    val targetReachedCount = remember(uiState.monitors, uiState.products) {
        uiState.monitors.count { rule ->
            val matchedProds = uiState.products.filter { it.matchedMonitorId == rule.id || it.title.contains(rule.searchKeyword, ignoreCase = true) }
            val minPrice = matchedProds.minOfOrNull { it.currentPrice }
            rule.maxFixedPrice != null && minPrice != null && minPrice <= rule.maxFixedPrice
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 10.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Scanning Engine Banner with luminous gradient
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, PrimaryNeon.copy(alpha = 0.2f), RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    PrimaryNeonLight,
                                    SecondaryCyanLight.copy(alpha = 0.5f)
                                )
                            )
                        )
                        .padding(14.dp)
                ) {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(PrimaryNeon)
                                        .border(1.dp, PrimaryNeonContainer, RoundedCornerShape(10.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(text = "⚡", fontSize = 18.sp)
                                }
                                Column {
                                    Text(
                                        text = "全天候即時撿漏引擎",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Black,
                                        color = SlateTextPrimary,
                                        fontSize = 15.sp
                                    )
                                    Text(
                                        text = "支援 15 大電商跨平台即時價格追蹤與異常雷達",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = SlateTextSecondary,
                                        fontSize = 11.sp
                                    )
                                }
                            }

                            Button(
                                onClick = { viewModel.triggerQuickScan() },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = PrimaryNeon
                                ),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                enabled = !uiState.scannerStatus.isScanning,
                                modifier = Modifier.height(32.dp)
                            ) {
                                if (uiState.scannerStatus.isScanning) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(14.dp),
                                        color = Color.White,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Filled.Refresh,
                                        contentDescription = "掃描",
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(text = "全網掃描", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        // Live scanning progress bar
                        AnimatedVisibility(visible = uiState.scannerStatus.isScanning) {
                            Column(modifier = Modifier.padding(top = 10.dp)) {
                                LinearProgressIndicator(
                                    progress = { uiState.scannerStatus.progressPercent },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(4.dp)
                                        .clip(RoundedCornerShape(2.dp)),
                                    color = PrimaryNeon,
                                    trackColor = PrimaryNeonLight
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = uiState.scannerStatus.currentPlatform,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = PrimaryNeon,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }

        // Metrics Grid (4 Stat Cards)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatCard(
                        title = "我監控的商品",
                        value = "${uiState.monitors.count { it.isEnabled }} / ${uiState.monitors.size}",
                        iconSymbol = "🎯",
                        accentColor = PrimaryBlue,
                        modifier = Modifier.weight(1f),
                        deltaBadge = "啟用中"
                    )
                    StatCard(
                        title = "目標價已達標",
                        value = "$targetReachedCount 項",
                        iconSymbol = "🟢",
                        accentColor = AnomalyHistoricGreen,
                        modifier = Modifier.weight(1f),
                        deltaBadge = "可立即入手"
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatCard(
                        title = "全網破盤異常",
                        value = "${uiState.allDeals.size}",
                        iconSymbol = "🚨",
                        accentColor = AnomalyGlitchRed,
                        modifier = Modifier.weight(1f),
                        deltaBadge = "Deals 雷達"
                    )
                    StatCard(
                        title = "已索引價格數",
                        value = "${uiState.totalScannedCount}",
                        iconSymbol = "📦",
                        accentColor = AnomalyClearanceOrange,
                        modifier = Modifier.weight(1f),
                        deltaBadge = "15 平台"
                    )
                }
            }
        }

        // 15 Platform Latency & Diagnostics Strip
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, HighDensityBorderLight, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = HighDensitySurface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "15 大電商平台狀態 (點擊可快速過濾)",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = SlateTextMuted,
                            fontSize = 10.sp,
                            letterSpacing = 0.5.sp
                        )
                        TextButton(
                            onClick = { viewModel.runDiagnostics() },
                            contentPadding = PaddingValues(0.dp),
                            modifier = Modifier.height(24.dp)
                        ) {
                            Text(text = "診斷延遲", fontSize = 11.sp, color = PrimaryBlue, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        PlatformType.entries.forEach { platform ->
                            val status = uiState.platformStatuses[platform]
                            val isUnsupported = status?.limitationsNote?.contains("尚未") == true || status?.isEnabled == false
                            val isOnline = status?.isOnline == true
                            val latency = status?.responseTimeMs ?: 0
                            val isCurrentFiltered = uiState.selectedPlatformFilter == platform

                            Column(
                                modifier = Modifier
                                    .widthIn(min = 78.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (isCurrentFiltered) PrimaryBlueLight else HighDensitySurfaceElevated
                                    )
                                    .border(
                                        1.dp,
                                        if (isCurrentFiltered) PrimaryBlue else HighDensityBorderLight,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable { viewModel.setSelectedPlatformFilter(if (isCurrentFiltered) null else platform) }
                                    .padding(vertical = 6.dp, horizontal = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(5.dp)
                                            .clip(CircleShape)
                                            .background(if (isUnsupported) SlateTextMuted else if (isOnline) AnomalyHistoricGreen else AnomalyGlitchRed)
                                    )
                                    Text(
                                        text = platform.displayName.split(" ").first(),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isCurrentFiltered) PrimaryBlue else SlateTextPrimary
                                    )
                                }
                                Spacer(modifier = Modifier.height(1.dp))
                                Text(
                                    text = if (isUnsupported) "未實作" else if (latency > 0) "${latency}ms" else "未測試",
                                    fontSize = 9.5.sp,
                                    color = if (isCurrentFiltered) PrimaryBlue else SlateTextSecondary,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }

        // Section Title: My Monitored Items (我監控中的商品)
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "🎯 我監控中的商品",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        color = SlateTextPrimary,
                        fontSize = 15.sp
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(PrimaryBlueLight)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "${monitoredRules.size} 個商品",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryBlue
                        )
                    }
                }

                TextButton(
                    onClick = onNavigateToMonitors,
                    contentPadding = PaddingValues(0.dp),
                    modifier = Modifier.height(28.dp)
                ) {
                    Text(
                        text = "管理規則 ⚙️",
                        fontSize = 11.sp,
                        color = PrimaryBlue,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Monitored Product Cards
        if (monitoredRules.isEmpty()) {
            item {
                EmptyStateView(
                    iconSymbol = "🎯",
                    title = "目前無監控中的商品",
                    subtitle = "點擊下方按鈕，貼上商品網址或輸入關鍵字，開始為您全天候監控 15 大電商價格！",
                    actionText = "➕ 新增監控商品",
                    onActionClick = { viewModel.openCreateMonitorSheet() }
                )
            }
        } else {
            items(monitoredRules, key = { it.id }) { rule ->
                DashboardMonitoredItemCard(
                    rule = rule,
                    uiState = uiState,
                    onToggle = { isEnabled -> viewModel.toggleMonitor(rule.id, isEnabled) },
                    onScanNow = { viewModel.triggerScanForSingleMonitor(rule) },
                    onClick = {
                        val matchedProds = uiState.products.filter { it.matchedMonitorId == rule.id || it.title.contains(rule.searchKeyword, ignoreCase = true) }
                        val lowestProd = matchedProds.minByOrNull { it.currentPrice }
                        val matchedDeal = uiState.allDeals.find { it.monitorRuleId == rule.id || it.productId == lowestProd?.id }
                        if (lowestProd != null) {
                            viewModel.openProductDetail(lowestProd, matchedDeal)
                        } else {
                            viewModel.openEditMonitorSheet(rule)
                        }
                    }
                )
            }
        }

        // Bottom CTA: Quick link to Deals radar
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToDeals() }
                    .border(1.dp, AnomalyGlitchRed.copy(alpha = 0.2f), RoundedCornerShape(14.dp)),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = AnomalyRoseContainer.copy(alpha = 0.5f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(text = "🚨", fontSize = 20.sp)
                        Column {
                            Text(
                                text = "全網價格異常雷達 (Deals)",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = SlateTextPrimary,
                                fontSize = 13.sp
                            )
                            Text(
                                text = "已偵測 ${uiState.allDeals.size} 筆標錯價/跳水/破盤商品 (不限監控中)",
                                style = MaterialTheme.typography.bodySmall,
                                color = SlateTextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Icon(
                        imageVector = Icons.Filled.ChevronRight,
                        contentDescription = "前往 Deals",
                        tint = AnomalyGlitchRed,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

/**
 * High-density card representing a Monitored Product in Dashboard
 */
@Composable
fun DashboardMonitoredItemCard(
    rule: MonitorRule,
    uiState: DealHunterUiState,
    onToggle: (Boolean) -> Unit,
    onScanNow: () -> Unit,
    onClick: () -> Unit
) {
    val formatter = remember {
        NumberFormat.getCurrencyInstance(Locale.TAIWAN).apply { maximumFractionDigits = 0 }
    }

    // Match products for this monitor
    val matchedProducts = remember(uiState.products, rule) {
        uiState.products.filter { it.matchedMonitorId == rule.id || (rule.searchKeyword.isNotBlank() && it.title.contains(rule.searchKeyword, ignoreCase = true)) }
    }

    val lowestProduct = remember(matchedProducts) {
        matchedProducts.minByOrNull { it.currentPrice }
    }

    val activeDeal = remember(uiState.allDeals, rule, lowestProduct) {
        uiState.allDeals.find { it.monitorRuleId == rule.id || (lowestProduct != null && it.productId == lowestProduct.id) }
    }

    val isTargetReached = remember(lowestProduct, rule.maxFixedPrice) {
        rule.maxFixedPrice != null && lowestProduct != null && lowestProduct.currentPrice <= rule.maxFixedPrice
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .border(
                1.dp,
                if (isTargetReached) AnomalyHistoricGreen.copy(alpha = 0.4f) else HighDensityBorderLight,
                RoundedCornerShape(14.dp)
            ),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = HighDensitySurface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Header Row: Track Badge + Title + Switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    if (rule.trackMode == "URL" || rule.targetUrl.isNotBlank()) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(PrimaryNeonLight)
                                .padding(horizontal = 5.dp, vertical = 1.5.dp)
                        ) {
                            Text(
                                text = "🔗 網址追蹤",
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryNeon
                            )
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(PrimaryBlueLight)
                                .padding(horizontal = 5.dp, vertical = 1.5.dp)
                        ) {
                            Text(
                                text = "🔍 關鍵字比價",
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryBlue
                            )
                        }
                    }

                    Text(
                        text = rule.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = SlateTextPrimary,
                        fontSize = 13.5.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Switch(
                    checked = rule.isEnabled,
                    onCheckedChange = onToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = PrimaryBlue
                    ),
                    modifier = Modifier.height(24.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Price Comparison Section
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(HighDensitySurfaceElevated)
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "目前全網最低價",
                        style = MaterialTheme.typography.labelSmall,
                        color = SlateTextMuted,
                        fontSize = 10.sp
                    )
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (lowestProduct != null) {
                            Text(
                                text = formatter.format(lowestProduct.currentPrice),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                color = if (isTargetReached) AnomalyHistoricGreen else SlateTextPrimary
                            )
                            PlatformBadge(platform = lowestProduct.platform, compact = true)
                        } else {
                            Text(
                                text = "掃描比價中...",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = SlateTextSecondary
                            )
                        }
                    }
                }

                // Target Price / Threshold Status
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "設定目標入手價",
                        style = MaterialTheme.typography.labelSmall,
                        color = SlateTextMuted,
                        fontSize = 10.sp
                    )
                    if (rule.maxFixedPrice != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "≤ ${formatter.format(rule.maxFixedPrice)}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = SlateTextPrimary
                            )
                            if (isTargetReached) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(AnomalyEmeraldContainer)
                                        .padding(horizontal = 4.dp, vertical = 1.dp)
                                ) {
                                    Text(
                                        text = "🟢 已達標",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = AnomalyHistoricGreen
                                    )
                                }
                            }
                        }
                    } else {
                        Text(
                            text = "低於市價 -${rule.discountThresholdPercent.toInt()}%",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = AnomalyClearanceOrange
                        )
                    }
                }
            }

            // Anomaly / Deal Score Callout if present
            if (activeDeal != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(AnomalyRoseContainer)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(text = "🚨", fontSize = 11.sp)
                        Text(
                            text = "發現異常價格: ${activeDeal.dealScore}分 (${activeDeal.anomalyType.title}) 較市價偏離 ${activeDeal.deviationPercent.toInt()}%",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = AnomalyGlitchRed
                        )
                    }
                    Text(
                        text = "查看 >",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = AnomalyGlitchRed
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Footer: Platform List & Scan Now button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "監控平台:",
                        fontSize = 10.sp,
                        color = SlateTextMuted
                    )
                    rule.enabledPlatforms.take(4).forEach { platform ->
                        PlatformBadge(platform = platform, compact = true)
                    }
                    if (rule.enabledPlatforms.size > 4) {
                        Text(
                            text = "+${rule.enabledPlatforms.size - 4}",
                            fontSize = 9.5.sp,
                            color = SlateTextMuted
                        )
                    }
                }

                OutlinedButton(
                    onClick = onScanNow,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.height(26.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryBlue)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Refresh,
                        contentDescription = "掃描",
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(text = "即時比價", fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
