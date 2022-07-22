/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.mobile.splash;


import static com.windscribe.vpn.Windscribe.appContext;
import static com.windscribe.vpn.constants.BillingConstants.GP_PACKAGE_NAME;
import static com.windscribe.vpn.constants.BillingConstants.GP_PRODUCT_ID;
import static com.windscribe.vpn.constants.BillingConstants.PURCHASED_ITEM;
import static com.windscribe.vpn.constants.BillingConstants.PURCHASE_TOKEN;

import com.google.gson.Gson;
import com.windscribe.vpn.ActivityInteractor;
import com.windscribe.vpn.api.response.ApiErrorResponse;
import com.windscribe.vpn.api.response.GenericResponseClass;
import com.windscribe.vpn.api.response.ItemPurchased;
import com.windscribe.vpn.constants.PreferencesKeyConstants;
import com.windscribe.vpn.constants.UserStatusConstants;
import com.windscribe.vpn.errormodel.WindError;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import io.reactivex.Completable;
import io.reactivex.Single;
import io.reactivex.SingleSource;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Function;
import io.reactivex.observers.DisposableCompletableObserver;
import io.reactivex.observers.DisposableSingleObserver;
import io.reactivex.schedulers.Schedulers;


public class SplashPresenterImpl implements SplashPresenter {

    private static final String TAG = "splash_p";

    private ActivityInteractor mInteractor;

    private final Logger mPresenterLog = LoggerFactory.getLogger(TAG);

    private SplashView mView;

    @Inject
    public SplashPresenterImpl(SplashView mView, ActivityInteractor activityInteractor) {
        this.mView = mView;
        this.mInteractor = activityInteractor;
    }


    /* Stop Session service if running
     * Check purchase token if available
     * Get User session.
     * Update User status
     * Check if server List need update
     * Update server config, server credentials op and ikEv2
     * Request , Parse  and save list
     * Check if ping test can be done
     * Ping every node and Save to database
     * Save best location id based on lowest ping.
     *
     * */

    @Override
    public void onDestroy() {
        if (mInteractor.getCompositeDisposable() != null) {
            if (!mInteractor.getCompositeDisposable().isDisposed()) {
                mPresenterLog.info("Disposing network observer...");
                mInteractor.getCompositeDisposable().dispose();
            }
        }
        mPresenterLog.info("Setting view and interactor to null...");
        mView = null;
        mInteractor = null;
    }

    public void checkApplicationInstanceAndDecideActivity() {

        if (mInteractor.getAppPreferenceInterface().isNewApplicationInstance()) {
            mInteractor.getAppPreferenceInterface().setNewApplicationInstance(false);
            String installation = mInteractor.getAppPreferenceInterface()
                    .getResponseString(PreferencesKeyConstants.NEW_INSTALLATION);
            if (PreferencesKeyConstants.I_NEW.equals(installation)) {
                //Record new install
                mPresenterLog.info("Recording new installation of the app");
                mInteractor.getAppPreferenceInterface()
                        .saveResponseStringData(PreferencesKeyConstants.NEW_INSTALLATION,
                                PreferencesKeyConstants.I_OLD);
                mInteractor.getCompositeDisposable().add(
                        mInteractor.getApiCallManager()
                                .recordAppInstall(null)
                                .subscribeOn(Schedulers.io())
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribeWith(
                                        new DisposableSingleObserver<GenericResponseClass<String, ApiErrorResponse>>() {
                                            @Override
                                            public void onError(@NotNull Throwable e) {
                                                mPresenterLog.debug("Error: " + e.getMessage());
                                                decideActivity();
                                            }

                                            @Override
                                            public void onSuccess(
                                                    @NotNull GenericResponseClass<String, ApiErrorResponse> recordInstallResponse) {
                                                if (recordInstallResponse.getDataClass() != null) {
                                                    mPresenterLog.info("Recording app install success. "
                                                            + recordInstallResponse.getDataClass());
                                                } else if (recordInstallResponse.getErrorClass() != null) {
                                                    mPresenterLog.debug("Recording app install failed. "
                                                            + recordInstallResponse.getErrorClass().toString());
                                                }
                                                decideActivity();
                                            }
                                        }));
            } else {
                //Not a new install, decide activity
                decideActivity();
            }
        } else {
            //Decide which activity to goto
            decideActivity();
        }
    }

