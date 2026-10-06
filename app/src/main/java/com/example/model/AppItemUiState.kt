package com.example.model

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.ImageBitmap

/**
 * Fully pre-rendered, immutable UI state for an app list item.
 * Contains zero heavy operations, no Drawables, and no runtime formatters.
 */
@Immutable
data class AppItemUiState(
    val packageName: String,
    val activityName: String,
    val label: String,
    val sectionHeader: Char,
    val usageTimeMillis: Long = 0L,
    val usageTimeText: String? = null,
    val iconBitmap: ImageBitmap? = null,
    val isPinned: Boolean = false,
    val firstCharUpper: String = label.firstOrNull()?.uppercase() ?: ""
) {
    val id: String get() = "$packageName/$activityName"
}
