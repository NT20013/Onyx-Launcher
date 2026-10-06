package com.example.ui.theme

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color

// Pure AMOLED / True Black & Monochrome palette
val PureBlack = Color(0xFF000000)
val DarkSurface = Color(0xFF0A0A0A)
val DarkSurfaceVariant = Color(0xFF141414)
val WireframeBorder = Color(0xFF282828)
val WireframeBorderSubtle = Color(0xFF1C1C1C)

val PureWhite = Color(0xFFFFFFFF)
val OffWhite = Color(0xFFEEEEEE)
val GrayText = Color(0xFF9E9E9E)
val GraySubtle = Color(0xFF555555)
val AccentRed = Color(0xFFFF5252)

val LocalAccentColor = compositionLocalOf { PureWhite }
