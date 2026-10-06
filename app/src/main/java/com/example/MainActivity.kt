package com.example

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.ui.MainPagerScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.LauncherViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: LauncherViewModel by viewModels()

    private val packageReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            viewModel.reloadApps()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Force Maximum Refresh Rate (120 Hz) on supported displays
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            @Suppress("DEPRECATION")
            val modes = window.windowManager.defaultDisplay.supportedModes
            val maxRefreshMode = modes.maxByOrNull { it.refreshRate }
            if (maxRefreshMode != null) {
                val layoutParams = window.attributes
                layoutParams.preferredDisplayModeId = maxRefreshMode.modeId
                window.attributes = layoutParams
            }
        }

        registerPackageReceiver()

        setContent {
            val accentPreset by viewModel.accentPreset.collectAsState()

            MyApplicationTheme(accentColor = accentPreset.color) {
                val uiState by viewModel.uiState.collectAsState()

                // Intercept back button: clear search or close dialogs if active
                BackHandler(
                    enabled = uiState.searchQuery.isNotEmpty() ||
                            uiState.selectedAppForActions != null ||
                            uiState.isSettingsOpen ||
                            uiState.isQuickSettingsOpen ||
                            uiState.isAIAssistantOpen ||
                            uiState.isIconPackSelectorOpen
                ) {
                    when {
                        uiState.isIconPackSelectorOpen -> viewModel.setIconPackSelectorOpen(false)
                        uiState.isAIAssistantOpen -> viewModel.setAIAssistantOpen(false)
                        uiState.selectedAppForActions != null -> viewModel.selectAppForActions(null)
                        uiState.isSettingsOpen -> viewModel.setSettingsOpen(false)
                        uiState.isQuickSettingsOpen -> viewModel.setQuickSettingsOpen(false)
                        uiState.searchQuery.isNotEmpty() -> viewModel.clearSearch()
                    }
                }

                MainPagerScreen(viewModel = viewModel)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.reloadApps()
        viewModel.checkMediaPermissions()
        com.example.util.BatteryMonitor.updateBluetoothDevices(this)
    }

    private fun registerPackageReceiver() {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_CHANGED)
            addDataScheme("package")
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(packageReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(packageReceiver, filter)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(packageReceiver)
        } catch (e: Exception) {
            // Receiver might not be registered
        }
    }
}
