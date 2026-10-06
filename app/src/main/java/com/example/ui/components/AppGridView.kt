package com.example.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.NorthWest
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AppItemUiState
import com.example.model.IconDisplayStyle
import com.example.ui.LauncherListItem
import com.example.ui.theme.GraySubtle
import com.example.ui.theme.LocalAccentColor
import com.example.ui.theme.OffWhite
import com.example.ui.theme.PureWhite

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppGridView(
    items: List<LauncherListItem>,
    gridState: LazyGridState,
    iconStyle: IconDisplayStyle,
    onAppClick: (AppItemUiState) -> Unit,
    onAppLongClick: (AppItemUiState) -> Unit,
    onWebSearchClick: (String) -> Unit = {},
    onAIAssistantClick: (String) -> Unit = {},
    rightPadding: Dp,
    modifier: Modifier = Modifier
) {
    val accentColor = LocalAccentColor.current
    val haptic = LocalHapticFeedback.current

    LazyVerticalGrid(
        columns = GridCells.Fixed(4),
        state = gridState,
        contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 96.dp),
        modifier = modifier
            .fillMaxSize()
            .padding(end = rightPadding)
            .testTag("app_grid")
    ) {
        items(
            items = items,
            key = { it.key },
            contentType = { it.contentType },
            span = { item ->
                when (item) {
                    is LauncherListItem.SectionHeader,
                    is LauncherListItem.WebSuggestionItem,
                    is LauncherListItem.AIAssistantSuggestionItem -> {
                        GridItemSpan(maxLineSpan)
                    }
                    is LauncherListItem.AppItem -> {
                        GridItemSpan(1)
                    }
                }
            }
        ) { item ->
            when (item) {
                is LauncherListItem.SectionHeader -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 12.dp, top = 16.dp, bottom = 8.dp)
                    ) {
                        Text(
                            text = item.title.uppercase(),
                            color = accentColor.copy(alpha = 0.75f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 1.sp
                        )
                    }
                }

                is LauncherListItem.AppItem -> {
                    val app = item.app
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .combinedClickable(
                                onClick = { onAppClick(app) },
                                onLongClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onAppLongClick(app)
                                }
                            )
                            .padding(vertical = 10.dp, horizontal = 4.dp)
                            .testTag("app_grid_item_${app.packageName}"),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        when (iconStyle) {
                            IconDisplayStyle.WIREFRAME_ICON -> {
                                if (app.iconBitmap != null) {
                                    Image(
                                        bitmap = app.iconBitmap,
                                        contentDescription = null,
                                        modifier = Modifier.size(36.dp)
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .border(1.dp, accentColor, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = app.firstCharUpper,
                                            color = accentColor,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                            }

                            IconDisplayStyle.MINIMAL_DOT -> {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(accentColor)
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                            }

                            IconDisplayStyle.TEXT_ONLY -> {
                                // Pure typography in grid
                            }
                        }

                        Text(
                            text = app.label,
                            color = OffWhite,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Normal,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (!app.usageTimeText.isNullOrEmpty()) {
                            Text(
                                text = app.usageTimeText,
                                color = accentColor.copy(alpha = 0.65f),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Light,
                                textAlign = TextAlign.Center,
                                maxLines = 1
                            )
                        }
                    }
                }

                is LauncherListItem.AIAssistantSuggestionItem -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .clickable { onAIAssistantClick(item.prompt) }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                            .testTag("ai_suggestion_item_grid"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(26.dp)
                                .clip(CircleShape)
                                .border(1.dp, accentColor, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.AutoAwesome,
                                contentDescription = "Gemini",
                                tint = accentColor,
                                modifier = Modifier.size(15.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Запитати у Gemini Flash Lite",
                                color = accentColor,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                            if (item.prompt.isNotBlank()) {
                                Text(
                                    text = "«${item.prompt}»",
                                    color = PureWhite,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                is LauncherListItem.WebSuggestionItem -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 46.dp)
                            .clickable { onWebSearchClick(item.query) }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                            .testTag("web_suggestion_${item.query}"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .border(1.dp, accentColor.copy(alpha = 0.4f), RoundedCornerShape(4.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Language,
                                contentDescription = null,
                                tint = accentColor,
                                modifier = Modifier.size(14.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Text(
                            text = item.query,
                            color = PureWhite,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Light,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )

                        Icon(
                            imageVector = Icons.Outlined.NorthWest,
                            contentDescription = "Search",
                            tint = GraySubtle,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}
