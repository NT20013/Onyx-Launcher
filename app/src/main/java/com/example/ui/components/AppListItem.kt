package com.example.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AppItemUiState
import com.example.model.IconDisplayStyle
import com.example.ui.theme.OffWhite

/**
 * Ultra-high-performance 120 FPS AppListItem.
 * Strictly immutable state, rigid 56.dp height, zero runtime allocations,
 * and direct ImageBitmap rendering without color filters or drawables.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppListItem(
    app: AppItemUiState,
    iconStyle: IconDisplayStyle,
    accentColor: Color,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .combinedClickable(
                onClick = onClick,
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLongClick()
                }
            )
            .padding(horizontal = 24.dp)
            .testTag("app_item_${app.packageName}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        when (iconStyle) {
            IconDisplayStyle.WIREFRAME_ICON -> {
                if (app.iconBitmap != null) {
                    Image(
                        bitmap = app.iconBitmap,
                        contentDescription = null,
                        modifier = Modifier.size(28.dp)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .border(1.dp, accentColor, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = app.firstCharUpper,
                            color = accentColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
            }

            IconDisplayStyle.MINIMAL_DOT -> {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(accentColor)
                )
                Spacer(modifier = Modifier.width(16.dp))
            }

            IconDisplayStyle.TEXT_ONLY -> {
                // Pure minimalist typography - 0dp icon space
            }
        }

        Text(
            text = app.label,
            color = OffWhite,
            fontSize = 17.sp,
            fontWeight = FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )

        // Pre-formatted usage screen time badge (calculated beforehand in background)
        if (!app.usageTimeText.isNullOrEmpty()) {
            Text(
                text = app.usageTimeText,
                color = accentColor.copy(alpha = 0.65f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Normal,
                modifier = Modifier.padding(horizontal = 6.dp)
            )
        }

        if (app.isPinned) {
            Icon(
                imageVector = Icons.Outlined.Star,
                contentDescription = "Pinned",
                tint = accentColor.copy(alpha = 0.5f),
                modifier = Modifier
                    .size(16.dp)
                    .padding(start = 4.dp)
            )
        }
    }
}
