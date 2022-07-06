/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.mobile.help;

import android.content.Context;

import com.windscribe.mobile.R;
import com.windscribe.vpn.ActivityInteractor;
import com.windscribe.vpn.api.CreateHashMap;
import com.windscribe.vpn.api.response.ApiErrorResponse;
import com.windscribe.vpn.api.response.GenericResponseClass;
import com.windscribe.vpn.api.response.GenericSuccess;
import com.windscribe.vpn.constants.NetworkKeyConstants;
import com.windscribe.vpn.constants.PreferencesKeyConstants;
import com.windscribe.vpn.constants.UserStatusConstants;
import com.windscribe.vpn.errormodel.WindError;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import io.reactivex.Single;
import io.reactivex.SingleSource;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Function;
import io.reactivex.observers.DisposableSingleObserver;
import io.reactivex.schedulers.Schedulers;

public class HelpPresenterImpl implements HelpPresenter {

    private final String TAG = "help_p";

    private final ActivityInteractor helpInteractor;

    private final HelpView helpView;

    private final Logger mPresenterLog = LoggerFactory.getLogger(TAG);

    @Inject
    public HelpPresenterImpl(HelpView helpView, ActivityInteractor activityInteractor) {
        this.helpView = helpView;
        this.helpInteractor = activityInteractor;
    }

    @Override
    public void init() {
        helpView.setActivityTitle(helpInteractor.getResourceString(R.string.help_me));
    }

    @Override
    public void onDiscordClick() {
        helpView.openInBrowser(NetworkKeyConstants.URL_DISCORD);
    }

    @Override
    public void onGarryClick() {
        helpView.openInBrowser(NetworkKeyConstants.getWebsiteLink(NetworkKeyConstants.URL_GARRY));
    }

    @Override
    public void onKnowledgeBaseClick() {
        helpView.openInBrowser(NetworkKeyConstants.getWebsiteLink(NetworkKeyConstants.URL_KNOWLEDGE));
    }

    @Override
    public void onRedditClick() {
        helpView.openInBrowser(NetworkKeyConstants.URL_REDDIT);
    }

    @Override
    public void onSendDebugClicked() {
        boolean userInGhostMode = helpInteractor.getAppPreferenceInterface().getUserName().equals("na");
        if (userInGhostMode) {
            helpView.showToast("Log in send logs.");
            return;
        }
        helpView.showProgress(true, false);
        mPresenterLog.info("Preparing debug file...");
        final Map<String, String> logMap = new HashMap<>();
        logMap.put(UserStatusConstants.CURRENT_USER_NAME, helpInteractor.getAppPreferenceInterface()
                .getUserName());
        helpInteractor.getCompositeDisposable().add(
                Single.fromCallable(helpInteractor::getEncodedLog)
                        .flatMap(
                                (Function<String, SingleSource<GenericResponseClass<GenericSuccess, ApiErrorResponse>>>) encodedLog -> {
                                    mPresenterLog.info("Reading log file successful, submitting app log...");
                                    //Add log file and user name
                                    logMap.put(NetworkKeyConstants.POST_LOG_FILE_KEY, encodedLog);
                                    return helpInteractor.getApiCallManager()
                                            .postDebugLog(logMap);
                                }).subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribeWith(
                                new DisposableSingleObserver<GenericResponseClass<GenericSuccess, ApiErrorResponse>>() {
                                    @Override
                                    public void onError(@NotNull Throwable e) {
                                        helpView.showProgress(false, false);
                                        if(e instanceof Exception){
                                            mPresenterLog.debug("Error Submitting Log: "
                                                    + WindError.getInstance().rxErrorToString((Exception) e));
                                        }
                                    }

                                    @Override
                                    public void onSuccess(
                                            @NotNull GenericResponseClass<GenericSuccess, ApiErrorResponse> appLogSubmissionResponse) {
                                        helpView.showProgress(false, appLogSubmissionResponse.getDataClass() != null
                                                && appLogSubmissionResponse.getDataClass().isSuccessful());
                                    }
                                }));
    }

    @Override
    public void onSendTicketClick() {
        helpView.goToSendTicket();
    }

    @Override
    public void setTheme(Context context) {
        String savedThem = helpInteractor.getAppPreferenceInterface().getSelectedTheme();
        if (savedThem.equals(PreferencesKeyConstants.DARK_THEME)) {
            context.setTheme(R.style.DarkTheme);
        } else {
            context.setTheme(R.style.LightTheme);
        }
    }
}
