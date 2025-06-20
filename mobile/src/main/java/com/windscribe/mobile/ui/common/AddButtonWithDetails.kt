package com.windscribe.mobile.ui.common

import android.content.res.Configuration
import android.media.MediaCodec.MetricsConstants.MODE
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.windscribe.mobile.R
import com.windscribe.mobile.ui.helper.MultiDevicePreview
import com.windscribe.mobile.ui.theme.AppColors
import com.windscribe.mobile.ui.theme.font16
import com.windscribe.mobile.ui.theme.serverItemTextColor
import com.windscribe.mobile.ui.theme.serverListBackgroundColor
import com.windscribe.mobile.ui.theme.serverListSecondaryColor

@Composable
fun AddButtonWithDetails(
    @StringRes title: Int?,
    @StringRes description: Int,
    @DrawableRes icon: Int,
    onClick: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(
                painter = painterResource(icon),
                contentDescription = stringResource(description),
                modifier = Modifier.size(32.dp),
                colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.serverItemTextColor)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(stringResource(description), style = font16, color = AppColors.white.copy(alpha = 0.70f))
            title?.let {
                Button(
                    onClick,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(top = 16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AppColors.white.copy(alpha = 0.10f),
                        contentColor = AppColors.white
                    )
                ) {
                    Text(stringResource(title), style = font16)
                }
            }
        }
    }
}

@Composable
@Preview(showBackground = true, showSystemUi = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun AddButtonPreview() {
    AddButtonWithDetails(
        title = com.windscribe.vpn.R.string.add_static_ip,
        description = com.windscribe.vpn.R.string.no_static_ip,
        icon = R.drawable.ic_location_static
    ) {

    }
}