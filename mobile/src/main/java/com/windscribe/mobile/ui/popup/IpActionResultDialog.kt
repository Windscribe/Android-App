package com.windscribe.mobile.ui.popup

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.windscribe.mobile.R
import com.windscribe.mobile.ui.AppStartActivity
import com.windscribe.mobile.ui.common.PopupContainer
import com.windscribe.mobile.ui.common.PopupDescription
import com.windscribe.mobile.ui.common.PopupHeroImage
import com.windscribe.mobile.ui.common.PopupPrimaryActionButton
import com.windscribe.mobile.ui.common.PopupSecondaryActionButton
import com.windscribe.mobile.ui.common.PopupTitle
import com.windscribe.mobile.ui.common.openUrl
import com.windscribe.mobile.ui.helper.MultiDevicePreview
import com.windscribe.mobile.ui.helper.PreviewWithNav
import com.windscribe.mobile.ui.nav.LocalNavController
import com.windscribe.vpn.constants.NetworkKeyConstants

@Composable
fun IpActionResultDialog(
    message: String?,
    description: String? = null
) {
    val navController = LocalNavController.current
    val activity = navController.context as? AppStartActivity
    PopupContainer {
        Spacer(Modifier.weight(1f))
        PopupHeroImage(R.drawable.garry_location_under_maintence)
        Spacer(modifier = Modifier.height(25.dp))
        PopupTitle(message ?: "")
        Spacer(Modifier.height(25.dp))
        PopupDescription(description ?: stringResource(com.windscribe.vpn.R.string.check_status_description))
        Spacer(Modifier.height(32.dp))
        PopupPrimaryActionButton(modifier = Modifier, "Check Location Status") {
            activity?.openUrl(NetworkKeyConstants.NODE_STATUS_URL)
            navController.popBackStack()
        }
        Spacer(Modifier.height(16.dp))
        PopupSecondaryActionButton(
            modifier = Modifier,
            stringResource(com.windscribe.vpn.R.string.back)
        ) {
            navController.popBackStack()
        }
        Spacer(Modifier.weight(1f))
    }
}

@Composable
@MultiDevicePreview
fun IpActionResultDialogPreview() {
    PreviewWithNav {
        IpActionResultDialog("Could not rotate IP")
    }
}