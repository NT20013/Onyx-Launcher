package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.model.WeatherCondition
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun WireframeWeatherIcon(
    condition: WeatherCondition,
    tint: Color,
    modifier: Modifier = Modifier,
    size: Dp = 18.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val strokeWidth = 1.4.dp.toPx()
        val stroke = Stroke(
            width = strokeWidth,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )

        val w = this.size.width
        val h = this.size.height

        when (condition) {
            WeatherCondition.CLEAR_SUNNY -> {
                val center = Offset(w / 2f, h / 2f)
                val sunRadius = w * 0.25f
                drawCircle(color = tint, radius = sunRadius, center = center, style = stroke)

                val rayLength = w * 0.16f
                val rayStart = sunRadius + w * 0.08f
                for (i in 0 until 8) {
                    val angle = Math.toRadians(i * 45.0)
                    val cosA = cos(angle).toFloat()
                    val sinA = sin(angle).toFloat()
                    val p1 = Offset(center.x + rayStart * cosA, center.y + rayStart * sinA)
                    val p2 = Offset(center.x + (rayStart + rayLength) * cosA, center.y + (rayStart + rayLength) * sinA)
                    drawLine(color = tint, start = p1, end = p2, strokeWidth = strokeWidth, cap = StrokeCap.Round)
                }
            }

            WeatherCondition.PARTLY_CLOUDY -> {
                // Sun peek top right
                val sunCenter = Offset(w * 0.72f, h * 0.32f)
                val sunRadius = w * 0.18f
                drawCircle(color = tint, radius = sunRadius, center = sunCenter, style = stroke)

                // Cloud in front
                val path = Path().apply {
                    moveTo(w * 0.2f, h * 0.75f)
                    lineTo(w * 0.8f, h * 0.75f)
                    cubicTo(w * 0.95f, h * 0.75f, w * 0.95f, h * 0.55f, w * 0.8f, h * 0.55f)
                    cubicTo(w * 0.8f, h * 0.38f, w * 0.6f, h * 0.38f, w * 0.5f, h * 0.48f)
                    cubicTo(w * 0.42f, h * 0.45f, w * 0.25f, h * 0.48f, w * 0.22f, h * 0.6f)
                    cubicTo(w * 0.1f, h * 0.62f, w * 0.1f, h * 0.75f, w * 0.2f, h * 0.75f)
                    close()
                }
                drawPath(path = path, color = tint, style = stroke)
            }

            WeatherCondition.CLOUDY -> {
                val path = Path().apply {
                    moveTo(w * 0.18f, h * 0.7f)
                    lineTo(w * 0.82f, h * 0.7f)
                    cubicTo(w * 0.96f, h * 0.7f, w * 0.96f, h * 0.48f, w * 0.8f, h * 0.48f)
                    cubicTo(w * 0.78f, h * 0.28f, w * 0.52f, h * 0.28f, w * 0.46f, h * 0.42f)
                    cubicTo(w * 0.36f, h * 0.38f, w * 0.22f, h * 0.44f, w * 0.2f, h * 0.56f)
                    cubicTo(w * 0.08f, h * 0.58f, w * 0.08f, h * 0.7f, w * 0.18f, h * 0.7f)
                    close()
                }
                drawPath(path = path, color = tint, style = stroke)
            }

            WeatherCondition.RAINY -> {
                val cloudPath = Path().apply {
                    moveTo(w * 0.18f, h * 0.6f)
                    lineTo(w * 0.82f, h * 0.6f)
                    cubicTo(w * 0.95f, h * 0.6f, w * 0.95f, h * 0.42f, w * 0.8f, h * 0.42f)
                    cubicTo(w * 0.78f, h * 0.25f, w * 0.52f, h * 0.25f, w * 0.46f, h * 0.38f)
                    cubicTo(w * 0.36f, h * 0.34f, w * 0.22f, h * 0.4f, w * 0.2f, h * 0.5f)
                    cubicTo(w * 0.08f, h * 0.52f, w * 0.08f, h * 0.6f, w * 0.18f, h * 0.6f)
                    close()
                }
                drawPath(path = cloudPath, color = tint, style = stroke)

                // Rain drops (slanted lines)
                val dropLen = h * 0.16f
                val drops = listOf(w * 0.32f, w * 0.5f, w * 0.68f)
                drops.forEach { x ->
                    drawLine(
                        color = tint,
                        start = Offset(x, h * 0.68f),
                        end = Offset(x - w * 0.06f, h * 0.68f + dropLen),
                        strokeWidth = strokeWidth,
                        cap = StrokeCap.Round
                    )
                }
            }

            WeatherCondition.SNOWY -> {
                // Delicate asterisk snowflake
                val center = Offset(w / 2f, h / 2f)
                val radius = w * 0.38f
                for (i in 0 until 6) {
                    val angle = Math.toRadians(i * 60.0)
                    val cosA = cos(angle).toFloat()
                    val sinA = sin(angle).toFloat()
                    val end = Offset(center.x + radius * cosA, center.y + radius * sinA)
                    drawLine(color = tint, start = center, end = end, strokeWidth = strokeWidth, cap = StrokeCap.Round)
                }
            }

            WeatherCondition.THUNDERSTORM -> {
                val cloudPath = Path().apply {
                    moveTo(w * 0.18f, h * 0.52f)
                    lineTo(w * 0.82f, h * 0.52f)
                    cubicTo(w * 0.95f, h * 0.52f, w * 0.95f, h * 0.36f, w * 0.8f, h * 0.36f)
                    cubicTo(w * 0.78f, h * 0.2f, w * 0.52f, h * 0.2f, w * 0.46f, h * 0.32f)
                    cubicTo(w * 0.36f, h * 0.3f, w * 0.22f, h * 0.34f, w * 0.2f, h * 0.44f)
                    cubicTo(w * 0.08f, h * 0.46f, w * 0.08f, h * 0.52f, w * 0.18f, h * 0.52f)
                    close()
                }
                drawPath(path = cloudPath, color = tint, style = stroke)

                // Lightning bolt
                val boltPath = Path().apply {
                    moveTo(w * 0.52f, h * 0.56f)
                    lineTo(w * 0.44f, h * 0.72f)
                    lineTo(w * 0.56f, h * 0.72f)
                    lineTo(w * 0.46f, h * 0.92f)
                }
                drawPath(path = boltPath, color = tint, style = stroke)
            }
        }
    }
}
