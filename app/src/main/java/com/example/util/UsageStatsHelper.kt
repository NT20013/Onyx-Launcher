package com.example.util

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Build
import android.os.Process
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.concurrent.TimeUnit

object UsageStatsHelper {

    fun hasUsageStatsPermission(context: Context): Boolean {
        return try {
            val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager ?: return false
            val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                appOps.unsafeCheckOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    Process.myUid(),
                    context.packageName
                )
            } else {
                @Suppress("DEPRECATION")
                appOps.checkOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    Process.myUid(),
                    context.packageName
                )
            }
            mode == AppOpsManager.MODE_ALLOWED
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Queries foreground usage statistics for the current calendar day (00:00 to now).
     * Runs strictly on Dispatchers.IO to maintain 120 FPS.
     */
    suspend fun getTodayUsageStats(context: Context): Map<String, Long> = withContext(Dispatchers.IO) {
        if (!hasUsageStatsPermission(context)) return@withContext emptyMap()

        try {
            val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
                ?: return@withContext emptyMap()

            val calendar = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val startTime = calendar.timeInMillis
            val endTime = System.currentTimeMillis()

            val stats = usageStatsManager.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY,
                startTime,
                endTime
            )

            if (stats.isNullOrEmpty()) return@withContext emptyMap()

            val resultMap = mutableMapOf<String, Long>()
            for (stat in stats) {
                val foregroundTime = stat.totalTimeInForeground
                if (foregroundTime > 0) {
                    val current = resultMap[stat.packageName] ?: 0L
                    resultMap[stat.packageName] = current + foregroundTime
                }
            }
            return@withContext resultMap
        } catch (e: Exception) {
            return@withContext emptyMap()
        }
    }

    /**
     * Formats milliseconds into clean Ukrainian screen time badge format:
     * e.g. "1г 15хв" or "45хв".
     */
    fun formatUsageTime(millis: Long): String {
        val totalMinutes = TimeUnit.MILLISECONDS.toMinutes(millis)
        if (totalMinutes <= 0) return ""

        val hours = totalMinutes / 60
        val mins = totalMinutes % 60

        return when {
            hours > 0 && mins > 0 -> "${hours}г ${mins}хв"
            hours > 0 -> "${hours}г"
            else -> "${mins}хв"
        }
    }
}
