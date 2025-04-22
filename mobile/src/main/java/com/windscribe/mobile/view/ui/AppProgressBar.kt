package com.windscribe.mobile.view.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.windscribe.mobile.view.theme.AppColors
import com.windscribe.mobile.view.theme.Dimen
import com.windscribe.mobile.view.theme.font16

@Composable
fun AppProgressBar(showProgressBar: Boolean, message: String) {
    if (showProgressBar) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .background(AppColors.white.copy(alpha = 0.02f))
                .fillMaxSize()
        ) {
            Spacer(modifier = Modifier.height(48.dp))
            Text(
                text = message, style = font16, color = AppColors.white50
            )
            Spacer(modifier = Modifier.height(Dimen.dp16))
            CircularProgressIndicator(
                modifier = Modifier.size(Dimen.dp48), color = AppColors.white
            )
        }
    }
}
