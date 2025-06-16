package com.windscribe.mobile.ui.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.windscribe.mobile.R
import com.windscribe.mobile.ui.common.AppBackground
import com.windscribe.mobile.ui.common.NextButtonLighter
import com.windscribe.mobile.ui.helper.MultiDevicePreview
import com.windscribe.mobile.ui.helper.PreviewWithNav
import com.windscribe.mobile.ui.nav.LocalNavController
import com.windscribe.mobile.ui.theme.AppColors
import com.windscribe.mobile.ui.theme.font16

@Composable
fun NoEmailAttentionScreen(
    isPro: Boolean,
    onContinueWithoutEmail: () -> Unit
) {
    val navController = LocalNavController.current
    AppBackground {
        Column(
            modifier = Modifier
                .navigationBarsPadding()
                .width(400.dp)
                .padding(horizontal = 32.dp)
                .align(Alignment.Center)
                .padding(vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.weight(1f))
            Image(
                painter = painterResource(id = R.drawable.ic_noemailprompticon),
                contentDescription = stringResource(R.string.image_description),
                modifier = Modifier.padding(bottom = 24.dp)
            )

            Text(
                text = stringResource(if (isPro) R.string.warning_no_email_pro_account else R.string.no_email_warning),
                style = font16,
                color = AppColors.white.copy(alpha = 0.50f),
                modifier = Modifier.padding(bottom = 24.dp)
            )
            Spacer(modifier = Modifier.weight(1f))
            NextButtonLighter(
                modifier = Modifier.padding(bottom = 16.dp),
                text = stringResource(R.string.continue_without_email),
            ) {
                onContinueWithoutEmail()
            }
            NextButtonLighter(
                modifier = Modifier.padding(bottom = 16.dp),
                text = stringResource(R.string.back),
            ) {
                navController.popBackStack()
            }
        }
    }
}

@Composable
@MultiDevicePreview
fun NoEmailAttentionFragmentPreview() {
    PreviewWithNav {
        NoEmailAttentionScreen(false) { }
    }
}
