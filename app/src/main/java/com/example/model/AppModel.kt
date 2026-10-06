package com.example.model

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.ImageBitmap

/**
 * Represents an installed application in the launcher.
 * Fully immutable to enable Compose compiler smart recomposition skipping.
 */
@Immutable
data class AppModel(
    val packageName: String,
    val activityName: String,
    val label: String,
    val sectionHeader: Char,
    val usageTimeMillis: Long = 0L,
    val usageTimeFormatted: String = "",
    val isPinned: Boolean = false,
    val iconImageBitmap: ImageBitmap? = null,
    val hasMonochromeSupport: Boolean = false
) {
    val id: String get() = "$packageName/$activityName"
}

enum class IconDisplayStyle {
    WIREFRAME_ICON, // White/accent stroke wireframe icon
    MINIMAL_DOT,    // Sleek geometric dot indicator
    TEXT_ONLY       // Clean pure typography
}

enum class LayoutMode {
    LIST, // 1 column vertical list
    GRID  // 3-4 column vertical grid
}

enum class SortMode {
    ALPHABETICAL, // Pure A-Z / А-Я alphabetical
    USAGE_TIME    // Screen time: Most used today at top
}
