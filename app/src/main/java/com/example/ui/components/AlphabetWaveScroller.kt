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
import androidx.compose.ui.graphics.graphicsLayer
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
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.roundToInt

@Composable
fun AlphabetWaveScroller(
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

    // Floating index calculated with high precision
    val currentFloatIndex by remember(availableLetters) {
        derivedStateOf {
            if (!isDragging || touchY < 0f || scrollerHeight <= 0f || availableLetters.isEmpty()) {
                -1f
            } else {
                val itemHeight = scrollerHeight / availableLetters.size.toFloat()
                (touchY / itemHeight).coerceIn(0f, (availableLetters.size - 1).toFloat())
            }
        }
    }

    // Discrete letter mapped from the nearest rounded index to guarantee exact finger alignment
    val currentLetter by remember(availableLetters) {
        derivedStateOf {
            if (currentFloatIndex < 0f || availableLetters.isEmpty()) {
                null
            } else {
                val idx = currentFloatIndex.roundToInt().coerceIn(0, availableLetters.lastIndex)
                availableLetters[idx]
            }
        }
    }

    // Trigger haptic feedback and scroll only when the discrete letter actually changes
    LaunchedEffect(currentLetter) {
        val letter = currentLetter ?: return@LaunchedEffect
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        onLetterSelected(letter)
    }

    // Lock horizontal pager scrolling when user is dragging the alphabet rail
    LaunchedEffect(isDragging) {
        onDraggingChanged(isDragging)
    }

    Box(
        modifier = modifier
            .fillMaxHeight()
            .width(60.dp)
            .testTag("alphabet_wave_scroller_container")
    ) {
        // Floating Letter Preview Badge centered on finger touch point
        AnimatedVisibility(
            visible = isDragging && currentLetter != null,
            enter = fadeIn() + scaleIn(initialScale = 0.85f),
            exit = fadeOut() + scaleOut(targetScale = 0.85f),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset {
                    val badgeSizePx = with(density) { 56.dp.toPx() }
                    val yOffset = (touchY - badgeSizePx / 2f)
                        .coerceIn(0f, (scrollerHeight - badgeSizePx).coerceAtLeast(0f))
                    IntOffset(
                        x = with(density) { (-48).dp.roundToPx() },
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

        // Vertical Alphabet Rail with Gaussian Wave displacement
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .width(36.dp)
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
            val maxShiftPx = with(density) { 26.dp.toPx() }
            val baseRailShiftPx = with(density) { 6.dp.toPx() }
            val spread = 2.2f // Gaussian spread factor

            for (index in availableLetters.indices) {
                val letter = availableLetters[index]
                val isSelected = currentLetter == letter

                Box(
                    modifier = Modifier
                        .graphicsLayer {
                            if (isDragging && currentFloatIndex >= 0f) {
                                val distance = abs(currentFloatIndex - index)
                                // Exact Gaussian distribution bell curve
                                val gaussian = exp(- (distance * distance) / (2f * spread * spread)).toFloat()

                                val scale = 1f + (0.85f * gaussian)
                                scaleX = scale
                                scaleY = scale
                                translationX = -(baseRailShiftPx + (gaussian * maxShiftPx))
                            } else {
                                scaleX = 1f
                                scaleY = 1f
                                translationX = 0f
                            }
                        }
                        .padding(vertical = 0.5.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = letter.toString(),
                        color = if (isSelected) accentColor else GraySubtle,
                        fontSize = if (isSelected) 13.sp else 10.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
