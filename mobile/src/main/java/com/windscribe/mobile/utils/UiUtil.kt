/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.mobile.utils

import com.windscribe.mobile.R
import com.windscribe.vpn.Windscribe.Companion.appContext
import com.windscribe.vpn.constants.BillingConstants
import com.windscribe.vpn.constants.UserStatusConstants

object UiUtil {
    fun getDataRemainingColor(dataRemaining: Float, maxData: Long): Int {
        return if (maxData != -1L) when {
            dataRemaining < BillingConstants.DATA_LOW_PERCENTAGE * (
                    maxData /
                            UserStatusConstants.GB_DATA.toFloat()
                    ) -> appContext.resources.getColor(R.color.colorRed)
            dataRemaining
                    < BillingConstants.DATA_WARNING_PERCENTAGE * (maxData / UserStatusConstants.GB_DATA.toFloat()) ->
                appContext.resources.getColor(R.color.colorYellow)
            else ->
                appContext.resources.getColor(R.color.colorWhite)
        } else appContext.resources.getColor(R.color.colorWhite)
    }
}