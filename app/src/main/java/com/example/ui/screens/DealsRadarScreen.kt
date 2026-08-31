package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.example.ui.components.EmptyStateView
import com.example.ui.components.PriceComparisonCard
import com.example.ui.theme.*
import com.example.ui.viewmodel.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DealsRadarScreen(
    uiState: DealHunterUiState,
    viewModel: DealHunterViewModel,
    modifier: Modifier = Modifier
) {
    var sortMenuExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(10.dp))

        // High Density Search Bar
        OutlinedTextField(
            value = uiState.searchQuery,
            onValueChange = { viewModel.setSearchQuery(it) },
            placeholder = { Text("搜尋異常商品、型號或品牌...", fontSize = 12.sp, color = SlateTextMuted) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = "搜尋",
                    tint = SlateTextMuted,
                    modifier = Modifier.size(18.dp)
                )
            },
            trailingIcon = {
                if (uiState.searchQuery.isNotBlank()) {
                    IconButton(onClick = { viewModel.setSearchQuery("") }) {
                        Icon(imageVector = Icons.Filled.Close, contentDescription = "清除", modifier = Modifier.size(16.dp))
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = HighDensitySurface,
                unfocusedContainerColor = HighDensitySurface,
                focusedBorderColor = PrimaryBlue,
                unfocusedBorderColor = HighDensityBorderLight
            )
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Filter Pills & Sort Button Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                DealFilterLevel.entries.forEach { filter ->
                    val isSelected = filter == uiState.selectedFilterLevel
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.setFilterLevel(filter) },
                        label = {
                            Text(
                                text = filter.label,
                                fontSize = 10.5.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        },
                        shape = RoundedCornerShape(8.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PrimaryBlueLight,
                            selectedLabelColor = PrimaryBlue,
                            containerColor = HighDensitySurface
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = if (isSelected) PrimaryBlueContainer else HighDensityBorderLight
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Sort Dropdown Button
            Box {
                IconButton(
                    onClick = { sortMenuExpanded = true },
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(HighDensitySurface)
                        .border(1.dp, HighDensityBorderLight, RoundedCornerShape(8.dp))
                ) {
                    Icon(
                        imageVector = Icons.Filled.Sort,
                        contentDescription = "排序",
                        tint = PrimaryBlue,
                        modifier = Modifier.size(16.dp)
                    )
                }

                DropdownMenu(
                    expanded = sortMenuExpanded,
                    onDismissRequest = { sortMenuExpanded = false }
                ) {
                    DealSortOption.entries.forEach { sort ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = sort.label,
                                    fontSize = 11.sp,
                                    fontWeight = if (sort == uiState.selectedSortOption) FontWeight.Bold else FontWeight.Normal,
                                    color = if (sort == uiState.selectedSortOption) PrimaryBlue else SlateTextPrimary
                                )
                            },
                            onClick = {
                                viewModel.setSortOption(sort)
                                sortMenuExpanded = false
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Results Count Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "共找到 ${uiState.filteredDeals.size} 筆符合條件特惠",
                style = MaterialTheme.typography.labelSmall,
                color = SlateTextMuted,
                fontSize = 10.sp
            )
            Text(
                text = "排序：${uiState.selectedSortOption.label.split(" ").first()}",
                style = MaterialTheme.typography.labelSmall,
                color = PrimaryBlue,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Deals List
        if (uiState.filteredDeals.isEmpty()) {
            EmptyStateView(
                iconSymbol = "🔍",
                title = "沒有符合條件的撿漏項目",
                subtitle = "試著切換篩選分類或清除關鍵字搜尋條件",
                actionText = "重設篩選",
                onActionClick = {
                    viewModel.setFilterLevel(DealFilterLevel.ALL)
                    viewModel.setSearchQuery("")
                },
                modifier = Modifier.weight(1f)
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(uiState.filteredDeals, key = { it.id }) { report ->
                    PriceComparisonCard(
                        report = report,
                        onClick = {
                            val prod = uiState.products.find { it.id == report.productId }
                            if (prod != null) {
                                viewModel.openProductDetail(prod, report)
                            }
                        },
                        onStarClick = { viewModel.toggleStar(report.id, !report.isStarred) }
                    )
                }
            }
        }
    }
}

