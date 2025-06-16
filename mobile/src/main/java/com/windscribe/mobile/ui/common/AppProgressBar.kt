package com.windscribe.mobile.ui.common

import android.R.id.message
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.windscribe.mobile.ui.theme.AppColors
import com.windscribe.mobile.ui.theme.font16
import com.windscribe.mobile.ui.theme.primaryTextColor

@Composable
fun AppProgressBar(showProgressBar: Boolean, message: String) {
    if (showProgressBar) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .background(AppColors.white.copy(alpha = 0.05f))
                .fillMaxSize()
        ) {
            Spacer(modifier = Modifier.height(48.dp))
            Text(
                text = message, style = font16, color = AppColors.white.copy(alpha = 0.50f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            CircularProgressIndicator(
                modifier = Modifier.size(48.dp), color = AppColors.white
            )
        }
    }
}

@Composable
fun PreferenceProgressBar(showProgressBar: Boolean) {
    if (showProgressBar) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .background(MaterialTheme.colorScheme.primaryTextColor.copy(alpha = 0.05f))
                .fillMaxSize()
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.primaryTextColor
            )
        }
    }
}
