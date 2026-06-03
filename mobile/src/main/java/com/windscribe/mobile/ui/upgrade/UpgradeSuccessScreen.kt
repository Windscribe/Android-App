package com.windscribe.mobile.ui.upgrade

import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.windscribe.mobile.R
import com.windscribe.mobile.ui.common.AppBackground
import com.windscribe.mobile.ui.common.PrimaryButton
import com.windscribe.mobile.ui.helper.MultiDevicePreview
import com.windscribe.mobile.ui.helper.PreviewWithNav
import com.windscribe.mobile.ui.helper.hapticClickable
import com.windscribe.mobile.ui.helper.hapticClickableRipple
import com.windscribe.mobile.ui.nav.LocalNavController
import com.windscribe.mobile.ui.nav.Screen
import com.windscribe.mobile.ui.theme.AppColors
import com.windscribe.mobile.ui.theme.font12
import com.windscribe.mobile.ui.theme.font14
import com.windscribe.mobile.ui.theme.font16
import com.windscribe.mobile.ui.theme.font22

/**
 * Stateful "upgrade success" screen mirroring `activity_upgrade_success.xml`. It owns its own
 * navigation (close, social links, the final CTA) so the upgrade flow can route here directly.
 */
@Composable
fun UpgradeSuccessScreen(isGhostAccount: Boolean) {
    val navController = LocalNavController.current
    val context = LocalContext.current

    fun openUrl(url: String) {
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
        } catch (_: Exception) {
            // Ignore: no browser available to handle the link.
        }
    }

    AppBackground {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .navigationBarsPadding()
                    .statusBarsPadding()
                    .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 48.dp, start = 8.dp, end = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_close_no_background),
                    contentDescription = null,
                    modifier =
                        Modifier
                            .hapticClickableRipple { navController.popBackStack() }
                            .padding(8.dp),
                )
                Spacer(modifier = Modifier.weight(1f))
            }
            Image(
                painter = painterResource(R.drawable.upgrade_welcome),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(width = 184.dp, height = 180.dp),
            )
            Text(
                text = stringResource(com.windscribe.vpn.R.string.welcome_to_windscribe_pro),
                style = font22,
                color = AppColors.white,
                textAlign = TextAlign.Center,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp, start = 16.dp, end = 16.dp),
            )
            Text(
                text = stringResource(com.windscribe.vpn.R.string.thanks_for_upgrading),
                style = font16,
                color = AppColors.white.copy(alpha = 0.8f),
                textAlign = TextAlign.Center,
                modifier =
                    Modifier
                        .widthIn(max = 295.dp)
                        .padding(top = 16.dp),
            )
            FeatureTitleRow(com.windscribe.vpn.R.string.set_up_on_all_your_devices)
            FeatureTitleRow(com.windscribe.vpn.R.string.connect_to_any_location)
            FeatureTitleRow(com.windscribe.vpn.R.string.unlimited_bandwidth)
            HorizontalDivider(
                color = AppColors.white.copy(alpha = 0.1f),
                modifier =
                    Modifier
                        .padding(top = 24.dp, start = 16.dp, end = 16.dp),
            )
            Text(
                text = stringResource(com.windscribe.vpn.R.string.share),
                style = font12,
                color = AppColors.white,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, start = 24.dp, end = 16.dp),
            )
            SocialLinkRow(
                iconRes = R.drawable.discord,
                titleRes = com.windscribe.vpn.R.string.join_our_discord_community,
                onClick = { openUrl("https://discord.com/invite/vpn") },
            )
            SocialLinkRow(
                iconRes = R.drawable.reddit,
                titleRes = com.windscribe.vpn.R.string.join_our_reddit_community,
                onClick = { openUrl("https://www.reddit.com/r/Windscribe/") },
            )
            SocialLinkRow(
                iconRes = R.drawable.youtube,
                titleRes = com.windscribe.vpn.R.string.find_us_on_youtube,
                onClick = { openUrl("https://www.youtube.com/c/Windscribe") },
            )
            SocialLinkRow(
                iconRes = R.drawable.x,
                titleRes = com.windscribe.vpn.R.string.follow_us_on_x,
                onClick = { openUrl("https://x.com/windscribecom") },
            )
            PrimaryButton(
                modifier = Modifier.padding(top = 24.dp, start = 16.dp, end = 16.dp),
                text = stringResource(com.windscribe.vpn.R.string.start_using_pro),
                onClick = {
                    if (isGhostAccount) {
                        navController.navigate(Screen.Account.route)
                        navController.popBackStack()
                    } else {
                        navController.popBackStack()
                    }
                },
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun FeatureTitleRow(titleRes: Int) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, start = 24.dp, end = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(titleRes),
            style = font14,
            color = AppColors.white,
            modifier = Modifier.weight(1f),
        )
        Image(
            painter = painterResource(R.drawable.ic_star),
            contentDescription = null,
        )
    }
}

@Composable
private fun SocialLinkRow(
    iconRes: Int,
    titleRes: Int,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(40.dp)
                .hapticClickable { onClick() }
                .padding(start = 8.dp, end = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start,
    ) {
        Image(
            painter = painterResource(iconRes),
            contentDescription = null,
            modifier = Modifier.size(24.dp),
        )
        Text(
            text = stringResource(titleRes),
            style = font14,
            color = AppColors.white,
            modifier =
                Modifier
                    .weight(1f)
                    .padding(start = 16.dp),
        )
        Image(
            painter = painterResource(R.drawable.ic_forward_arrow_white),
            contentDescription = null,
        )
    }
}

@MultiDevicePreview
@Composable
private fun UpgradeSuccessScreenPreview() {
    PreviewWithNav {
        UpgradeSuccessScreen(isGhostAccount = false)
    }
}
