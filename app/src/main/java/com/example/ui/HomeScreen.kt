package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.ViewList
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AppItemUiState
import com.example.model.LayoutMode
import com.example.model.SortMode
import com.example.ui.components.AIAssistantBottomSheet
import com.example.ui.components.AlphabetWaveScroller
import com.example.ui.components.AppActionsBottomSheet
import com.example.ui.components.AppGridView
import com.example.ui.components.AppListView
import com.example.ui.components.ClockWidget
import com.example.ui.components.IconPackSelectorBottomSheet
import com.example.ui.components.LauncherSettingsDialog
import com.example.ui.components.MediaPlayerWidget
import com.example.ui.components.QuickSettingsBottomSheet
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.GraySubtle
import com.example.ui.theme.GrayText
import com.example.ui.theme.LocalAccentColor
import com.example.ui.theme.OffWhite
import com.example.ui.theme.PureBlack
import com.example.ui.theme.PureWhite
import com.example.ui.theme.WireframeBorderSubtle
import com.example.util.PermissionHelper
import com.example.viewmodel.LauncherViewModel
import kotlinx.coroutines.launch

@Immutable
sealed class LauncherListItem {
    abstract val key: String
    abstract val contentType: String

    data class SectionHeader(val title: String) : LauncherListItem() {
        override val key: String = "header_$title"
        override val contentType: String = "section_header"
    }

    data class AppItem(val app: AppItemUiState) : LauncherListItem() {
        override val key: String = "app_${app.id}"
        override val contentType: String = "app_row"
    }

    data class AIAssistantSuggestionItem(val prompt: String) : LauncherListItem() {
        override val key: String = "ai_prompt_$prompt"
        override val contentType: String = "ai_suggestion"
    }

    data class WebSuggestionItem(val query: String) : LauncherListItem() {
        override val key: String = "web_$query"
        override val contentType: String = "web_suggestion"
    }
}

