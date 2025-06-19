package com.windscribe.mobile.ui.preferences.network_options

import PreferencesNavBar
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.windscribe.mobile.ui.common.PreferenceBackground
import com.windscribe.mobile.ui.common.ScreenDescription
import com.windscribe.mobile.ui.common.SwitchItemView
import com.windscribe.mobile.ui.helper.MultiDevicePreview
import com.windscribe.mobile.ui.helper.PreviewWithNav
import com.windscribe.mobile.ui.nav.LocalNavController
import com.windscribe.mobile.ui.nav.Screen
import com.windscribe.mobile.ui.theme.font12
import com.windscribe.mobile.ui.theme.font16
import com.windscribe.mobile.ui.theme.preferencesSubtitleColor
import com.windscribe.mobile.ui.theme.primaryTextColor
import com.windscribe.vpn.R
import com.windscribe.vpn.localdatabase.tables.NetworkInfo

@Composable
fun NetworkOptionsScreen(viewModel: NetworkOptionsViewModel? = null) {
    val navController = LocalNavController.current
    val autoSecureEnabled by viewModel?.autoSecureEnabled?.collectAsState()
        ?: remember { mutableStateOf(false) }
    val currentNetwork by viewModel?.currentNetwork?.collectAsState() ?: remember {
        mutableStateOf(
            null
        )
    }
    PreferenceBackground {
        Column(modifier = Modifier.padding(vertical = 16.dp, horizontal = 16.dp)) {
            PreferencesNavBar(stringResource(R.string.network_options)) {
                navController.popBackStack()
            }
            Spacer(modifier = Modifier.height(20.dp))
            SwitchItemView(
                title = com.windscribe.mobile.R.string.auto_secure_new_networks,
                icon = com.windscribe.mobile.R.drawable.ic_wifi,
                description = com.windscribe.mobile.R.string.auto_secure_new_networks_description,
                autoSecureEnabled,
                onSelect = {
                    viewModel?.onAutoSecureChanged()
                }
            )
            if (currentNetwork != null) {
                Spacer(modifier = Modifier.height(16.dp))
                CurrentNetwork(currentNetwork!!)
            }
            Spacer(modifier = Modifier.height(16.dp))
            OtherNetworks(viewModel)
        }
    }
}

@Composable
private fun CurrentNetwork(networkInfo: NetworkInfo) {
    Column {
        Text(
            text = stringResource(R.string.current_network),
            color = MaterialTheme.colorScheme.preferencesSubtitleColor,
            style = font12.copy(fontWeight = FontWeight.SemiBold),
            textAlign = TextAlign.Start,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        Network(networkInfo)
    }
}

@Composable
private fun Network(networkInfo: NetworkInfo) {
    val navController = LocalNavController.current
    Row(
        modifier = Modifier
            .height(48.dp)
            .background(
                color = MaterialTheme.colorScheme.primaryTextColor.copy(alpha = 0.05f),
                shape = RoundedCornerShape(12.dp)
            )
            .clickable {
                navController.currentBackStackEntry?.savedStateHandle?.set(
                    "network_name",
                    networkInfo.networkName
                )
                navController.navigate(Screen.NetworkDetails.route)
            }
            .padding(14.dp)
           , verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = networkInfo.networkName,
            color = MaterialTheme.colorScheme.primaryTextColor,
            style = font16.copy(fontWeight = FontWeight.Medium),
            textAlign = TextAlign.Start,
        )
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = stringResource(if (networkInfo.isAutoSecureOn) R.string.network_secured else R.string.network_unsecured),
            color = MaterialTheme.colorScheme.preferencesSubtitleColor,
            style = font16.copy(fontWeight = FontWeight.Normal),
            textAlign = TextAlign.Start,
        )
        Spacer(modifier = Modifier.width(8.dp))
        Icon(
            painterResource(com.windscribe.mobile.R.drawable.arrow_right),
            contentDescription = "", tint = MaterialTheme.colorScheme.primaryTextColor
        )
    }
}

@Composable
private fun OtherNetworks(viewModel: NetworkOptionsViewModel?) {
    val allNetworks by viewModel?.allNetworks?.collectAsState() ?: remember {
        mutableStateOf(
            emptyList()
        )
    }
    if (allNetworks.isNotEmpty()) {
        Column {
            Text(
                text = stringResource(R.string.other_networks),
                color = MaterialTheme.colorScheme.preferencesSubtitleColor,
                style = font12.copy(fontWeight = FontWeight.SemiBold),
                textAlign = TextAlign.Start,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            LazyColumn {
                items(allNetworks.size) { index ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Network(allNetworks[index])
                }
            }
        }
    }
}

@Composable
@MultiDevicePreview
private fun NetworkOptionsScreenPreview() {
    PreviewWithNav {
        NetworkOptionsScreen()
    }
}