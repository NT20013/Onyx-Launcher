package com.example.util

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.GraySubtle
import com.example.ui.theme.GrayText
import com.example.ui.theme.LocalAccentColor
import com.example.ui.theme.OffWhite
import com.example.ui.theme.PureBlack
import com.example.ui.theme.PureWhite
import com.example.ui.theme.WireframeBorder

object PermissionHelper {

    fun isNotificationAccessGranted(context: Context): Boolean {
        val grantedPackages = NotificationManagerCompat.getEnabledListenerPackages(context)
        val hasPackage = grantedPackages.contains(context.packageName)
        val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
        val hasFlat = flat != null && flat.contains(context.packageName)
        return hasPackage || hasFlat
    }

    fun hasLocationPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun openNotificationSettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            // Ignored
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingPermissionDialog(
    onComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val accentColor = LocalAccentColor.current

    var hasMediaAccess by remember {
        mutableStateOf(PermissionHelper.isNotificationAccessGranted(context))
    }
    var hasLocationAccess by remember {
        mutableStateOf(PermissionHelper.hasLocationPermission(context))
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasLocationAccess = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
    }

    BasicAlertDialog(
        onDismissRequest = { /* Require user action */ }
    ) {
        Column(
            modifier = modifier
                .clip(RoundedCornerShape(16.dp))
                .background(PureBlack)
                .border(1.dp, accentColor, RoundedCornerShape(16.dp))
                .padding(24.dp)
        ) {
            Text(
                text = "ONYX SETUP",
                color = accentColor,
                fontSize = 18.sp,
                fontWeight = FontWeight.Light,
                letterSpacing = 2.sp
            )
            Text(
                text = "Zero-friction permission setup for media & weather widgets.",
                color = GraySubtle,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
            )

            // Step 1: Media Session Permission
            PermissionCardRow(
                icon = Icons.Outlined.MusicNote,
                title = "Universal Media Player",
                subtitle = "Detect active playback from any app",
                isGranted = hasMediaAccess,
                actionLabel = "Enable in Settings",
                onClick = {
                    PermissionHelper.openNotificationSettings(context)
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Step 2: Location Permission for Weather
            PermissionCardRow(
                icon = Icons.Outlined.LocationOn,
                title = "Local Weather",
                subtitle = "Accurate wireframe temperature & conditions",
                isGranted = hasLocationAccess,
                actionLabel = "Allow Location",
                onClick = {
                    locationPermissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                    )
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Done / Continue button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(DarkSurface)
                    .border(1.dp, accentColor, RoundedCornerShape(8.dp))
                    .clickable { onComplete() }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "CONTINUE TO LAUNCHER",
                    color = accentColor,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}

@Composable
private fun PermissionCardRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    isGranted: Boolean,
    actionLabel: String,
    onClick: () -> Unit
) {
    val accentColor = LocalAccentColor.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(DarkSurface)
            .border(
                1.dp,
                if (isGranted) WireframeBorder else accentColor.copy(alpha = 0.4f),
                RoundedCornerShape(8.dp)
            )
            .clickable(enabled = !isGranted, onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isGranted) accentColor else GrayText,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = PureWhite,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = if (isGranted) "Enabled" else subtitle,
                color = if (isGranted) accentColor else GraySubtle,
                fontSize = 11.sp
            )
        }
        if (isGranted) {
            Icon(
                imageVector = Icons.Outlined.CheckCircle,
                contentDescription = "Granted",
                tint = accentColor,
                modifier = Modifier.size(18.dp)
            )
        } else {
            Text(
                text = actionLabel,
                color = accentColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
