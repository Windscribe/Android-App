package com.windscribe.mobile.ui.common

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.windscribe.mobile.ui.theme.AppColors
import com.windscribe.mobile.ui.theme.font16
import com.windscribe.mobile.ui.theme.serverListSecondaryColor

@Composable
fun AddButton(
    @StringRes buttonTitle: Int,
    secondaryTitle: String,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.background(color = MaterialTheme.colorScheme.serverListSecondaryColor.copy(0.05f))
    ) {
        Text(
            secondaryTitle,
            style = font16,
            color = MaterialTheme.colorScheme.serverListSecondaryColor,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        if (secondaryTitle.isNotEmpty()) {
            Spacer(modifier = Modifier.weight(1f))
            Text(
                stringResource(buttonTitle),
                style = font16,
                color = AppColors.cyberBlue.copy(0.7f),
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 13.dp)
                    .clickable {
                        onClick()
                    })
        }
    }
}

@Composable
fun AddButton(
    @StringRes buttonTitle: Int,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center,
        modifier = Modifier.background(color = MaterialTheme.colorScheme.serverListSecondaryColor.copy(0.05f)).fillMaxWidth()
    ) {
        Text(
            stringResource(buttonTitle),
            style = font16,
            color = AppColors.cyberBlue.copy(0.7f),
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 13.dp)
                .clickable {
                    onClick()
                })
    }
}