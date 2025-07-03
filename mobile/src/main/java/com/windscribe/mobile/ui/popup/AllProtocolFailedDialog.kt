package com.windscribe.mobile.ui.popup

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.navigation.NavController
import com.windscribe.mobile.R
import com.windscribe.mobile.ui.common.NextButton
import com.windscribe.mobile.ui.common.theme
import com.windscribe.mobile.ui.helper.MultiDevicePreview
import com.windscribe.mobile.ui.helper.PreviewWithNav
import com.windscribe.mobile.ui.nav.LocalNavController
import com.windscribe.mobile.ui.theme.AppColors
import com.windscribe.mobile.ui.theme.font16
import com.windscribe.mobile.ui.theme.font24
import com.windscribe.vpn.Windscribe.Companion.appContext
import com.windscribe.vpn.constants.PreferencesKeyConstants
import java.io.File

@Composable
fun AllProtocolFailedDialogScreen() {
    val navController = LocalNavController.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.deepBlue),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .padding(32.dp)
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            Row(modifier = Modifier.fillMaxWidth()) {
                Spacer(modifier = Modifier.weight(1f))
                Image(
                    painter = painterResource(R.drawable.ic_close),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(theme(R.attr.wdPrimaryColor)),
                    modifier = Modifier
                        .size(24.dp)
                        .clickable { navController.popBackStack() }
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            Image(
                painter = painterResource(R.drawable.ic_attention_icon),
                contentDescription = "Attention",
                colorFilter = ColorFilter.tint(theme(R.attr.wdPrimaryColor)),
                modifier = Modifier.padding(vertical = 16.dp)
            )

            Text(
                text = stringResource(com.windscribe.vpn.R.string.this_network_hates_us),
                style = font24,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Text(
                text = stringResource(com.windscribe.vpn.R.string.well_we_gave_it_our_best_shot_we_just_couldn_t_connect_you_on_this_network_send_us_your_debug_log_so_we_can_figure_out_what_happened),
                style = font16,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .widthIn(max = 400.dp)
                    .padding(bottom = 16.dp)
            )
            Spacer(modifier = Modifier.weight(1f))
            NextButton(
                modifier = Modifier.width(400.dp),
                text = stringResource(com.windscribe.vpn.R.string.export_log),
                enabled = true,
                onClick = { exportLog(navController.context, navController) }
            )
            Spacer(modifier = Modifier.height(16.dp))
            NextButton(
                modifier = Modifier.width(400.dp),
                text = stringResource(com.windscribe.vpn.R.string.contact_support),
                enabled = true,
                onClick = { contactSupport(navController.context, navController) }
            )

            Spacer(modifier = Modifier.height(8.dp))

            TextButton(onClick = { navController.popBackStack() }) {
                Text(
                    text = stringResource(com.windscribe.vpn.R.string.cancel),
                    style = font16,
                    color = Color.White
                )
            }
        }
    }
}

fun contactSupport(context: Context, navController: NavController) {
    val emailIntent = Intent(Intent.ACTION_SEND).apply {
        type = "message/rfc822"
        putExtra(Intent.EXTRA_EMAIL, arrayOf("helpdesk@windscribe.com"))
        putExtra(Intent.EXTRA_SUBJECT, "Restrictive Network Detected")
        putExtra(Intent.EXTRA_TEXT, "Please find the attached debug log.")
        val logFile = File(appContext.cacheDir.path + PreferencesKeyConstants.DEBUG_LOG_FILE_NAME)
        if (logFile.exists()) {
            val fileUri: Uri = FileProvider.getUriForFile(
                context,
                "com.windscribe.vpn.provider",
                logFile
            )
            putExtra(Intent.EXTRA_STREAM, fileUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
    val chooser = Intent.createChooser(emailIntent, "Select Email Provider")
    if (emailIntent.resolveActivity(context.packageManager) != null) {
        context.startActivity(chooser)
    }
    navController?.popBackStack()
}

fun exportLog(context: Context, navController: NavController) {
    val logFile = File(appContext.cacheDir.path + PreferencesKeyConstants.DEBUG_LOG_FILE_NAME)
    if (logFile.exists()) {
        val fileUri: Uri = FileProvider.getUriForFile(
            context,
            "com.windscribe.vpn.provider",
            logFile
        )
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_STREAM, fileUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(shareIntent, "Export Log File")
        if (shareIntent.resolveActivity(context.packageManager) != null) {
            context.startActivity(chooser)
        }
    }
    navController.popBackStack()
}

@Composable
@MultiDevicePreview
fun AllProtocolFailedDialogScreenPreview() {
    PreviewWithNav {
        AllProtocolFailedDialogScreen()
    }
}