@Composable
fun HomeScreen(
    viewModel: LauncherViewModel,
    modifier: Modifier = Modifier,
    onScrollerDraggingChanged: (Boolean) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val accentPreset by viewModel.accentPreset.collectAsState()

    val context = LocalContext.current
    val listState = rememberLazyListState()
    val gridState = rememberLazyGridState()
    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val searchFocusRequester = remember { FocusRequester() }
    val accentColor = LocalAccentColor.current

    // Memoize stable click callbacks to prevent recomposition allocations
    val onAppClick = remember<(AppItemUiState) -> Unit>(viewModel) {
        { app -> viewModel.launchApp(context, app) }
    }
    val onAppLongClick = remember<(AppItemUiState) -> Unit>(viewModel) {
        { app -> viewModel.selectAppForActions(app) }
    }
    val onWebSearchClick = remember<(String) -> Unit>(viewModel) {
        { query -> viewModel.launchWebSearch(context, query) }
    }
    val onAIAssistantClick = remember<(String) -> Unit>(viewModel) {
        { prompt -> viewModel.setAIAssistantOpen(true, prompt) }
    }

    // Pure Alphabetical or Usage Time list (NO categorization overhead)
    val listItems = remember(uiState.apps, uiState.searchQuery, uiState.webSuggestions, uiState.sortMode) {
        val items = mutableListOf<LauncherListItem>()

        if (uiState.searchQuery.isNotBlank()) {
            val query = uiState.searchQuery.trim()

            // If query contains '?' or user is asking something, offer Gemini Assistant at top
            if (query.startsWith("?") || query.contains("?")) {
                items.add(LauncherListItem.AIAssistantSuggestionItem(query.removePrefix("?").trim()))
            }

            // 1. Local App matches
            uiState.apps.forEach { items.add(LauncherListItem.AppItem(it)) }

            // Offer AI suggestion if not already at top
            if (!query.startsWith("?") && !query.contains("?")) {
                items.add(LauncherListItem.AIAssistantSuggestionItem(query))
            }

            // 2. Google Suggest Web Completions
            if (uiState.webSuggestions.isNotEmpty()) {
                items.add(LauncherListItem.SectionHeader("Web Search"))
                uiState.webSuggestions.forEach { suggestion ->
                    items.add(LauncherListItem.WebSuggestionItem(suggestion))
                }
            }
        } else {
            // Pure flat browsing: Alphabetical (A-Z) or Usage Time
            when (uiState.sortMode) {
                SortMode.USAGE_TIME -> {
                    var hasUsedSection = false
                    var hasOtherSection = false
                    for (app in uiState.apps) {
                        if (app.usageTimeMillis > 0) {
                            if (!hasUsedSection) {
                                items.add(LauncherListItem.SectionHeader("Використовувались сьогодні"))
                                hasUsedSection = true
                            }
                        } else {
                            if (!hasOtherSection) {
                                items.add(LauncherListItem.SectionHeader("Не запускались"))
                                hasOtherSection = true
                            }
                        }
                        items.add(LauncherListItem.AppItem(app))
                    }
                }
                SortMode.ALPHABETICAL -> {
                    var currentHeader: Char? = null
                    for (app in uiState.apps) {
                        if (app.sectionHeader != currentHeader) {
                            currentHeader = app.sectionHeader
                            items.add(LauncherListItem.SectionHeader(currentHeader.toString()))
                        }
                        items.add(LauncherListItem.AppItem(app))
                    }
                }
            }
        }
        items
    }

    // Map each letter to its position in listItems for fast scrolling
    val letterToListIndexMap = remember(listItems) {
        val map = mutableMapOf<Char, Int>()
        listItems.forEachIndexed { index, item ->
            if (item is LauncherListItem.SectionHeader && item.title.length == 1) {
                val char = item.title.first()
                if (!map.containsKey(char)) {
                    map[char] = index
                }
            }
        }
        map
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(PureBlack)
            .imePadding()
            // Long-press on empty space to open quick settings bottom sheet
            .pointerInput(Unit) {
                detectTapGestures(
                    onLongPress = {
                        viewModel.setQuickSettingsOpen(true)
                    }
                )
            }
            .testTag("home_screen_root")
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Top Section: Clock & Weather Widget + Battery + Layout Toggle & Settings icons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    ClockWidget(
                        weatherInfo = uiState.weather,
                        onSettingsClick = { viewModel.setSettingsOpen(true) }
                    )
                }

                // Quick Layout Toggle (List <-> Grid)
                IconButton(
                    onClick = { viewModel.toggleLayoutMode() },
                    modifier = Modifier
                        .size(38.dp)
                        .testTag("layout_toggle_button")
                ) {
                    Icon(
                        imageVector = if (uiState.layoutMode == LayoutMode.LIST) Icons.Outlined.GridView else Icons.Outlined.ViewList,
                        contentDescription = "Toggle List/Grid",
                        tint = GrayText,
                        modifier = Modifier.size(19.dp)
                    )
                }

                IconButton(
                    onClick = { viewModel.setSettingsOpen(true) },
                    modifier = Modifier
                        .size(38.dp)
                        .testTag("settings_button")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Settings,
                        contentDescription = "Launcher Settings",
                        tint = GrayText,
                        modifier = Modifier.size(19.dp)
                    )
                }
            }

            // Universal Minimalist Media Player Widget
            MediaPlayerWidget(
                viewModel = viewModel,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
            )

            // Minimalist Hybrid Search Bar with integrated Gemini Flash Lite Sparkle icon
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SearchBar(
                    query = uiState.searchQuery,
                    onQueryChange = { viewModel.updateSearchQuery(it) },
                    onClear = { viewModel.clearSearch() },
                    onSearchSubmit = {
                        viewModel.launchFirstResult(context)
                        focusManager.clearFocus()
                    },
                    focusRequester = searchFocusRequester,
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Gemini Flash Lite AI Assistant Button
                IconButton(
                    onClick = { viewModel.setAIAssistantOpen(true, uiState.searchQuery) },
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(DarkSurfaceVariant)
                        .border(1.dp, accentColor.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                        .testTag("gemini_assistant_button")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.AutoAwesome,
                        contentDescription = "Gemini Flash Lite",
                        tint = accentColor,
                        modifier = Modifier.size(19.dp)
                    )
                }
            }

            // Pinned Apps Quick Row (visible only when search query is blank)
            if (uiState.searchQuery.isBlank() && uiState.pinnedApps.isNotEmpty()) {
                PinnedAppsRow(
                    pinnedApps = uiState.pinnedApps,
                    onAppClick = onAppClick,
                    onAppLongClick = onAppLongClick,
                    modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Main Content: Pure Alphabetical App list/grid and Alphabet Wave Scroller
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (uiState.isLoading) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = accentColor,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                } else if (listItems.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (uiState.searchQuery.isNotBlank()) "No apps found" else "No apps installed",
                            color = GraySubtle,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Light,
                            letterSpacing = 1.sp
                        )
                    }
                } else {
                    val showScroller = uiState.searchQuery.isBlank() &&
                            uiState.sortMode == SortMode.ALPHABETICAL &&
                            uiState.availableLetters.isNotEmpty()

                    val scrollerPadding = if (showScroller) 38.dp else 0.dp

                    if (uiState.layoutMode == LayoutMode.LIST) {
                        AppListView(
                            items = listItems,
                            listState = listState,
                            iconStyle = uiState.iconStyle,
                            onAppClick = onAppClick,
                            onAppLongClick = onAppLongClick,
                            onWebSearchClick = onWebSearchClick,
                            onAIAssistantClick = onAIAssistantClick,
                            rightPadding = scrollerPadding
                        )
                    } else {
                        AppGridView(
                            items = listItems,
                            gridState = gridState,
                            iconStyle = uiState.iconStyle,
                            onAppClick = onAppClick,
                            onAppLongClick = onAppLongClick,
                            onWebSearchClick = onWebSearchClick,
                            onAIAssistantClick = onAIAssistantClick,
                            rightPadding = scrollerPadding
                        )
                    }

                    // Physical Wave / Fisheye Alphabet Scroller on the right
                    if (showScroller) {
                        AlphabetWaveScroller(
                            availableLetters = uiState.availableLetters,
                            onLetterSelected = { letter ->
                                val targetIndex = letterToListIndexMap[letter]
                                if (targetIndex != null) {
                                    coroutineScope.launch {
                                        if (uiState.layoutMode == LayoutMode.LIST) {
                                            listState.scrollToItem(targetIndex)
                                        } else {
                                            gridState.scrollToItem(targetIndex)
                                        }
                                    }
                                }
                            },
                            onDraggingChanged = onScrollerDraggingChanged,
                            modifier = Modifier.align(Alignment.CenterEnd)
                        )
                    }
                }
            }
        }

        // Long-press Actions Bottom Sheet on an app
        if (uiState.selectedAppForActions != null) {
            val selectedApp = uiState.selectedAppForActions!!
            AppActionsBottomSheet(
                app = selectedApp,
                onDismiss = { viewModel.selectAppForActions(null) },
                onOpenApp = { viewModel.launchApp(context, selectedApp) },
                onTogglePin = { viewModel.togglePin(selectedApp) },
                onAppInfo = { viewModel.openAppDetails(context, selectedApp) },
                onUninstall = { viewModel.requestUninstall(context, selectedApp) }
            )
        }

        // Quick Settings Bottom Sheet triggered by empty space long-press
        if (uiState.isQuickSettingsOpen) {
            QuickSettingsBottomSheet(
                currentAccent = accentPreset,
                currentSortMode = uiState.sortMode,
                currentLayoutMode = uiState.layoutMode,
                selectedIconPack = uiState.selectedIconPack,
                onSelectAccent = { viewModel.setAccentColor(it) },
                onSelectSortMode = { viewModel.setSortMode(it) },
                onSelectLayoutMode = { viewModel.setLayoutMode(it) },
                onOpenIconPackSelector = {
                    viewModel.setQuickSettingsOpen(false)
                    viewModel.setIconPackSelectorOpen(true)
                },
                onDismiss = { viewModel.setQuickSettingsOpen(false) }
            )
        }

        // Custom Icon Pack Selector Bottom Sheet
        if (uiState.isIconPackSelectorOpen) {
            IconPackSelectorBottomSheet(
                installedPacks = uiState.installedIconPacks,
                selectedPackPackage = uiState.selectedIconPack,
                onSelectPack = { viewModel.selectIconPack(it) },
                onDismiss = { viewModel.setIconPackSelectorOpen(false) }
            )
        }

        // Gemini Flash Lite AI Assistant Bottom Sheet
        if (uiState.isAIAssistantOpen) {
            AIAssistantBottomSheet(
                initialPrompt = uiState.aiInitialPrompt,
                onDismiss = { viewModel.setAIAssistantOpen(false) }
            )
        }

        // Settings Dialog
        if (uiState.isSettingsOpen) {
            LauncherSettingsDialog(
                currentStyle = uiState.iconStyle,
                currentAccent = accentPreset,
                currentLayoutMode = uiState.layoutMode,
                currentSortMode = uiState.sortMode,
                totalAppsCount = uiState.apps.size,
                hasMediaPermission = PermissionHelper.isNotificationAccessGranted(context),
                onSelectStyle = { viewModel.setIconStyle(it) },
                onSelectAccent = { viewModel.setAccentColor(it) },
                onSelectLayoutMode = { viewModel.setLayoutMode(it) },
                onSelectSortMode = { viewModel.setSortMode(it) },
                onDismiss = { viewModel.setSettingsOpen(false) }
            )
        }
    }
}

