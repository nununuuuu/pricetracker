package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.model.PlatformType
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.DealHunterViewModel
import com.example.ui.viewmodel.ProductDetailState
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductDetailScreen(
    state: ProductDetailState?,
    viewModel: DealHunterViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val formatter = NumberFormat.getCurrencyInstance(Locale.TAIWAN).apply {
        maximumFractionDigits = 0
    }

    if (state == null) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("尚未選擇任何商品", style = MaterialTheme.typography.titleMedium, color = SlateTextPrimary)
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onBack,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
            ) {
                Text("返回撿漏雷達")
            }
        }
        return
    }

    val product = state.product
    val anomaly = state.anomalyReport
    val stats = state.marketStats

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Top Bar Navigation
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(HighDensitySurface)
                        .border(1.dp, HighDensityBorderLight, RoundedCornerShape(8.dp))
                ) {
                    Icon(imageVector = Icons.Filled.ArrowBack, contentDescription = "返回", tint = SlateTextPrimary, modifier = Modifier.size(16.dp))
                }
                Text(
                    text = "跨平台商品價格診斷",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    color = SlateTextPrimary,
                    fontSize = 15.sp
                )
                Box(modifier = Modifier.size(34.dp))
            }
        }

        // Product Header Card
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
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(88.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(HighDensitySurfaceElevated)
                                .border(1.dp, HighDensityBorderLight, RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (product.imageUrl.isNotBlank()) {
                                AsyncImage(
                                    model = product.imageUrl,
                                    contentDescription = product.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Text(text = product.platform.iconSymbol, fontSize = 32.sp)
                            }
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                PlatformBadge(platform = product.platform)
                                if (anomaly != null) {
                                    DealScoreBadge(score = anomaly.dealScore)
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = product.title,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = SlateTextPrimary,
                                fontSize = 13.sp,
                                lineHeight = 18.sp
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = "賣家: ${product.sellerName} (${product.sellerRating} ★)",
                                style = MaterialTheme.typography.bodySmall,
                                color = SlateTextMuted,
                                fontSize = 10.5.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = HighDensityBorderLight)
                    Spacer(modifier = Modifier.height(10.dp))

                    // Current Deal Price vs Market Median
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "當前異常特惠價",
                                style = MaterialTheme.typography.labelSmall,
                                color = SlateTextMuted,
                                fontSize = 10.sp
                            )
                            Text(
                                text = formatter.format(product.currentPrice),
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Black,
                                color = if (anomaly != null && anomaly.dealScore >= 75) AnomalyGlitchRed else PrimaryBlue,
                                fontSize = 24.sp
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "全網中位行情",
                                style = MaterialTheme.typography.labelSmall,
                                color = SlateTextMuted,
                                fontSize = 10.sp
                            )
                            Text(
                                text = formatter.format(stats.medianPrice),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = SlateTextSecondary,
                                fontSize = 15.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Buy Now Button
                    Button(
                        onClick = {
                            val targetUrl = product.url.takeIf { it.isRealProductUrl() } ?: return@Button
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(targetUrl))
                            try {
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                // Fallback
                            }
                        },
                        enabled = product.url.isRealProductUrl(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (anomaly != null && anomaly.dealScore >= 90) AnomalyGlitchRed else PrimaryBlue
                        )
                    ) {
                        Icon(imageVector = Icons.Filled.ShoppingCart, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "前往 ${product.platform.displayName.split(" ").first()} 購買本商品", fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                    }
                }
            }
        }

        // Anomaly Evaluation & Reasons Card
        if (anomaly != null && anomaly.reasons.isNotEmpty()) {
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
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "異常判定與可解釋性 (Score ${anomaly.dealScore})",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryBlue,
                                fontSize = 13.sp
                            )
                            ConfidenceBadge(confidenceScore = anomaly.confidenceScore)
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        anomaly.reasons.forEach { reason ->
                            Row(
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.padding(vertical = 2.dp)
                            ) {
                                Text(text = "✓", color = AnomalyHistoricGreen, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Text(
                                    text = reason,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = SlateTextSecondary,
                                    fontSize = 11.5.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // Cross-Platform Price Comparison Table
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
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "台灣各大電商即時比價矩陣",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = SlateTextPrimary,
                                fontSize = 13.sp
                            )
                            Text(
                                text = "含 9 大主流電商與 FindPrice 比價資料 (依售價由低至高排序)",
                                style = MaterialTheme.typography.bodySmall,
                                color = SlateTextSecondary,
                                fontSize = 10.5.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    val sortedPlatforms = PlatformType.entries.sortedBy { platform ->
                        val platformProd = state.crossPlatformPrices.find { it.platform == platform }
                        val price = platformProd?.currentPrice ?: stats.platformPrices[platform] ?: Double.MAX_VALUE
                        if (price > 0) price else Double.MAX_VALUE
                    }

                    sortedPlatforms.forEach { platform ->
                        val platformProd = state.crossPlatformPrices.find { it.platform == platform }
                        val price = platformProd?.currentPrice ?: stats.platformPrices[platform] ?: 0.0
                        val isLowest = price > 0 && price == stats.currentLowest

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isLowest) AnomalyRoseContainer.copy(alpha = 0.3f) else Color.Transparent)
                                .padding(vertical = 5.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                PlatformBadge(platform = platform)
                                Text(
                                    text = if (price > 0) platformProd?.sellerName ?: "官方授權專案" else "暫無報價",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SlateTextSecondary,
                                    fontSize = 10.5.sp,
                                    maxLines = 1
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                if (isLowest) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(AnomalyRoseContainer)
                                            .padding(horizontal = 5.dp, vertical = 1.5.dp)
                                    ) {
                                        Text(text = "👑 最低價", color = AnomalyGlitchRed, fontSize = 9.sp, fontWeight = FontWeight.Black)
                                    }
                                }
                                Text(
                                    text = if (price > 0) formatter.format(price) else "—",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (isLowest) FontWeight.Black else FontWeight.Medium,
                                    color = if (isLowest) AnomalyGlitchRed else SlateTextPrimary,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // MCP Tool & AI Export Card (from mcp-taiwan-price-compare)
        item {
            var showMcpDialog by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, PrimaryNeon.copy(alpha = 0.3f), RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = HighDensitySurface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(text = "🤖", fontSize = 20.sp)
                            Column {
                                Text(
                                    text = "MCP 比價工具與 AI 提詞",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = SlateTextPrimary,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = "符合 MCP (Model Context Protocol) 規範之比價數據",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SlateTextSecondary,
                                    fontSize = 10.5.sp
                                )
                            }
                        }

                        Button(
                            onClick = { showMcpDialog = true },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PrimaryNeon
                            ),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Text(text = "檢視與匯出", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            if (showMcpDialog) {
                var selectedTab by androidx.compose.runtime.remember { androidx.compose.runtime.mutableIntStateOf(0) }
                val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
                val mcpJson = com.example.engine.McpExportHelper.formatToMcpJson(
                    query = product.normalizedTitle,
                    products = state.crossPlatformPrices.ifEmpty { listOf(product) },
                    stats = stats,
                    anomalies = if (anomaly != null) listOf(anomaly) else emptyList()
                )
                val markdownSummary = com.example.engine.McpExportHelper.formatToMarkdownSummary(
                    query = product.normalizedTitle,
                    products = state.crossPlatformPrices.ifEmpty { listOf(product) },
                    stats = stats
                )

                AlertDialog(
                    onDismissRequest = { showMcpDialog = false },
                    title = {
                        Text(
                            text = "🤖 MCP 台灣電商比價匯出",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = SlateTextPrimary
                        )
                    },
                    text = {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            TabRow(
                                selectedTabIndex = selectedTab,
                                containerColor = HighDensitySurfaceElevated,
                                contentColor = PrimaryBlue
                            ) {
                                Tab(
                                    selected = selectedTab == 0,
                                    onClick = { selectedTab = 0 },
                                    text = { Text("Markdown 表格", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                                )
                                Tab(
                                    selected = selectedTab == 1,
                                    onClick = { selectedTab = 1 },
                                    text = { Text("MCP JSON (Tool)", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 240.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(HighDensitySurfaceElevated)
                                    .border(1.dp, HighDensityBorderLight, RoundedCornerShape(8.dp))
                                    .padding(8.dp)
                            ) {
                                androidx.compose.foundation.lazy.LazyColumn {
                                    item {
                                        androidx.compose.foundation.text.selection.SelectionContainer {
                                            Text(
                                                text = if (selectedTab == 0) markdownSummary else mcpJson,
                                                style = MaterialTheme.typography.bodySmall,
                                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                                fontSize = 10.sp,
                                                color = SlateTextPrimary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                val textToCopy = if (selectedTab == 0) markdownSummary else mcpJson
                                clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(textToCopy))
                                viewModel.showBannerMessage("已成功複製比價內容至剪貼簿！可直接貼至 AI 對話框")
                                showMcpDialog = false
                            },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                        ) {
                            Text("複製內容", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = { showMcpDialog = false }
                        ) {
                            Text("關閉", fontSize = 11.sp, color = SlateTextSecondary)
                        }
                    }
                )
            }
        }

        // Price History Graph
        item {
            PriceHistoryChart(
                historyRecords = state.priceHistory,
                medianPrice = stats.medianPrice,
                selectedRange = state.selectedChartRange,
                onRangeChange = { viewModel.setChartTimeRange(it) }
            )
        }
    }
}

private fun String.isRealProductUrl(): Boolean =
    (startsWith("https://") || startsWith("http://")) &&
        !contains("/search", ignoreCase = true) &&
        !contains("search?", ignoreCase = true)

