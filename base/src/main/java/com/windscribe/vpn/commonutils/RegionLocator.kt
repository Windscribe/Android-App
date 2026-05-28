package com.windscribe.vpn.commonutils

import android.content.Context.TELEPHONY_SERVICE
import android.icu.util.TimeZone
import android.telephony.TelephonyManager
import android.telephony.TelephonyManager.PHONE_TYPE_CDMA
import com.windscribe.vpn.Windscribe.Companion.appContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory

object RegionLocator {
    val logger: Logger = LoggerFactory.getLogger("util")

    @Suppress("DEPRECATION")
    fun matchesCountryCode(code: String): Boolean {
        val manager = appContext.getSystemService(TELEPHONY_SERVICE) as TelephonyManager
        if (manager.phoneType != PHONE_TYPE_CDMA && manager.networkCountryIso.isNotEmpty()) {
            logger.debug("Location country code: ${manager.networkCountryIso}")
            return manager.networkCountryIso == code
        }
        val zones = TimeZone.getAvailableIDs(code.uppercase())
        val currentTimeZone = TimeZone.getDefault().id
        logger.debug("TimeZones: ${zones.contentToString()} | Default: $currentTimeZone")
        return zones.contains(currentTimeZone)
    }

    fun isCountry(code: String): Boolean {
        val countryCode =
            appContext.resources.configuration.locales
                .get(0)
                .language
        return countryCode == code
    }
}
