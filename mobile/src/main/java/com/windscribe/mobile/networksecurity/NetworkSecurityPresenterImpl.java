/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.mobile.networksecurity;


import android.content.Context;

import androidx.annotation.Nullable;

import com.windscribe.mobile.R;
import com.windscribe.vpn.ActivityInteractor;
import com.windscribe.vpn.constants.PreferencesKeyConstants;
import com.windscribe.vpn.errormodel.WindError;
import com.windscribe.vpn.localdatabase.tables.NetworkInfo;
import com.windscribe.vpn.state.NetworkInfoListener;
import com.windscribe.vpn.state.NetworkInfoManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.function.Predicate;

import javax.inject.Inject;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subscribers.DisposableSubscriber;

public class NetworkSecurityPresenterImpl implements NetworkSecurityPresenter, NetworkInfoListener {

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
        mNetworkInteractor.getNetworkInfoManager().removeNetworkInfoListener(this);
        //Dispose any observer
        if (!mNetworkInteractor.getCompositeDisposable().isDisposed()) {
            mNetworkPresenterLog.info("Disposing observer...");
            mNetworkInteractor.getCompositeDisposable().dispose();
        }
        mNetworkView = null;
        mNetworkInteractor = null;
    }

    @Override
    public void init() {
        mNetworkView.setupCurrentNetwork(mNetworkInteractor.getNetworkInfoManager().getNetworkInfo());
        mNetworkInteractor.getNetworkInfoManager().addNetworkInfoListener(this);
        mNetworkView.setAutoSecureToggle(mNetworkInteractor.getAppPreferenceInterface().isAutoSecureOn() ? R.drawable.ic_toggle_button_on: R.drawable.ic_toggle_button_off);
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
                        NetworkInfo activeNetworkInfo = mNetworkInteractor.getNetworkInfoManager().getNetworkInfo();
                        if(activeNetworkInfo != null){
                            networks.removeIf(networkInfo -> networkInfo.getNetworkName().equals(activeNetworkInfo.getNetworkName()));
                        }
                        mNetworkView.setAdapter(networks);
                    }
                }));


    }

    @Override
    public void onNetworkInfoUpdate(@Nullable NetworkInfo networkInfo, boolean userReload) {
        mNetworkView.setupCurrentNetwork(networkInfo);
    }

    @Override
    public void onCurrentNetworkClick() {
        NetworkInfo networkInfo = mNetworkInteractor.getNetworkInfoManager().getNetworkInfo();
        if(networkInfo!=null){
            mNetworkView.openNetworkSecurityDetails(networkInfo.getNetworkName());
        }
    }

    @Override
    public void onAutoSecureToggleClick() {
        if (mNetworkInteractor.getAppPreferenceInterface().isAutoSecureOn()){
            mNetworkInteractor.getAppPreferenceInterface().setAutoSecureOn(false);
            mNetworkView.setAutoSecureToggle(R.drawable.ic_toggle_button_off);
        }else{
            mNetworkInteractor.getAppPreferenceInterface().setAutoSecureOn(true);
            mNetworkView.setAutoSecureToggle(R.drawable.ic_toggle_button_on);
        }
    }
}
