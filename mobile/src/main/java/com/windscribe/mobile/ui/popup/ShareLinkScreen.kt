package com.windscribe.mobile.ui.popup

import PreferencesNavBar
import android.annotation.SuppressLint
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.app.ShareCompat.IntentBuilder
import com.windscribe.mobile.R
import com.windscribe.mobile.ui.common.NextButton
import com.windscribe.mobile.ui.common.PreferenceBackground
import com.windscribe.mobile.ui.helper.MultiDevicePreview
import com.windscribe.mobile.ui.helper.PreviewWithNav
import com.windscribe.mobile.ui.nav.LocalNavController
import com.windscribe.mobile.ui.theme.font12
import com.windscribe.mobile.ui.theme.font24
import com.windscribe.mobile.ui.theme.primaryTextColor

@Composable
fun ShareLinkScreen(viewmodel: SharedLinkViewmodel?) {
    HandleExit(viewmodel)
    val intentBuilder = buildIntentBuilder(viewmodel)
    val navController = LocalNavController.current
    PreferenceBackground {
        Column(
            modifier = Modifier
                .statusBarsPadding()
                .padding(vertical = 16.dp, horizontal = 16.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            PreferencesNavBar(stringResource(com.windscribe.vpn.R.string.refer_for_data)) {
                navController.popBackStack()
            }
            Spacer(modifier = Modifier.weight(1.0f))
            Column(
                horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier
                    .widthIn(max = 400.dp)
                    .padding(horizontal = 32.dp)
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
                    color = MaterialTheme.colorScheme.primaryTextColor,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .padding(vertical = 16.dp)
                        .fillMaxWidth()
                )
                Column() {
                    Spacer(modifier = Modifier.height(16.dp))
                    ReferralFeature(stringResource(R.string.first_reason_to_use_share))
                    Spacer(modifier = Modifier.height(16.dp))
                    ReferralFeature(stringResource(R.string.if_they_go_pro_you_ll_go_pro_to))
                }
                NextButton(
                    text = stringResource(R.string.share_invite_link), enabled = true, onClick = {
                        intentBuilder?.startChooser()
                        viewmodel?.exit()
                    }, modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 32.dp, start = 32.dp, end = 32.dp)
                )
                Text(
                    text = stringResource(id = R.string.referee_must_provide_your_username_at_sign_up_and_confirm_their_email_in_order_for_the_benefits_above_to_apply_to_your_account),
                    style = font12,
                    color = MaterialTheme.colorScheme.primaryTextColor.copy(alpha = 0.5f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 48.dp, start = 32.dp, end = 32.dp)
                )
            }
            Spacer(modifier = Modifier.weight(1.0f))
        }
    }
}

@Composable
private fun ReferralFeature(text: String) {
    Row(
        modifier = Modifier.padding(start = 16.dp, end = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_check),
            contentDescription = text,
            modifier = Modifier
                .size(16.dp),
        )
        Text(
            text,
            style = font12.copy(textAlign = TextAlign.Start),
            color = MaterialTheme.colorScheme.primaryTextColor.copy(alpha = 0.5f),
            modifier = Modifier.padding(start = 16.dp)
        )
    }
}

@SuppressLint("StringFormatInvalid")
@Composable
private fun buildIntentBuilder(viewmodel: SharedLinkViewmodel?): IntentBuilder? {
    val context = LocalContext.current
    val userName by viewmodel?.userName?.collectAsState() ?: remember { mutableStateOf("") }
    val launchUrl = "https://play.google.com/store/apps/details?id=${context.packageName}"
    val description = context.getString(R.string.share_app_description, userName, launchUrl)
    val title = context.getString(R.string.share_app)
    val launchActivity = LocalContext.current as? AppCompatActivity
    if (launchActivity == null) {
        return null
    }
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

@MultiDevicePreview
@Composable
private fun ShareLinkScreenPreview() {
    PreviewWithNav {
        ShareLinkScreen(null)
    }
}