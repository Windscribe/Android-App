package com.windscribe.mobile.ui.common

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.windscribe.mobile.R
import com.windscribe.mobile.ui.serverlist.DataCenterSplitBorderCircle
import com.windscribe.mobile.ui.theme.expandedServerItemTextColor
import com.windscribe.vpn.serverlist.entity.Datacenter
import com.windscribe.vpn.serverlist.entity.DatacenterStatus
import com.windscribe.vpn.serverlist.entity.DatacenterStatusHelper

@Composable
fun DataCenterIcon(city: Datacenter, health: Int, showLocationLoad: Boolean = false, serverCount: Int) {
    val status = DatacenterStatusHelper.getStatus(city, serverCount)

    when (status) {
        DatacenterStatus.UnderMaintenance -> {
            // Under maintenance or disabled
            Box(modifier = Modifier.size(24.dp), contentAlignment = Alignment.Center) {
                Image(
                    painter = painterResource(R.drawable.ic_under_construction),
                    contentDescription = "Under Maintenance",
                    modifier = Modifier.size(15.dp),
                    colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.expandedServerItemTextColor)
                )
            }
        }

        DatacenterStatus.Pro -> {
            // show pro badge
            DataCenterSplitBorderCircle(
                health = health,
                flagRes = R.drawable.pro,
                showLocationLoad = showLocationLoad,
                modifier = Modifier
            )
        }

        DatacenterStatus.Available -> {
            // Available - show 10G icon or regular datacenter icon
            if (city.linkSpeed == 10000) {
                DataCenterSplitBorderCircle(
                    health = health,
                    flagRes = R.drawable.city_ten_gbps,
                    showLocationLoad = showLocationLoad,
                    modifier = Modifier.width(15.dp).height(13.dp)
                )
            } else {
                DataCenterSplitBorderCircle(
                    health = health,
                    flagRes = R.drawable.ic_dc,
                    showLocationLoad = showLocationLoad,
                    modifier = Modifier.width(10.dp).height(14.dp)
                )
            }
        }
    }
}