package com.windscribe.mobile.ui.preferences.split_tunnel

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
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.windscribe.mobile.ui.common.AppProgressBar
import com.windscribe.mobile.ui.common.CustomDropDown
import com.windscribe.mobile.ui.common.Description
import com.windscribe.mobile.ui.common.PreferenceBackground
import com.windscribe.mobile.ui.common.ScreenDescription
import com.windscribe.mobile.ui.helper.MultiDevicePreview
import com.windscribe.mobile.ui.helper.PreviewWithNav
import com.windscribe.mobile.ui.nav.LocalNavController
import com.windscribe.mobile.ui.theme.font16
import com.windscribe.mobile.ui.theme.primaryTextColor
import com.windscribe.vpn.R

@Composable
fun SplitTunnelScreen(viewModel: SplitTunnelViewModel? = null) {
    val navController = LocalNavController.current
    val showProgress by viewModel?.showProgress?.collectAsState()
        ?: remember { mutableStateOf(false) }
    PreferenceBackground {
        Column(modifier = Modifier.padding(vertical = 16.dp, horizontal = 16.dp)) {
            PreferencesNavBar(stringResource(R.string.split_tunneling)) {
                navController.popBackStack()
            }
            Spacer(modifier = Modifier.height(20.dp))
            ScreenDescription(stringResource(R.string.split_tunneling_feature))
            Spacer(modifier = Modifier.height(16.dp))
            Mode(viewModel)
            Spacer(modifier = Modifier.height(16.dp))
            AppsTitle()
            Spacer(modifier = Modifier.height(8.dp))
            Apps(viewModel)
        }
        AppProgressBar(showProgress, "")
    }
}

@Composable
private fun Mode(viewModel: SplitTunnelViewModel?) {
    val modes = viewModel?.modes ?: emptyList()
    val selectedKey by viewModel?.selectedModeKey?.collectAsState()
        ?: remember { mutableStateOf("") }
    Column {
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
                    painterResource(com.windscribe.mobile.R.drawable.ic_split_routing),
                    contentDescription = "",
                    colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primaryTextColor)
                )
                Spacer(modifier = Modifier.padding(8.dp))
                Text(
                    stringResource(com.windscribe.mobile.R.string.split_tunneling),
                    style = font16,
                    color = MaterialTheme.colorScheme.primaryTextColor
                )
            }
            Spacer(modifier = Modifier.padding(8.dp))
            Description(stringResource(com.windscribe.mobile.R.string.sound_notifications_description))
        }
        Spacer(modifier = Modifier.height(1.dp))
        CustomDropDown (R.string.mode, modes, selectedKey) {
            viewModel?.onModeSelected(it)
        }
    }
}

@Composable
private fun AppsTitle() {
    Text(
        text = stringResource(R.string.apps),
        color = MaterialTheme.colorScheme.primaryTextColor.copy(alpha = 0.50f),
        style = font16,
        textAlign = TextAlign.Start,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun Apps(viewModel: SplitTunnelViewModel? = null) {
    val apps by viewModel?.apps?.collectAsState() ?: remember { mutableStateOf(listOf()) }
    LazyColumn {
        items(apps.size) { index ->
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .height(48.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primaryTextColor.copy(alpha = 0.05f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(14.dp)
                    .clickable {
                        viewModel?.onAppSelected(apps[index])
                    }, verticalAlignment = Alignment.CenterVertically) {
                val bitmap = (apps[index].appIconDrawable).toBitmap()
                Image(
                    painter = BitmapPainter(bitmap.asImageBitmap()),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = apps[index].appName,
                    color = MaterialTheme.colorScheme.primaryTextColor.copy(alpha = 0.50f),
                    style = font16,
                    textAlign = TextAlign.Start,
                )
                Spacer(modifier = Modifier.weight(1f))
                if (apps[index].isChecked) {
                    Icon(
                        painterResource(com.windscribe.mobile.R.drawable.ic_check),
                        contentDescription = ""
                        , tint = MaterialTheme.colorScheme.primaryTextColor
                    )
                }
            }
        }
    }
}

@Composable
@MultiDevicePreview
private fun SplitTunnelScreenPreview() {
    PreviewWithNav {
        SplitTunnelScreen()
    }
}