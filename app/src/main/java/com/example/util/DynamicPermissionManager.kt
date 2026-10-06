package com.example.util

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat

data class PermissionItem(
    val id: String,
    val title: String,
    val description: String,
    val isSpecialPermission: Boolean,
    val runtimePermissions: List<String> = emptyList(),
    val isGranted: (Context) -> Boolean,
    val requestAction: (Context) -> Unit = {}
)

object DynamicPermissionManager {

    /**
     * Complete modular list of launcher required & optional permissions.
     * To add any new permission in the future, simply add an entry here.
     */
    val launcherRequiredPermissions: List<PermissionItem> = listOf(
        PermissionItem(
            id = "notification_listener",
            title = "Медіаплеєр та сповіщення",
            description = "Виявлення активного треку з будь-якого аудіоплеєра (Spotify, YouTube тощо)",
            isSpecialPermission = true,
            isGranted = { context -> PermissionHelper.isNotificationAccessGranted(context) },
            requestAction = { context ->
                try {
                    val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                } catch (e: Exception) {
                    // Fallback
                }
            }
        ),
        PermissionItem(
            id = "usage_stats",
            title = "Статистика екранного часу",
            description = "Показ щоденного часу у кожному додатку та сортування за частотою використання",
            isSpecialPermission = true,
            isGranted = { context -> UsageStatsHelper.hasUsageStatsPermission(context) },
            requestAction = { context ->
                try {
                    val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                } catch (e: Exception) {
                    // Fallback
                }
            }
        ),
        PermissionItem(
            id = "bluetooth_connect",
            title = "Bluetooth-пристрої",
            description = "Відображення рівня заряду підключених навушників чи смарт-годинника",
            isSpecialPermission = false,
            runtimePermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                listOf(Manifest.permission.BLUETOOTH_CONNECT)
            } else {
                emptyList()
            },
            isGranted = { context ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.BLUETOOTH_CONNECT
                    ) == PackageManager.PERMISSION_GRANTED
                } else {
                    true
                }
            }
        ),
        PermissionItem(
            id = "location_weather",
            title = "Геолокація для погоди",
            description = "Відображення точної місцевої погоди та температури у шапці лаунчера",
            isSpecialPermission = false,
            runtimePermissions = listOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ),
            isGranted = { context -> PermissionHelper.hasLocationPermission(context) }
        )
    )

    fun areAllGranted(context: Context): Boolean {
        return launcherRequiredPermissions.all { it.isGranted(context) }
    }
}
