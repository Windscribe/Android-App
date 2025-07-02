package com.windscribe.mobile.ui.preferences.robert

import PreferencesNavBar
import android.content.res.Configuration
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.windscribe.mobile.ui.common.DescriptionWithLearnMore
import com.windscribe.mobile.ui.common.PreferenceBackground
import com.windscribe.mobile.ui.common.PreferenceProgressBar
import com.windscribe.mobile.ui.common.openUrl
import com.windscribe.mobile.ui.connection.ToastMessage
import com.windscribe.mobile.ui.helper.PreviewWithNav
import com.windscribe.mobile.ui.helper.hapticClickable
import com.windscribe.mobile.ui.nav.LocalNavController
import com.windscribe.mobile.ui.theme.AppColors
import com.windscribe.mobile.ui.theme.font12
import com.windscribe.mobile.ui.theme.font14
import com.windscribe.mobile.ui.theme.font16
import com.windscribe.mobile.ui.theme.preferencesSubtitleColor
import com.windscribe.mobile.ui.theme.primaryTextColor
import com.windscribe.vpn.R
import com.windscribe.vpn.api.response.RobertFilter
import com.windscribe.vpn.constants.FeatureExplainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow


@Composable
fun RobertScreen(viewModel: RobertViewModel? = null) {
    val navController = LocalNavController.current
    val state by viewModel?.robertFilterState?.collectAsState()
        ?: remember { mutableStateOf(RobertFilterState.Loading) }
    val showProgress by viewModel?.showProgress?.collectAsState()
        ?: remember { mutableStateOf(false) }
    PreferenceBackground {
        Column(
            modifier = Modifier.padding(vertical = 16.dp, horizontal = 16.dp).navigationBarsPadding(),
            horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
        ) {
            PreferencesNavBar(stringResource(R.string.robert)) {
                navController.popBackStack()
            }
            Spacer(modifier = Modifier.height(20.dp))
            RobertScreenDescription()
            Spacer(modifier = Modifier.height(16.dp))
            when (state) {
                is RobertFilterState.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = MaterialTheme.colorScheme.primaryTextColor,
                        strokeWidth = 2.dp
                    )
                }

                is RobertFilterState.Failure -> {
                    Text(
                        (state as RobertFilterState.Failure).error,
                        style = font12,
                        color = MaterialTheme.colorScheme.primaryTextColor
                    )
                }

                is RobertFilterState.Success -> {
                    val filters = (state as RobertFilterState.Success).filters
                    key(filters.hashCode()) {
                        Filters(filters, viewModel, Modifier.weight(1.0f))
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            ManageCustomRule(viewModel)
        }
        PreferenceProgressBar(showProgress)
        HandleGoto(viewModel)
    }
}

@Composable
private fun RobertScreenDescription() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.primaryTextColor.copy(alpha = 0.10f),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(top = 14.dp, bottom = 0.dp, start = 16.dp, end = 16.dp)
    ) {
        Icon(
            painter = painterResource(com.windscribe.mobile.R.drawable.robert),
            contentDescription = "Robert icon image.",
            tint = MaterialTheme.colorScheme.primaryTextColor,
            modifier = Modifier.align(androidx.compose.ui.Alignment.TopEnd)
                .offset(x = (15.0).dp, y = (-13).dp)
                .clip(RoundedCornerShape(topEnd = 11.dp))

        )
        DescriptionWithLearnMore(stringResource(R.string.robert_description), FeatureExplainer.ROBERT)
    }
}

@Composable
private fun HandleGoto(viewModel: RobertViewModel?) {
    val context = LocalContext.current
    val state by viewModel?.goToState?.collectAsState(initial = RobertGoToState.None) ?: remember {
        mutableStateOf(RobertGoToState.None)
    }
    LaunchedEffect(state) {
        when (state) {
            is RobertGoToState.ManageRules -> {
                val url = (state as RobertGoToState.ManageRules).url
                context.openUrl(url)
                viewModel?.clearGoToState()
            }

            is RobertGoToState.Toast -> {
                val toast = (state as RobertGoToState.Toast).message
                if (toast is ToastMessage.Raw) {
                    Toast.makeText(context, toast.message, Toast.LENGTH_SHORT).show()
                } else if (toast is ToastMessage.Localized) {
                    Toast.makeText(context, toast.message, Toast.LENGTH_SHORT).show()
                }

                viewModel?.clearGoToState()
            }

            RobertGoToState.None -> {}
        }
    }
}

