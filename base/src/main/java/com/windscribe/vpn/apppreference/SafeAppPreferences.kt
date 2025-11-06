package com.windscribe.vpn.apppreference

import android.content.Context
import net.grandcentrix.tray.AppPreferences
import org.slf4j.LoggerFactory

/**
 * Safe wrapper around AppPreferences that handles ConcurrentModificationException
 * from Tray library's ContentProviderStorage.
 *
 * This works around a known bug in Tray v0.12.0 where WeakHashMap iteration
 * can throw ConcurrentModificationException when keys are garbage collected
 * during iteration or when listeners modify the map.
 */
class SafeAppPreferences(context: Context) : AppPreferences(context) {

    companion object {
        private val logger = LoggerFactory.getLogger("safe_preferences")
    }

    init {
        // Set up a global exception handler for the observer thread
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            if (throwable is ConcurrentModificationException &&
                thread.name.contains("observer", ignoreCase = true)) {
                // Log and suppress the Tray library ConcurrentModificationException
                logger.warn("Caught ConcurrentModificationException in Tray observer thread. " +
                        "This is a known issue in Tray v0.12.0 and can be safely ignored.")
                logger.debug("Stack trace:", throwable)
            } else {
                // Pass all other exceptions to the default handler
                defaultHandler?.uncaughtException(thread, throwable)
            }
        }
    }
}