package com.windscribe.mobile.ui.common

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.windscribe.mobile.R
import com.windscribe.mobile.ui.serverlist.ServerListItem
import com.windscribe.vpn.constants.NetworkKeyConstants
import com.windscribe.vpn.serverlist.entity.City

fun averageHealth(item: ServerListItem): Int {
    var averageHealth = 0
    var numberOfCities = 0
    for (city in item.cities) {
        if (city.health > 0) {
            numberOfCities++
            averageHealth += city.health
        }
    }
    if (averageHealth > 0 && numberOfCities > 0) {
        averageHealth /= numberOfCities
    }
    return averageHealth
}

fun healthColor(health: Int): Int {
    return if (health < 60) {
        R.color.colorNeonGreen
    } else if (health < 89) {
        R.color.colorYellow
    } else {
        R.color.colorRed
    }
}

fun getLatencyBar(time: Int): Int {
    return when {
        time == -1 -> R.drawable.ic_bar_no
        time < NetworkKeyConstants.PING_TEST_3_BAR_UPPER_LIMIT -> R.drawable.ic_bar_high
        time < NetworkKeyConstants.PING_TEST_2_BAR_UPPER_LIMIT -> R.drawable.ic_bar_medium
        time < NetworkKeyConstants.PING_TEST_1_BAR_UPPER_LIMIT -> R.drawable.ic_bar_low
        else -> R.drawable.ic_bar_no
    }
}

fun City.isEnabled(isUserPro: Boolean): Boolean {
    return (nodesAvailable() || (!isUserPro && pro == 1))
}

fun Context.openUrl(path: String) {
    val url = if (path.startsWith("https://") || path.startsWith("http://")) {
        path
    } else {
        NetworkKeyConstants.getWebsiteLink(path)
    }
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
    if (intent.resolveActivity(packageManager) != null) {
        startActivity(intent)
    } else {
        Toast.makeText(this, "No browser found", Toast.LENGTH_SHORT).show()
    }
}