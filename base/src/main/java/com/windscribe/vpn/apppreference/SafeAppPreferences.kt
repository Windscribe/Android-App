package com.windscribe.vpn.apppreference

import android.content.Context
import android.net.Uri
import net.grandcentrix.tray.AppPreferences
import net.grandcentrix.tray.core.OnTrayPreferenceChangeListener
import net.grandcentrix.tray.core.TrayItem
import org.slf4j.LoggerFactory
import java.util.Collections

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

    // Thread-safe set to track registered listeners
    private val safeListeners = Collections.synchronizedSet(mutableSetOf<SafeListener>())

    override fun registerOnTrayPreferenceChangeListener(listener: OnTrayPreferenceChangeListener) {
        val safeListener = SafeListener(listener)
        safeListeners.add(safeListener)
        super.registerOnTrayPreferenceChangeListener(safeListener)
    }

    override fun unregisterOnTrayPreferenceChangeListener(listener: OnTrayPreferenceChangeListener) {
        // Find and remove the wrapper for this listener
        val safeListener = safeListeners.find { it.delegate == listener }
        if (safeListener != null) {
            safeListeners.remove(safeListener)
            super.unregisterOnTrayPreferenceChangeListener(safeListener)
        } else {
            // Fallback: try to unregister the listener directly
            super.unregisterOnTrayPreferenceChangeListener(listener)
        }
    }

    /**
     * Wrapper listener that catches ConcurrentModificationException
     */
    private inner class SafeListener(val delegate: OnTrayPreferenceChangeListener) : OnTrayPreferenceChangeListener {
        override fun onTrayPreferenceChanged(items: MutableCollection<TrayItem>?) {
            try {
                // Create a defensive copy of the items collection to avoid concurrent modification
                val itemsCopy = items?.let { ArrayList(it) }?.toMutableList()
                delegate.onTrayPreferenceChanged(itemsCopy)
            } catch (e: ConcurrentModificationException) {
                logger.warn("Caught ConcurrentModificationException in preference change listener. " +
                        "This is a known issue in Tray v0.12.0 and can be safely ignored.")
                logger.debug("Stack trace:", e)
            } catch (e: Exception) {
                logger.error("Unexpected exception in preference change listener", e)
            }
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is SafeListener) return false
            return delegate == other.delegate
        }

        override fun hashCode(): Int {
            return delegate.hashCode()
        }
    }
}