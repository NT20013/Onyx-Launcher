package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color

private fun createColorScheme(accentColor: Color) = darkColorScheme(
    primary = accentColor,
    onPrimary = PureBlack,
    primaryContainer = DarkSurfaceVariant,
    onPrimaryContainer = accentColor,
    secondary = accentColor.copy(alpha = 0.85f),
    onSecondary = PureBlack,
    secondaryContainer = DarkSurfaceVariant,
    onSecondaryContainer = accentColor,
    tertiary = GrayText,
    onTertiary = PureBlack,
    background = PureBlack,
    onBackground = accentColor,
    surface = PureBlack,
    onSurface = accentColor,
    surfaceVariant = DarkSurface,
    onSurfaceVariant = GrayText,
    outline = accentColor.copy(alpha = 0.25f),
    outlineVariant = WireframeBorderSubtle
)

@Composable
fun MyApplicationTheme(
    accentColor: Color = PureWhite,
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = createColorScheme(accentColor)

    CompositionLocalProvider(LocalAccentColor provides accentColor) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
