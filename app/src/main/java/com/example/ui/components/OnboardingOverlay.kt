package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.ColorLens
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Swipe
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.GraySubtle
import com.example.ui.theme.GrayText
import com.example.ui.theme.LocalAccentColor
import com.example.ui.theme.OffWhite
import com.example.ui.theme.PureBlack
import com.example.ui.theme.PureWhite
import com.example.ui.theme.WireframeBorder

@Composable
fun OnboardingOverlay(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accentColor = LocalAccentColor.current
    var currentStep by remember { mutableIntStateOf(1) }
    val totalSteps = 4

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.88f))
            .statusBarsPadding()
            .navigationBarsPadding()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { /* Consume clicks outside */ }
            .testTag("onboarding_overlay")
    ) {
        // Step specific visual target highlights
        when (currentStep) {
            1 -> {
                // Highlight at top right (list/grid toggle & settings)
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 10.dp, end = 12.dp)
                        .size(90.dp, 44.dp)
                        .border(1.5.dp, accentColor, RoundedCornerShape(8.dp))
                )
            }
            2 -> {
                // Highlight along the right edge (alphabet scroller rail)
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .fillMaxHeight(0.65f)
                        .width(36.dp)
                        .border(1.5.dp, accentColor, RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp))
                )
            }
            3 -> {
                // Gesture indicator across the screen center
                Row(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .fillMaxWidth(0.8f)
                        .padding(bottom = 120.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "<< СВАЙП ВЛІВО <<",
                        color = accentColor,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 2.sp
                    )
                }
            }
            4 -> {
                // Central icon highlight for color & customization
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(bottom = 120.dp)
                        .size(70.dp)
                        .border(1.5.dp, accentColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ColorLens,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }
        }

        // Bottom Tutorial Card
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 24.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(PureBlack)
                .border(1.dp, accentColor, RoundedCornerShape(16.dp))
                .padding(22.dp)
        ) {
            // Step counter & Progress dots
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ІНТЕРАКТИВНИЙ ГІД • $currentStep / $totalSteps",
                    color = accentColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 1.sp
                )

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    for (i in 1..totalSteps) {
                        Box(
                            modifier = Modifier
                                .size(if (i == currentStep) 8.dp else 6.dp)
                                .clip(CircleShape)
                                .background(if (i == currentStep) accentColor else GraySubtle)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Animated step content
            AnimatedContent(
                targetState = currentStep,
                transitionSpec = {
                    if (targetState > initialState) {
                        (slideInHorizontally { width -> width } + fadeIn()).togetherWith(
                            slideOutHorizontally { width -> -width } + fadeOut()
                        )
                    } else {
                        (slideInHorizontally { width -> -width } + fadeIn()).togetherWith(
                            slideOutHorizontally { width -> width } + fadeOut()
                        )
                    }
                },
                label = "step_animation"
            ) { step ->
                when (step) {
                    1 -> StepContent(
                        icon = Icons.Outlined.GridView,
                        title = "Список чи Сітка",
                        description = "Торкніться тут, щоб перемикати вигляд між мінімальним 1-колонковим списком та 4-колонковою сіткою."
                    )
                    2 -> StepContent(
                        icon = Icons.Outlined.TouchApp,
                        title = "Алфавітний скролер",
                        description = "Проведіть пальцем уздовж правого краю, щоб миттєво знайти додаток за літерою."
                    )
                    3 -> StepContent(
                        icon = Icons.Outlined.Swipe,
                        title = "Дошка віджетів",
                        description = "Свайпніть вліво, щоб перейти до дошки системних віджетів Android."
                    )
                    4 -> StepContent(
                        icon = Icons.Outlined.ColorLens,
                        title = "Кольорова тема",
                        description = "Затисніть порожнє місце або відкрийте налаштування, щоб вибрати власний колір акцентних ліній."
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ПРОПУСТИТИ",
                    color = GrayText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 1.sp,
                    modifier = Modifier
                        .clickable { onDismiss() }
                        .padding(8.dp)
                )

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(DarkSurface)
                        .border(1.dp, accentColor, RoundedCornerShape(8.dp))
                        .clickable {
                            if (currentStep < totalSteps) {
                                currentStep++
                            } else {
                                onDismiss()
                            }
                        }
                        .padding(horizontal = 20.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = if (currentStep < totalSteps) "ДАЛІ" else "ПОЧАТИ",
                        color = accentColor,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 1.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun StepContent(
    icon: ImageVector,
    title: String,
    description: String
) {
    val accentColor = LocalAccentColor.current

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(DarkSurface)
                .border(1.dp, accentColor, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = PureWhite,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = description,
                color = OffWhite,
                fontSize = 13.sp,
                fontWeight = FontWeight.Normal,
                lineHeight = 18.sp
            )
        }
    }
}
