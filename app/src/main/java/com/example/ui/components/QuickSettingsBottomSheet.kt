package com.example.ui.components

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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForwardIos
import androidx.compose.material.icons.automirrored.outlined.ViewList
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.SortByAlpha
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AccentColorPreset
import com.example.data.AccentPresets
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
fun QuickSettingsBottomSheet(
    currentAccent: AccentColorPreset,
    currentSortMode: SortMode,
    currentLayoutMode: LayoutMode,
    selectedIconPack: String = "",
    onSelectAccent: (AccentColorPreset) -> Unit,
    onSelectSortMode: (SortMode) -> Unit,
    onSelectLayoutMode: (LayoutMode) -> Unit,
    onOpenIconPackSelector: () -> Unit = {},
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val accentColor = LocalAccentColor.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = PureBlack,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .size(width = 36.dp, height = 3.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(accentColor.copy(alpha = 0.4f))
            )
        },
        modifier = modifier
            .imePadding()
            .testTag("quick_settings_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .border(1.dp, accentColor, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Tune,
                            contentDescription = "Quick Settings",
                            tint = accentColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "ШВИДКІ НАЛАШТУВАННЯ",
                            color = accentColor,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "Персоналізація робочого простору",
                            color = GraySubtle,
                            fontSize = 11.sp
                        )
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "Close",
                        tint = GrayText,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Accent Colors Strip
            Text(
                text = "АКЦЕНТНИЙ КОЛІР",
                color = GrayText,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(bottom = 18.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (preset in AccentPresets.allPresets) {
                    val isSelected = preset.hexString == currentAccent.hexString
                    Box(
                        modifier = Modifier
                            .size(36.dp)
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

            // Custom Icon Pack Selector Entry
            Text(
                text = "ПАК ІКОНОК (ICON PACK)",
                color = GrayText,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(DarkSurface)
                    .border(1.dp, WireframeBorder, RoundedCornerShape(8.dp))
                    .clickable { onOpenIconPackSelector() }
                    .padding(vertical = 12.dp, horizontal = 14.dp)
                    .testTag("open_icon_pack_selector_button"),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.Palette,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Пак іконок",
                        color = PureWhite,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Normal
                    )
                    Text(
                        text = if (selectedIconPack.isBlank()) "За замовчуванням (Мінімалістичний)" else selectedIconPack,
                        color = accentColor.copy(alpha = 0.8f),
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowForwardIos,
                    contentDescription = "Open",
                    tint = GraySubtle,
                    modifier = Modifier.size(14.dp)
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Sort Mode Switcher
            Text(
                text = "СОРТУВАННЯ",
                color = GrayText,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 18.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                QuickOptionCard(
                    title = "Алфавіт (A-Z)",
                    icon = Icons.Outlined.SortByAlpha,
                    isSelected = currentSortMode == SortMode.ALPHABETICAL,
                    accentColor = accentColor,
                    onClick = { onSelectSortMode(SortMode.ALPHABETICAL) },
                    modifier = Modifier.weight(1f)
                )

                QuickOptionCard(
                    title = "Екранний час",
                    icon = Icons.Outlined.Schedule,
                    isSelected = currentSortMode == SortMode.USAGE_TIME,
                    accentColor = accentColor,
                    onClick = { onSelectSortMode(SortMode.USAGE_TIME) },
                    modifier = Modifier.weight(1f)
                )
            }

            // Layout Mode Switcher (List vs Grid)
            Text(
                text = "ВИГЛЯД (LAYOUT)",
                color = GrayText,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                QuickOptionCard(
                    title = "Список (Рядки)",
                    icon = Icons.AutoMirrored.Outlined.ViewList,
                    isSelected = currentLayoutMode == LayoutMode.LIST,
                    accentColor = accentColor,
                    onClick = { onSelectLayoutMode(LayoutMode.LIST) },
                    modifier = Modifier.weight(1f)
                )

                QuickOptionCard(
                    title = "Сітка (Іконки)",
                    icon = Icons.Outlined.GridView,
                    isSelected = currentLayoutMode == LayoutMode.GRID,
                    accentColor = accentColor,
                    onClick = { onSelectLayoutMode(LayoutMode.GRID) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun QuickOptionCard(
    title: String,
    icon: ImageVector,
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
            .padding(vertical = 12.dp, horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isSelected) accentColor else GrayText,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            color = if (isSelected) PureWhite else OffWhite,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal
        )
    }
}
