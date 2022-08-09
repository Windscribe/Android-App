/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.mobile.email

import android.text.TextUtils
import android.util.Patterns
import com.windscribe.mobile.R
import com.windscribe.vpn.ActivityInteractor
import com.windscribe.vpn.api.response.AddEmailResponse
import com.windscribe.vpn.api.response.ApiErrorResponse
import com.windscribe.vpn.api.response.GenericResponseClass
import com.windscribe.vpn.constants.NetworkKeyConstants
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.observers.DisposableSingleObserver
import io.reactivex.schedulers.Schedulers
import org.slf4j.LoggerFactory
import javax.inject.Inject

class AddEmailPresenterImpl @Inject constructor(
    private val emailView: AddEmailView,
    private val interactor: ActivityInteractor
) : AddEmailPresenter {

    private val logger = LoggerFactory.getLogger("[add_email_p]")

    override fun onDestroy() {
        if (interactor.getCompositeDisposable().isDisposed.not()) {
            logger.info("Disposing network observer...")
            interactor.getCompositeDisposable().dispose()
        }
    }

    override fun onAddEmailClicked(emailAddress: String) {
        logger.info("Validating input email address...")
        if (TextUtils.isEmpty(emailAddress)) {
            logger.info("Email input empty...")
            emailView.showInputError(interactor.getResourceString(R.string.email_empty))
            return
        }
        if (Patterns.EMAIL_ADDRESS.matcher(emailAddress).matches()) {
            //Post email address
            emailView.hideSoftKeyboard()
            emailView.prepareUiForApiCallStart()
            logger.info("Posting users email address...")
            val emailMap: MutableMap<String, String> = HashMap()
            emailMap[NetworkKeyConstants.ADD_EMAIL_KEY] = emailAddress
            emailMap[NetworkKeyConstants.ADD_EMAIL_FORCED_KEY] = 1.toString()
            interactor.getCompositeDisposable().add(
                interactor.getApiCallManager()
                    .addUserEmailAddress(emailMap)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeWith(
                        object :
                            DisposableSingleObserver<GenericResponseClass<AddEmailResponse?, ApiErrorResponse?>>() {
                            override fun onError(e: Throwable) {
                                logger
                                    .debug("Error adding email address..." + e.localizedMessage)
                                emailView
                                    .showToast("Sorry! We were unable to add your email address...")
                                emailView.prepareUiForApiCallFinished()
                            }

                            override fun onSuccess(
                                postEmailResponseClass: GenericResponseClass<AddEmailResponse?, ApiErrorResponse?>
                            ) {
                                if (postEmailResponseClass.dataClass != null) {
                                    emailView.showToast("Added email successfully...")
                                    logger.info("Email address added successfully...")
                                    emailView.gotoWindscribeActivity()
                                } else {
                                    emailView.prepareUiForApiCallFinished()
                                    emailView.showToast(
                                        postEmailResponseClass.errorClass!!.errorMessage
                                    )
                                    logger.debug(
                                        "Server returned error. " + postEmailResponseClass
                                            .errorClass.toString()
                                    )
                                    emailView.showInputError(
                                        postEmailResponseClass.errorClass!!.errorMessage
                                    )
                                }
                            }
                        })
            )
        } else {
            emailView.showInputError(interactor.getResourceString(R.string.invalid_email_format))
        }
    }

    override fun setUpLayout() {
        emailView.setUpLayout(interactor.getResourceString(R.string.add_email))
    }
}