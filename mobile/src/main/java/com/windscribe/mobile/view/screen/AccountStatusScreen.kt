package com.windscribe.mobile.view.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.windscribe.mobile.dialogs.AccountStatusDialogData
import com.windscribe.mobile.upgradeactivity.UpgradeActivity
import com.windscribe.mobile.view.AppStartActivity
import com.windscribe.mobile.view.LocalNavController
import com.windscribe.mobile.view.theme.AppColors
import com.windscribe.mobile.view.theme.font16
import com.windscribe.mobile.view.theme.font24
import com.windscribe.mobile.view.ui.NextButton
import com.windscribe.mobile.view.ui.TextButton


@Composable
fun AccountStatusScreen(data: AccountStatusDialogData) {
    val navController = LocalNavController.current
    val activity = LocalContext.current as? AppStartActivity
    Box(modifier = Modifier
        .fillMaxSize()
        .background(color = AppColors.deepBlue)
        .clickable(enabled = false) {}) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 32.dp)
                .width(400.dp)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Image(
                painter = painterResource(id = data.icon),
                contentDescription = null,
                modifier = Modifier
                    .size(100.dp)
                    .padding(top = 8.dp)
            )
            Text(
                text = data.title,
                style = font24,
                color = AppColors.white,
                textAlign = TextAlign.Center
            )
            Text(
                text = data.description,
                style = font16,
                color = AppColors.white,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            if (data.showUpgradeButton) {
                Spacer(modifier = Modifier.padding(top = 24.dp))
                NextButton(Modifier, text = data.upgradeText, true) {
                    if (data.bannedLayout) {
                        navController.popBackStack()
                    } else {
                        activity?.startActivity(UpgradeActivity.getStartIntent(activity))
                        navController.popBackStack()
                    }
                }
            }
            if (data.showSkipButton) {
                TextButton(data.skipText) {
                    navController.popBackStack()
                }
            }
        }
    }
}
