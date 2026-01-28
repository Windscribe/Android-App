package com.windscribe.mobile.ui.preferences.icons

import PreferencesNavBar
import android.content.ComponentName
import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import com.windscribe.mobile.ui.common.PreferenceBackground
import com.windscribe.mobile.ui.helper.MultiDevicePreview
import com.windscribe.mobile.ui.helper.PreviewWithNav
import com.windscribe.mobile.ui.nav.LocalNavController
import com.windscribe.mobile.ui.preferences.icons.AppIconManager.IconConfig
import com.windscribe.mobile.ui.theme.expandedServerItemTextColor
import com.windscribe.mobile.ui.theme.font12
import com.windscribe.mobile.ui.theme.font16
import com.windscribe.mobile.ui.theme.preferencesBackgroundColor
import com.windscribe.mobile.ui.theme.preferencesSubtitleColor
import com.windscribe.mobile.ui.theme.primaryTextColor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

// UI Constants
private object CustomIconsConstants {
    val ICON_SIZE = 54.dp
    val ICON_SPACING = 12.dp
    val CATEGORY_SPACING = 16.dp
    val ITEM_PADDING = 14.dp
    val SECTION_BOTTOM_PADDING = 8.dp
    val TOP_SPACER_HEIGHT = 20.dp
    val ITEM_DIVIDER_HEIGHT = 1.dp
    val CORNER_RADIUS = 16.dp
    val HORIZONTAL_PADDING = 16.dp
    val VERTICAL_PADDING = 16.dp
    const val BACKGROUND_ALPHA = 0.05f
}

@Composable
fun CustomIconsScreen(viewModel: CustomIconsViewModel?) {
    val navController = LocalNavController.current
    val showDialog by viewModel?.showConfirmDialog?.collectAsState() ?: return

    PreferenceBackground {
        AppIconView(viewModel, navController)
    }

    // Show confirmation dialog
    showDialog?.let { appIcon ->
        IconChangeConfirmDialog(
            iconName = appIcon.name,
            onConfirm = { viewModel.confirmIconChange() },
            onDismiss = { viewModel.dismissDialog() }
        )
    }
}

@Composable
private fun IconChangeConfirmDialog(
    iconName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.preferencesBackgroundColor,
        title = {
            Text(
                text = "Change App Icon?",
                style = font16.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.primaryTextColor
            )
        },
        text = {
            Text(
                text = "Changing the app icon to \"$iconName\" will close the app. You can reopen it from your home screen with the new icon.",
                style = font12,
                color = MaterialTheme.colorScheme.primaryTextColor
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    "OK",
                    style = font16.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.primaryTextColor
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    "Cancel",
                    style = font16,
                    color = MaterialTheme.colorScheme.preferencesSubtitleColor
                )
            }
        }
    )
}


@Composable
private fun AppIconItemSection(category: AppIconCategory, viewModel: CustomIconsViewModel? = null) {
    val icons by viewModel?.icons?.collectAsState() ?: return
    val filteredIcons by remember(icons, category) {
        derivedStateOf {
            icons.values.filter { it.category == category }
        }
    }

    if (filteredIcons.isEmpty()) return

    val title = remember(category) {
        when (category) {
            AppIconCategory.Windscribe -> "Windscribe"
            AppIconCategory.Discreet -> "Discreet"
            AppIconCategory.Other -> "Other"
        }
    }

    Column {
        Text(
            text = title,
            style = font12.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.expandedServerItemTextColor,
            modifier = Modifier.padding(bottom = CustomIconsConstants.SECTION_BOTTOM_PADDING)
        )

        filteredIcons.forEachIndexed { index, item ->
            AppIconItem(
                icon = item,
                isFirst = index == 0,
                isLast = index == filteredIcons.lastIndex,
                onClick = { viewModel?.selectIcon(item) }
            )
            if (index < filteredIcons.lastIndex) {
                Spacer(modifier = Modifier.height(CustomIconsConstants.ITEM_DIVIDER_HEIGHT))
            }
        }
    }
}

