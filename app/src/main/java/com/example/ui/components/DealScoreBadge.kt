package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.DealScoreLevel
import com.example.ui.theme.*

@Composable
fun DealScoreBadge(
    score: Int,
    modifier: Modifier = Modifier,
    showLabel: Boolean = false
) {
    val level = DealScoreLevel.fromScore(score)
    val (bgColor, textColor) = when (level) {
        DealScoreLevel.EXTREME_ANOMALY -> AnomalyGlitchRed to Color.White
        DealScoreLevel.STRONG_DEAL -> AnomalyClearanceOrange to Color.White
        DealScoreLevel.GOOD_PRICE -> AnomalyGoodYellow to Color.Black
        DealScoreLevel.NORMAL -> AnomalyFakeGray to Color.White
    }

    // Subtle scale pulse for extreme anomaly (score >= 90)
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by if (level == DealScoreLevel.EXTREME_ANOMALY) {
        infiniteTransition.animateFloat(
            initialValue = 1.0f,
            targetValue = 1.06f,
            animationSpec = infiniteRepeatable(
                animation = tween(900, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "scale"
        )
    } else {
        rememberUpdatedState(1.0f)
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier
            .scale(pulseScale)
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .padding(horizontal = 7.dp, vertical = 2.5.dp)
    ) {
        Text(
            text = "$score",
            color = textColor,
            fontWeight = FontWeight.Black,
            fontSize = 11.sp
        )
        if (showLabel) {
            Text(
                text = level.label,
                color = textColor,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp
            )
        }
    }
}

@Composable
fun ConfidenceBadge(
    confidenceScore: Int,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        modifier = modifier
    ) {
        Text(
            text = "Confidence:",
            color = SlateTextMuted,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = "$confidenceScore%",
            color = if (confidenceScore >= 80) AnomalyHistoricGreen else PrimaryBlue,
            fontWeight = FontWeight.Bold,
            fontSize = 10.sp
        )
    }
}

