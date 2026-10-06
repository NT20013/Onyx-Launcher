package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AppItemUiState
import com.example.ui.theme.AccentRed
import com.example.ui.theme.GraySubtle
import com.example.ui.theme.GrayText
import com.example.ui.theme.PureBlack
import com.example.ui.theme.PureWhite
import com.example.ui.theme.WireframeBorder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppActionsBottomSheet(
    app: AppItemUiState,
    onDismiss: () -> Unit,
    onOpenApp: () -> Unit,
    onTogglePin: () -> Unit,
    onAppInfo: () -> Unit,
    onUninstall: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = PureBlack,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        dragHandle = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(
                    modifier = Modifier
                        .width(36.dp)
                        .height(3.dp)
                        .background(WireframeBorder, RoundedCornerShape(2.dp))
                )
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 8.dp)
        ) {
            // App Header
            Text(
                text = app.label,
                color = PureWhite,
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = app.packageName,
                color = GraySubtle,
                fontSize = 12.sp,
                fontWeight = FontWeight.Normal
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Action Items
            ActionRow(
                icon = Icons.AutoMirrored.Outlined.OpenInNew,
                title = "Open App",
                onClick = {
                    onOpenApp()
                    onDismiss()
                }
            )

            ActionRow(
                icon = if (app.isPinned) Icons.Outlined.StarBorder else Icons.Outlined.Star,
                title = if (app.isPinned) "Unpin from Favorites" else "Pin to Favorites",
                onClick = {
                    onTogglePin()
                    onDismiss()
                }
            )

            ActionRow(
                icon = Icons.Outlined.Info,
                title = "App Info",
                onClick = {
                    onAppInfo()
                    onDismiss()
                }
            )

            ActionRow(
                icon = Icons.Outlined.Delete,
                title = "Uninstall",
                iconTint = AccentRed,
                titleColor = AccentRed,
                onClick = {
                    onUninstall()
                    onDismiss()
                }
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun ActionRow(
    icon: ImageVector,
    title: String,
    iconTint: androidx.compose.ui.graphics.Color = PureWhite,
    titleColor: androidx.compose.ui.graphics.Color = PureWhite,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = title,
            color = titleColor,
            fontSize = 16.sp,
            fontWeight = FontWeight.Normal
        )
    }
}
