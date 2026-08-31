package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.FalsePriceFilter
import com.example.model.PlatformType
import com.example.ui.components.PlatformBadge
import com.example.ui.theme.*
import com.example.ui.viewmodel.DealHunterUiState
import com.example.ui.viewmodel.DealHunterViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    uiState: DealHunterUiState,
    viewModel: DealHunterViewModel,
    modifier: Modifier = Modifier
) {
    var minScore by remember { mutableFloatStateOf(uiState.minNotificationScore.toFloat()) }
    var selectedInterval by remember { mutableIntStateOf(uiState.autoScanIntervalMinutes) }
    var webhookUrl by remember { mutableStateOf(uiState.webhookUrl) }
    var isDiscord by remember { mutableStateOf(uiState.isDiscordEnabled) }
    var isWebPush by remember { mutableStateOf(uiState.isWebPushEnabled) }

    var newKeywordInput by remember { mutableStateOf("") }
    var showClearDialog by remember { mutableStateOf(false) }

    val intervalOptions = listOf(5, 10, 15, 30, 60)

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("確認清空通用字典庫？", fontWeight = FontWeight.Bold, fontSize = 15.sp) },
            text = { Text("清空後將不再對全域商品進行通用配件與假低價排除檢測，個別商品專屬排除規則仍會保留。", fontSize = 12.sp) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearUniversalDictionary()
                        showClearDialog = false
                    }
                ) {
                    Text("確認清空", color = AnomalyGlitchRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 10.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Section: System Safety & Disclaimer (無白底簡約風格)
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, AnomalyHistoricGreen.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Text(text = "🛡️", fontSize = 18.sp)
                    Column {
                        Text(
                            text = "安全規範聲明 (Zero Auto-Checkout Policy)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = AnomalyHistoricGreen,
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "本系統嚴格遵循合規安全機制，僅提供價格異常比價、標錯價偵測與即時推播。無自動登入、自動結帳或金流代扣行為，點擊直接前往各電商官方商品頁。",
                            style = MaterialTheme.typography.bodySmall,
                            color = SlateTextSecondary,
                            fontSize = 10.5.sp,
                            lineHeight = 15.sp
                        )
                    }
                }
            }
        }

        // Section: Notification Threshold & Channels
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
                    Text(
                        text = "🔔 通知管道與警報門檻",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = SlateTextPrimary,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "全域最低通知 Deal Score: ${minScore.toInt()} 分",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = SlateTextPrimary,
                        fontSize = 12.sp
                    )
                    Slider(
                        value = minScore,
                        onValueChange = { minScore = it },
                        valueRange = 60f..95f,
                        steps = 6,
                        colors = SliderDefaults.colors(
                            thumbColor = PrimaryBlue,
                            activeTrackColor = PrimaryBlue,
                            inactiveTrackColor = PrimaryBlueLight
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(color = HighDensityBorderLight)
                    Spacer(modifier = Modifier.height(10.dp))

                    // Channel switches
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("裝置推播通知 (Push Notifications)", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = SlateTextPrimary)
                            Text("發現重大偏離即時彈窗", fontSize = 10.5.sp, color = SlateTextMuted)
                        }
                        Switch(
                            checked = isWebPush,
                            onCheckedChange = { isWebPush = it },
                            colors = SwitchDefaults.colors(checkedTrackColor = PrimaryBlue)
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Discord Webhook 撿漏頻道", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = SlateTextPrimary)
                            Text("發送卡片至指定伺服器頻道", fontSize = 10.5.sp, color = SlateTextMuted)
                        }
                        Switch(
                            checked = isDiscord,
                            onCheckedChange = { isDiscord = it },
                            colors = SwitchDefaults.colors(checkedTrackColor = PrimaryBlue)
                        )
                    }

                    if (isDiscord) {
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = webhookUrl,
                            onValueChange = { webhookUrl = it },
                            label = { Text("Webhook URL", fontSize = 11.sp) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PrimaryBlue,
                                unfocusedBorderColor = HighDensityBorderLight
                            )
                        )
                    }
                }
            }
        }

        // Section: Scheduler Frequency
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
                    Text(
                        text = "⏱️ 背景巡檢週期 (WorkManager Scheduler)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = SlateTextPrimary,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "系統於背景定時並行檢索各電商平台新上架與特惠價格",
                        style = MaterialTheme.typography.bodySmall,
                        color = SlateTextSecondary,
                        fontSize = 10.5.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        intervalOptions.forEach { mins ->
                            val isSel = selectedInterval == mins
                            FilterChip(
                                selected = isSel,
                                onClick = { selectedInterval = mins },
                                label = { Text("${mins}分鐘", fontSize = 10.5.sp, fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium) },
                                shape = RoundedCornerShape(8.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = PrimaryBlueLight,
                                    selectedLabelColor = PrimaryBlue,
                                    containerColor = HighDensitySurfaceElevated
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = isSel,
                                    borderColor = if (isSel) PrimaryBlueContainer else HighDensityBorderLight
                                )
                            )
                        }
                    }
                }
            }
        }

        // Section: Platform Adapters Management
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
                    Text(
                        text = "🔌 全台 15 大電商平台連接器開關與比價支援",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = SlateTextPrimary,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = "支援 momo、PChome、Coupang、ETMall、Rakuten、Yahoo購物中心、Yahoo拍賣、Costco、全聯全電商、萬家福/家樂福、博客來、露天市集、生活市集、松果購物 及 蝦皮購物 共 15 個平台的價格搜尋與比較",
                        style = MaterialTheme.typography.bodySmall,
                        color = SlateTextSecondary,
                        fontSize = 10.5.sp,
                        lineHeight = 15.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    PlatformType.entries.forEach { platform ->
                        val status = uiState.platformStatuses[platform]
                        val isEnabled = status?.isEnabled != false

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                PlatformBadge(platform = platform)
                                Column {
                                    Text(platform.displayName, fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = SlateTextPrimary)
                                    Text(
                                        status?.limitationsNote ?: "正常監控",
                                        fontSize = 9.5.sp,
                                        color = SlateTextMuted
                                    )
                                }
                            }

                            Switch(
                                checked = isEnabled,
                                onCheckedChange = { viewModel.setPlatformEnabled(platform, it) },
                                colors = SwitchDefaults.colors(checkedTrackColor = PrimaryBlue)
                            )
                        }
                    }
                }
            }
        }

        // Section: Universal Exclusion Dictionary Management (通用字典庫管理)
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
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(text = "📚", fontSize = 18.sp)
                        Column {
                            Text(
                                text = "全域通用排除字典庫 (自由增刪設定)",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = SlateTextPrimary,
                                fontSize = 13.sp
                            )
                            Text(
                                text = "自動套用於所有監控項目的假低價與配件過濾",
                                style = MaterialTheme.typography.labelSmall,
                                color = PrimaryBlue,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "通用字典庫將作為所有商品掃描時的基礎排除詞。命中字詞將判定為假低價/配件，並扣減 Deal Score。您可以自由新增字詞或刪除不需要的詞彙，個別商品亦可保有獨立排除字典。",
                        style = MaterialTheme.typography.bodySmall,
                        color = SlateTextSecondary,
                        fontSize = 10.5.sp,
                        lineHeight = 15.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Input & Add Button Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = newKeywordInput,
                            onValueChange = { newKeywordInput = it },
                            placeholder = { Text("輸入排除詞 (如: 支架, 螺絲, 瑕疵)", fontSize = 11.sp) },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PrimaryBlue,
                                unfocusedBorderColor = HighDensityBorderLight
                            )
                        )

                        Button(
                            onClick = {
                                if (newKeywordInput.isNotBlank()) {
                                    val words = newKeywordInput.split(",", "，", " ", "、", "\n")
                                        .map { it.trim() }
                                        .filter { it.isNotBlank() }
                                    if (words.size == 1) {
                                        viewModel.addUniversalDictionaryWord(words.first())
                                    } else if (words.size > 1) {
                                        viewModel.addUniversalDictionaryWords(words)
                                    }
                                    newKeywordInput = ""
                                }
                            },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text("➕ 新增", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Quick Category Suggestions Chips
                    Text(
                        text = "快速批量匯入類別排除詞庫：",
                        style = MaterialTheme.typography.labelSmall,
                        color = SlateTextMuted,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FalsePriceFilter.CATEGORY_PRESET_SUGGESTIONS.forEach { (catName, words) ->
                            val unaddedCount = words.count { !uiState.universalDictionary.contains(it) }
                            AssistChip(
                                onClick = {
                                    viewModel.addUniversalDictionaryWords(words)
                                },
                                label = {
                                    Text(
                                        if (unaddedCount > 0) "+ $catName (+$unaddedCount)" else "✓ $catName (已收錄)",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = if (unaddedCount > 0) PrimaryBlueLight else HighDensitySurfaceElevated,
                                    labelColor = if (unaddedCount > 0) PrimaryBlue else SlateTextMuted
                                ),
                                border = AssistChipDefaults.assistChipBorder(
                                    enabled = true,
                                    borderColor = if (unaddedCount > 0) PrimaryBlueContainer else HighDensityBorderLight
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = HighDensityBorderLight)
                    Spacer(modifier = Modifier.height(8.dp))

                    // Dictionary Header & Reset / Clear actions
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "現有字典詞彙",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = SlateTextPrimary
                            )
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = PrimaryBlueLight,
                                modifier = Modifier.padding(horizontal = 2.dp)
                            ) {
                                Text(
                                    text = "${uiState.universalDictionary.size} 個",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryBlue,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            TextButton(
                                onClick = { viewModel.resetUniversalDictionary() },
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                modifier = Modifier.height(28.dp)
                            ) {
                                Text("🔄 恢復預設", fontSize = 10.5.sp, color = PrimaryBlue)
                            }
                            if (uiState.universalDictionary.isNotEmpty()) {
                                TextButton(
                                    onClick = { showClearDialog = true },
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    Text("🗑️ 清空", fontSize = 10.5.sp, color = AnomalyGlitchRed)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Flow of chips
                    if (uiState.universalDictionary.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(HighDensitySurfaceElevated)
                                .padding(vertical = 16.dp, horizontal = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "通用字典目前為空。點擊「恢復預設」或上方輸入框新增排除詞。",
                                fontSize = 11.sp,
                                color = SlateTextMuted
                            )
                        }
                    } else {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            uiState.universalDictionary.forEach { word ->
                                InputChip(
                                    selected = false,
                                    onClick = { viewModel.removeUniversalDictionaryWord(word) },
                                    label = { Text(word, fontSize = 10.5.sp, color = SlateTextPrimary) },
                                    trailingIcon = {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "刪除「$word」",
                                            modifier = Modifier.size(13.dp),
                                            tint = AnomalyGlitchRed
                                        )
                                    },
                                    shape = RoundedCornerShape(6.dp),
                                    colors = InputChipDefaults.inputChipColors(
                                        containerColor = HighDensitySurfaceElevated
                                    ),
                                    border = InputChipDefaults.inputChipBorder(
                                        enabled = true,
                                        selected = false,
                                        borderColor = HighDensityBorderLight
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }

        // Save Settings Button
        item {
            Button(
                onClick = {
                    viewModel.updateSettings(
                        minScore = minScore.toInt(),
                        intervalMinutes = selectedInterval,
                        webhook = webhookUrl,
                        discord = isDiscord,
                        webPush = isWebPush
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
            ) {
                Text("儲存所有設定變更", fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

