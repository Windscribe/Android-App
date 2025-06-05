package com.windscribe.mobile.ui.preferences.lipstick

import AppTheme
import PreferencesNavBar
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.windscribe.mobile.ui.common.PreferenceBackground
import com.windscribe.mobile.ui.connection.ToastMessage
import com.windscribe.mobile.ui.helper.MultiDevicePreview
import com.windscribe.mobile.ui.helper.PreviewWithNav
import com.windscribe.mobile.ui.nav.LocalNavController
import com.windscribe.mobile.ui.theme.backgroundColorInverted
import com.windscribe.mobile.ui.theme.font12
import com.windscribe.mobile.ui.theme.primaryTextColor
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
internal fun PreferencesBottomSection(@StringRes description: Int) {
    val color = MaterialTheme.colorScheme.backgroundColorInverted.copy(alpha = 0.08f)
    Box(
        modifier = Modifier
            .offset(y = (-16).dp)
            .fillMaxWidth()
            .background(Color.Transparent)
            .padding(horizontal = 0.8.dp)
            .drawBehind {
                val strokeWidth = 1.dp.toPx()
                val cornerRadius = 16.dp.toPx()

                // Draw left side
                drawLine(
                    color = color,
                    start = Offset(0f, 0f),
                    end = Offset(0f, size.height - cornerRadius),
                    strokeWidth = strokeWidth
                )

                // Draw right side
                drawLine(
                    color = color,
                    start = Offset(size.width, 0f),
                    end = Offset(size.width, size.height - cornerRadius),
                    strokeWidth = strokeWidth
                )

                // Draw bottom with rounded corners
                val path = Path().apply {
                    moveTo(0f, size.height - cornerRadius)
                    quadraticBezierTo(0f, size.height, cornerRadius, size.height)
                    lineTo(size.width - cornerRadius, size.height)
                    quadraticBezierTo(size.width, size.height, size.width, size.height - cornerRadius)
                }

                drawPath(
                    path = path,
                    color = color,
                    style = Stroke(width = strokeWidth)
                )
            }
    ) {
        Column {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                stringResource(description),
                modifier = Modifier.padding(12.dp),
                style = font12,
                color = MaterialTheme.colorScheme.primaryTextColor.copy(alpha = 0.5f)
            )
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