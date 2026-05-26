/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.tv.confirmemail

import com.windscribe.vpn.api.IApiCallManager
import com.windscribe.vpn.apppreference.PreferencesHelper
import com.windscribe.vpn.commonutils.Ext.result
import com.windscribe.vpn.commonutils.ResourceHelper
import com.windscribe.vpn.constants.UserStatusConstants
import com.windscribe.vpn.repository.CallResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import javax.inject.Inject

class ConfirmEmailPresenterImp @Inject constructor(
    private var preferencesHelper: PreferencesHelper,
    private var apiCallManager: IApiCallManager,
    private var resourceHelper: ResourceHelper
) : ConfirmEmailPresenter {
    private val logger = LoggerFactory.getLogger("basic")
    private lateinit var confirmEmailView: ConfirmEmailView
    private lateinit var activityScope: CoroutineScope

    override fun bind(view: ConfirmEmailView, scope: CoroutineScope) {
        this.confirmEmailView = view
        this.activityScope = scope
    }

    override fun onDestroy() {
        // Coroutine scope will be cancelled by the activity
    }

    override fun init() {
        val proUser = preferencesHelper.userStatus == UserStatusConstants.USER_STATUS_PREMIUM
        val reasonForConfirmEmail = resourceHelper.getString(
            if (proUser) com.windscribe.vpn.R.string.pro_reason_to_confirm
            else com.windscribe.vpn.R.string.free_reason_to_confirm
        )
        confirmEmailView.setReasonToConfirmEmail(reasonForConfirmEmail)
    }

    override val isUserPro: Boolean
        get() = preferencesHelper.userStatus == UserStatusConstants.USER_STATUS_PREMIUM

    override fun resendVerificationEmail() {
        confirmEmailView.showEmailConfirmProgress(true)

        activityScope.launch(Dispatchers.IO) {
            try {
                val result = result<String> {
                    apiCallManager.resendUserEmailAddress()
                }

                withContext(Dispatchers.Main) {
                    confirmEmailView.showEmailConfirmProgress(false)
                    when (result) {
                        is CallResult.Success -> {
                            confirmEmailView.showToast("Email confirmation sent successfully...")
                            logger.info("Email confirmation sent successfully...")
                            confirmEmailView.finishActivity()
                        }
                        is CallResult.Error -> {
                            confirmEmailView.showToast(result.errorMessage)
                            logger.debug("Server returned error. ${result.errorMessage}")
                        }
                    }
                }
            } catch (e: Throwable) {
                withContext(Dispatchers.Main) {
                    confirmEmailView.showToast("Error sending email..")
                    confirmEmailView.showEmailConfirmProgress(false)
                }
            }
        }
    }
}
