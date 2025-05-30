package com.windscribe.mobile.ui.common

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.windscribe.mobile.R

@Composable
fun TenGIcon() {
    Image(
        painter = painterResource(R.drawable.ic_ten_gb),
        contentDescription = "Ten 10 GBPS location.",
        modifier = Modifier.size(16.dp),
    )
}