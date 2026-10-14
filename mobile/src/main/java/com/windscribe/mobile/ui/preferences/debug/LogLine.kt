package com.windscribe.mobile.ui.preferences.debug

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.windscribe.mobile.ui.helper.MultiDevicePreview
import com.windscribe.mobile.ui.theme.AndroidTheme
import com.windscribe.mobile.ui.theme.logDebugColor
import com.windscribe.mobile.ui.theme.logErrorColor
import com.windscribe.mobile.ui.theme.logTimeColor
import com.windscribe.mobile.ui.theme.logWarnColor
import com.windscribe.mobile.ui.theme.mono11
import com.windscribe.mobile.ui.theme.mono12
import com.windscribe.mobile.ui.theme.preferencesBackgroundColor
import com.windscribe.mobile.ui.theme.primaryTextColor
import com.windscribe.vpn.commonutils.LogRecord

private const val DEBUG = "debug"
private const val WARN = "warn"
private const val ERROR = "error"

@Composable
fun LogDayHeader(date: String) {
    Text(
        date,
        style = mono11,
        color = MaterialTheme.colorScheme.logTimeColor,
        modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 4.dp),
    )
}

@Composable
fun LogLine(record: LogRecord) {
    if (record.raw != null) {
        Text(
            record.raw ?: "",
            style = mono12,
            color = MaterialTheme.colorScheme.logDebugColor,
            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        )
        return
    }
    Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min).padding(vertical = 3.dp)) {
        Accent(record.level)
        Column {
            Text(header(record), style = mono11, color = MaterialTheme.colorScheme.logTimeColor)
            Text(record.message, style = mono12, color = levelColor(record.level))
        }
    }
}

@Composable
internal fun levelColor(level: String): Color =
    when (level) {
        ERROR -> MaterialTheme.colorScheme.logErrorColor
        WARN -> MaterialTheme.colorScheme.logWarnColor
        DEBUG -> MaterialTheme.colorScheme.logDebugColor
        else -> MaterialTheme.colorScheme.primaryTextColor
    }

@Composable
private fun Accent(level: String) {
    val color =
        when (level) {
            ERROR -> MaterialTheme.colorScheme.logErrorColor
            WARN -> MaterialTheme.colorScheme.logWarnColor
            else -> Color.Transparent
        }
    Spacer(
        modifier =
            Modifier
                .padding(end = 6.dp)
                .width(2.dp)
                .fillMaxHeight()
                .background(color, RoundedCornerShape(1.dp)),
    )
}

private fun header(record: LogRecord): String =
    buildString {
        append(record.time)
        append("  ")
        append(record.module)
        if (record.thread.isNotEmpty()) {
            append(" · ${record.thread}")
        }
        record.connectionId?.let { append("  #$it") }
    }

private class LogRecordProvider : PreviewParameterProvider<List<LogRecord>> {
    override val values =
        sequenceOf(
            listOf(
                LogRecord(
                    "12:54:25.272",
                    "2026-09-21",
                    "debug",
                    "migration",
                    "worker-14",
                    null,
                    "Replaced notifications cache with 6 items from API",
                ),
                LogRecord(
                    "12:54:25.387",
                    "2026-09-21",
                    "info",
                    "server_list_repository",
                    "worker-16",
                    null,
                    "Full update completed successfully",
                ),
                LogRecord("12:54:26.608", "2026-09-21", "warn", "wsnet-wrapper", "Thread-23", "e52bf6d5", "Curl request failed, retrying"),
                LogRecord(
                    "12:54:27.192",
                    "2026-09-21",
                    "error",
                    "vpn",
                    "main",
                    "e52bf6d5",
                    "Connect failed\njava.lang.IllegalStateException: boom",
                ),
                LogRecord(raw = "00[DMN] Starting IKE charon daemon (strongSwan 5.9.14)"),
            ),
        )
}

@Composable
@MultiDevicePreview
private fun LogLinePreview(
    @PreviewParameter(LogRecordProvider::class) records: List<LogRecord>,
) {
    AndroidTheme {
        Column(
            modifier =
                Modifier
                    .background(MaterialTheme.colorScheme.preferencesBackgroundColor)
                    .padding(16.dp),
        ) {
            LogDayHeader(records.first().date)
            records.forEach { LogLine(it) }
        }
    }
}
