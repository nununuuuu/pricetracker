package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.PlatformType
import com.example.ui.theme.*

@Composable
fun PlatformBadge(
    platform: PlatformType,
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    val brandColor = Color(platform.brandColorHex)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(brandColor.copy(alpha = 0.12f))
            .border(0.8.dp, brandColor.copy(alpha = 0.35f), RoundedCornerShape(6.dp))
            .padding(horizontal = if (compact) 5.dp else 7.dp, vertical = if (compact) 2.dp else 3.dp)
    ) {
        Text(
            text = platform.iconSymbol,
            fontSize = if (compact) 10.sp else 12.sp
        )
        if (!compact) {
            Text(
                text = platform.displayName.split(" ").first(),
                color = brandColor,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp
            )
        }
    }
}
