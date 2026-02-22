/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.vpn.cache

import android.app.ActivityManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.LruCache
import com.windscribe.vpn.Windscribe.Companion.appContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import javax.inject.Inject
import javax.inject.Singleton

/**
 * LRU cache for app icons with background preloading
 * Optimizes memory usage by downsampling icons to 32dp x 32dp
 * Shared between mobile and TV modules for consistent performance
 */
@Singleton
class AppIconCache @Inject constructor() {
    private val logger = LoggerFactory.getLogger("app_icon_cache")

    // Calculate cache size as 1/8th of available memory
    private val maxMemory = (appContext.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager)
        .memoryClass * 1024 * 1024 / 8

    private val iconCache: LruCache<String, Bitmap> = object : LruCache<String, Bitmap>(maxMemory) {
        override fun sizeOf(key: String, bitmap: Bitmap): Int {
            return bitmap.byteCount
        }
    }

    // Target icon size in pixels (32dp)
    private val iconSizePx = (32 * appContext.resources.displayMetrics.density).toInt()

    /**
     * Preload all installed app icons in the background
     * Processes icons in chunks of 100 for better performance
     */
    fun preloadIcons(scope: CoroutineScope) {
        scope.launch(Dispatchers.IO) {
            try {
                val pm = appContext.packageManager
                val packages = pm.getInstalledApplications(PackageManager.GET_META_DATA)

                logger.info("Preloading ${packages.size} app icons...")

                // Process in chunks to avoid blocking
                packages.chunked(100).forEach { chunk ->
                    chunk.forEach { appInfo ->
                        try {
                            if (iconCache.get(appInfo.packageName) == null) {
                                val drawable = pm.getApplicationIcon(appInfo)
                                val bitmap = drawableToBitmap(drawable, iconSizePx, iconSizePx)
                                iconCache.put(appInfo.packageName, bitmap)
                            }
                        } catch (e: Exception) {
                            logger.debug("Error loading icon for ${appInfo.packageName}: ${e.message}")
                        }
                    }
                }

                logger.info("Icon preloading completed. Cache size: ${iconCache.size()}")
            } catch (e: Exception) {
                logger.error("Error during icon preloading: ${e.message}")
            }
        }
    }

    /**
     * Get icon bitmap for a package, loading and caching if needed
     */
    fun getIcon(packageName: String): Bitmap? {
        // Check cache first
        iconCache.get(packageName)?.let { return it }

        // Load from PackageManager if not cached
        return try {
            val pm = appContext.packageManager
            val drawable = pm.getApplicationIcon(packageName)
            val bitmap = drawableToBitmap(drawable, iconSizePx, iconSizePx)
            iconCache.put(packageName, bitmap)
            bitmap
        } catch (e: Exception) {
            logger.debug("Error loading icon for $packageName: ${e.message}")
            null
        }
    }

    /**
     * Get icon bitmap for ApplicationInfo, loading and caching if needed
     */
    fun getIcon(appInfo: ApplicationInfo, pm: PackageManager): Bitmap? {
        // Check cache first
        iconCache.get(appInfo.packageName)?.let { return it }

        // Load and cache
        return try {
            val drawable = pm.getApplicationIcon(appInfo)
            val bitmap = drawableToBitmap(drawable, iconSizePx, iconSizePx)
            iconCache.put(appInfo.packageName, bitmap)
            bitmap
        } catch (e: Exception) {
            logger.debug("Error loading icon for ${appInfo.packageName}: ${e.message}")
            null
        }
    }

    /**
     * Convert drawable to bitmap with specified dimensions
     * Downsamples to reduce memory usage
     */
    private fun drawableToBitmap(drawable: Drawable, width: Int, height: Int): Bitmap {
        if (drawable is BitmapDrawable && drawable.bitmap != null) {
            return Bitmap.createScaledBitmap(drawable.bitmap, width, height, true)
        }

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }

    /**
     * Clear the cache
     */
    fun clear() {
        iconCache.evictAll()
        logger.info("Icon cache cleared")
    }

    /**
     * Get cache statistics
     */
    fun getCacheStats(): String {
        return "Cache size: ${iconCache.size()}, Max size: ${iconCache.maxSize()}, " +
                "Hit count: ${iconCache.hitCount()}, Miss count: ${iconCache.missCount()}"
    }
}
