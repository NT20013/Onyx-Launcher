package com.example.util

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.BatteryManager
import android.os.Build
import androidx.compose.runtime.Immutable
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@Immutable
data class BluetoothDeviceInfo(
    val name: String,
    val batteryPercent: Int? = null
)

@Immutable
data class DeviceBatteryState(
    val phoneBatteryPercent: Int = 100,
    val isCharging: Boolean = false,
    val bluetoothDevices: List<BluetoothDeviceInfo> = emptyList()
)

object BatteryMonitor {

    private val _batteryState = MutableStateFlow(DeviceBatteryState())
    val batteryState: StateFlow<DeviceBatteryState> = _batteryState.asStateFlow()

    private var isRegistered = false

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_BATTERY_CHANGED -> {
                    updatePhoneBattery(intent)
                }
                "android.bluetooth.device.action.BATTERY_LEVEL_CHANGED",
                BluetoothDevice.ACTION_ACL_CONNECTED,
                BluetoothDevice.ACTION_ACL_DISCONNECTED,
                BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED -> {
                    updateBluetoothDevices(context)
                }
            }
        }
    }

    fun hasBluetoothPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun startMonitoring(context: Context) {
        if (isRegistered) return

        // Initial phone battery read via sticky broadcast
        val batteryFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val initialIntent = context.registerReceiver(null, batteryFilter)
        if (initialIntent != null) {
            updatePhoneBattery(initialIntent)
        }

        // Register for updates
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_BATTERY_CHANGED)
            addAction("android.bluetooth.device.action.BATTERY_LEVEL_CHANGED")
            addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
            addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
            addAction(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED)
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                context.registerReceiver(receiver, filter)
            }
            isRegistered = true
        } catch (e: Exception) {
            // Ignored
        }

        updateBluetoothDevices(context)
    }

    fun stopMonitoring(context: Context) {
        if (!isRegistered) return
        try {
            context.unregisterReceiver(receiver)
        } catch (e: Exception) {
            // Ignored
        }
        isRegistered = false
    }

    private fun updatePhoneBattery(intent: Intent) {
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)

        val percent = if (level >= 0 && scale > 0) {
            (level * 100 / scale.toFloat()).toInt()
        } else {
            100
        }

        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL

        _batteryState.value = _batteryState.value.copy(
            phoneBatteryPercent = percent,
            isCharging = isCharging
        )
    }

    @SuppressLint("MissingPermission")
    fun updateBluetoothDevices(context: Context) {
        if (!hasBluetoothPermission(context)) {
            _batteryState.value = _batteryState.value.copy(bluetoothDevices = emptyList())
            return
        }

        try {
            val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
            val adapter = bluetoothManager?.adapter ?: return
            if (!adapter.isEnabled) {
                _batteryState.value = _batteryState.value.copy(bluetoothDevices = emptyList())
                return
            }

            val devicesList = mutableListOf<BluetoothDeviceInfo>()

            // Query bonded devices
            val bondedDevices = adapter.bondedDevices ?: emptySet()
            for (device in bondedDevices) {
                val batteryLevel = try {
                    val method = device.javaClass.getMethod("getBatteryLevel")
                    val lvl = method.invoke(device) as? Int
                    if (lvl != null && lvl in 0..100) lvl else null
                } catch (e: Exception) {
                    null
                }

                // If battery is detected or device is audio/headset
                val deviceName = device.name ?: "Accessory"
                val deviceClass = device.bluetoothClass?.deviceClass
                val isAudioDevice = deviceClass != null && (
                        deviceClass == 1028 || // Headset
                                deviceClass == 1044 || // Headphones
                                deviceClass == 1048 || // Audio/Video Portable Audio
                                deviceClass == 1056    // Audio/Video Car Audio
                        )

                if (batteryLevel != null || isAudioDevice) {
                    devicesList.add(
                        BluetoothDeviceInfo(
                            name = deviceName,
                            batteryPercent = batteryLevel
                        )
                    )
                }
            }

            _batteryState.value = _batteryState.value.copy(bluetoothDevices = devicesList)
        } catch (e: Exception) {
            // Ignored
        }
    }
}