@Composable
private fun AppIconItem(
    icon: AppIcon,
    isFirst: Boolean,
    isLast: Boolean,
    onClick: () -> Unit
) {
    val context = LocalContext.current

    val shape = remember(isFirst, isLast) {
        when {
            isFirst && isLast -> RoundedCornerShape(CustomIconsConstants.CORNER_RADIUS)
            isFirst -> RoundedCornerShape(
                topStart = CustomIconsConstants.CORNER_RADIUS,
                topEnd = CustomIconsConstants.CORNER_RADIUS
            )

            isLast -> RoundedCornerShape(
                bottomStart = CustomIconsConstants.CORNER_RADIUS,
                bottomEnd = CustomIconsConstants.CORNER_RADIUS
            )

            else -> RectangleShape
        }
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .background(
                color = MaterialTheme.colorScheme.primaryTextColor.copy(alpha = CustomIconsConstants.BACKGROUND_ALPHA),
                shape = shape
            )
            .clickable(onClick = onClick)
            .padding(CustomIconsConstants.ITEM_PADDING)
    ) {
        val bitmap = remember(icon.icon) {
            ContextCompat.getDrawable(context, icon.icon)?.toBitmap()?.asImageBitmap()
        }

        bitmap?.let { imageBitmap ->
            Image(
                bitmap = imageBitmap,
                contentDescription = icon.name,
                modifier = Modifier.size(CustomIconsConstants.ICON_SIZE)
            )
        }

        Spacer(modifier = Modifier.width(CustomIconsConstants.ICON_SPACING))

        Text(
            text = icon.name,
            style = font16.copy(fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.primaryTextColor
        )

        Spacer(modifier = Modifier.weight(1f))

        if (icon.isSelected) {
            Image(
                painter = painterResource(id = com.windscribe.mobile.R.drawable.ic_green_check_dark),
                contentDescription = "${icon.name} selected",
            )
        }
    }
}

@Composable
private fun AppIconView(
    viewModel: CustomIconsViewModel? = null,
    navController: androidx.navigation.NavController? = null
) {
    Column(
        modifier = Modifier.padding(
            horizontal = CustomIconsConstants.HORIZONTAL_PADDING,
            vertical = CustomIconsConstants.VERTICAL_PADDING
        )
    ) {
        navController?.let {
            PreferencesNavBar(stringResource(com.windscribe.vpn.R.string.app_icon)) {
                it.popBackStack()
            }
        }
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(CustomIconsConstants.TOP_SPACER_HEIGHT))
            AppIconItemSection(AppIconCategory.Discreet, viewModel)
            Spacer(modifier = Modifier.height(CustomIconsConstants.CATEGORY_SPACING))
            AppIconItemSection(AppIconCategory.Windscribe, viewModel)
            Spacer(modifier = Modifier.height(CustomIconsConstants.CATEGORY_SPACING))
            AppIconItemSection(AppIconCategory.Other, viewModel)
        }
    }
}

@Composable
@MultiDevicePreview
private fun CustomIconScreenPreview() {
    val context = LocalContext.current
    val viewModel = object : CustomIconsViewModel() {
        override val icons: StateFlow<Map<String, AppIcon>>
            get() = MutableStateFlow(previewIcons(context))

        override val showConfirmDialog: StateFlow<AppIcon?>
            get() = MutableStateFlow(null)

        override fun selectIcon(appIcon: AppIcon) {}
        override fun confirmIconChange() {}
        override fun dismissDialog() {}
    }
    PreviewWithNav {
        CustomIconsScreen(viewModel)
    }
}

private fun previewIcons(context: Context): Map<String, AppIcon> {
    val selectedCustomIcon = "Clock"
    val localPackageName = "com.windscribe.mobile.ui.preferences.icons"
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
            com.windscribe.vpn.R.mipmap.ws_launcher,
            "com.windscribe.mobile.ui.AppStartActivity",
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
            "WindPass",
            com.windscribe.mobile.R.mipmap.ic_launcher_pass,
            "$localPackageName.PassActivity",
            AppIconCategory.Other
        ),
        IconConfig(
            "BSVpn",
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
