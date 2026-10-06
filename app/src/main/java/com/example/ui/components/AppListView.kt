package com.example.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.NorthWest
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AppItemUiState
import com.example.model.IconDisplayStyle
import com.example.ui.LauncherListItem
import com.example.ui.theme.GraySubtle
import com.example.ui.theme.LocalAccentColor
import com.example.ui.theme.PureWhite

/**
 * 120 FPS Zero-Overhead AppListView with rigid item sizes and stable keys.
 */
@Composable
fun AppListView(
    items: List<LauncherListItem>,
    listState: LazyListState,
    iconStyle: IconDisplayStyle,
    onAppClick: (AppItemUiState) -> Unit,
    onAppLongClick: (AppItemUiState) -> Unit,
    onWebSearchClick: (String) -> Unit = {},
    onAIAssistantClick: (String) -> Unit = {},
    rightPadding: Dp,
    modifier: Modifier = Modifier
) {
    val accentColor = LocalAccentColor.current

    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(bottom = 96.dp),
        modifier = modifier
            .fillMaxSize()
            .padding(end = rightPadding)
            .testTag("app_list")
    ) {
        items(
            items = items,
            key = { it.key },
            contentType = { it.contentType }
        ) { item ->
            when (item) {
                is LauncherListItem.AppItem -> {
                    AppListItem(
                        app = item.app,
                        iconStyle = iconStyle,
                        accentColor = accentColor,
                        onClick = remember(item.app.id) { { onAppClick(item.app) } },
                        onLongClick = remember(item.app.id) { { onAppLongClick(item.app) } }
                    )
                }

                is LauncherListItem.SectionHeader -> {
                    // Fixed rigid 36.dp height prevents layout measure passes during fast scrolling
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(36.dp)
                            .padding(horizontal = 24.dp),
                        contentAlignment = Alignment.CenterStart
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

                is LauncherListItem.AIAssistantSuggestionItem -> {
                    // Fixed rigid 54.dp height
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .clickable { onAIAssistantClick(item.prompt) }
                            .padding(horizontal = 24.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .border(1.dp, accentColor, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.AutoAwesome,
                                contentDescription = "Gemini",
                                tint = accentColor,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Запитати у Gemini Flash Lite",
                                color = accentColor,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                            if (item.prompt.isNotBlank()) {
                                Text(
                                    text = "«${item.prompt}»",
                                    color = PureWhite,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Normal,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Icon(
                            imageVector = Icons.Outlined.NorthWest,
                            contentDescription = "AI",
                            tint = accentColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                is LauncherListItem.WebSuggestionItem -> {
                    // Fixed rigid 48.dp height
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .clickable { onWebSearchClick(item.query) }
                            .padding(horizontal = 24.dp),
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

                        Spacer(modifier = Modifier.width(14.dp))

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
