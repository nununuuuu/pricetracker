package com.example.ui.theme

import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryNeon,
    onPrimary = Color.White,
    primaryContainer = PrimaryNeonContainer,
    onPrimaryContainer = PrimaryNeon,
    secondary = AnomalyHistoricGreen,
    onSecondary = Color.White,
    secondaryContainer = AnomalyEmeraldContainer,
    onSecondaryContainer = Color(0xFF047857),
    tertiary = AnomalyClearanceOrange,
    onTertiary = Color.White,
    background = DarkCanvas,
    onBackground = DarkTextPrimary,
    surface = DarkSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkSurfaceElevated,
    onSurfaceVariant = DarkTextSecondary,
    outline = DarkBorder,
    error = AnomalyGlitchRed,
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryNeon,
    onPrimary = Color.White,
    primaryContainer = PrimaryNeonLight,
    onPrimaryContainer = PrimaryNeon,
    secondary = SecondaryCyan,
    onSecondary = Color.White,
    secondaryContainer = SecondaryCyanLight,
    onSecondaryContainer = SecondaryCyan,
    tertiary = TertiaryAmber,
    onTertiary = Color.White,
    tertiaryContainer = TertiaryAmberLight,
    onTertiaryContainer = TertiaryAmber,
    background = HighDensityCanvas,
    onBackground = SlateTextPrimary,
    surface = HighDensitySurface,
    onSurface = SlateTextPrimary,
    surfaceVariant = HighDensitySurfaceElevated,
    onSurfaceVariant = SlateTextSecondary,
    outline = HighDensityBorder,
    error = AnomalyGlitchRed,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = false, // Explicitly default to bright vibrant light theme
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
