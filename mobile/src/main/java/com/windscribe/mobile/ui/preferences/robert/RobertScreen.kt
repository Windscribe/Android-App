package com.windscribe.mobile.ui.preferences.robert

import PreferencesNavBar
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.windscribe.mobile.ui.common.DescriptionWithLearnMore
import com.windscribe.mobile.ui.common.PreferenceBackground
import com.windscribe.mobile.ui.common.PreferenceProgressBar
import com.windscribe.mobile.ui.common.openUrl
import com.windscribe.mobile.ui.connection.ToastMessage
import com.windscribe.mobile.ui.helper.MultiDevicePreview
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

/**
 * Callbacks the Robert UI can raise. Hoisted out of the composables so the stateless
 * [RobertContent] never needs to know about [RobertViewModel] — previews supply no-op lambdas.
 */
class RobertActions(
    val onFilterSettingChanged: (RobertFilter, Int) -> Unit = { _, _ -> },
    val onManageRulesClick: () -> Unit = {},
)

/**
 * Stateful entry point. Owns the [RobertViewModel], collects its flows and wires up
 * side effects, then delegates rendering to [RobertContent].
 */
@Composable
fun RobertScreen(viewModel: RobertViewModel = hiltViewModel<RobertViewModelImpl>()) {
    val context = LocalContext.current
    val state by viewModel.robertFilterState.collectAsState()
    val showProgress by viewModel.showProgress.collectAsState()
    val goToState by viewModel.goToState.collectAsState(initial = RobertGoToState.None)

    LaunchedEffect(goToState) {
        when (val current = goToState) {
            is RobertGoToState.ManageRules -> {
                context.openUrl(current.url)
                viewModel.clearGoToState()
            }

            is RobertGoToState.Toast -> {
                val toast = current.message
                if (toast is ToastMessage.Raw) {
                    Toast.makeText(context, toast.message, Toast.LENGTH_SHORT).show()
                } else if (toast is ToastMessage.Localized) {
                    Toast.makeText(context, toast.message, Toast.LENGTH_SHORT).show()
                }
                viewModel.clearGoToState()
            }

            RobertGoToState.None -> {}
        }
    }

    RobertContent(
        state = state,
        showProgress = showProgress,
        actions =
            RobertActions(
                onFilterSettingChanged = viewModel::onFilterSettingChanged,
                onManageRulesClick = viewModel::onManageRulesClick,
            ),
    )
}

/**
 * Stateless Robert UI. Everything it needs is passed in, so it renders identically in the
 * app and in `@Preview`. This is the composable previews target.
 */
@Composable
fun RobertContent(
    state: RobertFilterState,
    showProgress: Boolean,
    actions: RobertActions,
) {
    val navController = LocalNavController.current
    PreferenceBackground {
        Column(
            modifier = Modifier.testTag("robert_screen").padding(vertical = 16.dp, horizontal = 16.dp).navigationBarsPadding(),
            horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
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
                        strokeWidth = 2.dp,
                    )
                }

                is RobertFilterState.Failure -> {
                    Text(
                        state.error,
                        style = font12,
                        color = MaterialTheme.colorScheme.primaryTextColor,
                    )
                }

                is RobertFilterState.Success -> {
                    val filters = state.filters
                    key(filters.hashCode()) {
                        Filters(filters, actions.onFilterSettingChanged, Modifier.weight(1.0f))
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            ManageCustomRule(actions.onManageRulesClick)
        }
        PreferenceProgressBar(showProgress)
    }
}

@Composable
private fun RobertScreenDescription() {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.primaryTextColor.copy(alpha = 0.10f),
                    shape = RoundedCornerShape(12.dp),
                ).padding(top = 14.dp, bottom = 0.dp, start = 16.dp, end = 16.dp),
    ) {
        Icon(
            painter = painterResource(com.windscribe.mobile.R.drawable.robert),
            contentDescription = "Robert icon image.",
            tint = MaterialTheme.colorScheme.primaryTextColor,
            modifier =
                Modifier
                    .align(androidx.compose.ui.Alignment.TopEnd)
                    .offset(x = (15.0).dp, y = (-13).dp)
                    .clip(RoundedCornerShape(topEnd = 11.dp)),
        )
        DescriptionWithLearnMore(stringResource(R.string.robert_description), FeatureExplainer.ROBERT)
    }
}

