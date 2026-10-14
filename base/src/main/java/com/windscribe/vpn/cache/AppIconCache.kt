/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.vpn.cache

import android.content.ComponentCallbacks2
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.util.DisplayMetrics
import android.util.LruCache
import androidx.core.graphics.drawable.toBitmap
import com.windscribe.vpn.Windscribe.Companion.appContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import javax.inject.Inject
import javax.inject.Singleton

/**
 * App icons for the split tunnel lists, decoded on demand and keyed by the size they are drawn at.
 *
 * Only those two screens read this, and neither is opened often, so nothing is loaded ahead of a
 * request and the cache is bounded and evictable rather than sized off the heap limit.
 */
@Singleton
class AppIconCache
    @Inject
    constructor() {
        private val logger = LoggerFactory.getLogger("app_icon_cache")

        private val cache =
            object : LruCache<String, Bitmap>(MAX_BYTES) {
                override fun sizeOf(
                    key: String,
                    value: Bitmap,
                ): Int = value.byteCount
            }

        init {
            appContext.registerComponentCallbacks(
                object : ComponentCallbacks2 {
                    override fun onTrimMemory(level: Int) {
                        if (level >= ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN) {
                            cache.evictAll()
                        }
                    }

                    override fun onConfigurationChanged(newConfig: Configuration) {}

                    @Deprecated("Required by ComponentCallbacks.")
                    override fun onLowMemory() {
                        cache.evictAll()
                    }
                },
            )
        }

        /** What is already decoded. Lets a bound view show a cached icon without a frame of placeholder. */
        fun cached(
            packageName: String,
            sizePx: Int,
        ): Bitmap? = cache.get(key(packageName, sizePx))

        suspend fun icon(
            packageName: String,
            sizePx: Int,
        ): Bitmap? {
            val key = key(packageName, sizePx)
            cache.get(key)?.let { return it }
            return withContext(Dispatchers.IO) {
                val drawable = drawableFor(packageName, sizePx) ?: return@withContext null
                runCatching { drawable.toBitmap(sizePx, sizePx) }
                    .onSuccess { cache.put(key, it) }
                    .onFailure { logger.debug("Could not render the icon for $packageName: ${it.message}") }
                    .getOrNull()
            }
        }

        /**
         * `getApplicationIcon` decodes at the display's own density, which for a 24 dp target is
         * several times the pixels drawn, so the variant closest to the target is asked for instead.
         */
        private fun drawableFor(
            packageName: String,
            sizePx: Int,
        ): Drawable? {
            val pm = appContext.packageManager
            val info = runCatching { pm.getApplicationInfo(packageName, 0) }.getOrNull() ?: return null
            if (info.icon != 0) {
                runCatching {
                    pm
                        .getResourcesForApplication(info)
                        .getDrawableForDensity(info.icon, densityFor(sizePx), null)
                }.getOrNull()?.let { return it }
            }
            return runCatching { pm.getApplicationIcon(info) }.getOrNull() ?: pm.defaultActivityIcon
        }

        /** Launcher icons are authored at [LAUNCHER_ICON_DP], so this is the bucket that covers [sizePx]. */
        private fun densityFor(sizePx: Int): Int {
            val wanted = sizePx * DisplayMetrics.DENSITY_DEFAULT / LAUNCHER_ICON_DP
            return BUCKETS.firstOrNull { it >= wanted } ?: DisplayMetrics.DENSITY_XXXHIGH
        }

        private fun key(
            packageName: String,
            sizePx: Int,
        ) = "$packageName@$sizePx"

        companion object {
            private const val MAX_BYTES = 4 * 1024 * 1024
            private const val LAUNCHER_ICON_DP = 48
            private val BUCKETS =
                intArrayOf(
                    DisplayMetrics.DENSITY_LOW,
                    DisplayMetrics.DENSITY_MEDIUM,
                    DisplayMetrics.DENSITY_HIGH,
                    DisplayMetrics.DENSITY_XHIGH,
                    DisplayMetrics.DENSITY_XXHIGH,
                    DisplayMetrics.DENSITY_XXXHIGH,
                )
        }
    }
