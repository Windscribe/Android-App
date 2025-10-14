/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.tv.email

import android.text.TextUtils
import android.util.Patterns
import com.windscribe.vpn.api.IApiCallManager
import com.windscribe.vpn.commonutils.Ext.result
import com.windscribe.vpn.commonutils.ResourceHelper
import com.windscribe.vpn.repository.CallResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import javax.inject.Inject

class AddEmailPresenterImpl @Inject constructor(
    private var addEmailView: AddEmailView,
    private var activityScope: CoroutineScope,
    private var apiCallManager: IApiCallManager,
    private var resourceHelper: ResourceHelper
) : AddEmailPresenter {
    private val logger = LoggerFactory.getLogger("basic")
    override fun onDestroy() {
        // Coroutine scope will be cancelled by the activity
    }

    override fun onAddEmailClicked(emailAddress: String) {
        logger.info("Validating input email address...")
        if (TextUtils.isEmpty(emailAddress)) {
            logger.info("Email input empty...")
            addEmailView.showInputError(resourceHelper.getString(com.windscribe.vpn.R.string.email_empty))
            return
        }
        if (Patterns.EMAIL_ADDRESS.matcher(emailAddress).matches()) {
            // Post email address
            addEmailView.hideSoftKeyboard()
            addEmailView.prepareUiForApiCallStart()
            logger.info("Posting users email address...")

            activityScope.launch(Dispatchers.IO) {
                try {
                    val result = result<String> {
                        apiCallManager.addUserEmailAddress(emailAddress)
                    }

                    withContext(Dispatchers.Main) {
                        addEmailView.prepareUiForApiCallFinished()
                        when (result) {
                            is CallResult.Success -> {
                                addEmailView.showToast(resourceHelper.getString(com.windscribe.vpn.R.string.added_email_successfully))
                                logger.info("Email address added successfully...")
                                addEmailView.decideActivity()
                            }
                            is CallResult.Error -> {
                                addEmailView.showToast(result.errorMessage)
                                logger.debug("Server returned error. ${result.errorMessage}")
                                addEmailView.showInputError(result.errorMessage)
                            }
                        }
                    }
                } catch (e: Throwable) {
                    withContext(Dispatchers.Main) {
                        addEmailView.prepareUiForApiCallFinished()
                        logger.debug("Error adding email address: ${e.message}")
                        addEmailView.showToast("Sorry! We were unable to add your email address...")
                    }
                }
            }
        } else {
            addEmailView.showInputError(
                resourceHelper.getString(com.windscribe.vpn.R.string.invalid_email_format)
            )
        }
    }

    override fun onResendEmail(emailAddress: String) {
        logger.info("Validating input email address...")
        if (TextUtils.isEmpty(emailAddress)) {
            logger.info("Email input empty...")
            addEmailView.showInputError(resourceHelper.getString(com.windscribe.vpn.R.string.email_empty))
            return
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(emailAddress).matches()) {
            addEmailView.showInputError(resourceHelper.getString(com.windscribe.vpn.R.string.invalid_email_format))
            return
        }
        addEmailView.hideSoftKeyboard()
        addEmailView.prepareUiForApiCallStart()
        logger.info("Email confirmation resend users email address...")

        activityScope.launch(Dispatchers.IO) {
            try {
                val result = result<String> {
                    apiCallManager.resendUserEmailAddress()
                }

                withContext(Dispatchers.Main) {
                    addEmailView.prepareUiForApiCallFinished()
                    when (result) {
                        is CallResult.Success -> {
                            addEmailView.showToast(resourceHelper.getString(com.windscribe.vpn.R.string.email_confirmation_sent_successfully))
                            logger.info("Email confirmation sent successfully...")
                            addEmailView.decideActivity()
                        }
                        is CallResult.Error -> {
                            addEmailView.showToast(result.errorMessage)
                            logger.debug("Server returned error. ${result.errorMessage}")
                            addEmailView.showInputError(result.errorMessage)
                        }
                    }
                }
            } catch (e: Throwable) {
                withContext(Dispatchers.Main) {
                    addEmailView.prepareUiForApiCallFinished()
                    logger.debug("Error resending email address: ${e.message}")
                    addEmailView.showToast(resourceHelper.getString(com.windscribe.vpn.R.string.error_sending_email))
                }
            }
        }
    }

    override fun onSkipEmailClicked() {
        addEmailView.decideActivityForSkipButton()
    }
}
