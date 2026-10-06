package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForwardIos
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.SortByAlpha
import androidx.compose.material.icons.outlined.ViewList
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AccentColorPreset
import com.example.data.AccentPresets
import com.example.model.IconDisplayStyle
import com.example.model.LayoutMode
import com.example.model.SortMode
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.GraySubtle
import com.example.ui.theme.GrayText
import com.example.ui.theme.LocalAccentColor
import com.example.ui.theme.PureBlack
import com.example.ui.theme.PureWhite
import com.example.ui.theme.WireframeBorder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LauncherSettingsDialog(
    currentStyle: IconDisplayStyle,
    currentAccent: AccentColorPreset,
    currentLayoutMode: LayoutMode = LayoutMode.LIST,
    currentSortMode: SortMode = SortMode.ALPHABETICAL,
    totalAppsCount: Int,
    hasMediaPermission: Boolean,
    onSelectStyle: (IconDisplayStyle) -> Unit,
    onSelectAccent: (AccentColorPreset) -> Unit,
    onSelectLayoutMode: (LayoutMode) -> Unit = {},
    onSelectSortMode: (SortMode) -> Unit = {},
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val activeAccentColor = LocalAccentColor.current

    BasicAlertDialog(
        onDismissRequest = onDismiss
    ) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(PureBlack)
                .border(1.dp, activeAccentColor.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                .padding(22.dp)
        ) {
            Text(
                text = "ONYX LAUNCHER",
                color = activeAccentColor,
                fontSize = 18.sp,
                fontWeight = FontWeight.Light,
                letterSpacing = 2.sp
            )
            Text(
                text = "$totalAppsCount apps installed • 120 FPS AMOLED",
                color = GraySubtle,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
            )

            // Accent Color Palette Row
            Text(
                text = "THEME ACCENT COLOR",
                color = GrayText,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(bottom = 10.dp)
            )

            val scrollState = rememberScrollState()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(scrollState)
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (preset in AccentPresets.allPresets) {
                    val isSelected = preset.hexString == currentAccent.hexString
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(DarkSurfaceVariant)
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) preset.color else WireframeBorder,
                                shape = CircleShape
                            )
                            .clickable { onSelectAccent(preset) },
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(if (isSelected) 18.dp else 22.dp)
                                .clip(CircleShape)
                                .background(preset.color)
                        )
                    }
                }
            }

            // Sort Mode Selector (Alphabet vs Smart Categories)
            Text(
                text = "SORT & GROUPING",
                color = GrayText,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ModeOptionCard(
                    title = "Алфавіт (A-Z)",
                    icon = Icons.Outlined.SortByAlpha,
                    isSelected = currentSortMode == SortMode.ALPHABETICAL,
                    accentColor = activeAccentColor,
                    onClick = { onSelectSortMode(SortMode.ALPHABETICAL) },
                    modifier = Modifier.weight(1f)
                )

                ModeOptionCard(
                    title = "Екранний час",
                    icon = Icons.Outlined.Schedule,
                    isSelected = currentSortMode == SortMode.USAGE_TIME,
                    accentColor = activeAccentColor,
                    onClick = { onSelectSortMode(SortMode.USAGE_TIME) },
                    modifier = Modifier.weight(1f)
                )
            }

            // Layout Mode Selector (List vs Grid)
            Text(
                text = "APP DRAWER LAYOUT",
                color = GrayText,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ModeOptionCard(
                    title = "List (Rows)",
                    icon = Icons.Outlined.ViewList,
                    isSelected = currentLayoutMode == LayoutMode.LIST,
                    accentColor = activeAccentColor,
                    onClick = { onSelectLayoutMode(LayoutMode.LIST) },
                    modifier = Modifier.weight(1f)
                )

                ModeOptionCard(
                    title = "Grid (Cells)",
                    icon = Icons.Outlined.GridView,
                    isSelected = currentLayoutMode == LayoutMode.GRID,
                    accentColor = activeAccentColor,
                    onClick = { onSelectLayoutMode(LayoutMode.GRID) },
                    modifier = Modifier.weight(1f)
                )
            }

            Text(
                text = "ICON STYLE",
                color = GrayText,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            StyleOptionRow(
                title = "Wireframe Monochrome",
                subtitle = "Pre-baked GPU contour lines in theme accent",
                isSelected = currentStyle == IconDisplayStyle.WIREFRAME_ICON,
                accentColor = activeAccentColor,
                onClick = { onSelectStyle(IconDisplayStyle.WIREFRAME_ICON) }
            )

            Spacer(modifier = Modifier.height(6.dp))

            StyleOptionRow(
                title = "Minimal Dot",
                subtitle = "Subtle geometric dot next to names",
                isSelected = currentStyle == IconDisplayStyle.MINIMAL_DOT,
                accentColor = activeAccentColor,
                onClick = { onSelectStyle(IconDisplayStyle.MINIMAL_DOT) }
            )

            Spacer(modifier = Modifier.height(6.dp))

            StyleOptionRow(
                title = "Text Only",
                subtitle = "Clean Swiss-style typographic list",
                isSelected = currentStyle == IconDisplayStyle.TEXT_ONLY,
                accentColor = activeAccentColor,
                onClick = { onSelectStyle(IconDisplayStyle.TEXT_ONLY) }
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Media Access Permission Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .border(1.dp, if (hasMediaPermission) WireframeBorder else activeAccentColor.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                    .clickable {
                        openNotificationListenerSettings(context)
                    }
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.MusicNote,
                    contentDescription = null,
                    tint = if (hasMediaPermission) activeAccentColor else GrayText,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Media Player Bridge",
                        color = PureWhite,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Normal
                    )
                    Text(
                        text = if (hasMediaPermission) "Active • Notification access granted" else "Tap to grant notification listener access",
                        color = if (hasMediaPermission) GraySubtle else activeAccentColor,
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Default Launcher button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .border(1.dp, WireframeBorder, RoundedCornerShape(8.dp))
                    .clickable {
                        openDefaultHomeSettings(context)
                    }
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.Home,
                    contentDescription = null,
                    tint = activeAccentColor,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Set as Default Launcher",
                        color = PureWhite,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Normal
                    )
                    Text(
                        text = "Manage system default apps",
                        color = GraySubtle,
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Close button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onDismiss)
                    .padding(vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "CLOSE",
                    color = GrayText,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}

@Composable
private fun ModeOptionCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) DarkSurface else PureBlack)
            .border(
                width = 1.dp,
                color = if (isSelected) accentColor else WireframeBorder,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isSelected) accentColor else GraySubtle,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = title,
            color = if (isSelected) accentColor else GrayText,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal
        )
    }
}

@Composable
private fun StyleOptionRow(
    title: String,
    subtitle: String,
    isSelected: Boolean,
    accentColor: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) DarkSurface else PureBlack)
            .border(
                width = 1.dp,
                color = if (isSelected) accentColor else WireframeBorder,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = PureWhite,
                fontSize = 13.sp,
                fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal
            )
            Text(
                text = subtitle,
                color = GraySubtle,
                fontSize = 11.sp
            )
        }
        if (isSelected) {
            Icon(
                imageVector = Icons.Outlined.Check,
                contentDescription = "Selected",
                tint = accentColor,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

private fun openNotificationListenerSettings(context: Context) {
    try {
        val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        try {
            val intent = Intent(Settings.ACTION_SECURITY_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (err: Exception) {
            // Ignored
        }
    }
}

private fun openDefaultHomeSettings(context: Context) {
    try {
        val intent = Intent(Settings.ACTION_HOME_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        try {
            val intent = Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (err: Exception) {
            val intent = Intent(Settings.ACTION_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }
}
