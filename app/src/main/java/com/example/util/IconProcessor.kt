package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.LruCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs

/**
 * High-performance asynchronous icon processing & caching engine.
 * Converts raw application drawables into pre-baked, GPU-ready ImageBitmaps
 * with theme accent color. NO ColorFilters, Canvas or PM calls during list scrolling.
 */
object IconProcessor {

    private const val ICON_SIZE = 72 // Optimal for 120 FPS rendering without high memory overhead
    private val imageBitmapCache = LruCache<String, ImageBitmap>(350)

    /**
     * Retrieves or produces a ready-to-render ImageBitmap strictly on Dispatchers.Default.
     */
    suspend fun processIcon(
        context: Context,
        drawable: Drawable?,
        cacheKey: String,
        accentColor: Color
    ): Pair<ImageBitmap?, Boolean> = withContext(Dispatchers.Default) {
        if (drawable == null) return@withContext Pair(null, false)

        val accentArgb = accentColor.toArgb()
        val fullCacheKey = "$cacheKey-$accentArgb"

        val cached = imageBitmapCache.get(fullCacheKey)
        if (cached != null) {
            return@withContext Pair(cached, false)
        }

        var isMonochrome = false
        val finalBitmap: Bitmap

        // 1. Check for official Monochrome Adaptive Icon (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && drawable is AdaptiveIconDrawable) {
            val monoDrawable = drawable.monochrome
            if (monoDrawable != null) {
                isMonochrome = true
                val bitmap = Bitmap.createBitmap(ICON_SIZE, ICON_SIZE, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)
                monoDrawable.setBounds(0, 0, ICON_SIZE, ICON_SIZE)
                monoDrawable.colorFilter = PorterDuffColorFilter(accentArgb, PorterDuff.Mode.SRC_IN)
                monoDrawable.draw(canvas)
                val imageBitmap = bitmap.asImageBitmap()
                imageBitmapCache.put(fullCacheKey, imageBitmap)
                return@withContext Pair(imageBitmap, true)
            }
        }

        // 2. High-contrast wireframe edge / contour conversion
        val sourceDrawable = if (drawable is AdaptiveIconDrawable) {
            drawable.foreground ?: drawable
        } else {
            drawable
        }

        val sourceBitmap = Bitmap.createBitmap(ICON_SIZE, ICON_SIZE, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(sourceBitmap)
        sourceDrawable.setBounds(0, 0, ICON_SIZE, ICON_SIZE)
        sourceDrawable.draw(canvas)

        finalBitmap = convertToWireframeOutline(sourceBitmap, accentArgb)
        val imageBitmap = finalBitmap.asImageBitmap()
        imageBitmapCache.put(fullCacheKey, imageBitmap)
        return@withContext Pair(imageBitmap, isMonochrome)
    }

    /**
     * Converts a full-color bitmap into an ultra-clean, accent-tinted wireframe contour
     * on a transparent background using direct IntArray manipulation.
     */
    private fun convertToWireframeOutline(src: Bitmap, tintColor: Int): Bitmap {
        val width = src.width
        val height = src.height
        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        val srcPixels = IntArray(width * height)
        val outPixels = IntArray(width * height)
        src.getPixels(srcPixels, 0, width, 0, 0, width, height)

        val luma = IntArray(width * height)
        val alpha = IntArray(width * height)

        for (i in srcPixels.indices) {
            val color = srcPixels[i]
            val a = (color ushr 24) and 0xFF
            val r = (color ushr 16) and 0xFF
            val g = (color ushr 8) and 0xFF
            val b = color and 0xFF
            alpha[i] = a
            luma[i] = (0.299f * r + 0.587f * g + 0.114f * b).toInt()
        }

        val transparent = 0x00000000

        for (y in 1 until height - 1) {
            val rowOffset = y * width
            for (x in 1 until width - 1) {
                val idx = rowOffset + x
                val a = alpha[idx]

                if (a < 35) {
                    outPixels[idx] = transparent
                    continue
                }

                // Check alpha boundary (silhouette contour)
                val aTop = alpha[idx - width]
                val aBottom = alpha[idx + width]
                val aLeft = alpha[idx - 1]
                val aRight = alpha[idx + 1]

                val isSilhouetteEdge = aTop < 35 || aBottom < 35 || aLeft < 35 || aRight < 35

                if (isSilhouetteEdge) {
                    outPixels[idx] = tintColor
                    continue
                }

                // Internal Sobel edge detection for internal wireframe lines
                val p00 = luma[idx - width - 1]
                val p02 = luma[idx - width + 1]
                val p10 = luma[idx - 1]
                val p12 = luma[idx + 1]
                val p20 = luma[idx + width - 1]
                val p22 = luma[idx + width + 1]

                val p01 = luma[idx - width]
                val p21 = luma[idx + width]

                val gx = (-p00 + p02) + 2 * (-p10 + p12) + (-p20 + p22)
                val gy = (-p00 - 2 * p01 - p02) + (p20 + 2 * p21 + p22)
                val gradient = abs(gx) + abs(gy)

                if (gradient > 75) {
                    outPixels[idx] = tintColor
                } else {
                    outPixels[idx] = transparent
                }
            }
        }

        output.setPixels(outPixels, 0, width, 0, 0, width, height)
        return output
    }
}
