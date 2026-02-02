package com.windscribe.mobile.ui.preferences.icons

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import com.windscribe.vpn.apppreference.PreferencesHelper
import com.windscribe.vpn.constants.ExtraConstants
import kotlinx.coroutines.flow.MutableStateFlow

data class AppIcon(
    val name: String,
    val icon: Int,
    val component: ComponentName,
    val category: AppIconCategory,
    var isSelected: Boolean = false
)
enum class AppIconCategory {
    Discreet, Windscribe, Other
}

class AppIconManager(val context: Context, val preferenceManager: PreferencesHelper) {
    var appIcons = mapOf<String, AppIcon>()
    private val _selectedIcon = MutableStateFlow<AppIcon?>(null)
    val selectedAppIcon = _selectedIcon

    companion object {
        private const val LOCAL_PACKAGE_NAME = "com.windscribe.mobile.ui.preferences.icons"

        /**
         * Gets the activity class name for a given icon name.
         * Centralized mapping to keep icon-to-component mapping in one place.
         */
        fun getActivityClassName(iconName: String): String {
            return when (iconName) {
                "Clock" -> "$LOCAL_PACKAGE_NAME.ClockActivity"
                "Calculator" -> "$LOCAL_PACKAGE_NAME.CalculatorActivity"
                "80" -> "$LOCAL_PACKAGE_NAME.EightyActivity"
                "Vapor" -> "$LOCAL_PACKAGE_NAME.VaporScribeActivity"
                "Glitch" -> "$LOCAL_PACKAGE_NAME.GlitchScribeActivity"
                "Neon" -> "$LOCAL_PACKAGE_NAME.NeonActivity"
                "64" -> "$LOCAL_PACKAGE_NAME.SixtyActivity"
                "WindPass" -> "$LOCAL_PACKAGE_NAME.PassActivity"
                "BSVPN" -> "$LOCAL_PACKAGE_NAME.BsActivity"
                "DickButt" -> "$LOCAL_PACKAGE_NAME.DickButtActivity"
                else -> "com.windscribe.mobile.ui.AppStartActivity" // Classic/default
            }
        }

        /**
         * Gets the ComponentName for a given icon name and context.
         */
        fun getComponentName(context: Context, iconName: String): ComponentName {
            return ComponentName(context, getActivityClassName(iconName))
        }
    }

    init {
        appIcons = buildAppIcons(context)
        selectedAppIcon.value = getSelectedIcon()
    }

    private fun buildAppIcons(context: Context): Map<String, AppIcon> {
        val selectedCustomIcon = preferenceManager.customIcon
        // List of icon configurations - add or remove items here
        val iconConfigs = listOf(
            IconConfig(
                "Clock",
                com.windscribe.mobile.R.mipmap.ic_launcher_clock,
                AppIconCategory.Discreet
            ),
            IconConfig(
                "Calculator",
                com.windscribe.mobile.R.mipmap.ic_launcher_calculator,
                AppIconCategory.Discreet
            ),
            IconConfig(
                ExtraConstants.DEFAULT_ICON,
                com.windscribe.mobile.R.mipmap.ic_launcher_og,
                AppIconCategory.Windscribe
            ),
            IconConfig(
                "80",
                com.windscribe.mobile.R.mipmap.ic_launcher_eighty,
                AppIconCategory.Windscribe
            ),
            IconConfig(
                "Vapor",
                com.windscribe.mobile.R.mipmap.ic_launcher_vapor,
                AppIconCategory.Windscribe
            ),
            IconConfig(
                "Glitch",
                com.windscribe.mobile.R.mipmap.ic_launcher_glitch,
                AppIconCategory.Windscribe
            ),
            IconConfig(
                "Neon",
                com.windscribe.mobile.R.mipmap.ic_launcher_neon,
                AppIconCategory.Windscribe
            ),
            IconConfig(
                "64",
                com.windscribe.mobile.R.mipmap.ic_launcher_sixty,
                AppIconCategory.Windscribe
            ),
            IconConfig(
                "WindPass",
                com.windscribe.mobile.R.mipmap.ic_launcher_pass,
                AppIconCategory.Other
            ),
            IconConfig(
                "BSVPN",
                com.windscribe.mobile.R.mipmap.ic_launcher_bs,
                AppIconCategory.Other
            ),
            IconConfig(
                "DickButt",
                com.windscribe.mobile.R.mipmap.ic_launcher_butt,
                AppIconCategory.Other
            ),
        )
        return iconConfigs.associate { config ->
            val component = getComponentName(context, config.name)
            val appIcon = AppIcon(
                name = config.name,
                icon = config.iconRes,
                component = component,
                category = config.category,
                isSelected = selectedCustomIcon == config.name
            )
            config.name to appIcon
        }
    }

    data class IconConfig(
        val name: String,
        val iconRes: Int,
        val category: AppIconCategory,
    )

    fun setEnable(name: String) {
        appIcons.forEach {
            context.packageManager.setComponentEnabledSetting(
                it.value.component,
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP
            )
        }
        appIcons[name]?.component?.let {
            context.packageManager.setComponentEnabledSetting(
                it,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP
            )
        }
        preferenceManager.customIcon = name
        selectedAppIcon.value = getSelectedIcon()
    }

    fun getSelectedIcon(): AppIcon? {
        return appIcons[preferenceManager.customIcon]
    }
}