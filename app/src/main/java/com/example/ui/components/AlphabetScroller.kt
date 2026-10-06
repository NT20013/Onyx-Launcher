package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.GraySubtle
import com.example.ui.theme.LocalAccentColor
import com.example.ui.theme.PureBlack
import kotlin.math.roundToInt

@Composable
fun AlphabetScroller(
    availableLetters: List<Char>,
    onLetterSelected: (Char) -> Unit,
    modifier: Modifier = Modifier,
    onDraggingChanged: (Boolean) -> Unit = {}
) {
    if (availableLetters.isEmpty()) return

    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    val accentColor = LocalAccentColor.current

    var isDragging by remember { mutableStateOf(false) }
    var touchY by remember { mutableFloatStateOf(-1f) }
    var scrollerHeight by remember { mutableFloatStateOf(1f) }

    // Use derivedStateOf to prevent state thrashing: only re-evaluates when resulting letter changes
    val currentLetter by remember(availableLetters) {
        derivedStateOf {
            if (!isDragging || touchY < 0f || scrollerHeight <= 0f || availableLetters.isEmpty()) {
                null
            } else {
                val fraction = (touchY / scrollerHeight).coerceIn(0f, 0.999f)
                val idx = (fraction * availableLetters.size).toInt().coerceIn(0, availableLetters.lastIndex)
                availableLetters[idx]
            }
        }
    }

    // Trigger haptic and scroll only when the discrete letter actually changes
    LaunchedEffect(currentLetter) {
        val letter = currentLetter ?: return@LaunchedEffect
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        onLetterSelected(letter)
    }

    // Notify parent pager to lock horizontal scrolling when user is dragging the alphabet rail
    LaunchedEffect(isDragging) {
        onDraggingChanged(isDragging)
    }

    Box(
        modifier = modifier
            .fillMaxHeight()
            .width(54.dp)
            .testTag("alphabet_scroller_container")
    ) {
        // Floating Letter Preview Badge near finger
        AnimatedVisibility(
            visible = isDragging && currentLetter != null,
            enter = fadeIn() + scaleIn(initialScale = 0.85f),
            exit = fadeOut() + scaleOut(targetScale = 0.85f),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset {
                    val badgeSizePx = with(density) { 56.dp.toPx() }
                    val yOffset = (touchY - badgeSizePx / 2)
                        .coerceIn(0f, (scrollerHeight - badgeSizePx).coerceAtLeast(0f))
                    IntOffset(
                        x = with(density) { (-44).dp.roundToPx() },
                        y = yOffset.roundToInt()
                    )
                }
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(PureBlack)
                    .border(1.5.dp, accentColor, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = currentLetter?.toString() ?: "",
                    color = accentColor,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }
        }

        // Vertical Alphabet Rail with Initial pass event consumption to block HorizontalPager
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .width(28.dp)
                .align(Alignment.CenterEnd)
                .onGloballyPositioned { coordinates ->
                    scrollerHeight = coordinates.size.height.toFloat()
                }
                .pointerInput(availableLetters) {
                    awaitEachGesture {
                        val down = awaitFirstDown(pass = PointerEventPass.Initial, requireUnconsumed = false)
                        down.consume()
                        isDragging = true
                        touchY = down.position.y

                        while (true) {
                            val event = awaitPointerEvent(pass = PointerEventPass.Initial)
                            val change = event.changes.firstOrNull() ?: break
                            if (!change.pressed) {
                                break
                            }
                            change.consume()
                            touchY = change.position.y
                        }

                        isDragging = false
                        touchY = -1f
                    }
                },
            verticalArrangement = Arrangement.SpaceEvenly,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            for (letter in availableLetters) {
                val isSelected = currentLetter == letter
                Text(
                    text = letter.toString(),
                    color = if (isSelected) accentColor else GraySubtle,
                    fontSize = if (isSelected) 13.sp else 10.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(vertical = 0.5.dp)
                )
            }
        }
    }
}
