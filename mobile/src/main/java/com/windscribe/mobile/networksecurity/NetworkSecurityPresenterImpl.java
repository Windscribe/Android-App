/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.mobile.networksecurity;


import android.content.Context;

import com.windscribe.mobile.R;
import com.windscribe.vpn.ActivityInteractor;
import com.windscribe.vpn.constants.PreferencesKeyConstants;
import com.windscribe.vpn.errormodel.WindError;
import com.windscribe.vpn.localdatabase.tables.NetworkInfo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import javax.inject.Inject;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subscribers.DisposableSubscriber;

public class NetworkSecurityPresenterImpl implements NetworkSecurityPresenter {

    private final String TAG = "net_security_p";

    private ActivityInteractor mNetworkInteractor;

    private final Logger mNetworkPresenterLog = LoggerFactory.getLogger(TAG);

    private NetworkSecurityView mNetworkView;

    @Inject
    public NetworkSecurityPresenterImpl(NetworkSecurityView mNetworkView,
            ActivityInteractor activityInteractor) {
        this.mNetworkView = mNetworkView;
        this.mNetworkInteractor = activityInteractor;
    }

    @Override
    public void onDestroy() {
        //Dispose any observer
        if (!mNetworkInteractor.getCompositeDisposable().isDisposed()) {
            mNetworkPresenterLog.info("Disposing observer...");
            mNetworkInteractor.getCompositeDisposable().dispose();
        }
        mNetworkView = null;
        mNetworkInteractor = null;
    }

    @Override
    public String getSavedLocale() {
        String selectedLanguage = mNetworkInteractor.getAppPreferenceInterface().getSavedLanguage();
        return selectedLanguage.substring(selectedLanguage.indexOf("(") + 1, selectedLanguage.indexOf(")"));
    }

    @Override
    public void onAdapterSet() {
        mNetworkView.hideProgress();
    }

    @Override
    public void onNetworkSecuritySelected(NetworkInfo networkInfo) {
        mNetworkView.openNetworkSecurityDetails(networkInfo.getNetworkName());
    }

    @Override
    public void setTheme(Context context) {
        String savedThem = mNetworkInteractor.getAppPreferenceInterface().getSelectedTheme();
        if (savedThem.equals(PreferencesKeyConstants.DARK_THEME)) {
            context.setTheme(R.style.DarkTheme);
        } else {
            context.setTheme(R.style.LightTheme);
        }
    }

    @Override
    public void setupNetworkListAdapter() {
        mNetworkPresenterLog.info("Setting up network list adapter...");
        mNetworkView.showProgress(mNetworkInteractor.getResourceString(R.string.loading_network_list));
        mNetworkInteractor.getCompositeDisposable().add(mNetworkInteractor.getNetworkInfoUpdated()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(new DisposableSubscriber<List<NetworkInfo>>() {
                    @Override
                    public void onComplete() {
                        mNetworkView.setAdapter(null);
                    }

                    @Override
                    public void onError(Throwable e) {
                        mNetworkView.setAdapter(null);
                        mNetworkPresenterLog.debug("Error reading network list data..." +
                                WindError.getInstance().convertThrowableToString(e));
                        mNetworkView.onAdapterLoadFailed(
                                mNetworkInteractor.getResourceString(R.string.no_saved_network_list));
                        mNetworkView.hideProgress();
                    }

                    @Override
                    public void onNext(List<NetworkInfo> networks) {
                        mNetworkPresenterLog.info("Reading network list data successful...");
                        mNetworkView.setAdapter(networks);

                    }
                }));


    }
}
