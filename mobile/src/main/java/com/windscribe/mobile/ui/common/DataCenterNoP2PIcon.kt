package com.windscribe.mobile.ui.common

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.windscribe.mobile.ui.theme.serverItemTextColor

@Composable
fun DataCenterNoP2PIcon() {
    Image(
        painter = painterResource(com.windscribe.mobile.R.drawable.p2p),
        contentDescription = "No 2 P2P Indiactor",
        modifier = Modifier.size(12.dp),
        colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.serverItemTextColor),
    )
}
