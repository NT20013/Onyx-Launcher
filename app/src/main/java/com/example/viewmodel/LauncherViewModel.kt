package com.example.viewmodel

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.drawable.Drawable
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.compose.runtime.Immutable
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AccentColorPreset
import com.example.data.AccentPresets
import com.example.data.LauncherPreferences
import com.example.data.ThemePreferences
import com.example.media.MediaManager
import com.example.media.MediaPlaybackState
import com.example.model.AppItemUiState
import com.example.model.IconDisplayStyle
import com.example.model.LayoutMode
import com.example.model.SortMode
import com.example.model.WeatherCondition
import com.example.model.WeatherInfo
import com.example.util.BatteryMonitor
import com.example.util.IconPackInfo
import com.example.util.IconPackManager
import com.example.util.IconProcessor
import com.example.util.UsageStatsHelper
import com.example.util.WebSearchHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.Collator
import java.util.Locale

@Immutable
data class LauncherUiState(
    val isLoading: Boolean = true,
    val apps: List<AppItemUiState> = emptyList(),
    val pinnedApps: List<AppItemUiState> = emptyList(),
    val searchQuery: String = "",
    val webSuggestions: List<String> = emptyList(),
    val letterIndexMap: Map<Char, Int> = emptyMap(),
    val availableLetters: List<Char> = emptyList(),
    val iconStyle: IconDisplayStyle = IconDisplayStyle.WIREFRAME_ICON,
    val layoutMode: LayoutMode = LayoutMode.LIST,
    val sortMode: SortMode = SortMode.ALPHABETICAL,
    val selectedAppForActions: AppItemUiState? = null,
    val isSettingsOpen: Boolean = false,
    val isQuickSettingsOpen: Boolean = false,
    val isAIAssistantOpen: Boolean = false,
    val aiInitialPrompt: String = "",
    val isIconPackSelectorOpen: Boolean = false,
    val installedIconPacks: List<IconPackInfo> = emptyList(),
    val selectedIconPack: String = "",
    val weather: WeatherInfo = WeatherInfo(),
    val accentPreset: AccentColorPreset = AccentPresets.CleanWhite,
    val onboardingCompleted: Boolean = true,
    val isFirstLaunchDone: Boolean = true,
    val widgetIds: List<Int> = emptyList()
)

class LauncherViewModel(application: Application) : AndroidViewModel(application) {

    private val preferences = LauncherPreferences(application)
    private val themePreferences = ThemePreferences(application)
    private val packageManager: PackageManager = application.packageManager
    private val collator = Collator.getInstance(Locale.getDefault()).apply {
        strength = Collator.PRIMARY
    }

    private val rawDrawables = mutableMapOf<String, Drawable>()

    private val _rawApps = MutableStateFlow<List<AppItemUiState>>(emptyList())
    private val _isLoading = MutableStateFlow(true)
    private val _searchQuery = MutableStateFlow("")
    private val _webSuggestions = MutableStateFlow<List<String>>(emptyList())
    private val _selectedAppForActions = MutableStateFlow<AppItemUiState?>(null)
    private val _isSettingsOpen = MutableStateFlow(false)
    private val _isQuickSettingsOpen = MutableStateFlow(false)
    private val _isAIAssistantOpen = MutableStateFlow(false)
    private val _aiInitialPrompt = MutableStateFlow("")
    private val _isIconPackSelectorOpen = MutableStateFlow(false)
    private val _installedIconPacks = MutableStateFlow<List<IconPackInfo>>(emptyList())
    private val _weather = MutableStateFlow(
        WeatherInfo(temperatureCelsius = 20, condition = WeatherCondition.CLEAR_SUNNY, conditionName = "Sunny", locationName = "Kyiv")
    )

    private var searchDebounceJob: Job? = null

    val mediaPlaybackState: StateFlow<MediaPlaybackState> = MediaManager.playbackState

