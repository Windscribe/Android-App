package com.windscribe.mobile.ui.popup

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.windscribe.mobile.BuildConfig
import com.windscribe.mobile.ui.common.PopupContainer
import com.windscribe.mobile.ui.common.PopupDescription
import com.windscribe.mobile.ui.common.PopupPrimaryActionButton
import com.windscribe.mobile.ui.common.PopupSecondaryActionButton
import com.windscribe.mobile.ui.common.PopupTitle
import com.windscribe.mobile.ui.nav.LocalNavController

@Composable
fun UpdateAvailableScreen(latestVersion: String?) {
    val navController = LocalNavController.current
    val context = LocalContext.current
    val isGoogleBuild = BuildConfig.FLAVOR == "google"

    PopupContainer {
        Spacer(Modifier.weight(1f))
        PopupTitle(stringResource(com.windscribe.vpn.R.string.update_available))
        Spacer(modifier = Modifier.height(25.dp))
        val message = latestVersion?.takeIf { it.isNotBlank() }
            ?.let { stringResource(com.windscribe.vpn.R.string.update_available_message, it) }
            ?: stringResource(com.windscribe.vpn.R.string.update_available_message_generic)
        PopupDescription(message)
        Spacer(modifier = Modifier.height(32.dp))
        PopupPrimaryActionButton(
            modifier = Modifier,
            stringResource(com.windscribe.vpn.R.string.update_now)
        ) {
            navController.popBackStack()
            if (isGoogleBuild) {
                // Google Play build - open Play Store
                val intent = Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("market://details?id=${context.packageName}")
                )
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                try {
                    context.startActivity(intent)
                } catch (_: Exception) {
                    val webIntent = Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("https://play.google.com/store/apps/details?id=${context.packageName}")
                    )
                    webIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(webIntent)
                }
            } else {
                // F-Droid build - open F-Droid or website
                val intent = Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://f-droid.org/en/packages/${context.packageName}")
                )
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                try {
                    context.startActivity(intent)
                } catch (_: Exception) {
                    // Fallback to Windscribe website
                    val webIntent = Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("https://windscribe.com/download")
                    )
                    webIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(webIntent)
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        PopupSecondaryActionButton(
            modifier = Modifier,
            stringResource(com.windscribe.vpn.R.string.update_later)
        ) {
            navController.popBackStack()
        }
        Spacer(Modifier.weight(1f))
    }
}
