package com.example.util

import android.content.Context
import android.content.pm.LauncherActivityInfo
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.LruCache
import kotlin.math.abs

object IconHelper {

    private val iconCache = LruCache<String, Bitmap>(200)
    private const val ICON_SIZE = 96

    fun getWireframeIcon(
        context: Context,
        drawable: Drawable?,
        cacheKey: String
    ): Pair<Bitmap?, Boolean> {
        if (drawable == null) return Pair(null, false)

        val cached = iconCache.get(cacheKey)
        if (cached != null) {
            return Pair(cached, false)
        }

        var isMonochrome = false
        val processedBitmap: Bitmap

        // 1. Check for official Monochrome Adaptive Icon (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && drawable is AdaptiveIconDrawable) {
            val monoDrawable = drawable.monochrome
            if (monoDrawable != null) {
                isMonochrome = true
                val bitmap = Bitmap.createBitmap(ICON_SIZE, ICON_SIZE, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)
                monoDrawable.setBounds(0, 0, ICON_SIZE, ICON_SIZE)
                // Tint purely white
                monoDrawable.colorFilter = PorterDuffColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN)
                monoDrawable.draw(canvas)
                iconCache.put(cacheKey, bitmap)
                return Pair(bitmap, true)
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

        processedBitmap = convertToWireframeOutline(sourceBitmap)
        iconCache.put(cacheKey, processedBitmap)
        return Pair(processedBitmap, isMonochrome)
    }

    /**
     * Converts a full-color bitmap into an ultra-clean, pure white wireframe contour
     * on a transparent background.
     */
    private fun convertToWireframeOutline(src: Bitmap): Bitmap {
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
            luma[i] = (0.299 * r + 0.587 * g + 0.114 * b).toInt()
        }

        val whiteColor = 0xFFFFFFFF.toInt()
        val transparent = 0x00000000

        // Detect silhouette perimeter edges & internal luminance gradients
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
                    outPixels[idx] = whiteColor
                    continue
                }

                // Internal Sobel edge detection for internal wireframe lines
                val p00 = luma[idx - width - 1]
                val p01 = luma[idx - width]
                val p02 = luma[idx - width + 1]
                val p10 = luma[idx - 1]
                val p12 = luma[idx + 1]
                val p20 = luma[idx + width - 1]
                val p21 = luma[idx + width]
                val p22 = luma[idx + width + 1]

                val gx = (-p00 + p02) + 2 * (-p10 + p12) + (-p20 + p22)
                val gy = (-p00 - 2 * p01 - p02) + (p20 + 2 * p21 + p22)
                val gradient = abs(gx) + abs(gy)

                if (gradient > 75) {
                    outPixels[idx] = whiteColor
                } else {
                    outPixels[idx] = transparent
                }
            }
        }

        output.setPixels(outPixels, 0, width, 0, 0, width, height)
        return output
    }
}
