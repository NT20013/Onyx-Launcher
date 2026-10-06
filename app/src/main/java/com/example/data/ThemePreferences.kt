package com.example.data

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "onyx_theme_preferences")

data class AccentColorPreset(
    val name: String,
    val color: Color,
    val hexString: String
)

object AccentPresets {
    val CleanWhite = AccentColorPreset("Clean White", Color(0xFFFFFFFF), "#FFFFFF")
    val AmberOrange = AccentColorPreset("Amber Orange", Color(0xFFFFB300), "#FFB300")
    val CyberpunkLime = AccentColorPreset("Cyberpunk Lime", Color(0xFFAEEA00), "#AEEA00")
    val ElectricBlue = AccentColorPreset("Electric Blue", Color(0xFF00E5FF), "#00E5FF")
    val NeonViolet = AccentColorPreset("Neon Violet", Color(0xFFD500F9), "#D500F9")
    val CarmineRed = AccentColorPreset("Carmine Red", Color(0xFFFF1744), "#FF1744")
    val EmeraldGreen = AccentColorPreset("Emerald Green", Color(0xFF00E676), "#00E676")

    val allPresets = listOf(
        CleanWhite,
        AmberOrange,
        CyberpunkLime,
        ElectricBlue,
        NeonViolet,
        CarmineRed,
        EmeraldGreen
    )

    fun fromHex(hex: String?): AccentColorPreset {
        return allPresets.firstOrNull { it.hexString.equals(hex, ignoreCase = true) }
            ?: CleanWhite
    }
}

class ThemePreferences(private val context: Context) {

    private val accentKey = stringPreferencesKey("accent_color_hex")

    val accentColorFlow: Flow<AccentColorPreset> = context.dataStore.data.map { preferences ->
        val hex = preferences[accentKey] ?: AccentPresets.CleanWhite.hexString
        AccentPresets.fromHex(hex)
    }

    suspend fun setAccentColor(preset: AccentColorPreset) {
        context.dataStore.edit { preferences ->
            preferences[accentKey] = preset.hexString
        }
    }
}
