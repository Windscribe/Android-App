package com.windscribe.mobile.ui.preferences.debug

import PreferencesNavBar
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import com.windscribe.mobile.ui.theme.logTimeColor
import com.windscribe.mobile.ui.theme.mono12
import com.windscribe.vpn.R
import com.windscribe.vpn.Windscribe.Companion.appContext
import com.windscribe.vpn.apppreference.PreferencesKeyConstants
import com.windscribe.vpn.commonutils.LogLevel
import com.windscribe.vpn.commonutils.LogRecord
import java.io.File

@Composable
fun DebugScreen(viewModel: DebugViewModel = hiltViewModel<DebugViewModelImpl>()) {
    val debugLog by viewModel.debugLog.collectAsState()
    val showProgress by viewModel.showProgress.collectAsState()
    val query by viewModel.query.collectAsState()
    val minLevel by viewModel.minLevel.collectAsState()
    val levelsAvailable by viewModel.levelsAvailable.collectAsState()
    DebugContent(
        debugLog = debugLog,
        showProgress = showProgress,
        query = query,
        minLevel = minLevel,
        levelsAvailable = levelsAvailable,
        onQueryChanged = viewModel::onQueryChanged,
        onMinLevelChanged = viewModel::onMinLevelChanged,
    )
}

@Composable
fun DebugContent(
    debugLog: List<LogRecord>,
    showProgress: Boolean,
    query: String = "",
    minLevel: LogLevel = LogLevel.DEBUG,
    levelsAvailable: Boolean = true,
    onQueryChanged: (String) -> Unit = {},
    onMinLevelChanged: (LogLevel) -> Unit = {},
) {
    val navController = LocalNavController.current
    val context = LocalContext.current

    PreferenceBackground {
        Column(modifier = Modifier.padding(vertical = 16.dp, horizontal = 16.dp).navigationBarsPadding().imePadding()) {
            PreferencesNavBar(stringResource(R.string.view_log)) {
                navController.popBackStack()
            }
            Spacer(modifier = Modifier.height(16.dp))
            LogFilterBar(
                query = query,
                minLevel = minLevel,
                levelsAvailable = levelsAvailable,
                onQueryChanged = onQueryChanged,
                onMinLevelChanged = onMinLevelChanged,
            )
            Spacer(modifier = Modifier.height(12.dp))
            val rows = remember(debugLog) { toRows(debugLog) }
            val listState = rememberLazyListState()
            LaunchedEffect(rows.size, query, minLevel) {
                if (rows.isNotEmpty()) {
                    listState.scrollToItem(rows.size - 1)
                }
            }
            if (rows.isEmpty() && !showProgress) {
                Text(
                    stringResource(R.string.load_nothing_found),
                    style = mono12,
                    color = MaterialTheme.colorScheme.logTimeColor,
                    modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
                    textAlign = TextAlign.Center,
                )
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
                items(rows.size, contentType = { rows[it]::class }) {
                    when (val row = rows[it]) {
                        is LogRow.Day -> LogDayHeader(row.date)
                        is LogRow.Entry -> LogLine(row.record)
                    }
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

private sealed interface LogRow {
    data class Day(
        val date: String,
    ) : LogRow

    data class Entry(
        val record: LogRecord,
    ) : LogRow
}

private fun toRows(records: List<LogRecord>): List<LogRow> {
    val rows = ArrayList<LogRow>(records.size + 1)
    var day = ""
    for (record in records) {
        if (record.date.isNotEmpty() && record.date != day) {
            day = record.date
            rows.add(LogRow.Day(day))
        }
        rows.add(LogRow.Entry(record))
    }
    return rows
}

private class DebugStateProvider : PreviewParameterProvider<Pair<List<LogRecord>, Boolean>> {
    override val values =
        sequenceOf(
            emptyList<LogRecord>() to true,
            listOf(
                LogRecord("23:59:58.004", "2026-09-20", "info", "vpn", "main", null, "Connected"),
                LogRecord("00:00:01.512", "2026-09-21", "debug", "ping", "worker-3", null, "Latency refresh started"),
                LogRecord("00:00:02.880", "2026-09-21", "warn", "wsnet-wrapper", "Thread-23", "e52bf6d5", "Curl request failed, retrying"),
            ) to false,
        )
}

@Composable
@MultiDevicePreview
private fun DebugContentPreview(
    @PreviewParameter(DebugStateProvider::class) state: Pair<List<LogRecord>, Boolean>,
) {
    PreviewWithNav {
        DebugContent(
            debugLog = state.first,
            showProgress = state.second,
        )
    }
}