    val accentPreset: StateFlow<AccentColorPreset> = themePreferences.accentColorFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = AccentPresets.CleanWhite
        )

    private val prefsFlow = combine(
        preferences.pinnedApps,
        preferences.iconStyle,
        preferences.layoutMode,
        preferences.sortMode,
        combine(
            preferences.onboardingCompleted,
            preferences.isFirstLaunchDone,
            preferences.widgetIds,
            preferences.selectedIconPack
        ) { onboarding, firstLaunch, widgets, iconPack ->
            Quad(onboarding, firstLaunch, widgets, iconPack)
        }
    ) { pinned, style, layout, sort, quad ->
        PrefsGroup(pinned, style, layout, sort, quad.onboarding, quad.firstLaunch, quad.widgets, quad.iconPack)
    }

    private val searchFlow = combine(_searchQuery, _webSuggestions) { query, suggestions ->
        query to suggestions
    }

    private val dialogFlow = combine(
        _selectedAppForActions,
        _isSettingsOpen,
        _isQuickSettingsOpen,
        combine(_isAIAssistantOpen, _aiInitialPrompt, _isIconPackSelectorOpen, _installedIconPacks) { aiOpen, aiPrompt, packOpen, packs ->
            PackDialogGroup(aiOpen, aiPrompt, packOpen, packs)
        }
    ) { selected, isSettings, isQuick, packGroup ->
        DialogGroup(
            selectedApp = selected,
            isSettingsOpen = isSettings,
            isQuickSettingsOpen = isQuick,
            isAIAssistantOpen = packGroup.isAIAssistantOpen,
            aiInitialPrompt = packGroup.aiInitialPrompt,
            isIconPackSelectorOpen = packGroup.isIconPackSelectorOpen,
            installedIconPacks = packGroup.installedIconPacks
        )
    }

    val uiState: StateFlow<LauncherUiState> = combine(
        _isLoading,
        _rawApps,
        searchFlow,
        prefsFlow,
        dialogFlow
    ) { isLoading, rawApps, (query, suggestions), prefs, dialogs ->
        val appsWithPinned = rawApps.map { app ->
            val pinned = prefs.pinnedApps.contains(app.id)
            if (app.isPinned != pinned) app.copy(isPinned = pinned) else app
        }

        val pinnedList = appsWithPinned.filter { it.isPinned }

        val filteredList = if (query.isBlank()) {
            when (prefs.sortMode) {
                SortMode.USAGE_TIME -> {
                    appsWithPinned.sortedWith { a, b ->
                        val timeCompare = b.usageTimeMillis.compareTo(a.usageTimeMillis)
                        if (timeCompare != 0) timeCompare else collator.compare(a.label, b.label)
                    }
                }
                SortMode.ALPHABETICAL -> {
                    appsWithPinned
                }
            }
        } else {
            val q = query.trim().lowercase(Locale.getDefault())
            appsWithPinned.filter { app ->
                app.label.lowercase(Locale.getDefault()).contains(q) ||
                        app.packageName.lowercase(Locale.getDefault()).contains(q)
            }
        }

        val indexMap = mutableMapOf<Char, Int>()
        filteredList.forEachIndexed { index, app ->
            if (!indexMap.containsKey(app.sectionHeader)) {
                indexMap[app.sectionHeader] = index
            }
        }

        val distinctLetters = indexMap.keys.toList()

        LauncherUiState(
            isLoading = isLoading,
            apps = filteredList,
            pinnedApps = pinnedList,
            searchQuery = query,
            webSuggestions = suggestions,
            letterIndexMap = indexMap,
            availableLetters = distinctLetters,
            iconStyle = prefs.iconStyle,
            layoutMode = prefs.layoutMode,
            sortMode = prefs.sortMode,
            selectedAppForActions = dialogs.selectedApp,
            isSettingsOpen = dialogs.isSettingsOpen,
            isQuickSettingsOpen = dialogs.isQuickSettingsOpen,
            isAIAssistantOpen = dialogs.isAIAssistantOpen,
            aiInitialPrompt = dialogs.aiInitialPrompt,
            isIconPackSelectorOpen = dialogs.isIconPackSelectorOpen,
            installedIconPacks = dialogs.installedIconPacks,
            selectedIconPack = prefs.selectedIconPack,
            weather = _weather.value,
            accentPreset = accentPreset.value,
            onboardingCompleted = prefs.onboardingCompleted,
            isFirstLaunchDone = prefs.isFirstLaunchDone,
            widgetIds = prefs.widgetIds
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = LauncherUiState()
    )

    init {
        BatteryMonitor.startMonitoring(application)
        loadInstalledApps()
        checkMediaPermissions()
        startBackgroundUsageRefresh()

        viewModelScope.launch {
            accentPreset.collect { preset ->
                if (_rawApps.value.isNotEmpty()) {
                    reprocessIconsForAccent(preset)
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        BatteryMonitor.stopMonitoring(getApplication())
    }

    fun checkMediaPermissions() {
        MediaManager.isNotificationAccessGranted(getApplication())
    }

    fun setAccentColor(preset: AccentColorPreset) {
        viewModelScope.launch {
            themePreferences.setAccentColor(preset)
        }
    }

    fun completeOnboarding() {
        preferences.setOnboardingCompleted(true)
    }

    fun setFirstLaunchDone(done: Boolean) {
        preferences.setFirstLaunchDone(done)
    }

    fun toggleLayoutMode() {
        preferences.toggleLayoutMode()
    }

    fun setLayoutMode(mode: LayoutMode) {
        preferences.setLayoutMode(mode)
    }

    fun toggleSortMode() {
        preferences.toggleSortMode()
    }

    fun setSortMode(mode: SortMode) {
        preferences.setSortMode(mode)
    }

    fun setQuickSettingsOpen(isOpen: Boolean) {
        _isQuickSettingsOpen.value = isOpen
    }

    fun setAIAssistantOpen(isOpen: Boolean, prompt: String = "") {
        _aiInitialPrompt.value = prompt
        _isAIAssistantOpen.value = isOpen
    }

    fun setIconPackSelectorOpen(isOpen: Boolean) {
        if (isOpen) {
            viewModelScope.launch(Dispatchers.IO) {
                _installedIconPacks.value = IconPackManager.getInstalledIconPacks(getApplication())
            }
        }
        _isIconPackSelectorOpen.value = isOpen
    }

    fun selectIconPack(packPackageName: String) {
        preferences.setSelectedIconPack(packPackageName)
        viewModelScope.launch {
            _isLoading.value = true
            withContext(Dispatchers.IO) {
                IconPackManager.loadIconPack(getApplication(), packPackageName)
                reprocessIconsForCurrentPack()
            }
            _isLoading.value = false
        }
    }

    fun addWidget(id: Int) {
        preferences.addWidgetId(id)
    }

    fun removeWidget(id: Int) {
        preferences.removeWidgetId(id)
    }

    fun playPauseMedia() {
        MediaManager.playPause()
    }

    fun nextMedia() {
        MediaManager.next()
    }

    fun previousMedia() {
        MediaManager.previous()
    }

    fun seekMedia(positionMs: Long) {
        MediaManager.seekTo(positionMs)
    }

    fun reloadApps() {
        loadInstalledApps()
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        searchDebounceJob?.cancel()
        if (query.trim().length >= 2) {
            searchDebounceJob = viewModelScope.launch {
                delay(250)
                val suggestions = WebSearchHelper.getSearchSuggestions(query)
                _webSuggestions.value = suggestions
            }
        } else {
            _webSuggestions.value = emptyList()
        }
    }

    fun clearSearch() {
        _searchQuery.value = ""
        _webSuggestions.value = emptyList()
        searchDebounceJob?.cancel()
    }

    fun launchWebSearch(context: Context, query: String) {
        WebSearchHelper.launchWebSearch(context, query)
        clearSearch()
    }

    fun selectAppForActions(app: AppItemUiState?) {
        _selectedAppForActions.value = app
    }

    fun setSettingsOpen(isOpen: Boolean) {
        _isSettingsOpen.value = isOpen
    }

    fun togglePin(app: AppItemUiState) {
        preferences.togglePin(app.id)
    }

    fun setIconStyle(style: IconDisplayStyle) {
        preferences.setIconStyle(style)
    }

    fun launchApp(context: Context, app: AppItemUiState) {
        launchApp(context, app.packageName, app.activityName, app.label)
    }

    fun launchApp(context: Context, packageName: String, activityName: String? = null, label: String = "") {
        try {
            if (activityName != null) {
                val intent = Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_LAUNCHER)
                    component = ComponentName(packageName, activityName)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
                }
                context.startActivity(intent)
                return
            }
            val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launchIntent)
            } else {
                Toast.makeText(context, "Cannot open $label", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            try {
                val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
                if (launchIntent != null) {
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(launchIntent)
                } else {
                    Toast.makeText(context, "Cannot open $label", Toast.LENGTH_SHORT).show()
                }
            } catch (err: Exception) {
                Toast.makeText(context, "Failed to launch $label", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun launchFirstResult(context: Context) {
        val state = uiState.value
        val query = state.searchQuery.trim()

        if (query.startsWith("?") || query.contains("?")) {
            val cleanedPrompt = query.removePrefix("?").trim()
            setAIAssistantOpen(true, cleanedPrompt)
            clearSearch()
            return
        }

        if (state.apps.isNotEmpty()) {
            launchApp(context, state.apps.first())
            clearSearch()
        } else if (query.isNotBlank()) {
            launchWebSearch(context, query)
        }
    }

    fun openAppDetails(context: Context, app: AppItemUiState) {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:${app.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Could not open settings", Toast.LENGTH_SHORT).show()
        }
    }

    fun requestUninstall(context: Context, app: AppItemUiState) {
        try {
            val intent = Intent(Intent.ACTION_DELETE).apply {
                data = Uri.parse("package:${app.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Cannot uninstall system app", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadInstalledApps() {
        viewModelScope.launch {
            _isLoading.value = true
            val apps = withContext(Dispatchers.IO) {
                scanApps()
            }
            _rawApps.value = apps
            _isLoading.value = false
        }
    }

    private fun startBackgroundUsageRefresh() {
        viewModelScope.launch(Dispatchers.IO) {
            while (isActive) {
                delay(5 * 60 * 1000L) // Debounced 5 minute refresh
                try {
                    val usageMap = UsageStatsHelper.getTodayUsageStats(getApplication())
                    val currentList = _rawApps.value
                    if (currentList.isNotEmpty()) {
                        val updated = currentList.map { app ->
                            val millis = usageMap[app.packageName] ?: 0L
                            val text = if (millis > 0) UsageStatsHelper.formatUsageTime(millis) else null
                            if (app.usageTimeMillis != millis || app.usageTimeText != text) {
                                app.copy(usageTimeMillis = millis, usageTimeText = text)
                            } else {
                                app
                            }
                        }
                        _rawApps.value = updated
                    }
                } catch (e: Exception) {
                    // Suppress background metric errors
                }
            }
        }
    }

    private suspend fun reprocessIconsForAccent(preset: AccentColorPreset) = withContext(Dispatchers.Default) {
        val currentPack = preferences.selectedIconPack.value
        val currentApps = _rawApps.value
        val updated = currentApps.map { app ->
            val customIcon = if (currentPack.isNotBlank()) {
                IconPackManager.getIconForApp(app.packageName, app.activityName)
            } else null

            val finalIcon = customIcon ?: run {
                val drawable = rawDrawables[app.id]
                val (imageBitmap, _) = IconProcessor.processIcon(
                    context = getApplication(),
                    drawable = drawable,
                    cacheKey = app.id,
                    accentColor = preset.color
                )
                imageBitmap
            }
            app.copy(iconBitmap = finalIcon)
        }
        _rawApps.value = updated
    }

    private suspend fun reprocessIconsForCurrentPack() = withContext(Dispatchers.Default) {
        val currentApps = _rawApps.value
        val currentAccent = accentPreset.value.color
        val updated = currentApps.map { app ->
            val customIcon = IconPackManager.getIconForApp(app.packageName, app.activityName)
            val finalIcon = customIcon ?: run {
                val drawable = rawDrawables[app.id]
                val (defaultIcon, _) = IconProcessor.processIcon(
                    context = getApplication(),
                    drawable = drawable,
                    cacheKey = app.id,
                    accentColor = currentAccent
                )
                defaultIcon
            }
            app.copy(iconBitmap = finalIcon)
        }
        _rawApps.value = updated
    }

    private suspend fun scanApps(): List<AppItemUiState> = withContext(Dispatchers.IO) {
        val intent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        val resolveInfos: List<ResolveInfo> = try {
            packageManager.queryIntentActivities(intent, 0)
        } catch (e: Exception) {
            emptyList()
        }

        val currentPackage = getApplication<Application>().packageName
        val list = mutableListOf<AppItemUiState>()
        val seenIds = mutableSetOf<String>()
        val currentAccent = accentPreset.value.color
        val selectedPack = preferences.selectedIconPack.value

        if (selectedPack.isNotBlank()) {
            IconPackManager.loadIconPack(getApplication(), selectedPack)
        }

        val usageMap = UsageStatsHelper.getTodayUsageStats(getApplication())

        for (info in resolveInfos) {
            val packageName = info.activityInfo.packageName
            val activityName = info.activityInfo.name

            if (packageName == currentPackage) continue

            val id = "$packageName/$activityName"
            if (seenIds.contains(id)) continue
            seenIds.add(id)

            val label = try {
                info.loadLabel(packageManager).toString().trim()
            } catch (e: Exception) {
                packageName
            }

            val firstChar = label.firstOrNull()?.uppercaseChar() ?: '#'
            val sectionHeader = if (firstChar.isLetter()) firstChar else '#'

            val usageMillis = usageMap[packageName] ?: 0L
            val usageFormatted = if (usageMillis > 0) UsageStatsHelper.formatUsageTime(usageMillis) else null

            val drawable = try {
                info.loadIcon(packageManager)
            } catch (e: Exception) {
                null
            }

            if (drawable != null) {
                rawDrawables[id] = drawable
            }

            // Priority 1: Check Icon Pack
            val packIcon = IconPackManager.getIconForApp(packageName, activityName)
            val finalIcon = if (packIcon != null) {
                packIcon
            } else {
                // Priority 2: System Monochrome / AMOLED Wireframe Fallback
                val (defaultIcon, _) = IconProcessor.processIcon(
                    context = getApplication(),
                    drawable = drawable,
                    cacheKey = id,
                    accentColor = currentAccent
                )
                defaultIcon
            }

            list.add(
                AppItemUiState(
                    packageName = packageName,
                    activityName = activityName,
                    label = label,
                    sectionHeader = sectionHeader,
                    usageTimeMillis = usageMillis,
                    usageTimeText = usageFormatted,
                    iconBitmap = finalIcon,
                    isPinned = false
                )
            )
        }

        return@withContext list.sortedWith { a, b ->
            val charA = a.sectionHeader
            val charB = b.sectionHeader
            if (charA == '#' && charB != '#') {
                1
            } else if (charA != '#' && charB == '#') {
                -1
            } else {
                collator.compare(a.label, b.label)
            }
        }
    }

    private data class Quad(
        val onboarding: Boolean,
        val firstLaunch: Boolean,
        val widgets: List<Int>,
        val iconPack: String
    )

    private data class PrefsGroup(
        val pinnedApps: Set<String>,
        val iconStyle: IconDisplayStyle,
        val layoutMode: LayoutMode,
        val sortMode: SortMode,
        val onboardingCompleted: Boolean,
        val isFirstLaunchDone: Boolean,
        val widgetIds: List<Int>,
        val selectedIconPack: String
    )

    private data class PackDialogGroup(
        val isAIAssistantOpen: Boolean,
        val aiInitialPrompt: String,
        val isIconPackSelectorOpen: Boolean,
        val installedIconPacks: List<IconPackInfo>
    )

    private data class DialogGroup(
        val selectedApp: AppItemUiState?,
        val isSettingsOpen: Boolean,
        val isQuickSettingsOpen: Boolean,
        val isAIAssistantOpen: Boolean,
        val aiInitialPrompt: String,
        val isIconPackSelectorOpen: Boolean,
        val installedIconPacks: List<IconPackInfo>
    )
}
