package com.windscribe.mobile.view.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.windscribe.mobile.view.theme.AppColors
import com.windscribe.mobile.view.theme.font16

@Composable
fun AddButton(
    @StringRes buttonTitle: Int,
    secondaryTitle: String,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.background(color = AppColors.white5)
    ) {
        Text(
            secondaryTitle,
            style = font16,
            color = AppColors.white,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        if (secondaryTitle.isNotEmpty()) {
            Spacer(modifier = Modifier.weight(1f))
            Text(
                stringResource(buttonTitle),
                style = font16,
                color = AppColors.neonGreen,
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
        modifier = Modifier.background(color = AppColors.white5).fillMaxWidth()
    ) {
        Text(
            stringResource(buttonTitle),
            style = font16,
            color = AppColors.neonGreen,
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 13.dp)
                .clickable {
                    onClick()
                })
    }
}