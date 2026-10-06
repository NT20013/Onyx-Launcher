package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.model.IconDisplayStyle
import com.example.model.LayoutMode
import com.example.model.SortMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class LauncherPreferences(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("onyx_launcher_prefs", Context.MODE_PRIVATE)

    private val _pinnedApps = MutableStateFlow<Set<String>>(loadPinnedApps())
    val pinnedApps: StateFlow<Set<String>> = _pinnedApps.asStateFlow()

    private val _iconStyle = MutableStateFlow(loadIconStyle())
    val iconStyle: StateFlow<IconDisplayStyle> = _iconStyle.asStateFlow()

    private val _layoutMode = MutableStateFlow(loadLayoutMode())
    val layoutMode: StateFlow<LayoutMode> = _layoutMode.asStateFlow()

    private val _sortMode = MutableStateFlow(loadSortMode())
    val sortMode: StateFlow<SortMode> = _sortMode.asStateFlow()

    private val _onboardingCompleted = MutableStateFlow(prefs.getBoolean(KEY_ONBOARDING_COMPLETED, false))
    val onboardingCompleted: StateFlow<Boolean> = _onboardingCompleted.asStateFlow()

    private val _isFirstLaunchDone = MutableStateFlow(prefs.getBoolean(KEY_FIRST_LAUNCH_DONE, false))
    val isFirstLaunchDone: StateFlow<Boolean> = _isFirstLaunchDone.asStateFlow()

    private val _widgetIds = MutableStateFlow<List<Int>>(loadWidgetIds())
    val widgetIds: StateFlow<List<Int>> = _widgetIds.asStateFlow()

    private val _selectedIconPack = MutableStateFlow(prefs.getString(KEY_SELECTED_ICON_PACK, "") ?: "")
    val selectedIconPack: StateFlow<String> = _selectedIconPack.asStateFlow()

    private fun loadPinnedApps(): Set<String> {
        return prefs.getStringSet(KEY_PINNED_APPS, emptySet()) ?: emptySet()
    }

    private fun loadIconStyle(): IconDisplayStyle {
        val name = prefs.getString(KEY_ICON_STYLE, IconDisplayStyle.WIREFRAME_ICON.name)
        return try {
            IconDisplayStyle.valueOf(name ?: IconDisplayStyle.WIREFRAME_ICON.name)
        } catch (e: Exception) {
            IconDisplayStyle.WIREFRAME_ICON
        }
    }

    private fun loadLayoutMode(): LayoutMode {
        val name = prefs.getString(KEY_LAYOUT_MODE, LayoutMode.LIST.name)
        return try {
            LayoutMode.valueOf(name ?: LayoutMode.LIST.name)
        } catch (e: Exception) {
            LayoutMode.LIST
        }
    }

    private fun loadSortMode(): SortMode {
        val name = prefs.getString(KEY_SORT_MODE, SortMode.ALPHABETICAL.name)
        return try {
            SortMode.valueOf(name ?: SortMode.ALPHABETICAL.name)
        } catch (e: Exception) {
            SortMode.ALPHABETICAL
        }
    }

    private fun loadWidgetIds(): List<Int> {
        val str = prefs.getString(KEY_WIDGET_IDS, "") ?: ""
        if (str.isBlank()) return emptyList()
        return str.split(",").mapNotNull { it.trim().toIntOrNull() }
    }

    fun togglePin(appId: String) {
        val current = _pinnedApps.value.toMutableSet()
        if (current.contains(appId)) {
            current.remove(appId)
        } else {
            current.add(appId)
        }
        prefs.edit().putStringSet(KEY_PINNED_APPS, current).apply()
        _pinnedApps.value = current
    }

    fun setIconStyle(style: IconDisplayStyle) {
        prefs.edit().putString(KEY_ICON_STYLE, style.name).apply()
        _iconStyle.value = style
    }

    fun setLayoutMode(mode: LayoutMode) {
        prefs.edit().putString(KEY_LAYOUT_MODE, mode.name).apply()
        _layoutMode.value = mode
    }

    fun toggleLayoutMode() {
        val newMode = if (_layoutMode.value == LayoutMode.LIST) LayoutMode.GRID else LayoutMode.LIST
        setLayoutMode(newMode)
    }

    fun setSortMode(mode: SortMode) {
        prefs.edit().putString(KEY_SORT_MODE, mode.name).apply()
        _sortMode.value = mode
    }

    fun toggleSortMode() {
        val newMode = if (_sortMode.value == SortMode.ALPHABETICAL) SortMode.USAGE_TIME else SortMode.ALPHABETICAL
        setSortMode(newMode)
    }

    fun setOnboardingCompleted(completed: Boolean) {
        prefs.edit().putBoolean(KEY_ONBOARDING_COMPLETED, completed).apply()
        _onboardingCompleted.value = completed
    }

    fun setFirstLaunchDone(done: Boolean) {
        prefs.edit().putBoolean(KEY_FIRST_LAUNCH_DONE, done).apply()
        _isFirstLaunchDone.value = done
    }

    fun addWidgetId(id: Int) {
        val current = _widgetIds.value.toMutableList()
        if (!current.contains(id)) {
            current.add(id)
            saveWidgetIds(current)
        }
    }

    fun removeWidgetId(id: Int) {
        val current = _widgetIds.value.toMutableList()
        current.remove(id)
        saveWidgetIds(current)
    }

    fun setSelectedIconPack(packageName: String) {
        prefs.edit().putString(KEY_SELECTED_ICON_PACK, packageName).apply()
        _selectedIconPack.value = packageName
    }

    private fun saveWidgetIds(ids: List<Int>) {
        val str = ids.joinToString(",")
        prefs.edit().putString(KEY_WIDGET_IDS, str).apply()
        _widgetIds.value = ids
    }

    companion object {
        private const val KEY_PINNED_APPS = "pinned_apps"
        private const val KEY_ICON_STYLE = "icon_style"
        private const val KEY_LAYOUT_MODE = "layout_mode"
        private const val KEY_SORT_MODE = "sort_mode"
        private const val KEY_ONBOARDING_COMPLETED = "onboarding_completed"
        private const val KEY_FIRST_LAUNCH_DONE = "is_first_launch_done"
        private const val KEY_WIDGET_IDS = "widget_ids"
        private const val KEY_SELECTED_ICON_PACK = "selected_icon_pack_package"
    }
}
