package com.windscribe.mobile.ui.common

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.windscribe.mobile.R
import com.windscribe.mobile.ui.home.UserState
import com.windscribe.mobile.ui.serverlist.SubLocationSplitBorderCircle
import com.windscribe.mobile.ui.theme.expandedServerItemTextColor
import com.windscribe.mobile.ui.theme.serverListSecondaryColor
import com.windscribe.vpn.serverlist.entity.City

@Composable
fun ServerListIcon(city: City, userState: UserState, angle: Float, color: androidx.compose.ui.graphics.Color, showLocationLoad: Boolean = false) {
    if (city.pro == 1 && (userState is UserState.Free || userState is UserState.UnlimitedData)) {
        SubLocationSplitBorderCircle(
            firstSectionAngle = angle,
            firstColor = color,
            secondColor = MaterialTheme.colorScheme.serverListSecondaryColor.copy(alpha = 0.20f),
            flagRes = R.drawable.pro,
            showLocationLoad = showLocationLoad,
            iconModifier = Modifier
        )
    } else if (!city.isEnabled(userState is UserState.Pro)) {
        Image(
            painter = painterResource(R.drawable.ic_under_construction),
            contentDescription = "Pro",
            modifier = Modifier.size(16.dp),
            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.expandedServerItemTextColor)
        )
    } else if (city.linkSpeed == "10000") {
        SubLocationSplitBorderCircle(
            firstSectionAngle = angle,
            firstColor = color,
            secondColor = MaterialTheme.colorScheme.serverListSecondaryColor.copy(alpha = 0.20f),
            flagRes = R.drawable.city_ten_gbps,
            showLocationLoad = showLocationLoad,
            iconModifier = Modifier.width(15.dp).height(13.dp),
        )
    } else {
        SubLocationSplitBorderCircle(
            firstSectionAngle = angle,
            firstColor = color,
            secondColor = MaterialTheme.colorScheme.serverListSecondaryColor.copy(alpha = 0.20f),
            flagRes = R.drawable.ic_dc,
            showLocationLoad = showLocationLoad,
            iconModifier = Modifier.width(10.dp).height(14.dp)
        )
    }
}