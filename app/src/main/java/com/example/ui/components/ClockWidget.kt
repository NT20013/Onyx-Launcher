package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.AlarmClock
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BatteryChargingFull
import androidx.compose.material.icons.outlined.BatteryFull
import androidx.compose.material.icons.outlined.Headphones
import androidx.compose.material.icons.outlined.Smartphone
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.WeatherInfo
import com.example.ui.theme.GraySubtle
import com.example.ui.theme.GrayText
import com.example.ui.theme.LocalAccentColor
import com.example.ui.theme.OffWhite
import com.example.util.BatteryMonitor
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ClockWidget(
    weatherInfo: WeatherInfo = WeatherInfo(),
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val accentColor = LocalAccentColor.current
    var currentTime by remember { mutableStateOf(Date()) }

    val batteryState by BatteryMonitor.batteryState.collectAsState()

    LaunchedEffect(Unit) {
        while (true) {
            currentTime = Date()
            delay(1000)
        }
    }

    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val dateFormat = remember { SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 6.dp)
            .testTag("clock_widget")
    ) {
        // Large Minimalist Clock
        Text(
            text = timeFormat.format(currentTime),
            color = accentColor,
            fontSize = 54.sp,
            fontWeight = FontWeight.ExtraLight,
            letterSpacing = (-1).sp,
            lineHeight = 56.sp,
            modifier = Modifier.clickable {
                openClockApp(context)
            }
        )

        Spacer(modifier = Modifier.height(3.dp))

        // Date & Integrated Wireframe Weather in one clean line
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Date
            Text(
                text = dateFormat.format(currentTime).replaceFirstChar {
                    if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
                },
                color = GrayText,
                fontSize = 13.sp,
                fontWeight = FontWeight.Normal,
                letterSpacing = 0.3.sp,
                modifier = Modifier.clickable {
                    openCalendarApp(context)
                }
            )

            Text(
                text = " • ",
                color = GraySubtle,
                fontSize = 13.sp,
                modifier = Modifier.padding(horizontal = 4.dp)
            )

            // Weather Block (Wireframe icon + temp + condition)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clickable { openWeatherApp(context) }
                    .testTag("weather_block")
            ) {
                WireframeWeatherIcon(
                    condition = weatherInfo.condition,
                    tint = accentColor,
                    size = 15.dp
                )

                Spacer(modifier = Modifier.width(5.dp))

                Text(
                    text = "${weatherInfo.temperatureCelsius}°C ${weatherInfo.conditionName}",
                    color = OffWhite,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Normal
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Battery row: Phone battery + Connected Bluetooth Accessories
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Phone Battery
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.testTag("phone_battery_indicator")
            ) {
                Icon(
                    imageVector = if (batteryState.isCharging) Icons.Outlined.BatteryChargingFull else Icons.Outlined.BatteryFull,
                    contentDescription = "Phone Battery",
                    tint = accentColor.copy(alpha = 0.8f),
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${batteryState.phoneBatteryPercent}%",
                    color = GraySubtle,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Normal
                )
            }

            // Connected Bluetooth Accessories
            for (device in batteryState.bluetoothDevices) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.testTag("bt_device_${device.name}")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Headphones,
                        contentDescription = "Bluetooth Device",
                        tint = accentColor.copy(alpha = 0.8f),
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    val label = if (device.batteryPercent != null) {
                        "${device.name} ${device.batteryPercent}%"
                    } else {
                        device.name
                    }
                    Text(
                        text = label,
                        color = GraySubtle,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Normal
                    )
                }
            }
        }
    }
}

private fun openClockApp(context: Context) {
    val intent = Intent(AlarmClock.ACTION_SHOW_ALARMS).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    try {
        context.startActivity(intent)
    } catch (e: Exception) {
        try {
            val alarmIntent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(alarmIntent)
        } catch (err: Exception) {
            Toast.makeText(context, "Clock app not found", Toast.LENGTH_SHORT).show()
        }
    }
}

private fun openCalendarApp(context: Context) {
    try {
        val calendarIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_APP_CALENDAR)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(calendarIntent)
    } catch (e: Exception) {
        try {
            val builder = Uri.parse("content://com.android.calendar/time").buildUpon()
            val intent = Intent(Intent.ACTION_VIEW, builder.build()).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (err: Exception) {
            Toast.makeText(context, "Calendar app not found", Toast.LENGTH_SHORT).show()
        }
    }
}

private fun openWeatherApp(context: Context) {
    val pm = context.packageManager
    val weatherPackages = listOf(
        "com.google.android.apps.weather",
        "com.sec.android.daemonapp",
        "com.weather.Weather",
        "com.accuweather.android"
    )

    for (pkg in weatherPackages) {
        val launchIntent = pm.getLaunchIntentForPackage(pkg)
        if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(launchIntent)
            return
        }
    }

    try {
        val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://weather.com")).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(webIntent)
    } catch (e: Exception) {
        Toast.makeText(context, "Weather app not found", Toast.LENGTH_SHORT).show()
    }
}
