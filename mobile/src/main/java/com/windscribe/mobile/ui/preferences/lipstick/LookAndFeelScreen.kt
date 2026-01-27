package com.windscribe.mobile.ui.preferences.lipstick

import AppTheme
import PreferencesNavBar
import android.R.attr.bitmap
import android.R.attr.description
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import com.windscribe.mobile.ui.AppStartActivity
import com.windscribe.mobile.ui.common.PreferenceBackground
import com.windscribe.mobile.ui.common.openUrl
import com.windscribe.mobile.ui.connection.ToastMessage
import com.windscribe.mobile.ui.helper.MultiDevicePreview
import com.windscribe.mobile.ui.helper.PreviewWithNav
import com.windscribe.mobile.ui.helper.hapticClickable
import com.windscribe.mobile.ui.nav.LocalNavController
import com.windscribe.mobile.ui.nav.Route
import com.windscribe.mobile.ui.nav.Screen
import com.windscribe.mobile.ui.theme.font14
import com.windscribe.mobile.ui.theme.font16
import com.windscribe.mobile.ui.theme.preferencesSubtitleColor
import com.windscribe.mobile.ui.theme.primaryTextColor
import com.windscribe.vpn.R


@Composable
fun LookAndFeelScreen(viewmodel: LipstickViewmodel? = null) {
    val navController = LocalNavController.current
    val scrollState = rememberScrollState()
    HandleToast(viewmodel)
    PreferenceBackground {
        Column(
            modifier = Modifier
                .padding(vertical = 16.dp, horizontal = 16.dp)
                .navigationBarsPadding()
        ) {
            PreferencesNavBar(stringResource(R.string.look_and_feel)) {
                navController.popBackStack()
            }
            Spacer(modifier = Modifier.height(20.dp))
            Column(modifier = Modifier.verticalScroll(scrollState)) {
                AppTheme(viewmodel)
                Spacer(modifier = Modifier.height(16.dp))
                AppCustomBackground(viewmodel)
                Spacer(modifier = Modifier.height(16.dp))
                AppCustomSound(viewmodel)
                Spacer(modifier = Modifier.height(16.dp))
                CustomIcon(viewmodel)
                Spacer(modifier = Modifier.height(16.dp))
                RenameLocations(viewmodel)
            }
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun CustomIcon(viewmodel: LipstickViewmodel?) {
    val navController = LocalNavController.current
    val context = LocalContext.current
    val selectedAppIcon by viewmodel?.selectedAppIcon?.collectAsState() ?: remember { mutableIntStateOf(R.mipmap.windscribe) }
    val bitmap = remember(selectedAppIcon) {
        ContextCompat.getDrawable(context, selectedAppIcon)?.toBitmap()?.asImageBitmap()
    }
    Column(
        modifier = Modifier
            .background(
                color = MaterialTheme.colorScheme.primaryTextColor.copy(
                    alpha = 0.05f
                ), shape = RoundedCornerShape(size = 12.dp)
            )
            .hapticClickable {
                navController.navigate(Screen.CustomIcon.route)
            }
            .padding(vertical = 14.dp, horizontal = 14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            bitmap?.let { imageBitmap ->
                Image(
                    bitmap = imageBitmap,
                    contentDescription = "App icon",
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                stringResource(R.string.app_icon),
                style = font16.copy(
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primaryTextColor
                )
            )
            Spacer(modifier = Modifier.weight(1f))
            Icon(
                modifier = Modifier.size(16.dp),
                painter = painterResource(com.windscribe.mobile.R.drawable.arrow_right),
                contentDescription = "",
                tint = MaterialTheme.colorScheme.primaryTextColor
            )
        }
        Spacer(modifier = Modifier.height(13.5.dp))
        Text(
            text = stringResource(R.string.app_icon_description),
            style = font14.copy(fontWeight = FontWeight.Normal),
            color = MaterialTheme.colorScheme.preferencesSubtitleColor,
            textAlign = TextAlign.Start
        )
    }
}

@Composable
private fun HandleToast(lipstickViewmodel: LipstickViewmodel?) {
    val context = LocalContext.current
    val toastMessage by lipstickViewmodel?.toastMessage?.collectAsState() ?: return
    LaunchedEffect(toastMessage) {
        when (toastMessage) {
            is ToastMessage.Localized -> {
                Toast.makeText(
                    context,
                    (toastMessage as ToastMessage.Localized).message,
                    Toast.LENGTH_SHORT
                ).show()
                lipstickViewmodel.clearToast()
            }

            is ToastMessage.Raw -> {
                Toast.makeText(
                    context,
                    (toastMessage as ToastMessage.Raw).message,
                    Toast.LENGTH_SHORT
                ).show()
                lipstickViewmodel.clearToast()
            }

            else -> {}
        }
    }
}


@Composable
@MultiDevicePreview
private fun LookAndFeelScreenPreview() {
    PreviewWithNav {
        LookAndFeelScreen()
    }
}