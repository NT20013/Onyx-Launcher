package com.example.util

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.content.res.Resources
import android.content.res.XmlResourceParser
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.LruCache
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.InputStream

@Immutable
data class IconPackInfo(
    val packageName: String,
    val name: String,
    val icon: ImageBitmap? = null,
    val isDefault: Boolean = false
)

object IconPackManager {

    private val THEME_INTENT_ACTIONS = listOf(
        "org.adw.launcher.THEMES",
        "com.novalauncher.THEME",
        "com.gau.go.launcherex.theme",
        "com.teslacoilsw.launcher.THEME"
    )

    private val iconCache = LruCache<String, ImageBitmap>(300)
    private val appFilterMap = mutableMapOf<String, String>()

    @Volatile
    var currentLoadedPack: String = ""
        private set

    private var cachedResources: Resources? = null

    /**
     * Finds all installed third-party icon packs on the device.
     */
    fun getInstalledIconPacks(context: Context): List<IconPackInfo> {
        val pm = context.packageManager
        val installedPacks = mutableMapOf<String, IconPackInfo>()

        for (action in THEME_INTENT_ACTIONS) {
            val intent = Intent(action)
            val resolveInfos: List<ResolveInfo> = try {
                pm.queryIntentActivities(intent, PackageManager.GET_META_DATA)
            } catch (e: Exception) {
                emptyList()
            }

            for (ri in resolveInfos) {
                val pkgName = ri.activityInfo.packageName
                if (pkgName == context.packageName || installedPacks.containsKey(pkgName)) continue

                val label = try {
                    ri.loadLabel(pm).toString().trim()
                } catch (e: Exception) {
                    pkgName
                }

                val iconBitmap = try {
                    val drawable = ri.loadIcon(pm)
                    drawableToImageBitmap(drawable)
                } catch (e: Exception) {
                    null
                }

                installedPacks[pkgName] = IconPackInfo(
                    packageName = pkgName,
                    name = label,
                    icon = iconBitmap,
                    isDefault = false
                )
            }
        }

        val result = mutableListOf<IconPackInfo>()
        // Default minimalist monochrome option always at top
        result.add(
            IconPackInfo(
                packageName = "",
                name = "За замовчуванням (Мінімалістичний монохром)",
                icon = null,
                isDefault = true
            )
        )
        result.addAll(installedPacks.values.sortedBy { it.name })
        return result
    }

    /**
     * Loads and parses appfilter.xml from the selected icon pack.
     * Guaranteed to execute asynchronously on Dispatchers.IO.
     */
    suspend fun loadIconPack(context: Context, packPackageName: String) = withContext(Dispatchers.IO) {
        if (packPackageName.isBlank()) {
            currentLoadedPack = ""
            cachedResources = null
            appFilterMap.clear()
            iconCache.evictAll()
            return@withContext
        }

        if (packPackageName == currentLoadedPack && appFilterMap.isNotEmpty()) {
            return@withContext
        }

        val pm = context.packageManager
        val res = try {
            pm.getResourcesForApplication(packPackageName)
        } catch (e: Exception) {
            currentLoadedPack = ""
            cachedResources = null
            appFilterMap.clear()
            iconCache.evictAll()
            return@withContext
        }

        cachedResources = res
        currentLoadedPack = packPackageName
        appFilterMap.clear()
        iconCache.evictAll()

        // 1. Try parsing from res/xml/appfilter.xml
        var parsedSuccessfully = false
        val resId = try {
            res.getIdentifier("appfilter", "xml", packPackageName)
        } catch (e: Exception) {
            0
        }

        if (resId != 0) {
            try {
                val parser: XmlResourceParser = res.getXml(resId)
                parseAppFilterXml(parser)
                parsedSuccessfully = true
            } catch (e: Exception) {
                parsedSuccessfully = false
            }
        }

        // 2. Fallback to assets/appfilter.xml
        if (!parsedSuccessfully || appFilterMap.isEmpty()) {
            try {
                val packContext = context.createPackageContext(packPackageName, Context.CONTEXT_IGNORE_SECURITY)
                val inputStream: InputStream = packContext.assets.open("appfilter.xml")
                val factory = XmlPullParserFactory.newInstance()
                val parser = factory.newPullParser()
                parser.setInput(inputStream, "UTF-8")
                parseAppFilterXml(parser)
                inputStream.close()
            } catch (e: Exception) {
                // Ignore missing assets
            }
        }
    }

    private fun parseAppFilterXml(parser: XmlPullParser) {
        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG) {
                val tagName = parser.name
                if (tagName == "item" || tagName == "calendar") {
                    val component = parser.getAttributeValue(null, "component")
                    val drawableName = parser.getAttributeValue(null, "drawable")
                    if (!component.isNullOrEmpty() && !drawableName.isNullOrEmpty()) {
                        val cleanedComponent = cleanComponentInfo(component)
                        if (cleanedComponent.isNotEmpty()) {
                            appFilterMap[cleanedComponent] = drawableName

                            // Also index pure package name as fallback
                            val slashIdx = cleanedComponent.indexOf('/')
                            if (slashIdx > 0) {
                                val pkgOnly = cleanedComponent.substring(0, slashIdx)
                                if (!appFilterMap.containsKey(pkgOnly)) {
                                    appFilterMap[pkgOnly] = drawableName
                                }
                            }
                        }
                    }
                }
            }
            eventType = parser.next()
        }
    }

    private fun cleanComponentInfo(component: String): String {
        // Formats usually come as ComponentInfo{com.example.app/com.example.app.MainActivity}
        var clean = component.trim()
        if (clean.startsWith("ComponentInfo{") && clean.endsWith("}")) {
            clean = clean.substring(14, clean.length - 1).trim()
        }
        return clean
    }

    /**
     * Retrieves the custom icon for a specific app package/activity.
     * Uses in-memory LruCache for locked 120 FPS scrolling performance.
     */
    fun getIconForApp(packageName: String, activityName: String?): ImageBitmap? {
        if (currentLoadedPack.isBlank() || cachedResources == null) return null

        val cacheKey = if (!activityName.isNullOrEmpty()) "$packageName/$activityName" else packageName

        iconCache.get(cacheKey)?.let { return it }

        val drawableName = appFilterMap[cacheKey] ?: appFilterMap[packageName] ?: return null
        val res = cachedResources ?: return null

        val iconId = try {
            res.getIdentifier(drawableName, "drawable", currentLoadedPack)
        } catch (e: Exception) {
            0
        }

        if (iconId == 0) return null

        val drawable = try {
            res.getDrawable(iconId, null)
        } catch (e: Exception) {
            null
        } ?: return null

        val bitmap = drawableToImageBitmap(drawable) ?: return null
        iconCache.put(cacheKey, bitmap)
        return bitmap
    }

    private fun drawableToImageBitmap(drawable: Drawable): ImageBitmap? {
        return try {
            if (drawable is BitmapDrawable && drawable.bitmap != null) {
                return drawable.bitmap.asImageBitmap()
            }
            val width = drawable.intrinsicWidth.takeIf { it > 0 } ?: 96
            val height = drawable.intrinsicHeight.takeIf { it > 0 } ?: 96
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
            bitmap.asImageBitmap()
        } catch (e: Exception) {
            null
        }
    }
}
