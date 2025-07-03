package com.windscribe.mobile.ui.preferences.lipstick

import AppTheme
import PreferencesNavBar
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.windscribe.mobile.ui.common.PreferenceBackground
import com.windscribe.mobile.ui.connection.ToastMessage
import com.windscribe.mobile.ui.helper.MultiDevicePreview
import com.windscribe.mobile.ui.helper.PreviewWithNav
import com.windscribe.mobile.ui.nav.LocalNavController
import com.windscribe.vpn.R


@Composable
fun LookAndFeelScreen(viewmodel: LipstickViewmodel? = null) {
    val navController = LocalNavController.current
    val scrollState = rememberScrollState()
    HandleToast(viewmodel)
    PreferenceBackground {
        Column(modifier = Modifier.padding(vertical = 16.dp, horizontal = 16.dp).navigationBarsPadding()) {
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
                RenameLocations(viewmodel)
            }
            Spacer(modifier = Modifier.height(20.dp))
        }
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
                lipstickViewmodel?.clearToast()
            }

            is ToastMessage.Raw -> {
                Toast.makeText(
                    context,
                    (toastMessage as ToastMessage.Raw).message,
                    Toast.LENGTH_SHORT
                ).show()
                lipstickViewmodel?.clearToast()
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