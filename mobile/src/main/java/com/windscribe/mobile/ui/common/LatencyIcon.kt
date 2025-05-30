package com.windscribe.mobile.ui.common

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.windscribe.mobile.ui.theme.AppColors
import com.windscribe.mobile.ui.theme.font9

@Composable
fun LatencyIcon(latency: Int) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Image(
            painter = painterResource(getLatencyBar(latency)),
            contentDescription = "Ping Indicator",
            modifier = Modifier.size(12.dp),
        )
        Text(
            text = "$latency",
            style = font9,
            color = AppColors.white70
        )
    }
}