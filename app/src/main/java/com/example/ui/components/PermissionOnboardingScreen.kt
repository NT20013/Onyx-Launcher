package com.example.ui.components

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.GraySubtle
import com.example.ui.theme.GrayText
import com.example.ui.theme.LocalAccentColor
import com.example.ui.theme.PureBlack
import com.example.ui.theme.PureWhite
import com.example.ui.theme.WireframeBorder
import com.example.util.DynamicPermissionManager
import com.example.util.PermissionItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionOnboardingScreen(
    onComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val accentColor = LocalAccentColor.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Map tracking dynamically granted permissions
    val permissionStates = remember {
        mutableStateMapOf<String, Boolean>().apply {
            for (item in DynamicPermissionManager.launcherRequiredPermissions) {
                put(item.id, item.isGranted(context))
            }
        }
    }

    // Refresh permission states on resume (when returning from System Settings screens)
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                for (item in DynamicPermissionManager.launcherRequiredPermissions) {
                    permissionStates[item.id] = item.isGranted(context)
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Runtime Permission Launcher for standard permissions
    val runtimePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        for (item in DynamicPermissionManager.launcherRequiredPermissions) {
            permissionStates[item.id] = item.isGranted(context)
        }
    }

    BasicAlertDialog(
        onDismissRequest = { /* Non-dismissible without explicit action */ }
    ) {
        Column(
            modifier = modifier
                .clip(RoundedCornerShape(16.dp))
                .background(PureBlack)
                .border(1.dp, accentColor, RoundedCornerShape(16.dp))
                .padding(22.dp)
                .testTag("dynamic_permission_dialog")
        ) {
            Text(
                text = "ONYX PERMISSIONS",
                color = accentColor,
                fontSize = 17.sp,
                fontWeight = FontWeight.Light,
                letterSpacing = 2.sp
            )
            Text(
                text = "Надайте дозволи для роботи віджетів, медіаплеєра та статистики.",
                color = GraySubtle,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
            )

            // Dynamic list of permissions
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f, fill = false)
            ) {
                items(
                    items = DynamicPermissionManager.launcherRequiredPermissions,
                    key = { it.id }
                ) { permission ->
                    val isGranted = permissionStates[permission.id] ?: false

                    PermissionRowCard(
                        item = permission,
                        isGranted = isGranted,
                        onActionClick = {
                            if (permission.isSpecialPermission) {
                                permission.requestAction(context)
                            } else {
                                if (permission.runtimePermissions.isNotEmpty()) {
                                    runtimePermissionLauncher.launch(
                                        permission.runtimePermissions.toTypedArray()
                                    )
                                }
                            }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Bottom Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ПРОПУСТИТИ",
                    color = GrayText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 1.sp,
                    modifier = Modifier
                        .clickable { onComplete() }
                        .padding(8.dp)
                )

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(DarkSurface)
                        .border(1.dp, accentColor, RoundedCornerShape(8.dp))
                        .clickable { onComplete() }
                        .padding(horizontal = 20.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = "ПРОДОВЖИТИ",
                        color = accentColor,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 1.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun PermissionRowCard(
    item: PermissionItem,
    isGranted: Boolean,
    onActionClick: () -> Unit
) {
    val accentColor = LocalAccentColor.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(DarkSurface)
            .border(
                1.dp,
                if (isGranted) WireframeBorder else accentColor.copy(alpha = 0.45f),
                RoundedCornerShape(8.dp)
            )
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                color = PureWhite,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = item.description,
                color = GraySubtle,
                fontSize = 11.sp,
                lineHeight = 15.sp
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        if (isGranted) {
            Icon(
                imageVector = Icons.Outlined.CheckCircle,
                contentDescription = "Надано",
                tint = accentColor,
                modifier = Modifier.size(20.dp)
            )
        } else {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(PureBlack)
                    .border(1.dp, accentColor, RoundedCornerShape(6.dp))
                    .clickable(onClick = onActionClick)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Надати",
                    color = accentColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
