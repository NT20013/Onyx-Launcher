package com.example

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.test.core.app.ApplicationProvider
import com.example.model.AppItemUiState
import com.example.model.AppModel
import com.example.model.LayoutMode
import com.example.model.SortMode
import com.example.ui.components.parseMarkdownToAnnotatedString
import com.example.util.ChatMessage
import com.example.util.UsageStatsHelper
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Onyx", appName)
    }

    @Test
    fun `app model id formatting`() {
        val app = AppModel(
            packageName = "com.test.app",
            activityName = "com.test.app.MainActivity",
            label = "Test App",
            sectionHeader = 'T'
        )
        assertEquals("com.test.app/com.test.app.MainActivity", app.id)
    }

    @Test
    fun `app item ui state formatting`() {
        val app = AppItemUiState(
            packageName = "com.test.app",
            activityName = "com.test.app.MainActivity",
            label = "Test App",
            sectionHeader = 'T',
            usageTimeText = "15 хв"
        )
        assertEquals("com.test.app/com.test.app.MainActivity", app.id)
        assertEquals("15 хв", app.usageTimeText)
        assertEquals("T", app.firstCharUpper)
    }

    @Test
    fun `accent presets fallback to clean white`() {
        val preset = com.example.data.AccentPresets.fromHex("#UNKNOWN")
        assertEquals(com.example.data.AccentPresets.CleanWhite.hexString, preset.hexString)
    }

    @Test
    fun `layout mode enum values`() {
        assertEquals(LayoutMode.LIST, LayoutMode.valueOf("LIST"))
        assertEquals(LayoutMode.GRID, LayoutMode.valueOf("GRID"))
    }

    @Test
    fun `sort mode enum values`() {
        assertEquals(SortMode.ALPHABETICAL, SortMode.valueOf("ALPHABETICAL"))
        assertEquals(SortMode.USAGE_TIME, SortMode.valueOf("USAGE_TIME"))
    }

    @Test
    fun `usage time formatting`() {
        assertEquals("15хв", UsageStatsHelper.formatUsageTime(15 * 60 * 1000L))
        assertEquals("1г 15хв", UsageStatsHelper.formatUsageTime((75) * 60 * 1000L))
        assertEquals("2г", UsageStatsHelper.formatUsageTime(120 * 60 * 1000L))
    }

    @Test
    fun `chat message data model`() {
        val msg = ChatMessage(role = "user", text = "Hello Gemini")
        assertEquals("user", msg.role)
        assertEquals("Hello Gemini", msg.text)
    }

    @Test
    fun `markdown parser formatting test`() = runBlocking {
        val markdown = "# Heading\n**Bold Text** and *Italic Text*\n- Bullet item"
        val result = parseMarkdownToAnnotatedString(markdown, Color.Cyan)
        assertTrue(result.text.contains("Heading"))
        assertTrue(result.text.contains("Bold Text"))
        assertTrue(result.text.contains("•  Bullet item"))
    }
}