@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit,
    onSearchSubmit: () -> Unit,
    focusRequester: FocusRequester,
    modifier: Modifier = Modifier
) {
    val accentColor = LocalAccentColor.current

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(DarkSurfaceVariant)
            .border(1.dp, accentColor.copy(alpha = 0.25f), RoundedCornerShape(8.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .testTag("search_bar"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Outlined.Search,
            contentDescription = "Search",
            tint = GrayText,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))

        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.CenterStart
        ) {
            if (query.isEmpty()) {
                Text(
                    text = "Пошук або запитання (?)",
                    color = GraySubtle,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Light
                )
            }
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                cursorBrush = SolidColor(accentColor),
                textStyle = TextStyle(
                    color = PureWhite,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Normal
                ),
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Search
                ),
                keyboardActions = KeyboardActions(
                    onSearch = { onSearchSubmit() }
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
                    .testTag("search_text_input")
            )
        }

        if (query.isNotEmpty()) {
            IconButton(
                onClick = onClear,
                modifier = Modifier
                    .size(24.dp)
                    .testTag("clear_search_button")
            ) {
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = "Clear search",
                    tint = GrayText,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun PinnedAppsRow(
    pinnedApps: List<AppItemUiState>,
    onAppClick: (AppItemUiState) -> Unit,
    onAppLongClick: (AppItemUiState) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val accentColor = LocalAccentColor.current

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        for (app in pinnedApps) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(DarkSurface)
                    .border(1.dp, WireframeBorderSubtle, RoundedCornerShape(6.dp))
                    .clickable { onAppClick(app) }
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.Star,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = app.label,
                    color = OffWhite,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Normal
                )
            }
        }
    }
}
