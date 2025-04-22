package com.windscribe.mobile.lipstick

import android.content.Context
import com.windscribe.mobile.R
import com.windscribe.vpn.constants.PreferencesKeyConstants.DARK_THEME
import com.windscribe.vpn.constants.PreferencesKeyConstants.LIGHT_THEME
import java.io.File

object LookAndFeelHelper {
    /**
     * List of options used by connected & disconnected app background
     * dropdowns
     */
    fun getBackgroundOptions(): List<DropDownItem> {
        return listOf(
            DropDownItem(1, R.string.flags),
            DropDownItem(2, R.string.bundle),
            DropDownItem(3, R.string.None),
            DropDownItem(4, R.string.custom),
        )
    }

    /**
     * List of options used by aspect ratio option in app background
     */
    fun getAspectRatioOptions(): List<DropDownItem> {
        return listOf(
            DropDownItem(1, R.string.fill),
            DropDownItem(2, R.string.scale),
            DropDownItem(3, R.string.tile)
        )
    }

    /**
     * List of options used by connected & disconnected sounds notifications
     * dropdowns
     */
    fun getSoundOptions(): List<DropDownItem> {
        return listOf(
            DropDownItem(1, R.string.None),
            DropDownItem(2, R.string.bundle),
            DropDownItem(3, R.string.custom),
        )
    }

    /**
     * Mappings of app bundled backgrounds ids to labels
     * Note: Items should be in same order as bundledBackgrounds
     */
    fun getBundledBackgroundOptions(): List<DropDownItem> {
        return listOf(
            DropDownItem(1, label = "Square"),
            DropDownItem(2, label = "Palm"),
            DropDownItem(3, label = "City"),
            DropDownItem(4, label = "Drip"),
            DropDownItem(5, label = "Windscribe")
        )
    }

    /**
     * Mappings of app bundled backgrounds ids to resource ids.
     */
    val bundledBackgrounds = mapOf(
        Pair(1, R.mipmap.square),
        Pair(2, R.mipmap.palm),
        Pair(3, R.mipmap.city),
        Pair(4, R.mipmap.drip),
        Pair(5, R.mipmap.windscribe)
    )

    /**
     * Mappings of app bundled sounds ids to labels.
     * Note: Items should be in same order as bundledSoundsConnected & bundledSoundsDisconnected
     */
    fun getBundledSoundOptions(): List<DropDownItem> {
        return listOf(
            DropDownItem(1, label = "Arcade"),
            DropDownItem(2, label = "Boing"),
            DropDownItem(3, label = "Fart"),
            DropDownItem(4, label = "Sword"),
            DropDownItem(5, label = "Windscribe"),
        )
    }

    /**
     * Mappings of app bundled sounds ids to sound resource ids.
     * This is used for ON state.
     */
    private val bundledSoundsConnected = mapOf(
        Pair(1, R.raw.arcade_on),
        Pair(2, R.raw.boing_on),
        Pair(3, R.raw.fart_on),
        Pair(4, R.raw.sword_on),
        Pair(5, R.raw.windscribe_on),
    )

    /**
     * Mappings of app bundled sounds ids to sound resource ids.
     * This is used for off state.
     */
    private val bundledSoundsDisconnected = mapOf(
        Pair(1, R.raw.arcade_off),
        Pair(2, R.raw.boing_off),
        Pair(3, R.raw.fart_off),
        Pair(4, R.raw.sword_off),
        Pair(5, R.raw.windscribe_off),
    )

    /**
     * Returns correct bundled sound resource id based on connection state
     */
    fun getBundleSoundResource(isConnected: Boolean, id: Int): Int? {
        return if (isConnected) {
            bundledSoundsConnected[id]
        } else {
            bundledSoundsDisconnected[id]
        }
    }

    /**
     * Returns custom sound file to play based on connection state
     */
    fun getSoundFile(context: Context, isConnected: Boolean, fileName: String): File {
        val soundDir = context.getDir(
            if (isConnected) "connected_sounds" else "disconnected_sounds",
            Context.MODE_PRIVATE
        )
        return File(soundDir, fileName)
    }

    /**
     * Returns theme options
     */
    fun getThemeOptions(): List<ThemeItem> {
        return listOf(
            ThemeItem(DARK_THEME, R.string.dark),
            ThemeItem(LIGHT_THEME, R.string.light),
        )
    }
}
