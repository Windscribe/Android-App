package com.windscribe.mobile.view.screen

import android.annotation.SuppressLint
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.app.ShareCompat.IntentBuilder
import com.windscribe.mobile.R
import com.windscribe.mobile.view.LocalNavController
import com.windscribe.mobile.view.theme.AppColors
import com.windscribe.mobile.view.theme.Dimen
import com.windscribe.mobile.view.theme.font12
import com.windscribe.mobile.view.theme.font24
import com.windscribe.mobile.view.ui.NextButton
import com.windscribe.mobile.viewmodel.SharedLinkViewmodel

@Composable
fun ShareLinkScreen(viewmodel: SharedLinkViewmodel?) {
    HandleExit(viewmodel)
    val intentBuilder = buildIntentBuilder(viewmodel)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(color = AppColors.darkBlue)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_share_favourite),
                contentDescription = null,
                modifier = Modifier
                    .size(86.dp)
                    .padding(top = 32.dp)
            )
            Text(
                text = stringResource(id = R.string.share_windscribe_with_a_friend),
                style = font24,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(vertical = 16.dp)
                    .fillMaxWidth()
            )
            Column() {
                Spacer(modifier = Modifier.height(16.dp))
                ReferralFeature(stringResource(R.string.first_reason_to_use_share))
                Spacer(modifier = Modifier.height(Dimen.dp16))
                ReferralFeature(stringResource(R.string.if_they_go_pro_you_ll_go_pro_to))
            }
            NextButton(
                text = stringResource(R.string.share_invite_link), enabled = true, onClick = {
                    intentBuilder.startChooser()
                    viewmodel?.exit()
                }, modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 32.dp)
            )
            Text(
                text = stringResource(id = R.string.referee_must_provide_your_username_at_sign_up_and_confirm_their_email_in_order_for_the_benefits_above_to_apply_to_your_account),
                style = font12,
                color = Color.White.copy(alpha = 0.5f),
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 550.dp)
                    .padding(top = 48.dp)
            )
        }
    }
}

@SuppressLint("StringFormatInvalid")
@Composable
private fun buildIntentBuilder(viewmodel: SharedLinkViewmodel?): IntentBuilder {
    val context = LocalContext.current
    val userName by viewmodel?.userName?.collectAsState() ?: remember { mutableStateOf("") }
    val launchUrl = "https://play.google.com/store/apps/details?id=${context.packageName}"
    val description = context.getString(R.string.share_app_description, userName, launchUrl)
    val title = context.getString(R.string.share_app)
    val launchActivity = LocalContext.current as AppCompatActivity
    return IntentBuilder(launchActivity).setType("text/plain")
        .setChooserTitle(title)
        .setText(description)
}

@Composable
private fun HandleExit(viewmodel: SharedLinkViewmodel?) {
    val navController = LocalNavController.current
    val shouldExit by viewmodel?.shouldExit?.collectAsState() ?: remember { mutableStateOf(false) }
    LaunchedEffect(shouldExit) {
        if (shouldExit) {
            navController.popBackStack()
        }
    }
}