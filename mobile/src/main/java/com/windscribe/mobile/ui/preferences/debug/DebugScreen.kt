package com.windscribe.mobile.ui.preferences.debug

import PreferencesNavBar
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.windscribe.mobile.ui.common.PreferenceBackground
import com.windscribe.mobile.ui.common.PreferenceProgressBar
import com.windscribe.mobile.ui.helper.MultiDevicePreview
import com.windscribe.mobile.ui.helper.PreviewWithNav
import com.windscribe.mobile.ui.nav.LocalNavController
import com.windscribe.mobile.ui.theme.font12
import com.windscribe.mobile.ui.theme.primaryTextColor
import com.windscribe.vpn.R

@Composable
fun DebugScreen(viewModel: DebugViewModel? = null) {
    val navController = LocalNavController.current
    val debugLog by viewModel?.debugLog?.collectAsState()
        ?: remember { mutableStateOf(emptyList()) }
    val showProgress by viewModel?.showProgress?.collectAsState()
        ?: remember { mutableStateOf(false) }

    PreferenceBackground {
        Column(modifier = Modifier.padding(vertical = 16.dp, horizontal = 16.dp)) {
            PreferencesNavBar(stringResource(R.string.view_log)) {
                navController.popBackStack()
            }
            Spacer(modifier = Modifier.height(20.dp))
            val listState = rememberLazyListState()
            LaunchedEffect(debugLog.size) {
                if (debugLog.isNotEmpty()) {
                    listState.scrollToItem(debugLog.size - 1)
                }
            }
            LazyColumn(state = listState) {
                items(debugLog.size) {
                    Text(
                        debugLog[it],
                        style = font12,
                        color = MaterialTheme.colorScheme.primaryTextColor,
                        textAlign = TextAlign.Start
                    )
                }
            }
        }
        PreferenceProgressBar(showProgress)
    }
}

@Composable
@MultiDevicePreview
private fun DebugScreenPreview() {
    PreviewWithNav {
        DebugScreen()
    }
}