package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.PlatformType
import com.example.ui.screens.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.AppTab
import com.example.ui.viewmodel.DealHunterUiState
import com.example.ui.viewmodel.DealHunterViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: DealHunterViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 0.dp,
                shadowElevation = 0.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.statusBars)
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    // Header Row: Title & Status + Quick Scan Button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = when (uiState.currentTab) {
                                    AppTab.DASHBOARD -> "電商監控中心"
                                    AppTab.DEALS -> "即時異常偵測"
                                    AppTab.MONITORS -> "商品關鍵字監控"
                                    AppTab.HISTORY -> "價格歷史診斷"
                                    AppTab.SETTINGS -> "系統與通知設定"
                                },
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 20.sp,
                                letterSpacing = (-0.5).sp
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.padding(top = 2.dp)
                            ) {
                                Text(
                                    text = "System Status:",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(AnomalyHistoricGreen)
                                )
                                Text(
                                    text = if (uiState.scannerStatus.isScanning) "Scanning..." else "Online",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = AnomalyHistoricGreen,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        // Right Action: Quick Scan Action Button
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(PrimaryBlueLight)
                                .border(1.dp, PrimaryBlueContainer, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            IconButton(
                                onClick = { viewModel.triggerQuickScan() },
                                modifier = Modifier.size(38.dp)
                            ) {
                                if (uiState.scannerStatus.isScanning) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                        color = PrimaryBlue
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Filled.NotificationsNone,
                                        contentDescription = "立即掃描",
                                        tint = PrimaryBlue,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Horizontal Platform Pills Strip (Interactive Filter Bar)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // "All Platforms" Pill (全部)
                        val isAllSelected = uiState.selectedPlatformFilter == null
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                            modifier = Modifier
                                .clip(RoundedCornerShape(100.dp))
                                .background(
                                    if (isAllSelected) PrimaryBlue
                                    else HighDensitySurfaceElevated
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (isAllSelected) PrimaryBlue else HighDensityBorderLight,
                                    shape = RoundedCornerShape(100.dp)
                                )
                                .clickable {
                                    viewModel.setSelectedPlatformFilter(null)
                                    if (uiState.currentTab != AppTab.DASHBOARD && uiState.currentTab != AppTab.DEALS) {
                                        viewModel.selectTab(AppTab.DASHBOARD)
                                    }
                                }
                                .padding(horizontal = 11.dp, vertical = 5.dp)
                                .testTag("platform_filter_all")
                        ) {
                            Text(
                                text = "全部",
                                fontSize = 11.sp,
                                fontWeight = if (isAllSelected) FontWeight.Black else FontWeight.Bold,
                                color = if (isAllSelected) Color.White else SlateTextPrimary
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (isAllSelected) Color.White.copy(alpha = 0.25f)
                                        else PrimaryBlueLight
                                    )
                                    .padding(horizontal = 5.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = "${uiState.allDeals.size}",
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isAllSelected) Color.White else PrimaryBlue
                                )
                            }
                        }

                        // Individual Platform Pills
                        PlatformType.entries.forEach { platform ->
                            val isSelected = uiState.selectedPlatformFilter == platform
                            val dealCount = uiState.allDeals.count { it.platform == platform }
                            val activeBgColor = Color(platform.brandColorHex)

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(100.dp))
                                    .background(
                                        if (isSelected) activeBgColor
                                        else HighDensitySurfaceElevated
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = if (isSelected) activeBgColor else HighDensityBorderLight,
                                        shape = RoundedCornerShape(100.dp)
                                    )
                                    .clickable {
                                        viewModel.setSelectedPlatformFilter(platform)
                                        if (uiState.currentTab != AppTab.DASHBOARD && uiState.currentTab != AppTab.DEALS) {
                                            viewModel.selectTab(AppTab.DASHBOARD)
                                        }
                                    }
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                                    .testTag("platform_filter_${platform.name.lowercase()}")
                            ) {
                                Text(
                                    text = platform.iconSymbol,
                                    fontSize = 11.sp
                                )
                                Text(
                                    text = platform.displayName.split(" ").first(),
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                                    color = if (isSelected) Color.White else SlateTextPrimary
                                )
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (isSelected) Color.White.copy(alpha = 0.25f)
                                            else MaterialTheme.colorScheme.surfaceVariant
                                        )
                                        .padding(horizontal = 5.dp, vertical = 1.dp)
                                ) {
                                    Text(
                                        text = "$dealCount",
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color.White else SlateTextSecondary
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    HorizontalDivider(color = HighDensityBorderLight, thickness = 1.dp)
                }
            }
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 0.dp,
                modifier = Modifier
                    .border(width = 1.dp, color = HighDensityBorderLight)
                    .windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                AppTab.entries.forEach { tab ->
                    val isSelected = uiState.currentTab == tab
                    val badgeCount = if (tab == AppTab.DEALS) uiState.allDeals.count { it.dealScore >= 90 } else 0

                    NavigationBarItem(
                        selected = isSelected,
                        onClick = {
                            if (tab == AppTab.HISTORY && uiState.selectedProductDetail == null) {
                                if (uiState.products.isNotEmpty()) {
                                    val firstAnomaly = uiState.allDeals.firstOrNull()
                                    val targetProd = if (firstAnomaly != null) {
                                        uiState.products.find { it.id == firstAnomaly.productId } ?: uiState.products.first()
                                    } else {
                                        uiState.products.first()
                                    }
                                    viewModel.openProductDetail(targetProd, firstAnomaly)
                                } else {
                                    viewModel.selectTab(tab)
                                }
                            } else {
                                viewModel.selectTab(tab)
                            }
                        },
                        icon = {
                            BadgedBox(
                                badge = {
                                    if (badgeCount > 0) {
                                        Badge(
                                            containerColor = AnomalyGlitchRed,
                                            contentColor = Color.White
                                        ) {
                                            Text("$badgeCount", fontWeight = FontWeight.Black, fontSize = 9.sp)
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = when (tab) {
                                        AppTab.DASHBOARD -> if (isSelected) Icons.Filled.Home else Icons.Outlined.Home
                                        AppTab.DEALS -> if (isSelected) Icons.Filled.LocalFireDepartment else Icons.Outlined.LocalFireDepartment
                                        AppTab.MONITORS -> if (isSelected) Icons.Filled.Sensors else Icons.Outlined.Sensors
                                        AppTab.HISTORY -> if (isSelected) Icons.Filled.ShowChart else Icons.Outlined.ShowChart
                                        AppTab.SETTINGS -> if (isSelected) Icons.Filled.Settings else Icons.Outlined.Settings
                                    },
                                    contentDescription = tab.title,
                                    tint = if (isSelected) PrimaryBlue else SlateTextMuted
                                )
                            }
                        },
                        label = {
                            Text(
                                text = when (tab) {
                                    AppTab.DASHBOARD -> "Dashboard"
                                    AppTab.DEALS -> "Deals"
                                    AppTab.MONITORS -> "Monitor"
                                    AppTab.HISTORY -> "History"
                                    AppTab.SETTINGS -> "Settings"
                                },
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Black else FontWeight.Medium,
                                color = if (isSelected) PrimaryBlue else SlateTextMuted
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = PrimaryBlueLight
                        )
                    )
                }
            }
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
        ) {
            // Tab contents
            AnimatedContent(
                targetState = uiState.currentTab,
                label = "tab_transition"
            ) { tab ->
                when (tab) {
                    AppTab.DASHBOARD -> DashboardScreen(
                        uiState = uiState,
                        viewModel = viewModel,
                        onNavigateToDeals = { viewModel.selectTab(AppTab.DEALS) },
                        onNavigateToMonitors = { viewModel.selectTab(AppTab.MONITORS) }
                    )
                    AppTab.DEALS -> DealsRadarScreen(
                        uiState = uiState,
                        viewModel = viewModel
                    )
                    AppTab.MONITORS -> MonitorsScreen(
                        uiState = uiState,
                        viewModel = viewModel
                    )
                    AppTab.HISTORY -> ProductDetailScreen(
                        state = uiState.selectedProductDetail,
                        viewModel = viewModel,
                        onBack = {
                            viewModel.clearSelectedProductDetail()
                            viewModel.selectTab(AppTab.DEALS)
                        }
                    )
                    AppTab.SETTINGS -> SettingsScreen(
                        uiState = uiState,
                        viewModel = viewModel
                    )
                }
            }

            // Floating Toast/Banner Message
            AnimatedVisibility(
                visible = uiState.bannerMessage != null,
                enter = fadeIn() + slideInVertically(initialOffsetY = { -40 }),
                exit = fadeOut() + slideOutVertically(targetOffsetY = { -40 }),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 8.dp, start = 16.dp, end = 16.dp)
            ) {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF1E293B)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(text = "✨", fontSize = 14.sp)
                        Text(
                            text = uiState.bannerMessage ?: "",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Modal Sheet for Creating/Editing Monitors
            if (uiState.isCreateEditSheetOpen && uiState.editingMonitor != null) {
                CreateEditMonitorSheet(
                    initialRule = uiState.editingMonitor!!,
                    onDismiss = { viewModel.closeCreateEditSheet() },
                    onSave = { updatedRule -> viewModel.saveEditingMonitor(updatedRule) }
                )
            }
        }
    }
}
