package com.example.ui.components

import android.app.Activity
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Widgets
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.GraySubtle
import com.example.ui.theme.GrayText
import com.example.ui.theme.LocalAccentColor
import com.example.ui.theme.PureBlack
import com.example.ui.theme.PureWhite
import com.example.ui.theme.WireframeBorder

const val ONYX_APPWIDGET_HOST_ID = 2048

@Composable
fun WidgetBoardScreen(
    widgetIds: List<Int>,
    onAddWidgetId: (Int) -> Unit,
    onRemoveWidgetId: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val accentColor = LocalAccentColor.current

    val appWidgetHost = remember { AppWidgetHost(context, ONYX_APPWIDGET_HOST_ID) }
    val appWidgetManager = remember { AppWidgetManager.getInstance(context) }

    DisposableEffect(appWidgetHost) {
        try {
            appWidgetHost.startListening()
        } catch (e: Exception) {
            // Ignored
        }
        onDispose {
            try {
                appWidgetHost.stopListening()
            } catch (e: Exception) {
                // Ignored
            }
        }
    }

    var pendingWidgetId by remember { mutableIntStateOf(-1) }

    val configureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && pendingWidgetId != -1) {
            onAddWidgetId(pendingWidgetId)
        } else if (pendingWidgetId != -1) {
            appWidgetHost.deleteAppWidgetId(pendingWidgetId)
        }
        pendingWidgetId = -1
    }

    val widgetPickLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val widgetId = result.data?.getIntExtra(
                AppWidgetManager.EXTRA_APPWIDGET_ID,
                -1
            ) ?: -1

            if (widgetId != -1) {
                val appWidgetInfo = appWidgetManager.getAppWidgetInfo(widgetId)
                if (appWidgetInfo?.configure != null) {
                    pendingWidgetId = widgetId
                    val configIntent = Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE).apply {
                        component = appWidgetInfo.configure
                        putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                    }
                    configureLauncher.launch(configIntent)
                } else {
                    onAddWidgetId(widgetId)
                }
            }
        } else if (pendingWidgetId != -1) {
            appWidgetHost.deleteAppWidgetId(pendingWidgetId)
            pendingWidgetId = -1
        }
    }

    fun startPickWidget() {
        val newWidgetId = appWidgetHost.allocateAppWidgetId()
        pendingWidgetId = newWidgetId
        val pickIntent = Intent(AppWidgetManager.ACTION_APPWIDGET_PICK).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, newWidgetId)
        }
        widgetPickLauncher.launch(pickIntent)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(PureBlack)
            .padding(horizontal = 20.dp)
            .testTag("widget_board_screen")
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "WIDGET BOARD",
                    color = accentColor,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Light,
                    letterSpacing = 2.sp
                )
                Text(
                    text = "Android System Widgets",
                    color = GraySubtle,
                    fontSize = 12.sp
                )
            }

            // Minimalist Wireframe Add Button
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(DarkSurface)
                    .border(1.dp, accentColor, RoundedCornerShape(8.dp))
                    .clickable { startPickWidget() }
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .testTag("add_widget_button"),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.Add,
                    contentDescription = "Add Widget",
                    tint = accentColor,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "ADD",
                    color = accentColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 1.sp
                )
            }
        }

        if (widgetIds.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 60.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .border(1.dp, accentColor.copy(alpha = 0.5f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Widgets,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "NO WIDGETS ADDED",
                        color = PureWhite,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Light,
                        letterSpacing = 1.5.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Tap '+ ADD' above to place any system or app widget here.\nSwipe right to return to app list.",
                        color = GraySubtle,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp
                    )
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(
                    items = widgetIds,
                    key = { it }
                ) { widgetId ->
                    val appWidgetInfo = appWidgetManager.getAppWidgetInfo(widgetId)
                    if (appWidgetInfo != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(DarkSurface)
                                .border(1.dp, WireframeBorder, RoundedCornerShape(12.dp))
                                .padding(8.dp)
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = appWidgetInfo.loadLabel(context.packageManager) ?: "Widget",
                                        color = GraySubtle,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Normal,
                                        modifier = Modifier.padding(start = 4.dp)
                                    )

                                    IconButton(
                                        onClick = {
                                            appWidgetHost.deleteAppWidgetId(widgetId)
                                            onRemoveWidgetId(widgetId)
                                        },
                                        modifier = Modifier.size(20.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Close,
                                            contentDescription = "Remove Widget",
                                            tint = GrayText,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }

                                AndroidView(
                                    factory = { ctx ->
                                        try {
                                            appWidgetHost.createView(ctx, widgetId, appWidgetInfo).apply {
                                                setAppWidget(widgetId, appWidgetInfo)
                                            }
                                        } catch (e: Exception) {
                                            AppWidgetHostView(ctx)
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 100.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
