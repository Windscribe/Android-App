package com.windscribe.mobile.ui.common

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.text.TextPaint
import android.widget.Toast
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.sp
import com.windscribe.mobile.R
import com.windscribe.mobile.ui.serverlist.ServerListItem
import com.windscribe.vpn.constants.NetworkKeyConstants
import com.windscribe.vpn.serverlist.entity.Datacenter
import org.slf4j.LoggerFactory

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

/**
 * Safely start an activity, catching SecurityException when app is in background.
 * Use this for all activity launches from Compose UI.
 */
fun Context.safeStartActivity(intent: Intent, onError: (() -> Unit)? = null) {
    try {
        startActivity(intent)
    } catch (e: SecurityException) {
        val logger = LoggerFactory.getLogger("ui")
        logger.error("Cannot start activity from background: ${e.message}", e)
        onError?.invoke()
    } catch (e: Exception) {
        val logger = LoggerFactory.getLogger("ui")
        logger.error("Failed to start activity: ${e.message}", e)
        onError?.invoke()
    }
}

/**
 * Safely start an activity from an Activity context.
 */
fun Activity.safeStartActivity(intent: Intent, onError: (() -> Unit)? = null) {
    try {
        startActivity(intent)
    } catch (e: SecurityException) {
        val logger = LoggerFactory.getLogger("ui")
        logger.error("Cannot start activity from background: ${e.message}", e)
        onError?.invoke()
    } catch (e: Exception) {
        val logger = LoggerFactory.getLogger("ui")
        logger.error("Failed to start activity: ${e.message}", e)
        onError?.invoke()
    }
}

fun Context.openUrl(path: String) {
    val url = if (path.startsWith("https://") || path.startsWith("http://")) {
        path
    } else {
        NetworkKeyConstants.getWebsiteLink(path)
    }
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
    if (intent.resolveActivity(packageManager) != null) {
        safeStartActivity(intent) {
            Toast.makeText(this, "Cannot open browser from background", Toast.LENGTH_SHORT).show()
        }
    } else {
        Toast.makeText(this, "No browser found", Toast.LENGTH_SHORT).show()
    }
}

fun fitsInOneLine(text: String, fontSizeSp: Float, maxWidthPx: Float, density: Density): Boolean {
    val paint = TextPaint().apply {
        isAntiAlias = true
        textSize = with(density) { fontSizeSp.sp.toPx() }
    }
    val textWidth = paint.measureText(text)
    return textWidth <= maxWidthPx
}