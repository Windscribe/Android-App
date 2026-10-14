package com.windscribe.mobile.ui.preferences.debug

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.windscribe.mobile.ui.helper.hapticClickableRipple
import com.windscribe.mobile.ui.theme.font14
import com.windscribe.mobile.ui.theme.logTimeColor
import com.windscribe.mobile.ui.theme.mono11
import com.windscribe.mobile.ui.theme.primaryTextColor
import com.windscribe.vpn.commonutils.LogLevel

@Composable
fun LogFilterBar(
    query: String,
    minLevel: LogLevel,
    levelsAvailable: Boolean,
    onQueryChanged: (String) -> Unit,
    onMinLevelChanged: (LogLevel) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Search(query, onQueryChanged)
        if (levelsAvailable) {
            LevelRow(minLevel, onMinLevelChanged)
        }
    }
}

@Composable
private fun Search(
    query: String,
    onQueryChanged: (String) -> Unit,
) {
    val tint = MaterialTheme.colorScheme.primaryTextColor
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(48.dp)
                .background(tint.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                .padding(horizontal = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxSize()) {
            Image(
                painter = painterResource(com.windscribe.mobile.R.drawable.ic_location_search),
                contentDescription = stringResource(com.windscribe.vpn.R.string.search),
                colorFilter = ColorFilter.tint(tint.copy(alpha = 0.70f)),
            )
            TextField(
                value = query,
                onValueChange = onQueryChanged,
                singleLine = true,
                placeholder = {
                    Text(
                        stringResource(com.windscribe.vpn.R.string.search),
                        style = font14.copy(textAlign = TextAlign.Start),
                        color = MaterialTheme.colorScheme.logTimeColor,
                    )
                },
                modifier = Modifier.testTag("debug_log_search").weight(1f).fillMaxHeight(),
                textStyle = font14.copy(textAlign = TextAlign.Start, color = tint),
                colors =
                    TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent,
                        cursorColor = tint,
                        selectionColors = TextSelectionColors(tint, tint.copy(alpha = 0.3f)),
                    ),
                keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Search),
            )
        }
    }
}

private fun LogLevel.chipLabel(): String = token.uppercase() + if (this == LogLevel.entries.last()) "" else "+"

@Composable
private fun LevelRow(
    minLevel: LogLevel,
    onMinLevelChanged: (LogLevel) -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
    ) {
        LogLevel.entries.forEach { level ->
            LevelChip(level, selected = level == minLevel) { onMinLevelChanged(level) }
        }
    }
}

@Composable
private fun LevelChip(
    level: LogLevel,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val colour = levelColor(level.token)
    val dim = MaterialTheme.colorScheme.logTimeColor
    val shape = RoundedCornerShape(8.dp)
    Box(
        modifier =
            Modifier
                .clip(shape)
                .background(
                    if (selected) colour.copy(alpha = 0.22f) else MaterialTheme.colorScheme.primaryTextColor.copy(alpha = 0.03f),
                ).border(1.dp, if (selected) colour.copy(alpha = 0.55f) else Color.Transparent, shape)
                .hapticClickableRipple(onClick = onClick)
                .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Text(
            level.chipLabel(),
            style = mono11,
            color = if (selected) colour else dim.copy(alpha = 0.55f),
        )
    }
}
