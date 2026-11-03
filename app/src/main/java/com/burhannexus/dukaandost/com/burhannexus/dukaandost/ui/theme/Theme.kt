package com.burhannexus.dukaandost.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Navy,
    onPrimary = Color.White,
    primaryContainer = NavyLight,
    onPrimaryContainer = Navy,
    secondary = Teal,
    onSecondary = Color.White,
    secondaryContainer = TealLight,
    onSecondaryContainer = Color(0xFF053B37),
    tertiary = Amber,
    tertiaryContainer = AmberLight,
    background = CanvasLight,
    surface = Color.White,
    surfaceVariant = Color(0xFFE6EBF1),
    onSurfaceVariant = Color(0xFF4A5663)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFA9C7F0),
    onPrimary = Color(0xFF0B2540),
    primaryContainer = Color(0xFF244A73),
    onPrimaryContainer = NavyLight,
    secondary = Color(0xFF4FD1C5),
    onSecondary = Color(0xFF003733),
    secondaryContainer = Color(0xFF0F4F4A),
    onSecondaryContainer = TealLight,
    tertiary = Color(0xFFF5B574),
    background = CanvasDark,
    surface = SurfaceDark,
    surfaceVariant = Color(0xFF263341),
    onSurfaceVariant = Color(0xFFB8C4D1)
)

@Composable
fun DukaanDostTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = AppTypography,
        content = content
    )
}
