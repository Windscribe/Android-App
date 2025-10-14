/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.tv.rate

import com.windscribe.vpn.apppreference.PreferencesHelper
import com.windscribe.vpn.constants.RateDialogConstants
import com.windscribe.vpn.services.FirebaseManager
import java.util.Date
import javax.inject.Inject

class RateMyAppPresenterImp @Inject constructor(
    private val rateView: RateView,
    private val preferencesHelper: PreferencesHelper,
    private val firebaseManager: FirebaseManager
) : RateMyAppPresenter {
    override fun onAskMeLaterClick() {
        preferencesHelper.saveResponseStringData(
            RateDialogConstants.LAST_UPDATE_TIME,
            Date().time.toString()
        )
        preferencesHelper.saveResponseIntegerData(RateDialogConstants.CURRENT_STATUS_KEY, RateDialogConstants.STATUS_ASK_LATER)
        rateView.onGoWindScribeActivity()
    }

    override fun onNeverAskClick() {
        preferencesHelper.saveResponseStringData(
            RateDialogConstants.LAST_UPDATE_TIME,
            Date().time.toString()
        )
        preferencesHelper.saveResponseIntegerData(RateDialogConstants.CURRENT_STATUS_KEY, RateDialogConstants.STATUS_NEVER_ASK)
        rateView.onGoWindScribeActivity()
    }

    override fun onRateNowClick() {
        preferencesHelper.saveResponseStringData(
            RateDialogConstants.LAST_UPDATE_TIME,
            Date().time.toString()
        )
        preferencesHelper.saveResponseIntegerData(RateDialogConstants.CURRENT_STATUS_KEY, RateDialogConstants.STATUS_ALREADY_ASKED)
        if (firebaseManager.isPlayStoreInstalled) {
            rateView.openPlayStoreWithLink()
        } else {
            rateView.showToast("No Play store installed.")
            rateView.onGoWindScribeActivity()
        }
    }
}
