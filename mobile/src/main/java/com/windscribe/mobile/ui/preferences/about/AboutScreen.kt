package com.windscribe.mobile.ui.preferences.about

import PreferencesNavBar
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.windscribe.mobile.ui.AppStartActivity
import com.windscribe.mobile.ui.common.PreferenceBackground
import com.windscribe.mobile.ui.common.openUrl
import com.windscribe.mobile.ui.helper.MultiDevicePreview
import com.windscribe.mobile.ui.helper.PreviewWithNav
import com.windscribe.mobile.ui.nav.LocalNavController
import com.windscribe.mobile.ui.theme.font16
import com.windscribe.mobile.ui.theme.primaryTextColor
import com.windscribe.vpn.R
import com.windscribe.vpn.constants.NetworkKeyConstants

@Composable
fun AboutScreen() {
    val navController = LocalNavController.current
    val aboutItems = listOf(
        R.string.status to NetworkKeyConstants.URL_STATUS,
        R.string.about to NetworkKeyConstants.URL_ABOUT,
        R.string.privacy_policy to NetworkKeyConstants.URL_PRIVACY,
        R.string.terms_title to NetworkKeyConstants.URL_TERMS,
        R.string.blog to NetworkKeyConstants.URL_BLOG,
        R.string.jobs to NetworkKeyConstants.URL_JOB,
        R.string.software_licenses to NetworkKeyConstants.URL_VIEW_LICENCE,
        R.string.changelog to NetworkKeyConstants.URL_CHANGELOG
    )
    PreferenceBackground {
        Column(modifier = Modifier.padding(vertical = 16.dp, horizontal = 16.dp)) {
            PreferencesNavBar(stringResource(R.string.about_us)) {
                navController.popBackStack()
            }
            Spacer(modifier = Modifier.height(20.dp))
            aboutItems.forEachIndexed { index, (title, url) ->
                AboutItem(title, url)
                if (index != aboutItems.lastIndex) {
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}

@Composable
private fun AboutItem(
    @StringRes title: Int,
    path: String,
) {
    val activity = LocalContext.current as? AppStartActivity
    Row(
        Modifier
            .fillMaxWidth()
            .height(44.dp)
            .background(
                color = MaterialTheme.colorScheme.primaryTextColor.copy(alpha = 0.05f),
                shape = RoundedCornerShape(size = 12.dp)
            )
            .clickable {
                activity?.openUrl(path)
            }
            .padding(start = 14.dp, end = 14.dp)
           , verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            stringResource(title),
            style = font16.copy(fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.primaryTextColor
        )
        Spacer(modifier = Modifier.weight(1f))
        Icon(
            modifier = Modifier.size(16.dp),
            painter = painterResource(com.windscribe.mobile.R.drawable.arrow_right),
            contentDescription = "",
            tint = MaterialTheme.colorScheme.primaryTextColor
        )
    }
}

@Composable
@MultiDevicePreview
private fun AboutScreenPreview() {
    PreviewWithNav {
        AboutScreen()
    }
}