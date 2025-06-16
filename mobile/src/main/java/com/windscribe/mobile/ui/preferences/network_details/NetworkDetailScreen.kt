package com.windscribe.mobile.ui.preferences.network_details

import PreferencesNavBar
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.windscribe.mobile.ui.common.CustomDropDown
import com.windscribe.mobile.ui.common.Description
import com.windscribe.mobile.ui.common.PreferenceBackground
import com.windscribe.mobile.ui.common.SwitchItemView
import com.windscribe.mobile.ui.helper.MultiDevicePreview
import com.windscribe.mobile.ui.helper.PreviewWithNav
import com.windscribe.mobile.ui.model.DropDownStringItem
import com.windscribe.mobile.ui.nav.LocalNavController
import com.windscribe.mobile.ui.theme.font16
import com.windscribe.mobile.ui.theme.primaryTextColor
import com.windscribe.vpn.R
import com.windscribe.vpn.localdatabase.tables.NetworkInfo

@Composable
fun NetworkDetailScreen(viewModel: NetworkDetailViewModel? = null) {
    val navController = LocalNavController.current
    val networkDetail by viewModel?.networkDetail?.collectAsState() ?: remember {
        mutableStateOf(
            null
        )
    }
    val isMyNetwork by viewModel?.isMyNetwork?.collectAsState()
        ?: remember { mutableStateOf(false) }
    if (networkDetail == null) {
        return
    }
    PreferenceBackground {
        Column(modifier = Modifier.padding(vertical = 16.dp, horizontal = 16.dp)) {
            PreferencesNavBar(stringResource(R.string.network_options)) {
                navController.popBackStack()
            }
            Spacer(modifier = Modifier.height(20.dp))
            SwitchItemView(
                title = com.windscribe.mobile.R.string.auto_secure,
                icon = com.windscribe.mobile.R.drawable.ic_auto_secure_check,
                description = com.windscribe.mobile.R.string.auto_secure_description,
                networkDetail!!.isAutoSecureOn,
                onSelect = {
                    viewModel?.onAutoSecureChanged()
                }
            )
            Spacer(modifier = Modifier.height(16.dp))
            PreferredProtocol(viewModel, networkDetail)
            Spacer(modifier = Modifier.height(16.dp))
            if (!isMyNetwork) {
                ForgetNetwork(viewModel)
            }
        }
    }
}

@Composable
private fun PreferredProtocol(
    viewModel: NetworkDetailViewModel? = null,
    networkInfo: NetworkInfo? = null
) {
    val protocols by viewModel?.protocols?.collectAsState()
        ?: remember { mutableStateOf(emptyList<DropDownStringItem>()) }
    val ports by viewModel?.ports?.collectAsState()
        ?: remember { mutableStateOf(emptyList<DropDownStringItem>()) }
    Column {
        Header(viewModel, networkInfo)
        Spacer(modifier = Modifier.height(1.dp))
        CustomDropDown(
            R.string.protocol,
            protocols,
            networkInfo?.protocol ?: "",
            onSelect = {
                viewModel?.onProtocolSelected(it)
            }, shape = RoundedCornerShape(0.dp)
        )
        Spacer(modifier = Modifier.height(1.dp))
        CustomDropDown(
            R.string.port,
            ports,
            networkInfo?.port ?: "",
            onSelect = {
                viewModel?.onPortSelected(it)
            }, shape = RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp)
        )
    }
}

@Composable
private fun Header(viewModel: NetworkDetailViewModel?, networkDetail: NetworkInfo?) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.primaryTextColor.copy(alpha = 0.05f),
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
            )
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
                painterResource(com.windscribe.mobile.R.drawable.ic_connection_mode_icon),
                contentDescription = "",
                colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primaryTextColor)
            )
            Spacer(modifier = Modifier.padding(8.dp))
            Text(
                stringResource(com.windscribe.mobile.R.string.preferred_protocol),
                style = font16,
                color = MaterialTheme.colorScheme.primaryTextColor
            )
            Spacer(modifier = Modifier.weight(1f))
            if (networkDetail?.isPreferredOn == true) {
                Image(
                    painter = painterResource(id = com.windscribe.mobile.R.drawable.ic_toggle_button_on),
                    contentDescription = null,
                    modifier = Modifier.clickable {
                        viewModel?.onPreferredChanged()
                    }
                )
            } else {
                Image(
                    painter = painterResource(id = com.windscribe.mobile.R.drawable.ic_toggle_button_off),
                    contentDescription = null,
                    modifier = Modifier.clickable {
                        viewModel?.onPreferredChanged()
                    }
                )
            }
        }
        Spacer(modifier = Modifier.padding(8.dp))
        Description(stringResource(com.windscribe.mobile.R.string.preferred_protocol_description))
    }
}

@Composable
private fun ForgetNetwork(viewModel: NetworkDetailViewModel? = null) {
    val navController = LocalNavController.current
    Row(
        modifier = Modifier
            .background(
                color = MaterialTheme.colorScheme.primaryTextColor.copy(alpha = 0.05f),
                shape = RoundedCornerShape(size = 12.dp)
            )
            .clickable {
                viewModel?.forgetNetwork()
                navController.popBackStack()
            }
            .padding(vertical = 14.dp, horizontal = 14.dp)

    ) {
        Spacer(modifier = Modifier.weight(1f))
        Text(
            stringResource(R.string.forget_network),
            style = font16.copy(
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primaryTextColor
            )
        )
        Spacer(modifier = Modifier.weight(1f))
    }
}

@Composable
@MultiDevicePreview
private fun NetworkDetailScreenPreview() {
    PreviewWithNav {
        NetworkDetailScreen()
    }
}