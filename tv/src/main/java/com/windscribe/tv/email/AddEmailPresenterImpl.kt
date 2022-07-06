/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.tv.email

import android.text.TextUtils
import android.util.Patterns
import com.windscribe.tv.R
import com.windscribe.vpn.ActivityInteractor
import com.windscribe.vpn.constants.NetworkKeyConstants
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import org.slf4j.LoggerFactory
import javax.inject.Inject

class AddEmailPresenterImpl @Inject constructor(
    private var addEmailView: AddEmailView,
    private var interactor: ActivityInteractor
) : AddEmailPresenter {
    private val logger = LoggerFactory.getLogger("add_email_p")
    override fun onDestroy() {
        if (!interactor.getCompositeDisposable().isDisposed) {
            logger.info("Disposing network observer...")
            interactor.getCompositeDisposable().dispose()
        }
    }

    override fun onAddEmailClicked(emailAddress: String) {
        logger.info("Validating input email address...")
        if (TextUtils.isEmpty(emailAddress)) {
            logger.info("Email input empty...")
            addEmailView.showInputError(interactor.getResourceString(R.string.email_empty))
            return
        }
        if (Patterns.EMAIL_ADDRESS.matcher(emailAddress).matches()) {
            // Post email address
            addEmailView.hideSoftKeyboard()
            addEmailView.prepareUiForApiCallStart()
            logger.info("Posting users email address...")
            val emailMap: MutableMap<String, String> = HashMap()
            emailMap[NetworkKeyConstants.ADD_EMAIL_KEY] = emailAddress
            emailMap[NetworkKeyConstants.ADD_EMAIL_FORCED_KEY] = 1.toString()
            interactor.getCompositeDisposable().add(
                interactor.getApiCallManager()
                    .addUserEmailAddress(emailMap)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({
                        addEmailView.prepareUiForApiCallFinished()
                        it.dataClass?.let { data ->
                            addEmailView.showToast("Added email successfully...")
                            logger.info("Email address added successfully...")
                            addEmailView.decideActivity()
                        } ?: it.errorClass?.let { error ->
                            addEmailView.prepareUiForApiCallFinished()
                            addEmailView.showToast(error.errorMessage)
                            logger.debug("Server returned error. $error")
                            addEmailView.showInputError(error.errorMessage)
                        }
                    }, {
                        addEmailView.prepareUiForApiCallFinished()
                        logger.debug("Error adding email address..." + it.localizedMessage)
                        addEmailView.showToast("Sorry! We were unable to add your email address...")
                    })
            )
        } else {
            addEmailView.showInputError(
                interactor.getResourceString(R.string.invalid_email_format)
            )
        }
    }

    override fun onResendEmail(emailAddress: String) {
        logger.info("Validating input email address...")
        if (TextUtils.isEmpty(emailAddress)) {
            logger.info("Email input empty...")
            addEmailView.showInputError(interactor.getResourceString(R.string.email_empty))
            return
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(emailAddress).matches()) {
            addEmailView.showInputError(interactor.getResourceString(R.string.invalid_email_format))
            return
        }
        addEmailView.hideSoftKeyboard()
        addEmailView.prepareUiForApiCallStart()
        logger.info("Email confirmation resend users email address...")
        interactor.getCompositeDisposable().add(
            interactor.getApiCallManager()
                .resendUserEmailAddress()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ data ->
                    addEmailView.prepareUiForApiCallFinished()
                    data.dataClass?.let {
                        addEmailView.showToast("Email confirmation sent successfully...")
                        logger.info("Email confirmation sent successfully...")
                        addEmailView.decideActivity()
                    } ?: data?.errorClass?.let {
                        addEmailView.prepareUiForApiCallFinished()
                        addEmailView.showToast(it.errorMessage)
                        logger.debug("Server returned error. $it")
                        addEmailView.showInputError(it.errorMessage)
                    }
                }, { error ->
                    addEmailView.prepareUiForApiCallFinished()
                    logger.debug("Error resending email address..." + error.localizedMessage)
                    addEmailView.showToast("Sorry! We were unable to resend your email address...")
                })
        )
    }

    override fun onSkipEmailClicked() {
        addEmailView.decideActivityForSkipButton()
    }
}
