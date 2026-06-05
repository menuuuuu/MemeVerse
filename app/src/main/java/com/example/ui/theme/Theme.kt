package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val CosmicMemeColorScheme = darkColorScheme(
    primary = MemeAccentNeon,
    secondary = MemeSecondaryCyan,
    tertiary = MemeTertiaryPink,
    background = MemeBlack,
    surface = MemeDarkSurface,
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onTertiary = Color.White,
    onBackground = Color.White,
    onSurface = Color.White,
    surfaceVariant = MemeCardBg,
    onSurfaceVariant = Color.White
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Force our beautiful, custom branding
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = CosmicMemeColorScheme,
        typography = Typography,
        content = content
    )
}
