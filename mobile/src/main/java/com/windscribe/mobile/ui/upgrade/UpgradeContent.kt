package com.windscribe.mobile.ui.upgrade

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.windscribe.mobile.R
import com.windscribe.mobile.ui.common.AppBackground
import com.windscribe.mobile.ui.common.AppProgressBar
import com.windscribe.mobile.ui.common.PrimaryButton
import com.windscribe.mobile.ui.helper.MultiDevicePreview
import com.windscribe.mobile.ui.helper.PreviewWithNav
import com.windscribe.mobile.ui.helper.hapticClickable
import com.windscribe.mobile.ui.helper.hapticClickableRipple
import com.windscribe.mobile.ui.theme.AppColors
import com.windscribe.mobile.ui.theme.font12
import com.windscribe.mobile.ui.theme.font14
import com.windscribe.mobile.ui.theme.font9

/**
 * Callbacks the upgrade UI can raise. Hoisted out of the composables so the stateless
 * [UpgradeContent] never needs to know about the view models — previews supply no-op lambdas.
 */
class UpgradeActions(
    val onClose: () -> Unit = {},
    val onRestore: () -> Unit = {},
    val onSelectMonthly: () -> Unit = {},
    val onSelectYearly: () -> Unit = {},
    val onSubscribe: () -> Unit = {},
    val onTermsClick: () -> Unit = {},
    val onPrivacyClick: () -> Unit = {},
    val onDismissError: () -> Unit = {},
)

/**
 * Stateless upgrade UI mirroring the original `activity_upgrade.xml`. Everything it needs is
 * passed in, so it renders identically in the app and in `@Preview`. The flavor-specific
 * `UpgradeScreen` owns the state and wires the actions.
 */
@Composable
fun UpgradeContent(
    state: UpgradeState,
    actions: UpgradeActions,
) {
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
            TopBar(state, actions)
            Image(
                painter = painterResource(R.drawable.upgrade_logo),
                contentDescription = null,
                modifier = Modifier.padding(top = 24.dp),
            )
            Hero()
            Spacer(modifier = Modifier.height(16.dp))
            FeatureRow(
                titleRes = com.windscribe.vpn.R.string.unlimited_everything,
                subtitleRes = com.windscribe.vpn.R.string.use_on_all_devices_with_no_data_limits,
            )
            Spacer(modifier = Modifier.height(10.dp))
            FeatureRow(
                titleRes = com.windscribe.vpn.R.string.all_vpn_locations,
                subtitleRes = com.windscribe.vpn.R.string.servers_in_cities_more_than_any_other_vpn,
            )
            Spacer(modifier = Modifier.height(10.dp))
            FeatureRow(
                titleRes = com.windscribe.vpn.R.string.increased_speed_and_security,
                subtitleRes = com.windscribe.vpn.R.string.blocks_malicious_websites_trackers,
            )
            PlansRow(state, actions)
            PrimaryButton(
                modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp),
                text = stringResource(com.windscribe.vpn.R.string.subscribe),
                enabled = state.selectedSku != null,
                onClick = actions.onSubscribe,
            )
            Text(
                text = stringResource(com.windscribe.vpn.R.string.subscriptions_info),
                style = font9,
                color = AppColors.white.copy(alpha = 0.5f),
                textAlign = TextAlign.Center,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp, start = 16.dp, end = 16.dp),
            )
            TermsLine(actions)
            Spacer(modifier = Modifier.height(16.dp))
        }
        if (state.errorMessage != null) {
            ErrorOverlay(message = state.errorMessage, onDismiss = actions.onDismissError)
        }
        AppProgressBar(state.showProgress, message = state.loadingMessage ?: "")
    }
}

@Composable
private fun TopBar(
    state: UpgradeState,
    actions: UpgradeActions,
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
                    .hapticClickableRipple { actions.onClose() }
                    .padding(8.dp),
        )
        Spacer(modifier = Modifier.weight(1f))
        if (state.showRestore) {
            Text(
                text = stringResource(com.windscribe.vpn.R.string.restore),
                style = font14,
                color = AppColors.white.copy(alpha = 0.8f),
                modifier =
                    Modifier
                        .hapticClickable { actions.onRestore() }
                        .padding(8.dp),
            )
        }
    }
}

@Composable
private fun Hero() {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(180.dp)
                .padding(top = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        AndroidView(
            factory = { context -> StarsView(context) },
            modifier = Modifier.fillMaxSize(),
        )
        Image(
            painter = painterResource(R.drawable.upgrade_hero_grid),
            contentDescription = null,
            modifier = Modifier.fillMaxWidth(),
        )
        Image(
            painter = painterResource(R.drawable.upgrade_hero),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
private fun FeatureRow(
    titleRes: Int,
    subtitleRes: Int,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(titleRes),
                style = font14,
                color = AppColors.white,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(subtitleRes),
                style = font12,
                color = AppColors.white.copy(alpha = 0.8f),
            )
        }
        Image(
            painter = painterResource(R.drawable.ic_star),
            contentDescription = null,
            modifier = Modifier.padding(start = 16.dp),
        )
    }
}

@Composable
private fun PlansRow(
    state: UpgradeState,
    actions: UpgradeActions,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, start = 16.dp, end = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        state.monthly?.let { tile ->
            PlanTileView(
                modifier = Modifier.weight(1f),
                tile = tile,
                titleRes = com.windscribe.vpn.R.string.plan_monthly,
                selected = state.monthlySelected,
                showRadio = !state.isPromo,
                onClick = actions.onSelectMonthly,
            )
        }
        state.yearly?.let { tile ->
            PlanTileView(
                modifier = Modifier.weight(1f),
                tile = tile,
                titleRes = com.windscribe.vpn.R.string.plan_yearly,
                selected = !state.monthlySelected,
                showRadio = !state.isPromo,
                onClick = actions.onSelectYearly,
            )
        }
    }
}