@Composable
private fun Filters(filters: List<RobertFilter>, viewModel: RobertViewModel?, modifier: Modifier = Modifier) {
    var iconMap = mapOf(
        Pair("malware", com.windscribe.mobile.R.drawable.ic_malware),
        Pair("ads", com.windscribe.mobile.R.drawable.ic_ads),
        Pair("social", com.windscribe.mobile.R.drawable.ic_social),
        Pair("porn", com.windscribe.mobile.R.drawable.ic_porn),
        Pair("gambling", com.windscribe.mobile.R.drawable.ic_gambling),
        Pair("fakenews", com.windscribe.mobile.R.drawable.ic_fake_news),
        Pair("competitors", com.windscribe.mobile.R.drawable.ic_other_vpn),
        Pair("cryptominers", com.windscribe.mobile.R.drawable.ic_crypto)
    )
    LazyColumn(modifier = modifier) {
        items(filters.size) { index ->
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .background(
                        color = MaterialTheme.colorScheme.primaryTextColor.copy(alpha = 0.05f),
                        shape = RoundedCornerShape(size = 12.dp)
                    )
                    .padding(vertical = 14.dp, horizontal = 14.dp)
            ) {
                Icon(
                    modifier = Modifier
                        .size(24.dp)
                        .padding(4.dp),
                    painter = painterResource(
                        iconMap.getOrDefault(
                            filters[index].id,
                            com.windscribe.mobile.R.drawable.ic_preference_placeholder
                        )
                    ),
                    contentDescription = "",
                    tint = MaterialTheme.colorScheme.primaryTextColor
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    filters[index].title,
                    style = font16.copy(
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primaryTextColor
                    )
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    if (filters[index].status == 1) stringResource(R.string.blocking) else stringResource(R.string.allowing),
                    style = font14.copy(
                        fontWeight = FontWeight.Normal,
                        color = if (filters[index].status == 1) AppColors.neonGreen else MaterialTheme.colorScheme.preferencesSubtitleColor
                    )
                )
                Spacer(modifier = Modifier.width(8.dp))
                var isEnabled = filters[index].status == 1
                if (isEnabled) {
                    Image(
                        painter = painterResource(id = com.windscribe.mobile.R.drawable.ic_toggle_button_on),
                        contentDescription = null,
                        modifier = Modifier.clickable {
                            isEnabled = !isEnabled
                            viewModel?.onFilterSettingChanged(
                                filters[index],
                                if (isEnabled) 1 else 0
                            )
                        }
                    )
                } else {
                    Image(
                        painter = painterResource(id = com.windscribe.mobile.R.drawable.ic_toggle_button_off),
                        contentDescription = null,
                        modifier = Modifier.clickable {
                            isEnabled = !isEnabled
                            viewModel?.onFilterSettingChanged(
                                filters[index],
                                if (isEnabled) 1 else 0
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ManageCustomRule(viewModel: RobertViewModel? = null) {
    Row(
        modifier = Modifier
            .background(
                color = MaterialTheme.colorScheme.primaryTextColor.copy(alpha = 0.05f),
                shape = RoundedCornerShape(size = 12.dp)
            )
            .hapticClickable {
                viewModel?.onManageRulesClick()
            }
            .padding(vertical = 14.dp, horizontal = 14.dp)

    ) {
        Spacer(modifier = Modifier.weight(1f))
        Text(
            stringResource(R.string.manage_custom_rules),
            style = font16.copy(
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primaryTextColor
            )
        )
        Spacer(modifier = Modifier.weight(1f))
    }
}

@Composable
private fun RobertScreenPreview(robertFilterState: RobertFilterState) {
    PreviewWithNav {
        val viewModel = object : RobertViewModel() {
            override val showProgress: StateFlow<Boolean> = MutableStateFlow(false)
            override val robertFilterState: StateFlow<RobertFilterState> =
                MutableStateFlow(robertFilterState)
            override val goToState: SharedFlow<RobertGoToState> =
                MutableStateFlow(RobertGoToState.None)
        }
        RobertScreen(viewModel)
    }
}

@Composable
@Preview(showBackground = true, showSystemUi = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun RobertScreenLoading() {
    RobertScreenPreview(RobertFilterState.Loading)
}

@Composable
@Preview(showBackground = true, showSystemUi = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun RobertScreenError() {
    RobertScreenPreview(RobertFilterState.Failure("Failed to load to Robert settings. Check your network connection."))
}

@Composable
@Preview(showBackground = true, showSystemUi = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun RobertScreenSuccess() {
    val list = listOf(
        RobertFilter(
            title = "Malware",
            description = "Blocks all kinds of malware.",
            id = "malware",
            status = 1,
        ),
        RobertFilter(
            title = "Social",
            description = "Blocks facebook.",
            id = "social",
            status = 0,
        )
    )
    RobertScreenPreview(RobertFilterState.Success(list))
}