package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import com.example.model.*
import com.example.platform.UrlParserHelper
import com.example.ui.components.PlatformBadge
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateEditMonitorSheet(
    initialRule: MonitorRule,
    onDismiss: () -> Unit,
    onSave: (MonitorRule) -> Unit
) {
    var trackMode by remember { mutableStateOf(initialRule.trackMode.ifBlank { "KEYWORD" }) }
    var targetUrl by remember { mutableStateOf(initialRule.targetUrl) }
    var name by remember { mutableStateOf(initialRule.name) }
    var searchKeyword by remember { mutableStateOf(initialRule.searchKeyword) }
    var matchMode by remember { mutableStateOf(initialRule.matchMode) }

    var mustIncludeList by remember { mutableStateOf(initialRule.mustIncludeWords) }
    var newMustWord by remember { mutableStateOf("") }

    var excludeList by remember { mutableStateOf(initialRule.excludeKeywords) }
    var newExcludeWord by remember { mutableStateOf("") }

    var enabledPlatforms by remember { mutableStateOf(initialRule.enabledPlatforms) }
    var thresholdMode by remember { mutableStateOf(initialRule.thresholdMode) }

    var fixedPriceText by remember { mutableStateOf(initialRule.maxFixedPrice?.toInt()?.toString() ?: "") }
    var discountPercent by remember { mutableFloatStateOf(initialRule.discountThresholdPercent.toFloat()) }
    var minDealScore by remember { mutableFloatStateOf(initialRule.minDealScore.toFloat()) }
    var scanInterval by remember { mutableIntStateOf(initialRule.scanIntervalMinutes) }

    var selectedCategoryPreset by remember { mutableStateOf("3C/耳機/鍵盤") }

    // Live URL parsing info
    val parsedUrlInfo = remember(targetUrl) {
        if (targetUrl.isNotBlank()) UrlParserHelper.parseProductUrl(targetUrl) else null
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = HighDensitySurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 36.dp)
        ) {
            // Title & Close
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (initialRule.id == 0L) "➕ 建立新商品追蹤" else "✏️ 編輯監控 (${initialRule.name})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    color = SlateTextPrimary,
                    fontSize = 16.sp
                )
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(Icons.Filled.Close, contentDescription = "關閉", tint = SlateTextMuted, modifier = Modifier.size(18.dp))
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Mode Selector: [🔍 關鍵字比價] vs [🔗 貼網址追蹤]
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(HighDensitySurfaceElevated)
                    .border(1.dp, HighDensityBorderLight, RoundedCornerShape(10.dp))
                    .padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (trackMode == "KEYWORD") PrimaryBlue else Color.Transparent)
                        .clickable { trackMode = "KEYWORD" }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(text = "🔍", fontSize = 12.sp)
                        Text(
                            text = "關鍵字全網比價",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = if (trackMode == "KEYWORD") Color.White else SlateTextSecondary
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (trackMode == "URL") PrimaryNeon else Color.Transparent)
                        .clickable { trackMode = "URL" }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(text = "🔗", fontSize = 12.sp)
                        Text(
                            text = "貼商品網址追蹤",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = if (trackMode == "URL") Color.White else SlateTextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // ================= URL TRACKING MODE =================
            if (trackMode == "URL") {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, PrimaryNeon.copy(alpha = 0.3f), RoundedCornerShape(14.dp)),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = PrimaryNeonLight.copy(alpha = 0.5f)
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(text = "🔗", fontSize = 14.sp)
                            Text(
                                text = "貼上商品頁面網址 (支援 15 大主流電商)",
                                fontWeight = FontWeight.Bold,
                                color = SlateTextPrimary,
                                fontSize = 12.5.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "支援蝦皮、momo、PChome、酷澎、Costco、家樂福、全聯、博客來等商品連結",
                            fontSize = 10.5.sp,
                            color = SlateTextSecondary
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = targetUrl,
                            onValueChange = { newUrl ->
                                targetUrl = newUrl
                                val parsed = UrlParserHelper.parseProductUrl(newUrl)
                                if (parsed.isValidUrl) {
                                    if (name.isBlank() || name.startsWith("【")) {
                                        name = parsed.suggestedName
                                    }
                                    if (searchKeyword.isBlank()) {
                                        searchKeyword = parsed.suggestedKeyword
                                    }
                                    enabledPlatforms = listOf(parsed.platform)
                                }
                            },
                            label = { Text("商品網址 (URL)", fontSize = 11.sp) },
                            placeholder = { Text("https://shopee.tw/product/... 或 momoshop.com.tw...", fontSize = 11.sp) },
                            singleLine = false,
                            maxLines = 3,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PrimaryNeon,
                                unfocusedBorderColor = HighDensityBorderLight,
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White
                            )
                        )

                        if (parsedUrlInfo != null && parsedUrlInfo.isValidUrl) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.White)
                                    .border(1.dp, PrimaryNeon.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    PlatformBadge(platform = parsedUrlInfo.platform)
                                    Text(
                                        text = "已自動識別所屬平台",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = PrimaryNeon
                                    )
                                }
                                Text(
                                    text = "🟢 解析成功",
                                    fontSize = 10.5.sp,
                                    color = AnomalyHistoricGreen,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
            }

            // Basic Info (Name & Search Keyword)
            Text(
                text = if (trackMode == "URL") "商品名稱與比價設定" else "基本比價設定",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = PrimaryBlue,
                fontSize = 12.5.sp
            )
            Spacer(modifier = Modifier.height(6.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("商品/追蹤名稱", fontSize = 11.sp) },
                placeholder = { Text("例如：Nova Pro Wireless、RTX 5070 Ti", fontSize = 11.sp) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryBlue,
                    unfocusedBorderColor = HighDensityBorderLight
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = searchKeyword,
                onValueChange = { searchKeyword = it },
                label = { Text("搜尋主關鍵字 (跨平台比價用)", fontSize = 11.sp) },
                placeholder = { Text("向各平台檢索之核心詞彙", fontSize = 11.sp) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryBlue,
                    unfocusedBorderColor = HighDensityBorderLight
                )
            )

            Spacer(modifier = Modifier.height(14.dp))

            // ================= KEYWORD MODE SPECIFIC: MUST INCLUDE WORDS =================
            if (trackMode == "KEYWORD") {
                Text(
                    text = "必須包含關鍵字 (全部符合方可成案)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryBlue,
                    fontSize = 12.5.sp
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = newMustWord,
                        onValueChange = { newMustWord = it },
                        placeholder = { Text("新增必須包含詞 (如 Nova)", fontSize = 11.sp) },
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
                            if (newMustWord.isNotBlank() && !mustIncludeList.contains(newMustWord.trim())) {
                                mustIncludeList = mustIncludeList + newMustWord.trim()
                                newMustWord = ""
                            }
                        },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                    ) {
                        Text("新增", fontSize = 11.sp)
                    }
                }

                if (mustIncludeList.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        mustIncludeList.forEach { word ->
                            InputChip(
                                selected = true,
                                onClick = { mustIncludeList = mustIncludeList.filter { it != word } },
                                label = { Text(word, fontSize = 11.sp, color = PrimaryBlue) },
                                trailingIcon = { Icon(Icons.Filled.Close, contentDescription = "刪除", modifier = Modifier.size(12.dp), tint = PrimaryBlue) },
                                colors = InputChipDefaults.inputChipColors(
                                    selectedContainerColor = PrimaryBlueLight
                                ),
                                border = InputChipDefaults.inputChipBorder(
                                    enabled = true,
                                    selected = true,
                                    borderColor = PrimaryBlueContainer
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
            }

            // Exclude Keywords (獨立排除字典設定)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "專屬排除字典 (過濾配件/二手/零件)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = AnomalyGlitchRed,
                        fontSize = 12.5.sp
                    )
                    Text(
                        text = "本商品獨立設定 (已排除 ${excludeList.size} 個詞彙)",
                        style = MaterialTheme.typography.labelSmall,
                        color = SlateTextMuted,
                        fontSize = 10.sp
                    )
                }
                if (excludeList.isNotEmpty()) {
                    TextButton(
                        onClick = { excludeList = emptyList() },
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Text("清空全部", fontSize = 10.5.sp, color = AnomalyGlitchRed)
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Category preset buttons to quickly populate exclusions for this item
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FalsePriceFilter.CATEGORY_PRESET_SUGGESTIONS.keys.forEach { catName ->
                    val isSelected = selectedCategoryPreset == catName
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedCategoryPreset = catName },
                        label = { Text(catName, fontSize = 10.sp) },
                        shape = RoundedCornerShape(8.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AnomalyRoseContainer,
                            selectedLabelColor = AnomalyGlitchRed,
                            containerColor = HighDensitySurfaceElevated
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = if (isSelected) AnomalyGlitchRed.copy(alpha = 0.4f) else HighDensityBorderLight
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Preset words under the selected category
            val currentCategoryWords = FalsePriceFilter.CATEGORY_PRESET_SUGGESTIONS[selectedCategoryPreset] ?: emptyList()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                currentCategoryWords.forEach { kw ->
                    val isAlreadyAdded = excludeList.contains(kw)
                    AssistChip(
                        onClick = {
                            excludeList = if (isAlreadyAdded) {
                                excludeList.filter { it != kw }
                            } else {
                                excludeList + kw
                            }
                        },
                        label = { Text(if (isAlreadyAdded) "✓ $kw" else "+ $kw", fontSize = 10.sp, color = if (isAlreadyAdded) AnomalyGlitchRed else SlateTextSecondary) },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = if (isAlreadyAdded) AnomalyRoseContainer else Color.Transparent
                        ),
                        border = AssistChipDefaults.assistChipBorder(
                            enabled = true,
                            borderColor = if (isAlreadyAdded) AnomalyGlitchRed.copy(alpha = 0.4f) else HighDensityBorderLight
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Custom exclusion input
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = newExcludeWord,
                    onValueChange = { newExcludeWord = it },
                    placeholder = { Text("自訂排除詞 (如: 二手, 散裝, 鍵帽, 保護套)", fontSize = 11.sp) },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AnomalyGlitchRed,
                        unfocusedBorderColor = HighDensityBorderLight
                    )
                )
                Button(
                    onClick = {
                        if (newExcludeWord.isNotBlank() && !excludeList.contains(newExcludeWord.trim())) {
                            excludeList = excludeList + newExcludeWord.trim()
                            newExcludeWord = ""
                        }
                    },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AnomalyGlitchRed)
                ) {
                    Text("新增", fontSize = 11.sp)
                }
            }

            if (excludeList.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    excludeList.forEach { word ->
                        InputChip(
                            selected = true,
                            onClick = { excludeList = excludeList.filter { it != word } },
                            label = { Text(word, fontSize = 11.sp, color = AnomalyGlitchRed) },
                            trailingIcon = { Icon(Icons.Filled.Close, contentDescription = "刪除", modifier = Modifier.size(12.dp), tint = AnomalyGlitchRed) },
                            colors = InputChipDefaults.inputChipColors(
                                selectedContainerColor = AnomalyRoseContainer
                            ),
                            border = InputChipDefaults.inputChipBorder(
                                enabled = true,
                                selected = true,
                                borderColor = AnomalyGlitchRed.copy(alpha = 0.3f)
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Platform Selection
            Text(
                text = if (trackMode == "URL") "監控平台 (可勾選其他平台同時跨平台比價)" else "選擇要監控的購物平台",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = SlateTextPrimary,
                fontSize = 12.5.sp
            )
            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                PlatformType.entries.forEach { platform ->
                    val isChecked = enabledPlatforms.contains(platform)
                    FilterChip(
                        selected = isChecked,
                        onClick = {
                            enabledPlatforms = if (isChecked) {
                                if (enabledPlatforms.size > 1) enabledPlatforms - platform else enabledPlatforms
                            } else {
                                enabledPlatforms + platform
                            }
                        },
                        label = { Text(platform.displayName.split(" ").first(), fontSize = 10.5.sp) },
                        leadingIcon = { Text(platform.iconSymbol, fontSize = 10.sp) },
                        shape = RoundedCornerShape(8.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PrimaryBlueLight,
                            selectedLabelColor = PrimaryBlue,
                            containerColor = HighDensitySurfaceElevated
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isChecked,
                            borderColor = if (isChecked) PrimaryBlueContainer else HighDensityBorderLight
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Price & Anomaly Threshold
            Text(
                text = "目標入手價與異常門檻",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = AnomalyClearanceOrange,
                fontSize = 12.5.sp
            )
            Spacer(modifier = Modifier.height(6.dp))

            OutlinedTextField(
                value = fixedPriceText,
                onValueChange = { fixedPriceText = it.filter { ch -> ch.isDigit() } },
                label = { Text("目標最高入手價 (NT$) (低於此價立即警報)", fontSize = 11.sp) },
                placeholder = { Text("例如 5000", fontSize = 11.sp) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryBlue,
                    unfocusedBorderColor = HighDensityBorderLight
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "低於市場中位價門檻: -${discountPercent.toInt()}%",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = SlateTextPrimary,
                fontSize = 11.5.sp
            )
            Slider(
                value = discountPercent,
                onValueChange = { discountPercent = it },
                valueRange = 10f..60f,
                steps = 4,
                colors = SliderDefaults.colors(
                    thumbColor = PrimaryBlue,
                    activeTrackColor = PrimaryBlue,
                    inactiveTrackColor = PrimaryBlueLight
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "最低通知 Deal Score: ${minDealScore.toInt()} 分 (建議 75+)",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = SlateTextPrimary,
                fontSize = 11.5.sp
            )
            Slider(
                value = minDealScore,
                onValueChange = { minDealScore = it },
                valueRange = 50f..95f,
                steps = 8,
                colors = SliderDefaults.colors(
                    thumbColor = PrimaryBlue,
                    activeTrackColor = PrimaryBlue,
                    inactiveTrackColor = PrimaryBlueLight
                )
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Submit Button
            val isFormValid = if (trackMode == "URL") {
                targetUrl.isNotBlank() && (name.isNotBlank() || searchKeyword.isNotBlank())
            } else {
                searchKeyword.isNotBlank()
            }

            Button(
                onClick = {
                    val resolvedSearchKeyword = if (searchKeyword.isNotBlank()) {
                        searchKeyword.trim()
                    } else if (name.isNotBlank()) {
                        name.trim()
                    } else {
                        "網址追蹤商品"
                    }

                    val finalRule = initialRule.copy(
                        name = if (name.isNotBlank()) name.trim() else resolvedSearchKeyword,
                        searchKeyword = resolvedSearchKeyword,
                        mustIncludeWords = mustIncludeList,
                        excludeKeywords = excludeList,
                        enabledPlatforms = enabledPlatforms,
                        maxFixedPrice = fixedPriceText.toDoubleOrNull(),
                        discountThresholdPercent = discountPercent.toDouble(),
                        minDealScore = minDealScore.toInt(),
                        scanIntervalMinutes = scanInterval,
                        targetUrl = targetUrl.trim(),
                        trackMode = trackMode
                    )
                    onSave(finalRule)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp),
                shape = RoundedCornerShape(12.dp),
                enabled = isFormValid,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (trackMode == "URL") PrimaryNeon else PrimaryBlue
                )
            ) {
                Text(
                    text = if (trackMode == "URL") "🔗 儲存並開始網址追蹤" else "🔍 儲存商品比價設定",
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
