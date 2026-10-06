package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.example.ui.components.OnboardingOverlay
import com.example.ui.components.PermissionOnboardingScreen
import com.example.ui.components.WidgetBoardScreen
import com.example.ui.theme.PureBlack
import com.example.viewmodel.LauncherViewModel
import kotlinx.coroutines.launch

@Composable
fun MainPagerScreen(
    viewModel: LauncherViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { 2 })
    val coroutineScope = rememberCoroutineScope()

    // Isolated gesture control: when user is interacting with the alphabet scroller,
    // horizontal pager scrolling is locked to completely eliminate gesture conflicts and frame drops!
    var isScrollerDragging by remember { mutableStateOf(false) }

    // Handle back button on widget page: return to home screen
    BackHandler(enabled = pagerState.currentPage == 1) {
        coroutineScope.launch {
            pagerState.animateScrollToPage(0)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(PureBlack)
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("main_pager_screen")
    ) {
        HorizontalPager(
            state = pagerState,
            beyondViewportPageCount = 0,
            userScrollEnabled = !isScrollerDragging,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            when (page) {
                0 -> {
                    HomeScreen(
                        viewModel = viewModel,
                        onScrollerDraggingChanged = { dragging ->
                            isScrollerDragging = dragging
                        }
                    )
                }
                1 -> {
                    WidgetBoardScreen(
                        widgetIds = uiState.widgetIds,
                        onAddWidgetId = { viewModel.addWidget(it) },
                        onRemoveWidgetId = { viewModel.removeWidget(it) }
                    )
                }
            }
        }

        // 1. Universal Dynamic Permission Setup Dialog
        if (!uiState.onboardingCompleted) {
            PermissionOnboardingScreen(
                onComplete = { viewModel.completeOnboarding() }
            )
        }
        // 2. Interactive Feature Tutorial Overlay (shown once after initial permissions setup)
        else if (!uiState.isFirstLaunchDone) {
            OnboardingOverlay(
                onDismiss = { viewModel.setFirstLaunchDone(true) }
            )
        }
    }
}
