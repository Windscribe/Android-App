package com.windscribe.mobile.ui.upgrade

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
            HeroSection(state, actions)
            FeatureRow(
                titleRes = com.windscribe.vpn.R.string.unlimited_everything,
                subtitleRes = com.windscribe.vpn.R.string.use_on_all_devices_with_no_data_limits,
                topPadding = 24.dp,
            )
            FeatureRow(
                titleRes = com.windscribe.vpn.R.string.all_vpn_locations,
                subtitleRes = com.windscribe.vpn.R.string.servers_in_cities_more_than_any_other_vpn,
                topPadding = 10.dp,
            )
            FeatureRow(
                titleRes = com.windscribe.vpn.R.string.increased_speed_and_security,
                subtitleRes = com.windscribe.vpn.R.string.blocks_malicious_websites_trackers,
                topPadding = 10.dp,
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
                style = font12.copy(fontSize = 10.sp, textAlign = TextAlign.Start),
                color = AppColors.white.copy(alpha = 0.5f),
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

/**
 * Top section: a single tall box so the [StarsView] starfield fills the whole upper region —
 * behind the close row, the logo and the hero plane — matching the original constraint where the
 * stars span from the top of the screen to the bottom of the hero image.
 */
@Composable
private fun HeroSection(
    state: UpgradeState,
    actions: UpgradeActions,
) {
    // No fixed height: the box wraps its content (close row + logo + hero) so the StarsView
    // starfield (matchParentSize) covers exactly that region and the feature list follows
    // immediately — a fixed height left a dead gap between the hero and "Unlimited Everything".
    Box(modifier = Modifier.fillMaxWidth()) {
        AndroidView(
            factory = { context -> StarsView(context) },
            modifier = Modifier.matchParentSize(),
        )
        Column(
            modifier = Modifier.fillMaxWidth(),
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
                            .hapticClickableRipple { actions.onClose() }
                            .padding(8.dp),
                )
                Spacer(modifier = Modifier.weight(1f))
                if (state.showRestore) {
                    Text(
                        text = stringResource(com.windscribe.vpn.R.string.restore),
                        style = font14.copy(fontWeight = FontWeight.SemiBold),
                        color = AppColors.white.copy(alpha = 0.8f),
                        modifier =
                            Modifier
                                .hapticClickable { actions.onRestore() }
                                .padding(8.dp),
                    )
                }
            }
            Image(
                painter = painterResource(R.drawable.upgrade_logo),
                contentDescription = null,
                modifier = Modifier.padding(top = 24.dp),
            )
            // Hero region (matches activity_upgrade.xml): the perspective-floor grid
            // (upgrade_hero_grid, 458x82dp) sits 105dp down from the hero top — wider than the
            // screen and low, so it reads as a receding horizon — with the plane drawn on top of
            // it. The grid must keep its intrinsic 458x82 size (not fillMaxWidth) and must NOT be
            // covered by the plane, otherwise the floor disappears (the previous bug).
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .padding(top = 16.dp),
            ) {
                Image(
                    painter = painterResource(R.drawable.upgrade_hero_grid),
                    contentDescription = null,
                    contentScale = ContentScale.FillBounds,
                    modifier =
                        Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 105.dp)
                            .width(458.dp)
                            .height(82.dp),
                )
                Image(
                    painter = painterResource(R.drawable.upgrade_hero),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier =
                        Modifier
                            .align(Alignment.Center)
                            .fillMaxSize(),
                )
            }
        }
    }
}

@Composable
private fun FeatureRow(
    titleRes: Int,
    subtitleRes: Int,
    topPadding: Dp,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = topPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(titleRes),
                style = font14.copy(textAlign = TextAlign.Start),
                color = AppColors.white,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(subtitleRes),
                style = font12.copy(textAlign = TextAlign.Start),
                color = AppColors.white.copy(alpha = 0.5f),
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
            modifier = Modifier.matchParentSize(),
        )
        // Original tile (activity_upgrade.xml): title pinned top, price directly below it
        // (marginTop 2dp), and the billed line pinned to the BOTTOM. So price belongs to the top
        // group and only the billed line floats down — not price+billed pushed down together.
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(titleRes),
                    style = font14.copy(fontWeight = FontWeight.Normal, textAlign = TextAlign.Start),
                    color = AppColors.white,
                )
                tile.discountLabel?.let { label ->
                    Text(
                        text = label,
                        style =
                            font14.copy(
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                            ),
                        color = AppColors.deepBlue,
                        modifier =
                            Modifier
                                .padding(start = 8.dp)
                                .background(
                                    color = AppColors.white,
                                    shape = RoundedCornerShape(4.dp),
                                ).padding(horizontal = 4.dp, vertical = 2.dp),
                    )
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = tile.price,
                style =
                    font14.copy(
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Start,
                    ),
                color = AppColors.white,
            )
            Spacer(modifier = Modifier.weight(1f))
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
                style =
                    font14.copy(
                        fontWeight = FontWeight.Normal,
                        textAlign = TextAlign.Start,
                    ),
                color = AppColors.white,
                maxLines = 2,
            )
        }
        if (showRadio) {
            Box(modifier = Modifier.align(Alignment.TopEnd).padding(top = 16.dp, end = 16.dp)) {
                if (selected) {
                    Image(
                        painter = painterResource(R.drawable.checked_radio_circle),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                    )
                } else {
                    Box(
                        modifier =
                            Modifier
                                .size(20.dp)
                                .border(
                                    width = 2.dp,
                                    color = AppColors.white.copy(alpha = 0.5f),
                                    shape = CircleShape,
                                ),
                    )
                }
            }
        }
        tile.promoLabel?.let { promo ->
            Text(
                text = promo.uppercase(),
                style =
                    font14.copy(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                    ),
                color = AppColors.black,
                modifier =
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 8.dp, end = 8.dp)
                        .background(
                            color = Color(0xFFCADFF2),
                            shape = RoundedCornerShape(4.dp),
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
        style = font12.copy(fontSize = 10.sp, textAlign = TextAlign.Start),
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
                            billedLabel = PlanBilledLabel(text = "$ 8.00/month, Billed Annually"),
                            discountLabel = "-33%",
                        ),
                    isPromo = false,
                    monthlySelected = true,
                    showRestore = true,
                ),
            actions = UpgradeActions(),
        )
    }
}
