package com.windscribe.mobile.ui.preferences.help

import PreferencesNavBar
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.windscribe.mobile.ui.AppStartActivity
import com.windscribe.mobile.ui.common.PreferenceBackground
import com.windscribe.mobile.ui.common.openUrl
import com.windscribe.mobile.ui.helper.MultiDevicePreview
import com.windscribe.mobile.ui.helper.PreviewWithNav
import com.windscribe.mobile.ui.nav.LocalNavController
import com.windscribe.mobile.ui.nav.Route
import com.windscribe.mobile.ui.nav.Screen
import com.windscribe.mobile.ui.theme.font14
import com.windscribe.mobile.ui.theme.font16
import com.windscribe.mobile.ui.theme.preferencesSubtitleColor
import com.windscribe.mobile.ui.theme.primaryTextColor
import com.windscribe.vpn.R
import com.windscribe.vpn.constants.NetworkKeyConstants


@Composable
fun HelpScreen(viewModel: HelpViewModel? = null) {
    val navController = LocalNavController.current
    val scrollState = rememberScrollState()
    PreferenceBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(vertical = 16.dp, horizontal = 16.dp)
                .navigationBarsPadding()
        ) {
            PreferencesNavBar(stringResource(R.string.help_me)) {
                navController.popBackStack()
            }
            Spacer(modifier = Modifier.height(20.dp))
            HelpItem(
                R.string.knowledge_base,
                R.string.knowledge_base_description,
                com.windscribe.mobile.R.drawable.ic_apple,
                Route.Web(NetworkKeyConstants.URL_KNOWLEDGE),
            )
            Spacer(modifier = Modifier.height(16.dp))
            HelpItem(
                R.string.talk_to_garry,
                R.string.talk_to_garry_description,
                com.windscribe.mobile.R.drawable.ic_garry,
                Route.Web(NetworkKeyConstants.URL_GARRY),
            )
            Spacer(modifier = Modifier.height(16.dp))
            HelpItem(
                R.string.contact_humans,
                R.string.contact_humans_description,
                com.windscribe.mobile.R.drawable.ic_ticket,
                Route.Nav(Screen.Ticket),
            )
            Spacer(modifier = Modifier.height(16.dp))
            CommunitySupport()
            Spacer(modifier = Modifier.height(16.dp))
            HelpItem(
                R.string.advance,
                R.string.advance_description,
                com.windscribe.mobile.R.drawable.advance,
                Route.Nav(Screen.Advance),
            )
            Spacer(modifier = Modifier.height(16.dp))
            DebugView()
            Spacer(modifier = Modifier.height(16.dp))
            DebugSend(viewModel)
        }
    }
}

@Composable
private fun CommunitySupport() {
    val activity = LocalContext.current as? AppStartActivity

    val backgroundColor = MaterialTheme.colorScheme.primaryTextColor.copy(alpha = 0.05f)
    val textColor = MaterialTheme.colorScheme.primaryTextColor

    Column {
        HeaderRow(
            iconRes = com.windscribe.mobile.R.drawable.ic_fav,
            text = stringResource(R.string.community_support),
            backgroundColor = backgroundColor,
            textColor = textColor
        )
        DescriptionRow(
            text = stringResource(R.string.community_support_description),
            backgroundColor = backgroundColor
        )
        Spacer(modifier = Modifier.height(1.dp))
        CommunityRow(
            label = stringResource(R.string.discord),
            backgroundColor = backgroundColor,
            textColor = textColor,
            onClick = {
                activity?.openUrl(NetworkKeyConstants.URL_DISCORD)
            }
        )
        Spacer(modifier = Modifier.height(1.dp))
        CommunityRow(
            label = stringResource(R.string.reddit),
            backgroundColor = backgroundColor,
            textColor = textColor,
            shape = RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp),
            onClick = {
                activity?.openUrl(NetworkKeyConstants.URL_REDDIT)
            }
        )
    }
}

@Composable
private fun HeaderRow(iconRes: Int, text: String, backgroundColor: Color, textColor: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                backgroundColor,
                shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
            )
            .padding(start = 12.dp, end = 12.dp, top = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = textColor
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            style = font16.copy(fontWeight = FontWeight.Medium, color = textColor)
        )
    }
}

@Composable
private fun CommunityRow(
    label: String,
    backgroundColor: Color,
    textColor: Color,
    shape: RoundedCornerShape = RoundedCornerShape(size = 0.dp),
    onClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor, shape = shape)
            .clickable { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = font16.copy(fontWeight = FontWeight.Medium, color = textColor)
        )
        Spacer(modifier = Modifier.weight(1f))
        Icon(
            painter = painterResource(com.windscribe.mobile.R.drawable.arrow_right),
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = textColor
        )
    }
}

