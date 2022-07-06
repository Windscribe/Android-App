/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.mobile.email;

import android.text.TextUtils;
import android.util.Patterns;

import com.windscribe.mobile.R;
import com.windscribe.vpn.ActivityInteractor;
import com.windscribe.vpn.api.CreateHashMap;
import com.windscribe.vpn.api.response.AddEmailResponse;
import com.windscribe.vpn.api.response.ApiErrorResponse;
import com.windscribe.vpn.api.response.GenericResponseClass;
import com.windscribe.vpn.constants.NetworkKeyConstants;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.observers.DisposableSingleObserver;
import io.reactivex.schedulers.Schedulers;


public class AddEmailPresenterImpl implements AddEmailPresenter {

    private final String TAG = "[add_email_p]";

    private ActivityInteractor mAddEmailInteractor;

    private AddEmailView mAddEmailView;

    private final Logger mPresenterLog = LoggerFactory.getLogger(TAG);

    @Inject
    public AddEmailPresenterImpl(AddEmailView mAddEmailView, ActivityInteractor activityInteractor) {
        this.mAddEmailView = mAddEmailView;
        this.mAddEmailInteractor = activityInteractor;
    }

    @Override
    public void onDestroy() {
        mAddEmailInteractor.getCompositeDisposable();
        if (!mAddEmailInteractor.getCompositeDisposable().isDisposed()) {
            mPresenterLog.info("Disposing network observer...");
            mAddEmailInteractor.getCompositeDisposable().dispose();
        }
        mAddEmailView = null;
        mAddEmailInteractor = null;
    }

    @Override
    public void onAddEmailClicked(String emailAddress) {
        mPresenterLog.info("Validating input email address...");
        if (TextUtils.isEmpty(emailAddress)) {
            mPresenterLog.info("Email input empty...");
            mAddEmailView.showInputError(mAddEmailInteractor.getResourceString(R.string.email_empty));
            return;
        }

        if (Patterns.EMAIL_ADDRESS.matcher(emailAddress).matches()) {
            //Post email address
            mAddEmailView.hideSoftKeyboard();
            mAddEmailView.prepareUiForApiCallStart();
            mPresenterLog.info("Posting users email address...");
            final Map<String, String> emailMap = new HashMap<>();
            emailMap.put(NetworkKeyConstants.ADD_EMAIL_KEY, emailAddress);
            emailMap.put(NetworkKeyConstants.ADD_EMAIL_FORCED_KEY, String.valueOf(1));
            mAddEmailInteractor.getCompositeDisposable().add(
                    mAddEmailInteractor.getApiCallManager()
                            .addUserEmailAddress(emailMap)
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribeWith(
                                    new DisposableSingleObserver<GenericResponseClass<AddEmailResponse, ApiErrorResponse>>() {
                                        @Override
                                        public void onError(@NotNull Throwable e) {
                                            mPresenterLog
                                                    .debug("Error adding email address..." + e.getLocalizedMessage());
                                            mAddEmailView
                                                    .showToast("Sorry! We were unable to add your email address...");
                                            mAddEmailView.prepareUiForApiCallFinished();
                                        }

                                        @Override
                                        public void onSuccess(
                                                @NotNull GenericResponseClass<AddEmailResponse, ApiErrorResponse> postEmailResponseClass) {
                                            if (postEmailResponseClass.getDataClass() != null) {
                                                mAddEmailView.showToast("Added email successfully...");
                                                mPresenterLog.info("Email address added successfully...");
                                                mAddEmailView.gotoWindscribeActivity();
                                            } else {
                                                mAddEmailView.prepareUiForApiCallFinished();
                                                mAddEmailView.showToast(
                                                        postEmailResponseClass.getErrorClass().getErrorMessage());
                                                mPresenterLog.debug("Server returned error. " + postEmailResponseClass
                                                        .getErrorClass().toString());
                                                mAddEmailView.showInputError(
                                                        postEmailResponseClass.getErrorClass().getErrorMessage());
                                            }
                                        }
                                    }));


        } else {
            mAddEmailView.showInputError(mAddEmailInteractor.getResourceString(R.string.invalid_email_format));
        }
    }

    @Override
    public void setUpLayout() {
        mAddEmailView.setUpLayout(mAddEmailInteractor.getResourceString(R.string.add_email));
    }
}