@Composable
private fun Filters(
    filters: List<RobertFilter>,
    onFilterSettingChanged: (RobertFilter, Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val iconMap =
        mapOf(
            Pair("malware", com.windscribe.mobile.R.drawable.ic_malware),
            Pair("ads", com.windscribe.mobile.R.drawable.ic_ads),
            Pair("social", com.windscribe.mobile.R.drawable.ic_social),
            Pair("porn", com.windscribe.mobile.R.drawable.ic_porn),
            Pair("gambling", com.windscribe.mobile.R.drawable.ic_gambling),
            Pair("fakenews", com.windscribe.mobile.R.drawable.ic_fake_news),
            Pair("competitors", com.windscribe.mobile.R.drawable.ic_other_vpn),
            Pair("cryptominers", com.windscribe.mobile.R.drawable.ic_crypto),
        )
    LazyColumn(modifier = modifier) {
        items(filters.size) { index ->
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier =
                    Modifier
                        .testTag("robert_filter_${filters[index].id}")
                        .background(
                            color = MaterialTheme.colorScheme.primaryTextColor.copy(alpha = 0.05f),
                            shape = RoundedCornerShape(size = 12.dp),
                        ).padding(vertical = 14.dp, horizontal = 14.dp),
            ) {
                Icon(
                    modifier =
                        Modifier
                            .size(24.dp)
                            .padding(4.dp),
                    painter =
                        painterResource(
                            iconMap.getOrDefault(
                                filters[index].id,
                                com.windscribe.mobile.R.drawable.ic_preference_placeholder,
                            ),
                        ),
                    contentDescription = "",
                    tint = MaterialTheme.colorScheme.primaryTextColor,
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    filters[index].title,
                    style =
                        font16.copy(
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primaryTextColor,
                        ),
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    if (filters[index].status == 1) stringResource(R.string.blocking) else stringResource(R.string.allowing),
                    style =
                        font14.copy(
                            fontWeight = FontWeight.Normal,
                            color =
                                if (filters[index].status ==
                                    1
                                ) {
                                    AppColors.neonGreen
                                } else {
                                    MaterialTheme.colorScheme.preferencesSubtitleColor
                                },
                        ),
                )
                Spacer(modifier = Modifier.width(8.dp))
                var isEnabled = filters[index].status == 1
                if (isEnabled) {
                    Image(
                        painter = painterResource(id = com.windscribe.mobile.R.drawable.ic_toggle_button_on),
                        contentDescription = null,
                        modifier =
                            Modifier
                                .testTag("robert_toggle_on_${filters[index].id}")
                                .clickable {
                                    isEnabled = !isEnabled
                                    onFilterSettingChanged(filters[index], if (isEnabled) 1 else 0)
                                },
                    )
                } else {
                    Image(
                        painter = painterResource(id = com.windscribe.mobile.R.drawable.ic_toggle_button_off),
                        contentDescription = null,
                        modifier =
                            Modifier
                                .testTag("robert_toggle_off_${filters[index].id}")
                                .clickable {
                                    isEnabled = !isEnabled
                                    onFilterSettingChanged(filters[index], if (isEnabled) 1 else 0)
                                },
                    )
                }
            }
        }
    }
}

@Composable
private fun ManageCustomRule(onManageRulesClick: () -> Unit = {}) {
    Row(
        modifier =
            Modifier
                .background(
                    color = MaterialTheme.colorScheme.primaryTextColor.copy(alpha = 0.05f),
                    shape = RoundedCornerShape(size = 12.dp),
                ).hapticClickable {
                    onManageRulesClick()
                }.padding(vertical = 14.dp, horizontal = 14.dp),
    ) {
        Spacer(modifier = Modifier.weight(1f))
        Text(
            stringResource(R.string.manage_custom_rules),
            style =
                font16.copy(
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primaryTextColor,
                ),
        )
        Spacer(modifier = Modifier.weight(1f))
    }
}

/**
 * Feeds representative [RobertFilterState] values into the preview. The renderer draws
 * [RobertContent] once per value, so the preview pane shows the loading, error and success states.
 */
private class RobertStateProvider : PreviewParameterProvider<RobertFilterState> {
    override val values =
        sequenceOf(
            RobertFilterState.Loading,
            RobertFilterState.Failure("Failed to load to Robert settings. Check your network connection."),
            RobertFilterState.Success(
                listOf(
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
                    ),
                ),
            ),
        )
}

@Composable
@MultiDevicePreview
private fun RobertContentPreview(
    @PreviewParameter(RobertStateProvider::class) state: RobertFilterState,
) {
    PreviewWithNav {
        RobertContent(
            state = state,
            showProgress = false,
            actions = RobertActions(),
        )
    }
}
