package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.SkipNext
import androidx.compose.material.icons.outlined.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.media.MediaPlaybackState
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.GraySubtle
import com.example.ui.theme.LocalAccentColor
import com.example.ui.theme.OffWhite
import com.example.ui.theme.PureBlack
import com.example.ui.theme.WireframeBorderSubtle
import com.example.viewmodel.LauncherViewModel
import java.util.Locale

@Composable
fun MediaPlayerWidget(
    viewModel: LauncherViewModel,
    modifier: Modifier = Modifier
) {
    // Isolated StateFlow subscription: only this composable recomposes on media position ticks
    val state by viewModel.mediaPlaybackState.collectAsState()
    val accentColor = LocalAccentColor.current

    AnimatedVisibility(
        visible = state.hasActiveSession,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically(),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(PureBlack)
                .border(1.dp, accentColor.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                .padding(horizontal = 14.dp, vertical = 10.dp)
                .testTag("media_player_widget")
        ) {
            // Track Info & Duration Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .border(1.dp, accentColor, CircleShape)
                )

                Spacer(modifier = Modifier.width(10.dp))

                val trackDisplay = if (state.artist.isNotBlank()) {
                    "${state.artist} — ${state.title}"
                } else {
                    state.title
                }

                Text(
                    text = trackDisplay,
                    color = OffWhite,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                if (state.durationMs > 0) {
                    val posStr = formatMillis(state.positionMs)
                    val durStr = formatMillis(state.durationMs)
                    Text(
                        text = "$posStr / $durStr",
                        color = GraySubtle,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Light,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Thin Wireframe Progress Bar with Seek capability
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(14.dp)
                    .pointerInput(state.durationMs) {
                        detectTapGestures { offset ->
                            if (state.durationMs > 0) {
                                val fraction = (offset.x / size.width).coerceIn(0f, 1f)
                                val seekPos = (fraction * state.durationMs).toLong()
                                viewModel.seekMedia(seekPos)
                            }
                        }
                    },
                contentAlignment = Alignment.CenterStart
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .background(WireframeBorderSubtle)
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth(state.progressFraction)
                        .height(2.dp)
                        .background(accentColor)
                )
            }

            Spacer(modifier = Modifier.height(2.dp))

            // Wireframe Transport Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { viewModel.previousMedia() },
                    modifier = Modifier.size(38.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.SkipPrevious,
                        contentDescription = "Previous Track",
                        tint = accentColor,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .border(1.dp, accentColor, CircleShape)
                        .clickable { viewModel.playPauseMedia() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (state.isPlaying) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
                        contentDescription = if (state.isPlaying) "Pause" else "Play",
                        tint = accentColor,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                IconButton(
                    onClick = { viewModel.nextMedia() },
                    modifier = Modifier.size(38.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.SkipNext,
                        contentDescription = "Next Track",
                        tint = accentColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

private fun formatMillis(millis: Long): String {
    val totalSec = millis / 1000
    val min = totalSec / 60
    val sec = totalSec % 60
    return String.format(Locale.US, "%02d:%02d", min, sec)
}
