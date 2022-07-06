/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.mobile.confirmemail;

import com.windscribe.mobile.R;
import com.windscribe.vpn.ActivityInteractor;
import com.windscribe.vpn.api.CreateHashMap;
import com.windscribe.vpn.api.response.AddEmailResponse;
import com.windscribe.vpn.api.response.ApiErrorResponse;
import com.windscribe.vpn.api.response.GenericResponseClass;
import com.windscribe.vpn.constants.UserStatusConstants;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

import javax.inject.Inject;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.observers.DisposableSingleObserver;
import io.reactivex.schedulers.Schedulers;

public class ConfirmEmailPresenterImp implements ConfirmEmailPresenter {

    private ActivityInteractor confirmEmailInteractor;

    private ConfirmEmailView confirmEmailView;

    private final Logger mPresenterLog = LoggerFactory.getLogger("[confirm-email-i]");

    @Inject
    public ConfirmEmailPresenterImp(ConfirmEmailView confirmEmailView,
            ActivityInteractor confirmEmailInteractor) {
        this.confirmEmailInteractor = confirmEmailInteractor;
        this.confirmEmailView = confirmEmailView;
    }

    @Override
    public void onDestroy() {
        if (!confirmEmailInteractor.getCompositeDisposable().isDisposed()) {
            confirmEmailInteractor.getCompositeDisposable().dispose();
        }
        confirmEmailInteractor = null;
        confirmEmailView = null;
    }

    @Override
    public void init() {
        boolean proUser = confirmEmailInteractor.getAppPreferenceInterface().getUserStatus()
                == UserStatusConstants.USER_STATUS_PREMIUM;
        String reasonForConfirmEmail = confirmEmailInteractor
                .getResourceString(proUser ? R.string.pro_reason_to_confirm : R.string.free_reason_to_confirm);
        confirmEmailView.setReasonToConfirmEmail(reasonForConfirmEmail);
    }

    @Override
    public void resendVerificationEmail() {
        confirmEmailView.showEmailConfirmProgress(true);
        confirmEmailInteractor.getCompositeDisposable().add(confirmEmailInteractor.getApiCallManager()
                .resendUserEmailAddress(null)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(
                        new DisposableSingleObserver<GenericResponseClass<AddEmailResponse, ApiErrorResponse>>() {
                            @Override
                            public void onError(@NotNull Throwable e) {
                                confirmEmailView.showToast("Error sending email..");
                                confirmEmailView.showEmailConfirmProgress(false);
                            }

                            @Override
                            public void onSuccess(
                                    @NotNull GenericResponseClass<AddEmailResponse, ApiErrorResponse> postEmailResponseClass) {
                                confirmEmailView.showEmailConfirmProgress(false);
                                if (postEmailResponseClass.getDataClass() != null) {
                                    confirmEmailView.showToast("Email confirmation sent successfully...");
                                    mPresenterLog.info("Email confirmation sent successfully...");
                                    confirmEmailView.finishActivity();
                                } else {
                                    confirmEmailView
                                            .showToast(postEmailResponseClass.getErrorClass().getErrorMessage());
                                    mPresenterLog
                                            .debug("Server returned error. " + postEmailResponseClass.getErrorClass()
                                                    .toString());
                                }
                            }
                        })

        );
    }
}
