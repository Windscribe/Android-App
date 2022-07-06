/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.mobile.splittunneling;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;

import com.windscribe.mobile.R;
import com.windscribe.mobile.adapter.InstalledAppsAdapter;
import com.windscribe.vpn.ActivityInteractor;
import com.windscribe.vpn.Windscribe;
import com.windscribe.vpn.api.response.InstalledAppsData;
import com.windscribe.vpn.commonutils.SortByName;
import com.windscribe.vpn.commonutils.SortBySelected;
import com.windscribe.vpn.constants.PreferencesKeyConstants;
import com.windscribe.vpn.state.VPNConnectionStateManager;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import io.reactivex.Single;
import io.reactivex.SingleSource;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Function;
import io.reactivex.observers.DisposableSingleObserver;
import io.reactivex.schedulers.Schedulers;

public class SplitTunnelingPresenterImpl
        implements SplitTunnelingPresenter, InstalledAppsAdapter.InstalledAppListener {

    InstalledAppsAdapter mInstalledAppsAdapter;

    private final String TAG = "split_settings_p";

    private final List<InstalledAppsData> mInstalledAppList = new ArrayList<>();

    private final Logger mPresenterLog = LoggerFactory.getLogger(TAG);

    private ActivityInteractor mSplitTunnelInteractor;

    private SplitTunnelingView mSplitTunnelView;


    @Inject
    public SplitTunnelingPresenterImpl(SplitTunnelingView mSplitTunnelView,
            ActivityInteractor activityInteractor) {
        this.mSplitTunnelView = mSplitTunnelView;
        this.mSplitTunnelInteractor = activityInteractor;
    }

    @Override
    public void onDestroy() {
        //Dispose any composite disposable
        if (!mSplitTunnelInteractor.getCompositeDisposable().isDisposed()) {
            mPresenterLog.info("Disposing observer...");
            mSplitTunnelInteractor.getCompositeDisposable().dispose();
        }
        mSplitTunnelView = null;
        mSplitTunnelInteractor = null;
    }

    @Override
    public void onBackPressed() {
        boolean isReconnectRequired = mSplitTunnelInteractor.getAppPreferenceInterface().requiredReconnect();
        if (isReconnectRequired && mSplitTunnelInteractor.getVpnConnectionStateManager().isVPNActive()) {
            mPresenterLog
                    .info("Split routing settings were changes and connection state is connected. Reconnecting to apply settings..");
            mSplitTunnelInteractor.getAppPreferenceInterface().setReconnectRequired(false);
            mSplitTunnelView.restartConnection();
        }
    }

    @Override
    public void onFilter(String query) {
        if (mInstalledAppsAdapter != null) {
            mInstalledAppsAdapter.filter(query);
        }
    }

    @Override
    public void onInstalledAppClick(InstalledAppsData updatedApp, boolean reloadAdapter) {
        mSplitTunnelInteractor.getCompositeDisposable()
                .add(mSplitTunnelInteractor.getAppPreferenceInterface().getInstalledApps()
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribeOn(Schedulers.io())
                        .subscribeWith(new DisposableSingleObserver<List<String>>() {
                            @Override
                            public void onError(@NotNull Throwable ignore) {
                                List<String> list = new ArrayList<>();
                                saveApps(list, updatedApp, reloadAdapter);
                            }

                            @Override
                            public void onSuccess(@NotNull List<String> installedAppsData) {
                                saveApps(installedAppsData, updatedApp, reloadAdapter);
                            }
                        }));

    }

    @Override
    public void onNewRoutingModeSelected(String mode) {
        mSplitTunnelInteractor.getAppPreferenceInterface().setReconnectRequired(true);
        String savedMode = mSplitTunnelInteractor.getAppPreferenceInterface().getSplitRoutingMode();
        if (!savedMode.equals(mode)) {
            mSplitTunnelInteractor.getAppPreferenceInterface().saveSplitRoutingMode(mode);
            if (mode.equals(PreferencesKeyConstants.EXCLUSIVE_MODE)) {
                mSplitTunnelView.setSplitModeTextView(mode, R.string.feature_tunnel_mode_exclusive);
            } else {
                PackageManager pm = Windscribe.getAppContext().getPackageManager();
                String packageName = Windscribe.getAppContext().getPackageName();
                try {
                    ApplicationInfo applicationInfo = pm
                            .getApplicationInfo(packageName, PackageManager.GET_META_DATA);
                    InstalledAppsData mData = new InstalledAppsData(
                            pm.getApplicationLabel(applicationInfo).toString(),
                            applicationInfo.packageName, pm.getApplicationIcon(applicationInfo));
                    mData.setChecked(true);
                    onInstalledAppClick(mData, true);
                } catch (PackageManager.NameNotFoundException e) {
                    e.printStackTrace();
                }
                mSplitTunnelView.setSplitModeTextView(mode, R.string.feature_tunnel_mode_inclusive);
            }

        }
    }

    @Override
    public void onToggleButtonClicked() {
        mSplitTunnelInteractor.getAppPreferenceInterface().setReconnectRequired(true);
        if (mSplitTunnelInteractor.getAppPreferenceInterface().getSplitTunnelToggle()) {
            mPresenterLog.info("Previous Split Tunnel Toggle Settings: True");
            mSplitTunnelInteractor.getAppPreferenceInterface().setSplitTunnelToggle(false);
            mSplitTunnelView.setupToggleImage(R.drawable.ic_toggle_button_off);
            mSplitTunnelView.hideTunnelSettingsLayout();
        } else {
            mPresenterLog.info("Previous Split Tunnel Toggle Settings: False");
            mSplitTunnelInteractor.getAppPreferenceInterface().setSplitTunnelToggle(true);
            mSplitTunnelView.setupToggleImage(R.drawable.ic_toggle_button_on);
            mSplitTunnelView.showTunnelSettingsLayout();
        }
    }

    @Override
    public void setTheme(Context context) {
        String savedThem = mSplitTunnelInteractor.getAppPreferenceInterface().getSelectedTheme();
        mPresenterLog.debug("Setting theme to " + savedThem);
        if (savedThem.equals(PreferencesKeyConstants.DARK_THEME)) {
            context.setTheme(R.style.DarkTheme);
        } else {
            context.setTheme(R.style.LightTheme);
        }
    }

    @Override
    public void setupLayoutBasedOnPreviousSettings() {
        if (mSplitTunnelInteractor.getAppPreferenceInterface().getSplitTunnelToggle()) {
            //Toggle is on so show the mode and selected app settings
            mSplitTunnelView.showTunnelSettingsLayout();
            //Toggle Button ON
            mSplitTunnelView.setupToggleImage(R.drawable.ic_toggle_button_on);
        } else {
            //Hide the settings and List
            mSplitTunnelView.hideTunnelSettingsLayout();
            //Toggle Button OFF
            mSplitTunnelView.setupToggleImage(R.drawable.ic_toggle_button_off);
        }

        //Setup application list adapter
        setupAppListAdapter();
        setupSplitRoutingMode();

    }

    private void modifyList(List<String> savedApps) {
        final PackageManager pm = Windscribe.getAppContext().getPackageManager();
        mSplitTunnelInteractor.getCompositeDisposable()
                .add(Single.fromCallable(() -> pm.getInstalledApplications(PackageManager.GET_META_DATA))
                        .flatMap(
                                (Function<List<ApplicationInfo>, SingleSource<List<InstalledAppsData>>>) packages -> {
                                    for (ApplicationInfo applicationInfo : packages) {
                                        InstalledAppsData mData = new InstalledAppsData(
                                                pm.getApplicationLabel(applicationInfo).toString(),
                                                applicationInfo.packageName, pm.getApplicationIcon(applicationInfo));

                                        for (String installedAppsData : savedApps) {
                                            if (mData.getPackageName().equals(installedAppsData)) {
                                                mData.setChecked(true);
                                            }
                                        }
                                        mInstalledAppList.add(mData);

                                    }
                                    Collections.sort(mInstalledAppList, new SortByName());
                                    Collections.sort(mInstalledAppList, new SortBySelected());
                                    return Single.fromCallable(() -> mInstalledAppList);
                                }).cache()
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribeOn(Schedulers.io())
                        .subscribeWith(new DisposableSingleObserver<List<InstalledAppsData>>() {
                            @Override
                            public void onError(@NotNull Throwable ignored) {
                                mSplitTunnelView.showProgress(false);
                            }

                            @Override
                            public void onSuccess(@NotNull List<InstalledAppsData> packages) {
                                mSplitTunnelView.showProgress(false);
                                mInstalledAppsAdapter = new InstalledAppsAdapter(mInstalledAppList,
                                        SplitTunnelingPresenterImpl.this);
                                mSplitTunnelView.setRecyclerViewAdapter(mInstalledAppsAdapter);
                            }
                        }));

    }

    @SuppressLint("NotifyDataSetChanged")
    private void saveApps(List<String> savedList, InstalledAppsData updatedApp, boolean reloadAdapter) {
        mSplitTunnelInteractor.getAppPreferenceInterface().setReconnectRequired(true);
        if (updatedApp.isChecked()) {
            savedList.add(updatedApp.getPackageName());
        } else {
            savedList.remove(updatedApp.getPackageName());
            mSplitTunnelInteractor.getAppPreferenceInterface().saveInstalledApps(savedList);
        }
        mSplitTunnelInteractor.getAppPreferenceInterface().saveInstalledApps(savedList);
        if (reloadAdapter) {
            mInstalledAppsAdapter.notifyDataSetChanged();
        }

    }

    private void setupAppListAdapter() {
        mSplitTunnelView.showProgress(true);
        mSplitTunnelInteractor.getCompositeDisposable()
                .add(mSplitTunnelInteractor.getAppPreferenceInterface().getInstalledApps()
                        .cache()
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribeOn(Schedulers.io())
                        .subscribeWith(new DisposableSingleObserver<List<String>>() {
                            @Override
                            public void onError(@NotNull Throwable ignored) {
                                mSplitTunnelView.showProgress(false);
                                modifyList(Collections.emptyList());
                            }

                            @Override
                            public void onSuccess(@NotNull List<String> installedAppsData) {
                                modifyList(installedAppsData);
                            }
                        }));

    }

    private void setupSplitRoutingMode() {
        String mode = mSplitTunnelInteractor.getAppPreferenceInterface().getSplitRoutingMode();
        mSplitTunnelView.setSplitRoutingModeAdapter(mSplitTunnelView.getSplitRoutingModes(), mode);
        if (mode.equals(PreferencesKeyConstants.EXCLUSIVE_MODE)) {
            mSplitTunnelView.setSplitModeTextView(mode, R.string.feature_tunnel_mode_exclusive);
        } else {
            mSplitTunnelView.setSplitModeTextView(mode, R.string.feature_tunnel_mode_inclusive);
        }
    }

}