@Composable
private fun PlanTileView(
    modifier: Modifier,
    tile: PlanTile,
    titleRes: Int,
    selected: Boolean,
    showRadio: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier =
            modifier
                .height(142.dp)
                .hapticClickable { onClick() },
    ) {
        AndroidView(
            factory = { context -> PlanUpgradeStarsBackgroundView(context) },
            update = { it.active = selected },
            modifier = Modifier.fillMaxSize(),
        )
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(titleRes),
                    style = font14,
                    color = AppColors.white,
                )
                tile.discountLabel?.let { label ->
                    Text(
                        text = label,
                        style = font9,
                        color = AppColors.deepBlue,
                        modifier =
                            Modifier
                                .padding(start = 8.dp)
                                .background(
                                    color = AppColors.neonGreen,
                                    shape = RoundedCornerShape(4.dp),
                                ).padding(horizontal = 4.dp, vertical = 2.dp),
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                if (showRadio) {
                    Box(
                        modifier =
                            Modifier
                                .size(20.dp)
                                .background(
                                    color =
                                        if (selected) {
                                            AppColors.neonGreen
                                        } else {
                                            AppColors.white.copy(alpha = 0.2f)
                                        },
                                    shape = RoundedCornerShape(10.dp),
                                ),
                    )
                }
            }
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = tile.price,
                style = font14,
                color = AppColors.white,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text =
                    buildAnnotatedString {
                        tile.billedLabel.strikeThroughPrice?.let { struck ->
                            withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough)) {
                                append(struck)
                            }
                            append(" ")
                        }
                        append(tile.billedLabel.text)
                    },
                style = font9,
                color = AppColors.white.copy(alpha = 0.8f),
                maxLines = 2,
            )
        }
        tile.promoLabel?.let { promo ->
            Text(
                text = promo,
                style = font9,
                color = AppColors.deepBlue,
                modifier =
                    Modifier
                        .align(Alignment.TopEnd)
                        .background(
                            color = AppColors.neonGreen,
                            shape = RoundedCornerShape(bottomStart = 8.dp, topEnd = 12.dp),
                        ).padding(horizontal = 8.dp, vertical = 4.dp),
            )
        }
    }
}

@Composable
private fun TermsLine(actions: UpgradeActions) {
    val appName = stringResource(com.windscribe.vpn.R.string.app_name)
    val termsPolicy = stringResource(com.windscribe.vpn.R.string.terms_policy_en)
    val linkStyles =
        TextLinkStyles(
            style =
                SpanStyle(
                    color = AppColors.white,
                    textDecoration = TextDecoration.Underline,
                ),
        )
    val annotated =
        buildAnnotatedString {
            withStyle(SpanStyle(color = AppColors.white.copy(alpha = 0.8f))) {
                append(appName)
                append(" ")
            }
            val separatorIndex = termsPolicy.indexOf('&')
            if (separatorIndex == -1) {
                withStyle(SpanStyle(color = AppColors.white.copy(alpha = 0.8f))) {
                    append(termsPolicy)
                }
            } else {
                val termsPart = termsPolicy.substring(0, separatorIndex).trim()
                val privacyPart = termsPolicy.substring(separatorIndex + 1).trim()
                withLink(LinkAnnotation.Clickable("terms", linkStyles) { actions.onTermsClick() }) {
                    append(termsPart)
                }
                withStyle(SpanStyle(color = AppColors.white.copy(alpha = 0.8f))) {
                    append(" & ")
                }
                withLink(LinkAnnotation.Clickable("privacy", linkStyles) { actions.onPrivacyClick() }) {
                    append(privacyPart)
                }
            }
        }
    Text(
        text = annotated,
        style = font9,
        textAlign = TextAlign.Center,
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, start = 16.dp, end = 16.dp),
    )
}

@Composable
private fun BoxScope.ErrorOverlay(
    message: String,
    onDismiss: () -> Unit,
) {
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(AppColors.deepBlue.copy(alpha = 0.9f))
                .hapticClickable { onDismiss() },
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier =
                Modifier
                    .padding(32.dp)
                    .background(
                        color = AppColors.charcoalBlue,
                        shape = RoundedCornerShape(9.dp),
                    ).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = message,
                style = font14,
                color = AppColors.white,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "OK",
                style = font14,
                color = AppColors.neonGreen,
                modifier =
                    Modifier
                        .hapticClickable { onDismiss() }
                        .padding(8.dp),
            )
        }
    }
}

@MultiDevicePreview
@Composable
private fun UpgradeContentPreview() {
    PreviewWithNav {
        UpgradeContent(
            state =
                UpgradeState(
                    loadingMessage = null,
                    monthly =
                        PlanTile(
                            sku = "monthly_sku",
                            title = "Monthly",
                            price = "USD 9.00",
                            billedLabel = PlanBilledLabel(text = "Billed Monthly"),
                        ),
                    yearly =
                        PlanTile(
                            sku = "yearly_sku",
                            title = "Yearly",
                            price = "USD 69.00",
                            billedLabel = PlanBilledLabel(text = "$5.75/month, Billed Annually"),
                            discountLabel = "33%",
                        ),
                    isPromo = false,
                    monthlySelected = false,
                    showRestore = true,
                ),
            actions = UpgradeActions(),
        )
    }
}
