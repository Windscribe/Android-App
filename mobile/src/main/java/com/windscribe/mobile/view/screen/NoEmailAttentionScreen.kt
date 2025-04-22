package com.windscribe.mobile.view.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import com.windscribe.mobile.R
import com.windscribe.mobile.view.LocalNavController
import com.windscribe.mobile.view.NavigationStack
import com.windscribe.mobile.view.theme.AppColors
import com.windscribe.mobile.view.theme.Dimen
import com.windscribe.mobile.view.theme.font16
import com.windscribe.mobile.view.ui.AppBackground
import com.windscribe.mobile.view.ui.NextButtonLighter

@Composable
fun NoEmailAttentionScreen(
    isPro: Boolean,
    onContinueWithoutEmail: () -> Unit
) {
    val navController = LocalNavController.current
    AppBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .background(AppColors.darkBlue)
                .padding(horizontal = Dimen.dp16, vertical = Dimen.dp16),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.weight(1f))
            Image(
                painter = painterResource(id = R.drawable.ic_noemailprompticon),
                contentDescription = stringResource(R.string.image_description),
                modifier = Modifier.padding(bottom = Dimen.dp24)
            )

            Text(
                text = stringResource(if (isPro) R.string.warning_no_email_pro_account else R.string.no_email_warning),
                style = font16,
                color = AppColors.white50,
                modifier = Modifier.padding(bottom = Dimen.dp24)
            )
            Spacer(modifier = Modifier.weight(1f))
            NextButtonLighter(
                modifier = Modifier.padding(bottom = Dimen.dp16),
                text = stringResource(R.string.continue_without_email),
            ) {
                onContinueWithoutEmail()
            }
            NextButtonLighter(
                modifier = Modifier.padding(bottom = Dimen.dp16),
                text = stringResource(R.string.back),
            ) {
                navController.popBackStack()
            }
        }
    }
}

@Composable
@Preview
@PreviewScreenSizes
fun NoEmailAttentionFragmentPreview() {
    NavigationStack(Screen.NoEmailAttention)
}
