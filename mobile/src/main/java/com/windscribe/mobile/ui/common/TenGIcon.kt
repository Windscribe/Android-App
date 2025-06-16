package com.windscribe.mobile.ui.common

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.windscribe.mobile.R
import com.windscribe.mobile.ui.theme.expandedServerItemTextColor

@Composable
fun TenGIcon() {
    Image(
        painter = painterResource(R.drawable.ic_ten_gb),
        contentDescription = "Ten 10 GBPS location.",
        modifier = Modifier.size(16.dp),
        colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.expandedServerItemTextColor)
    )
}