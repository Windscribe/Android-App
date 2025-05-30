package com.windscribe.mobile.ui.common

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.windscribe.mobile.R
import com.windscribe.mobile.ui.serverlist.SplitBorderCircle
import com.windscribe.mobile.ui.theme.AppColors
import com.windscribe.mobile.ui.home.UserState
import com.windscribe.vpn.serverlist.entity.City

@Composable
fun ServerListIcon(city: City, userState: UserState, angle: Float, color: androidx.compose.ui.graphics.Color) {
    if (city.pro == 1 && (userState is UserState.Free || userState is UserState.UnlimitedData)) {
        Image(
            painter = painterResource(R.drawable.ic_hs_pro_badge),
            contentDescription = "Pro",
            modifier = Modifier.size(24.dp),
        )
    } else if (!city.isEnabled(userState is UserState.Pro)) {
        Image(
            painter = painterResource(R.drawable.ic_under_construction),
            contentDescription = "Pro",
            modifier = Modifier.size(16.dp),
            colorFilter = ColorFilter.tint(AppColors.white)
        )
    } else {
        SplitBorderCircle(
            firstSectionAngle = angle,
            firstColor = color,
            secondColor = AppColors.white70,
            flagRes = R.drawable.ic_dc
        )
    }
}