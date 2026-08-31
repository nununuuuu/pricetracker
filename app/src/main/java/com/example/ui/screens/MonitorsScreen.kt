package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.MonitorRule
import com.example.ui.components.EmptyStateView
import com.example.ui.components.PlatformBadge
import com.example.ui.theme.*
import com.example.ui.viewmodel.DealHunterUiState
import com.example.ui.viewmodel.DealHunterViewModel
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonitorsScreen(
    uiState: DealHunterUiState,
    viewModel: DealHunterViewModel,
    modifier: Modifier = Modifier
) {
    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.openCreateMonitorSheet() },
                containerColor = PrimaryBlue,
                contentColor = Color.White,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.padding(bottom = 64.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Filled.Add, contentDescription = "新增", modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "新增追蹤 (網址/關鍵字)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(10.dp))

            // Header Summary
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "監控與追蹤管理",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        color = SlateTextPrimary,
                        fontSize = 15.sp
                    )
                    Text(
                        text = "支援貼上商品網址專屬追蹤 或 關鍵字 15 平台跨網比價",
                        style = MaterialTheme.typography.bodySmall,
                        color = SlateTextSecondary,
                        fontSize = 11.sp
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(AnomalyEmeraldContainer)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "${uiState.monitors.count { it.isEnabled }} / ${uiState.monitors.size} 啟用",
                        style = MaterialTheme.typography.labelSmall,
                        color = AnomalyHistoricGreen,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (uiState.monitors.isEmpty()) {
                EmptyStateView(
                    iconSymbol = "🎯",
                    title = "尚未建立任何追蹤項目",
                    subtitle = "點擊右下角按鈕，貼上商品網址（如蝦皮、momo、PChome）或輸入關鍵字開始監控！",
                    actionText = "立即新增追蹤",
                    onActionClick = { viewModel.openCreateMonitorSheet() },
                    modifier = Modifier.weight(1f)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 120.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(uiState.monitors, key = { it.id }) { rule ->
                        MonitorItemCard(
                            rule = rule,
                            onToggle = { isEnabled -> viewModel.toggleMonitor(rule.id, isEnabled) },
                            onEdit = { viewModel.openEditMonitorSheet(rule) },
                            onDuplicate = { viewModel.duplicateMonitor(rule) },
                            onDelete = { viewModel.deleteMonitor(rule.id) },
                            onScanNow = { viewModel.triggerScanForSingleMonitor(rule) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MonitorItemCard(
    rule: MonitorRule,
    onToggle: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
    onScanNow: () -> Unit
) {
    val context = LocalContext.current
    var menuExpanded by remember { mutableStateOf(false) }
    val formatter = remember {
        NumberFormat.getCurrencyInstance(Locale.TAIWAN).apply { maximumFractionDigits = 0 }
    }

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
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Header: Track Badge + Name + Switch + More Menu
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
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
                                    fontSize = 9.sp,
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
                                    fontSize = 9.sp,
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
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(
                                    if (rule.isEnabled) AnomalyEmeraldContainer
                                    else HighDensitySurfaceElevated
                                )
                                .padding(horizontal = 5.dp, vertical = 1.5.dp)
                        ) {
                            Text(
                                text = if (rule.isEnabled) "ON" else "OFF",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (rule.isEnabled) AnomalyHistoricGreen else SlateTextMuted
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = if (rule.trackMode == "URL" && rule.targetUrl.isNotBlank()) "追蹤網址: ${rule.targetUrl}" else "搜尋核心詞: ${rule.searchKeyword}",
                        style = MaterialTheme.typography.bodySmall,
                        color = SlateTextSecondary,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Switch(
                        checked = rule.isEnabled,
                        onCheckedChange = onToggle,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = PrimaryBlue
                        ),
                        modifier = Modifier.padding(end = 2.dp)
                    )

                    Box {
                        IconButton(
                            onClick = { menuExpanded = true },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.MoreVert,
                                contentDescription = "選單",
                                tint = SlateTextMuted,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false }
                        ) {
                            if (rule.targetUrl.isNotBlank()) {
                                DropdownMenuItem(
                                    text = { Text("開啟原商品網址", fontSize = 12.sp) },
                                    leadingIcon = { Icon(Icons.Outlined.OpenInBrowser, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                    onClick = {
                                        menuExpanded = false
                                        try {
                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(rule.targetUrl))
                                            context.startActivity(intent)
                                        } catch (e: Exception) {
                                            // fallback
                                        }
                                    }
                                )
                            }
                            DropdownMenuItem(
                                text = { Text("修改設定", fontSize = 12.sp) },
                                leadingIcon = { Icon(Icons.Outlined.Edit, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                onClick = {
                                    menuExpanded = false
                                    onEdit()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("複製監控", fontSize = 12.sp) },
                                leadingIcon = { Icon(Icons.Outlined.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                onClick = {
                                    menuExpanded = false
                                    onDuplicate()
                                }
                            )
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text("刪除", color = AnomalyGlitchRed, fontSize = 12.sp) },
                                leadingIcon = { Icon(Icons.Outlined.Delete, contentDescription = null, tint = AnomalyGlitchRed, modifier = Modifier.size(16.dp)) },
                                onClick = {
                                    menuExpanded = false
                                    onDelete()
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Must include words chips
            if (rule.mustIncludeWords.isNotEmpty()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "必須包含:",
                        style = MaterialTheme.typography.labelSmall,
                        color = PrimaryBlue,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                    rule.mustIncludeWords.forEach { word ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(PrimaryBlueLight)
                                .padding(horizontal = 5.dp, vertical = 1.5.dp)
                        ) {
                            Text(text = word, fontSize = 9.5.sp, color = PrimaryBlue, fontWeight = FontWeight.Medium)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
            }

            // Exclude keywords chips
            if (rule.excludeKeywords.isNotEmpty()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "排除:",
                        style = MaterialTheme.typography.labelSmall,
                        color = AnomalyGlitchRed,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                    rule.excludeKeywords.take(4).forEach { word ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(AnomalyRoseContainer)
                                .padding(horizontal = 5.dp, vertical = 1.5.dp)
                        ) {
                            Text(text = word, fontSize = 9.5.sp, color = AnomalyGlitchRed)
                        }
                    }
                    if (rule.excludeKeywords.size > 4) {
                        Text(
                            text = "+${rule.excludeKeywords.size - 4}",
                            fontSize = 9.5.sp,
                            color = SlateTextMuted
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
            }

            // Enabled Platform Badges
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "監控平台:",
                    style = MaterialTheme.typography.labelSmall,
                    color = SlateTextMuted,
                    fontSize = 10.sp
                )
                rule.enabledPlatforms.forEach { platform ->
                    PlatformBadge(platform = platform, compact = true)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = HighDensityBorderLight)
            Spacer(modifier = Modifier.height(6.dp))

            // Footer: Threshold Info + Manual Scan Button
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Text(
                        text = if (rule.maxFixedPrice != null) "門檻: ≤ ${formatter.format(rule.maxFixedPrice)} 或 偏離 -${rule.discountThresholdPercent.toInt()}%"
                        else "門檻: 低於市場中位價 -${rule.discountThresholdPercent.toInt()}%",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = AnomalyClearanceOrange,
                        fontSize = 11.sp
                    )
                    Text(
                        text = "歷史索引 ${rule.totalFoundCount} 筆 • 發現 ${rule.anomalyCount} 筆異常",
                        style = MaterialTheme.typography.labelSmall,
                        color = SlateTextMuted,
                        fontSize = 9.5.sp
                    )
                }

                OutlinedButton(
                    onClick = onScanNow,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.height(28.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = SlateTextSecondary)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = "掃描",
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "單獨掃描", fontSize = 10.5.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}