@Composable
private fun DescriptionRow(text: String, backgroundColor: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            style = font14.copy(fontWeight = FontWeight.Normal),
            color = MaterialTheme.colorScheme.preferencesSubtitleColor,
            textAlign = TextAlign.Start
        )
    }
}

@Composable
private fun DebugView() {
    val navController = LocalNavController.current
    Row(
        modifier = Modifier
            .background(
                color = MaterialTheme.colorScheme.primaryTextColor.copy(
                    alpha = 0.05f
                ), shape = RoundedCornerShape(size = 12.dp)
            )
            .clickable {
                navController.navigate(Screen.Debug.route)
            }
            .padding(vertical = 14.dp, horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            modifier = Modifier.size(16.dp),
            painter = painterResource(com.windscribe.mobile.R.drawable.ic_view_log),
            contentDescription = "",
            tint = MaterialTheme.colorScheme.primaryTextColor
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            stringResource(R.string.view_log),
            style = font16.copy(
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primaryTextColor
            )
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
private fun DebugSend(viewModel: HelpViewModel? = null) {
    val state by viewModel?.sendLogState?.collectAsState()
        ?: remember { mutableStateOf(SendLogState.Failure) }
    val text = if (state is SendLogState.Success) {
        stringResource(R.string.sent_thanks)
    } else if (state is SendLogState.Loading) {
        stringResource(R.string.sending_log)
    } else {
        stringResource(R.string.send_log)
    }
    Row(
        modifier = Modifier
            .background(
                color = MaterialTheme.colorScheme.primaryTextColor.copy(
                    alpha = 0.05f
                ), shape = RoundedCornerShape(size = 12.dp)
            )
            .clickable {
                if (state is SendLogState.Idle) {
                    viewModel?.sendLogClicked()
                }
            }
            .padding(vertical = 14.dp, horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            modifier = Modifier.size(16.dp),
            painter = painterResource(com.windscribe.mobile.R.drawable.ic_send_log_icon),
            contentDescription = "",
            tint = MaterialTheme.colorScheme.primaryTextColor
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text,
            style = font16.copy(
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primaryTextColor
            )
        )
        Spacer(modifier = Modifier.weight(1f))
        if (state is SendLogState.Loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                color = MaterialTheme.colorScheme.primaryTextColor,
                strokeWidth = 2.dp
            )
        } else if (state is SendLogState.Success) {
            Icon(
                modifier = Modifier.size(16.dp),
                painter = painterResource(com.windscribe.mobile.R.drawable.ic_check),
                contentDescription = "",
                tint = MaterialTheme.colorScheme.primaryTextColor
            )
        } else if (state is SendLogState.Failure) {
            Text(
                "Failure to send log",
                style = font16.copy(
                    fontWeight = FontWeight.Normal,
                    color = MaterialTheme.colorScheme.preferencesSubtitleColor
                )
            )
        } else {
            Icon(
                modifier = Modifier.size(16.dp),
                painter = painterResource(com.windscribe.mobile.R.drawable.arrow_right),
                contentDescription = "",
                tint = MaterialTheme.colorScheme.primaryTextColor
            )
        }
    }
}

@Composable
private fun HelpItem(
    title: Int,
    description: Int,
    icon: Int,
    route: Route
) {
    val navController = LocalNavController.current
    val activity = LocalContext.current as? AppStartActivity
    Column(
        modifier = Modifier
            .background(
                color = MaterialTheme.colorScheme.primaryTextColor.copy(
                    alpha = 0.05f
                ), shape = RoundedCornerShape(size = 12.dp)
            ).clickable {
                if (route is Route.Nav) {
                    val route = route.screen.route
                    navController.navigate(route)
                } else {
                    val link = (route as Route.Web).url
                    activity?.openUrl(link)
                }
            }
            .padding(vertical = 14.dp, horizontal = 14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                modifier = Modifier.size(16.dp),
                painter = painterResource(icon),
                contentDescription = "",
                tint = MaterialTheme.colorScheme.primaryTextColor
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                stringResource(title),
                style = font16.copy(
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primaryTextColor
                )
            )
            Spacer(modifier = Modifier.weight(1f))
            Icon(
                modifier = Modifier.size(16.dp),
                painter = painterResource(com.windscribe.mobile.R.drawable.arrow_right),
                contentDescription = "",
                tint = MaterialTheme.colorScheme.primaryTextColor
            )
        }
        Spacer(modifier = Modifier.height(13.5.dp))
        Text(
            text = stringResource(description),
            style = font14.copy(fontWeight = FontWeight.Normal),
            color = MaterialTheme.colorScheme.preferencesSubtitleColor,
            textAlign = TextAlign.Start
        )
    }
}

@Composable
@MultiDevicePreview
private fun HelpScreenPreview() {
    PreviewWithNav {
        HelpScreen()
    }
}