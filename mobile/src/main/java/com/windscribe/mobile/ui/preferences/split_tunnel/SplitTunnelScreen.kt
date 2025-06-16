package com.windscribe.mobile.ui.preferences.split_tunnel

import PreferencesNavBar
import android.annotation.SuppressLint
import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.windscribe.mobile.ui.common.AppProgressBar
import com.windscribe.mobile.ui.common.CustomDropDown
import com.windscribe.mobile.ui.common.PreferenceBackground
import com.windscribe.mobile.ui.common.PreferenceProgressBar
import com.windscribe.mobile.ui.common.SwitchItemView
import com.windscribe.mobile.ui.helper.MultiDevicePreview
import com.windscribe.mobile.ui.helper.PreviewWithNav
import com.windscribe.mobile.ui.model.DropDownStringItem
import com.windscribe.mobile.ui.nav.LocalNavController
import com.windscribe.mobile.ui.theme.AppColors
import com.windscribe.mobile.ui.theme.font16
import com.windscribe.mobile.ui.theme.preferencesBackgroundColor
import com.windscribe.mobile.ui.theme.preferencesSubtitleColor
import com.windscribe.mobile.ui.theme.primaryTextColor
import com.windscribe.vpn.R
import com.windscribe.vpn.api.response.InstalledAppsData
import com.windscribe.vpn.constants.FeatureExplainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@Composable
fun SplitTunnelScreen(viewModel: SplitTunnelViewModel? = null) {
    val navController = LocalNavController.current
    val showProgress by viewModel?.showProgress?.collectAsState()
        ?: remember { mutableStateOf(false) }
    PreferenceBackground {
        Column(
            modifier = Modifier
                .padding(vertical = 16.dp, horizontal = 16.dp)
                .navigationBarsPadding()
        ) {
            PreferencesNavBar(stringResource(R.string.split_tunneling)) {
                navController.popBackStack()
            }
            Spacer(modifier = Modifier.height(20.dp))
            Mode(viewModel)
            Spacer(modifier = Modifier.height(14.dp))
            AppsTitle()
            Spacer(modifier = Modifier.height(8.dp))
            Search(viewModel)
            Apps(viewModel)
        }
        PreferenceProgressBar(showProgress)
    }
}

@Composable
private fun Search(viewModel: SplitTunnelViewModel? = null) {
    val query by viewModel?.searchKeyword?.collectAsState() ?: remember { mutableStateOf("") }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .background(
                color = MaterialTheme.colorScheme.primaryTextColor.copy(alpha = 0.05f),
                shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
            )
            .padding(horizontal = 12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxSize()
        ) {
            Image(
                painter = painterResource(com.windscribe.mobile.R.drawable.ic_location_search),
                contentDescription = "Search",
                modifier = Modifier.clickable {

                },
                colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primaryTextColor.copy(alpha = 0.70f))
            )
            TextField(
                value = query,
                onValueChange = {
                    viewModel?.onQueryTextChange(it)
                },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                textStyle = font16.copy(textAlign = TextAlign.Start, color = MaterialTheme.colorScheme.primaryTextColor),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent,
                    cursorColor = MaterialTheme.colorScheme.primaryTextColor,
                ),
                keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Search),
            )
        }
    }
}

@Composable
private fun Mode(viewModel: SplitTunnelViewModel?) {
    val modes = viewModel?.modes ?: emptyList()
    val selectedKey by viewModel?.selectedModeKey?.collectAsState()
        ?: remember { mutableStateOf("") }
    val modeDescription =
        if (selectedKey == "Exclusive") R.string.feature_tunnel_mode_exclusive else R.string.feature_tunnel_mode_inclusive
    val splitTunnelEnabled by viewModel?.isSplitTunnelEnabled?.collectAsState()
        ?: remember { mutableStateOf(false) }
    Column {
        SwitchItemView(
            title = com.windscribe.mobile.R.string.split_tunneling,
            icon = com.windscribe.mobile.R.drawable.ic_split_routing,
            description = com.windscribe.mobile.R.string.split_tunneling_feature,
            splitTunnelEnabled,
            shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp),
            onSelect = {
                viewModel?.onSplitTunnelSettingChanged()
            },
            explainer = FeatureExplainer.SPLIT_TUNNELING
        )
        Spacer(modifier = Modifier.height(1.dp))
        CustomDropDown(R.string.mode, modes, selectedKey, description = modeDescription) {
            viewModel?.onModeSelected(it)
        }
    }
}

@Composable
private fun AppsTitle() {
    Text(
        text = stringResource(R.string.apps),
        color = MaterialTheme.colorScheme.primaryTextColor.copy(alpha = 0.50f),
        style = font16.copy(fontWeight = FontWeight.SemiBold),
        textAlign = TextAlign.Start,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun Apps(viewModel: SplitTunnelViewModel? = null) {
    val apps by viewModel?.filteredApps?.collectAsState()
        ?: remember { mutableStateOf(emptyList<InstalledAppsData>()) }
    LazyColumn {
        itemsIndexed(apps) { index, app ->
            val shape = if (index == apps.lastIndex) {
                RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp)
            } else {
                RoundedCornerShape(0.dp)
            }
            Spacer(modifier = Modifier.height(1.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primaryTextColor.copy(alpha = 0.05f),
                        shape = shape
                    )
                    .padding(start = 14.dp)
                    .clickable { viewModel?.onAppSelected(app) },
                verticalAlignment = Alignment.CenterVertically
            ) {
                val bitmap = app.appIconDrawable.toBitmap()
                Image(
                    painter = BitmapPainter(bitmap.asImageBitmap()),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = app.appName,
                    color = MaterialTheme.colorScheme.primaryTextColor,
                    style = font16.copy(fontWeight = FontWeight.Medium),
                    textAlign = TextAlign.Start,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Checkbox(
                    app.isChecked, onCheckedChange = {
                        viewModel?.onAppSelected(app)
                    }, colors = CheckboxDefaults.colors(
                        checkmarkColor = MaterialTheme.colorScheme.preferencesBackgroundColor,
                        checkedColor = MaterialTheme.colorScheme.primaryTextColor,
                        uncheckedColor = MaterialTheme.colorScheme.preferencesSubtitleColor
                    ),
                )
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

@SuppressLint("UseCompatLoadingForDrawables")
@Composable
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun SplitTunnelScreenApps() {
    val apps = listOf<InstalledAppsData>().toMutableList()
    val chrome = InstalledAppsData(
        "Chrome",
        "com.google.chrome",
        LocalContext.current.getDrawable(R.drawable.it_small)
    )
    chrome.isChecked = true
    apps.add(chrome)
    val discord = InstalledAppsData(
        "Discord",
        "com.discord",
        LocalContext.current.getDrawable(R.drawable.ca_small)
    )
    apps.add(discord)
    val telegram = InstalledAppsData(
        "Telegram",
        "org.telegram.messenger",
        LocalContext.current.getDrawable(R.drawable.jp_small)
    )
    apps.add(telegram)
    val viewmodel = object : SplitTunnelViewModel() {
        override val showProgress: StateFlow<Boolean> = MutableStateFlow(false)
        override val modes: List<DropDownStringItem> = listOf()
        override val selectedModeKey: StateFlow<String> = MutableStateFlow("")
        override val filteredApps: StateFlow<List<InstalledAppsData>> = MutableStateFlow(apps)
        override val isSplitTunnelEnabled: StateFlow<Boolean> = MutableStateFlow(true)
        override val searchKeyword: StateFlow<String> = MutableStateFlow("")
    }
    PreviewWithNav {
        SplitTunnelScreen(viewmodel)
    }
}

