/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.tv.rate

import com.windscribe.vpn.apppreference.PreferencesHelper
import com.windscribe.vpn.constants.RateDialogConstants
import com.windscribe.vpn.services.FirebaseManager
import kotlinx.coroutines.CoroutineScope
import java.util.Date
import javax.inject.Inject

class RateMyAppPresenterImp @Inject constructor(
    private val preferencesHelper: PreferencesHelper,
    private val firebaseManager: FirebaseManager
) : RateMyAppPresenter {
    private lateinit var rateView: RateView
    private lateinit var activityScope: CoroutineScope

    override fun bind(view: RateView, scope: CoroutineScope) {
        this.rateView = view
        this.activityScope = scope
    }

    override fun onAskMeLaterClick() {
        preferencesHelper.rateDialogLastUpdateTime = Date().time.toString()
        preferencesHelper.rateDialogStatus = RateDialogConstants.STATUS_ASK_LATER
        rateView.onGoWindScribeActivity()
    }

    override fun onNeverAskClick() {
        preferencesHelper.rateDialogLastUpdateTime = Date().time.toString()
        preferencesHelper.rateDialogStatus = RateDialogConstants.STATUS_NEVER_ASK
        rateView.onGoWindScribeActivity()
    }

    override fun onRateNowClick() {
        preferencesHelper.rateDialogLastUpdateTime = Date().time.toString()
        preferencesHelper.rateDialogStatus = RateDialogConstants.STATUS_ALREADY_ASKED
        if (firebaseManager.isPlayStoreInstalled) {
            rateView.openPlayStoreWithLink()
        } else {
            rateView.showToast("No Play store installed.")
            rateView.onGoWindScribeActivity()
        }
    }
}
