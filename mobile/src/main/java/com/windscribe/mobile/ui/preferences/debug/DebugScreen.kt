package com.windscribe.mobile.ui.preferences.debug

import PreferencesNavBar
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.windscribe.mobile.ui.common.PreferenceBackground
import com.windscribe.mobile.ui.common.PreferenceProgressBar
import com.windscribe.mobile.ui.helper.MultiDevicePreview
import com.windscribe.mobile.ui.helper.PreviewWithNav
import com.windscribe.mobile.ui.nav.LocalNavController
import com.windscribe.mobile.ui.theme.font12
import com.windscribe.mobile.ui.theme.primaryTextColor
import com.windscribe.vpn.R
import com.windscribe.vpn.Windscribe.Companion.appContext
import com.windscribe.vpn.apppreference.PreferencesKeyConstants
import java.io.File

@Composable
fun DebugScreen(viewModel: DebugViewModel = hiltViewModel<DebugViewModelImpl>()) {
    val debugLog by viewModel.debugLog.collectAsState()
    val showProgress by viewModel.showProgress.collectAsState()
    DebugContent(
        debugLog = debugLog,
        showProgress = showProgress,
    )
}

@Composable
fun DebugContent(
    debugLog: List<String>,
    showProgress: Boolean,
) {
    val navController = LocalNavController.current
    val context = LocalContext.current

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
            LazyColumn(
                state = listState,
                modifier =
                    Modifier.combinedClickable(
                        onClick = {},
                        onLongClick = {
                            exportLog(context)
                        },
                    ),
            ) {
                items(debugLog.size) {
                    Text(
                        debugLog[it],
                        style = font12,
                        color = MaterialTheme.colorScheme.primaryTextColor,
                        textAlign = TextAlign.Start,
                    )
                }
            }
        }
        PreferenceProgressBar(showProgress)
    }
}

private fun exportLog(context: Context) {
    val logFile = File(appContext.filesDir.path + PreferencesKeyConstants.DEBUG_LOG_FILE_NAME)
    if (logFile.exists()) {
        val fileUri: Uri =
            FileProvider.getUriForFile(
                context,
                "com.windscribe.vpn.provider",
                logFile,
            )
        val shareIntent =
            Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_STREAM, fileUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        val chooser = Intent.createChooser(shareIntent, "Export Log File")
        if (shareIntent.resolveActivity(context.packageManager) != null) {
            context.startActivity(chooser)
        }
    }
}

private class DebugStateProvider : PreviewParameterProvider<Pair<List<String>, Boolean>> {
    override val values =
        sequenceOf(
            emptyList<String>() to true,
            listOf("Log line 1", "Log line 2", "Log line 3") to false,
        )
}

@Composable
@MultiDevicePreview
private fun DebugContentPreview(
    @PreviewParameter(DebugStateProvider::class) state: Pair<List<String>, Boolean>,
) {
    PreviewWithNav {
        DebugContent(
            debugLog = state.first,
            showProgress = state.second,
        )
    }
}
