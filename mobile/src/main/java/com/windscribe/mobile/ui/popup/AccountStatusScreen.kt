package com.windscribe.mobile.ui.popup

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.rememberNavController
import com.windscribe.mobile.R
import com.windscribe.mobile.ui.AppStartActivity
import com.windscribe.mobile.ui.common.PopupContainer
import com.windscribe.mobile.ui.common.PopupDescription
import com.windscribe.mobile.ui.common.PopupHeroImage
import com.windscribe.mobile.ui.common.PopupPrimaryActionButton
import com.windscribe.mobile.ui.common.PopupSecondaryActionButton
import com.windscribe.mobile.ui.common.PopupTitle
import com.windscribe.mobile.ui.helper.MultiDevicePreview
import com.windscribe.mobile.ui.model.AccountStatusDialogData
import com.windscribe.mobile.ui.nav.LocalNavController
import com.windscribe.mobile.upgradeactivity.UpgradeActivity


@Composable
fun AccountStatusScreen(data: AccountStatusDialogData) {
    val navController = LocalNavController.current
    val activity = LocalContext.current as? AppStartActivity
    PopupContainer {
        Spacer(Modifier.weight(1f))
        PopupHeroImage(data.icon)
        Spacer(modifier = Modifier.height(25.dp))
        PopupTitle(data.title)
        Spacer(Modifier.height(25.dp))
        PopupDescription(data.description)
        Spacer(Modifier.height(32.dp))
        if (data.showPrimaryButton) {
            PopupPrimaryActionButton(modifier = Modifier, data.primaryText) {
                activity?.startActivity(UpgradeActivity.getStartIntent(activity))
                navController.popBackStack()
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
        if (data.showSecondaryButton) {
            PopupSecondaryActionButton(modifier = Modifier, data.secondaryText) {
                navController.popBackStack()
            }
        }
        Spacer(Modifier.weight(1f))
    }
}

@MultiDevicePreview
@Composable
private fun AccountStateBannedScreenPreview() {
    val bannedData = AccountStatusDialogData(
        title = stringResource(com.windscribe.vpn.R.string.you_ve_been_banned),
        icon = R.drawable.garry_account_ban,
        description = stringResource(com.windscribe.vpn.R.string.you_ve_violated_our_terms),
        showSecondaryButton = true,
        secondaryText = stringResource(com.windscribe.vpn.R.string.close),
        showPrimaryButton = false,
        primaryText = ""
    )
    CompositionLocalProvider(
        LocalNavController provides rememberNavController()
    ) {
        AccountStatusScreen(bannedData)
    }
}

@MultiDevicePreview
@Composable
private fun AccountStateOutOfDataScreenPreview() {
    val expireData = AccountStatusDialogData(
        title = stringResource(com.windscribe.vpn.R.string.you_re_out_of_data),
        icon = R.drawable.garry_account_no_data,
        description = stringResource(
            com.windscribe.vpn.R.string.upgrade_to_stay_protected,
            "YYYY-MM-DD"
        ),
        showSecondaryButton = true,
        secondaryText = stringResource(com.windscribe.vpn.R.string.back),
        showPrimaryButton = true,
        primaryText = stringResource(com.windscribe.vpn.R.string.upgrade_case_normal)
    )
    CompositionLocalProvider(
        LocalNavController provides rememberNavController()
    ) {
        AccountStatusScreen(expireData)
    }
}

@MultiDevicePreview
@Composable
private fun AccountStateDowngradedPreview() {
    val downgraded = AccountStatusDialogData(
        title = stringResource(com.windscribe.vpn.R.string.you_r_pro_plan_expired),
        icon = R.drawable.garry_downgraded,
        description = stringResource(com.windscribe.vpn.R.string.you_ve_been_downgraded_to_free_for_now),
        showSecondaryButton = true,
        secondaryText = stringResource(com.windscribe.vpn.R.string.back),
        showPrimaryButton = true,
        primaryText = stringResource(com.windscribe.vpn.R.string.renew_plan)
    )
    CompositionLocalProvider(
        LocalNavController provides rememberNavController()
    ) {
        AccountStatusScreen(downgraded)
    }
}