    @Override
    public void checkNewMigration() {
        migrateSessionAuthIfRequired();
        boolean userLoggedIn = mInteractor.getAppPreferenceInterface().getSessionHash() != null;
        if (userLoggedIn) {
            mInteractor.getCompositeDisposable().add(mInteractor.serverDataAvailable()
                    .subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
                    .subscribeWith(new DisposableSingleObserver<Boolean>() {
                        @Override
                        public void onError(@NotNull Throwable ignored) {
                            checkApplicationInstanceAndDecideActivity();
                        }

                        @Override
                        public void onSuccess(@NotNull Boolean serverListAvailable) {
                            if (serverListAvailable) {
                                mPresenterLog.info("Migration not required.");
                                checkApplicationInstanceAndDecideActivity();
                            } else {
                                mPresenterLog.info("Migration required. updating server list.");
                                updateDataFromApiAndOldStorage();
                            }
                        }
                    }));
        } else {
            checkApplicationInstanceAndDecideActivity();
        }

    }

    public void decideActivity() {
        mPresenterLog.info("Checking if user already logged in...");
        final String sessionHash = mInteractor.getAppPreferenceInterface().getSessionHash();
        if (sessionHash != null) {
            mPresenterLog.info("Session auth hash present. User is already logged in...");
            if (!mView.isConnectedToNetwork()) {
                mPresenterLog.info("NO ACTIVE NETWORK FOUND! Starting home activity with stale data.");
            }
            if (shouldShowAccountSetUp()) {
                mView.navigateToAccountSetUp();
            } else {
                mView.navigateToHome();
            }
        } else {
            //Goto Login/Registration Activity
            mPresenterLog.info("Session auth hash not present. User not logged in...");
            mView.navigateToLogin();
        }
    }

    // Move SessionAuth to secure preferences
    private void migrateSessionAuthIfRequired() {
        String oldSessionAuth = mInteractor.getAppPreferenceInterface().getOldSessionAuth();
        String newSessionAuth = mInteractor.getAppPreferenceInterface().getSessionHash();
        if (oldSessionAuth != null && newSessionAuth == null) {
            mPresenterLog.debug("Migrating session auth to secure preferences");
            mInteractor.getAppPreferenceInterface().setSessionHash(oldSessionAuth);
            mInteractor.getAppPreferenceInterface().clearOldSessionAuth();
        }
    }

    private boolean shouldShowAccountSetUp() {
        boolean ghostAccount = mInteractor.getAppPreferenceInterface().userIsInGhostMode();
        boolean proUser = mInteractor.getAppPreferenceInterface().getUserStatus()
                == UserStatusConstants.USER_STATUS_PREMIUM;
        return ghostAccount && proUser;
    }

    private void updateDataFromApiAndOldStorage() {
        mInteractor.getCompositeDisposable().add(mInteractor.getServerListUpdater().update()
                .doOnError(throwable -> mPresenterLog.info("Failed to download server list."))
                .andThen(mInteractor.getStaticListUpdater().update())
                .doOnError(throwable -> mPresenterLog.info("Failed to download static server list."))
                .andThen(mInteractor.updateUserData())
                .andThen(Completable.fromAction(mInteractor.getPreferenceChangeObserver()::postCityServerChange))
                .onErrorResumeNext(throwable -> {
                    mPresenterLog.info("*********Preparing dashboard failed: " + throwable.toString()
                            + " Use reload button in server list in home activity.*******");
                    return mInteractor.updateUserData().andThen(
                            (Completable.fromAction(mInteractor.getPreferenceChangeObserver()::postCityServerChange)));
                })
                .subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(new DisposableCompletableObserver() {
                    @Override
                    public void onComplete() {
                        checkApplicationInstanceAndDecideActivity();
                    }

                    @Override
                    public void onError(@NotNull Throwable ignored) {
                        checkApplicationInstanceAndDecideActivity();
                    }
                }));
    }

}
