package com.example.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.model.AnomalyReport
import com.example.ui.theme.*
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun PriceComparisonCard(
    report: AnomalyReport,
    onClick: () -> Unit,
    onStarClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isExpanded by remember { mutableStateOf(false) }
    val formatter = NumberFormat.getCurrencyInstance(Locale.TAIWAN).apply {
        maximumFractionDigits = 0
    }

    val priceDiffPercent = report.deviationPercent
    val isDrop = priceDiffPercent < 0

    // Left border indicator color based on deal tier
    val leftAccentColor = when {
        report.dealScore >= 90 -> AnomalyGlitchRed
        report.dealScore >= 75 -> AnomalyClearanceOrange
        else -> HighDensityBorder
    }

    val priceTextColor = when {
        report.dealScore >= 90 -> AnomalyGlitchRed
        report.dealScore >= 75 -> AnomalyClearanceOrange
        else -> SlateTextPrimary
    }

    val discountBgColor = when {
        report.dealScore >= 90 -> AnomalyRoseContainer
        report.dealScore >= 75 -> AnomalyAmberContainer
        else -> HighDensitySurfaceElevated
    }

    val discountTextColor = when {
        report.dealScore >= 90 -> AnomalyGlitchRed
        report.dealScore >= 75 -> AnomalyClearanceOrange
        else -> SlateTextSecondary
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, HighDensityBorderLight, RoundedCornerShape(16.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            // Left Accent Bar (High Density signature)
            Box(
                modifier = Modifier
                    .width(4.5.dp)
                    .fillMaxHeight()
                    .background(leftAccentColor)
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                // Main High Density Content Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    // 1. Thumbnail (64 x 64 dp)
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(HighDensitySurfaceElevated)
                            .border(1.dp, HighDensityBorderLight, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (report.imageUrl.isNotBlank()) {
                            AsyncImage(
                                model = report.imageUrl,
                                contentDescription = report.productTitle,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Text(text = report.platform.iconSymbol, fontSize = 24.sp)
                        }
                    }

                    // 2. Info Block
                    Column(modifier = Modifier.weight(1f)) {
                        // Title & Score Badge
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Text(
                                text = report.productTitle,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = SlateTextPrimary,
                                fontSize = 13.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            DealScoreBadge(score = report.dealScore)
                        }

                        // Pricing Row: Price + Strikethrough Reference + Discount Badge
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(top = 3.dp)
                        ) {
                            Text(
                                text = formatter.format(report.currentPrice),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black,
                                color = priceTextColor,
                                fontSize = 17.sp
                            )
                            Text(
                                text = formatter.format(report.referencePrice),
                                style = MaterialTheme.typography.labelSmall,
                                color = SlateTextMuted,
                                textDecoration = TextDecoration.LineThrough,
                                fontSize = 10.sp
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(discountBgColor)
                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = "${if (priceDiffPercent > 0) "+" else ""}${priceDiffPercent.roundToInt()}%",
                                    color = discountTextColor,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                )
                            }
                        }

                        // Subtitle Metadata Line: Platform · Time & Confidence
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${report.platform.displayName.split(" ").first()} · 剛剛",
                                style = MaterialTheme.typography.labelSmall,
                                color = SlateTextSecondary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium
                            )

                            Text(
                                text = "Confidence: ${report.confidenceScore}%",
                                style = MaterialTheme.typography.labelSmall,
                                color = AnomalyHistoricGreen,
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                fontStyle = FontStyle.Italic
                            )
                        }
                    }
                }

                // Expandable Reason Analysis Section
                AnimatedVisibility(visible = isExpanded) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(HighDensitySurfaceElevated)
                            .padding(8.dp)
                    ) {
                        Text(
                            text = "異常判定因素：",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = SlateTextPrimary,
                            fontSize = 10.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        report.reasons.forEach { reason ->
                            Text(
                                text = "• $reason",
                                style = MaterialTheme.typography.bodySmall,
                                color = SlateTextSecondary,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(vertical = 0.5.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Action Buttons Row: Compact [判斷依據] & [前往購買]
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onStarClick,
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(
                            imageVector = if (report.isStarred) Icons.Filled.Star else Icons.Outlined.StarBorder,
                            contentDescription = "收藏",
                            tint = if (report.isStarred) AnomalyClearanceOrange else SlateTextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    OutlinedButton(
                        onClick = { isExpanded = !isExpanded },
                        modifier = Modifier
                            .weight(1f)
                            .height(30.dp),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = SlateTextSecondary
                        )
                    ) {
                        Text(
                            text = if (isExpanded) "收合" else "判定依據",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Button(
                        onClick = {
                            val targetUrl = report.productUrl.takeIf { it.isRealProductUrl() } ?: return@Button
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(targetUrl))
                            try {
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                // Fallback
                            }
                        },
                        enabled = report.productUrl.isRealProductUrl(),
                        modifier = Modifier
                            .weight(1.3f)
                            .height(30.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (report.dealScore >= 90) AnomalyGlitchRed else PrimaryBlue
                        ),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ShoppingCart,
                            contentDescription = "購買",
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "前往購買",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}

private fun String.isRealProductUrl(): Boolean =
    (startsWith("https://") || startsWith("http://")) &&
        !contains("/search", ignoreCase = true) &&
        !contains("search?", ignoreCase = true)

