/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.tv.confirmemail

import com.windscribe.tv.R
import com.windscribe.tv.di.PerActivity
import com.windscribe.vpn.ActivityInteractor
import com.windscribe.vpn.constants.UserStatusConstants
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import org.slf4j.LoggerFactory
import javax.inject.Inject

@PerActivity
class ConfirmEmailPresenterImp @Inject constructor(
    private var confirmEmailView: ConfirmEmailView,
    private var interactor: ActivityInteractor
) : ConfirmEmailPresenter {
    private val logger = LoggerFactory.getLogger("basic")
    override fun onDestroy() {
        if (!interactor.getCompositeDisposable().isDisposed) {
            interactor.getCompositeDisposable().dispose()
        }
    }

    override fun init() {
        val proUser = (
            interactor.getAppPreferenceInterface().userStatus
                == UserStatusConstants.USER_STATUS_PREMIUM
            )
        val reasonForConfirmEmail = interactor
            .getResourceString(if (proUser) com.windscribe.vpn.R.string.pro_reason_to_confirm else com.windscribe.vpn.R.string.free_reason_to_confirm)
        confirmEmailView.setReasonToConfirmEmail(reasonForConfirmEmail)
    }

    override val isUserPro: Boolean
        get() = (
            interactor.getAppPreferenceInterface().userStatus
                == UserStatusConstants.USER_STATUS_PREMIUM
            )

    override fun resendVerificationEmail() {
        confirmEmailView.showEmailConfirmProgress(true)
        interactor.getCompositeDisposable().add(
            interactor.getApiCallManager()
                .resendUserEmailAddress()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    confirmEmailView.showEmailConfirmProgress(false)
                    it.dataClass?.let {
                        confirmEmailView.showToast("Email confirmation sent successfully...")
                        logger.info("Email confirmation sent successfully...")
                        confirmEmailView.finishActivity()
                    } ?: it.errorClass?.let { error ->
                        confirmEmailView.showToast(error.errorMessage)
                        logger.debug("Server returned error. $error")
                    }
                }, {
                    confirmEmailView.showToast("Error sending email..")
                    confirmEmailView.showEmailConfirmProgress(false)
                })
        )
    }
}
