package com.windscribe.mobile.ui.preferences.icons

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import com.windscribe.vpn.R
import com.windscribe.vpn.apppreference.PreferencesHelper
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
    private val localPackageName = "com.windscribe.mobile.ui.preferences.icons"
    private val _selectedIcon = MutableStateFlow<AppIcon?>(null)
    val selectedAppIcon = _selectedIcon


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
                "$localPackageName.ClockActivity",
                AppIconCategory.Discreet
            ),
            IconConfig(
                "Calculator",
                com.windscribe.mobile.R.mipmap.ic_launcher_calculator,
                "$localPackageName.CalculatorActivity",
                AppIconCategory.Discreet
            ),
            IconConfig(
                "Classic",
                com.windscribe.mobile.R.mipmap.ic_launcher_og,
                "com.windscribe.mobile.ui.AppStartActivity",
                AppIconCategory.Windscribe
            ),
            IconConfig(
                "80",
                com.windscribe.mobile.R.mipmap.ic_launcher_eighty,
                "$localPackageName.EightyActivity",
                AppIconCategory.Windscribe
            ),
            IconConfig(
                "Vapor",
                com.windscribe.mobile.R.mipmap.ic_launcher_vapor,
                "$localPackageName.VaporScribeActivity",
                AppIconCategory.Windscribe
            ),
            IconConfig(
                "Glitch",
                com.windscribe.mobile.R.mipmap.ic_launcher_glitch,
                "$localPackageName.GlitchScribeActivity",
                AppIconCategory.Windscribe
            ),
            IconConfig(
                "Neon",
                com.windscribe.mobile.R.mipmap.ic_launcher_neon,
                "$localPackageName.NeonActivity",
                AppIconCategory.Windscribe
            ),
            IconConfig(
                "64",
                com.windscribe.mobile.R.mipmap.ic_launcher_sixty,
                "$localPackageName.SixtyActivity",
                AppIconCategory.Windscribe
            ),
            IconConfig(
                "WindPass",
                com.windscribe.mobile.R.mipmap.ic_launcher_pass,
                "$localPackageName.PassActivity",
                AppIconCategory.Other
            ),
            IconConfig(
                "BSVPN",
                com.windscribe.mobile.R.mipmap.ic_launcher_bs,
                "$localPackageName.BsActivity",
                AppIconCategory.Other
            ),
            IconConfig(
                "DickButt",
                com.windscribe.mobile.R.mipmap.ic_launcher_butt,
                "$localPackageName.DickButtActivity",
                AppIconCategory.Other
            ),
        )
        return iconConfigs.associate { config ->
            val component = ComponentName(context, config.activityClassName)
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
        val activityClassName: String,
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