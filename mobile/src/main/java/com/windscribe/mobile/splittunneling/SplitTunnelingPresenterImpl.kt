/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.mobile.splittunneling

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import com.windscribe.mobile.R
import com.windscribe.mobile.adapter.InstalledAppsAdapter
import com.windscribe.vpn.ActivityInteractor
import com.windscribe.vpn.Windscribe.Companion.appContext
import com.windscribe.vpn.api.response.InstalledAppsData
import com.windscribe.vpn.commonutils.SortByName
import com.windscribe.vpn.commonutils.SortBySelected
import com.windscribe.vpn.constants.PreferencesKeyConstants
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.observers.DisposableSingleObserver
import io.reactivex.schedulers.Schedulers
import org.slf4j.LoggerFactory
import java.util.*
import javax.inject.Inject

class SplitTunnelingPresenterImpl @Inject constructor(
    private val splitTunnelView: SplitTunnelingView,
    private val interactor: ActivityInteractor
) : SplitTunnelingPresenter, InstalledAppsAdapter.InstalledAppListener {
    var installedAppsAdapter: InstalledAppsAdapter? = null
    private val mInstalledAppList: MutableList<InstalledAppsData> = ArrayList()
    private val logger = LoggerFactory.getLogger("split_settings_p")
    override fun onDestroy() {
        //Dispose any composite disposable
        if (!interactor.getCompositeDisposable().isDisposed) {
            logger.info("Disposing observer...")
            interactor.getCompositeDisposable().dispose()
        }
    }

    override fun onBackPressed() {
        val isReconnectRequired =
            interactor.getAppPreferenceInterface().requiredReconnect()
        if (isReconnectRequired && interactor.getVpnConnectionStateManager()
                .isVPNActive()
        ) {
            logger
                .info("Split routing settings were changes and connection state is connected. Reconnecting to apply settings..")
            interactor.getAppPreferenceInterface().setReconnectRequired(false)
            splitTunnelView.restartConnection()
        }
    }

    override fun onFilter(query: String) {
        installedAppsAdapter?.filter(query)
    }

    override fun onInstalledAppClick(updatedApp: InstalledAppsData, reloadAdapter: Boolean) {
        interactor.getCompositeDisposable()
            .add(
                interactor.getAppPreferenceInterface().installedApps
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeOn(Schedulers.io())
                    .subscribeWith(object : DisposableSingleObserver<List<String>>() {
                        override fun onError(ignore: Throwable) {
                            val list: MutableList<String> = ArrayList()
                            saveApps(list, updatedApp, reloadAdapter)
                        }

                        override fun onSuccess(installedAppsData: List<String>) {
                            saveApps(installedAppsData.toMutableList(), updatedApp, reloadAdapter)
                        }
                    })
            )
    }

    override fun onNewRoutingModeSelected(mode: String) {
        interactor.getAppPreferenceInterface().setReconnectRequired(true)
        val savedMode = interactor.getAppPreferenceInterface().splitRoutingMode
        if (savedMode != mode) {
            interactor.getAppPreferenceInterface().saveSplitRoutingMode(mode)
            if (mode == PreferencesKeyConstants.EXCLUSIVE_MODE) {
                splitTunnelView.setSplitModeTextView(
                    mode,
                    R.string.feature_tunnel_mode_exclusive
                )
            } else {
                val pm = appContext.packageManager
                val packageName = appContext.packageName
                try {
                    val applicationInfo = pm
                        .getApplicationInfo(packageName, PackageManager.GET_META_DATA)
                    val mData = InstalledAppsData(
                        pm.getApplicationLabel(applicationInfo).toString(),
                        applicationInfo.packageName, pm.getApplicationIcon(applicationInfo)
                    )
                    mData.isChecked = true
                    onInstalledAppClick(mData, true)
                } catch (e: PackageManager.NameNotFoundException) {
                    e.printStackTrace()
                }
                splitTunnelView.setSplitModeTextView(
                    mode,
                    R.string.feature_tunnel_mode_inclusive
                )
            }
        }
    }

    override fun onToggleButtonClicked() {
        interactor.getAppPreferenceInterface().setReconnectRequired(true)
        if (interactor.getAppPreferenceInterface().splitTunnelToggle) {
            logger.info("Previous Split Tunnel Toggle Settings: True")
            interactor.getAppPreferenceInterface().splitTunnelToggle = false
            splitTunnelView.setupToggleImage(R.drawable.ic_toggle_button_off)
            splitTunnelView.hideTunnelSettingsLayout()
        } else {
            logger.info("Previous Split Tunnel Toggle Settings: False")
            interactor.getAppPreferenceInterface().splitTunnelToggle = true
            splitTunnelView.setupToggleImage(R.drawable.ic_toggle_button_on)
            splitTunnelView.showTunnelSettingsLayout()
        }
    }

    override fun setTheme(context: Context) {
        val savedThem = interactor.getAppPreferenceInterface().selectedTheme
        logger.debug("Setting theme to $savedThem")
        if (savedThem == PreferencesKeyConstants.DARK_THEME) {
            context.setTheme(R.style.DarkTheme)
        } else {
            context.setTheme(R.style.LightTheme)
        }
    }

    override fun setupLayoutBasedOnPreviousSettings() {
        if (interactor.getAppPreferenceInterface().splitTunnelToggle) {
            //Toggle is on so show the mode and selected app settings
            splitTunnelView.showTunnelSettingsLayout()
            //Toggle Button ON
            splitTunnelView.setupToggleImage(R.drawable.ic_toggle_button_on)
        } else {
            //Hide the settings and List
            splitTunnelView.hideTunnelSettingsLayout()
            //Toggle Button OFF
            splitTunnelView.setupToggleImage(R.drawable.ic_toggle_button_off)
        }

        //Setup application list adapter
        setupAppListAdapter()
        setupSplitRoutingMode()
    }

    private fun modifyList(savedApps: List<String>) {
        val pm = appContext.packageManager
        interactor.getCompositeDisposable()
            .add(Single.fromCallable { pm.getInstalledApplications(PackageManager.GET_META_DATA) }
                .flatMap { packages: List<ApplicationInfo> ->
                    for (applicationInfo in packages) {
                        val mData = InstalledAppsData(
                            pm.getApplicationLabel(applicationInfo).toString(),
                            applicationInfo.packageName,
                            pm.getApplicationIcon(applicationInfo)
                        )
                        for (installedAppsData in savedApps) {
                            if (mData.packageName == installedAppsData) {
                                mData.isChecked = true
                            }
                        }
                        mInstalledAppList.add(mData)
                    }
                    Collections.sort(mInstalledAppList, SortByName())
                    Collections.sort(mInstalledAppList, SortBySelected())
                    Single.fromCallable { mInstalledAppList }
                }.cache()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribeWith(object : DisposableSingleObserver<List<InstalledAppsData?>?>() {
                    override fun onError(ignored: Throwable) {
                        splitTunnelView.showProgress(false)
                    }

                    override fun onSuccess(packages: List<InstalledAppsData?>) {
                        splitTunnelView.showProgress(false)
                        installedAppsAdapter = InstalledAppsAdapter(
                            mInstalledAppList,
                            this@SplitTunnelingPresenterImpl
                        )
                        splitTunnelView.setRecyclerViewAdapter(installedAppsAdapter!!)
                    }
                })
            )
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun saveApps(
        savedList: MutableList<String>,
        updatedApp: InstalledAppsData,
        reloadAdapter: Boolean
    ) {
        interactor.getAppPreferenceInterface().setReconnectRequired(true)
        if (updatedApp.isChecked) {
            savedList.add(updatedApp.packageName)
        } else {
            savedList.remove(updatedApp.packageName)
            interactor.getAppPreferenceInterface().saveInstalledApps(savedList)
        }
        interactor.getAppPreferenceInterface().saveInstalledApps(savedList)
        if (reloadAdapter) {
            installedAppsAdapter?.notifyDataSetChanged()
        }
    }

    private fun setupAppListAdapter() {
        splitTunnelView.showProgress(true)
        interactor.getCompositeDisposable()
            .add(
                interactor.getAppPreferenceInterface().installedApps
                    .cache()
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeOn(Schedulers.io())
                    .subscribeWith(object : DisposableSingleObserver<List<String>>() {
                        override fun onError(ignored: Throwable) {
                            splitTunnelView.showProgress(false)
                            modifyList(emptyList())
                        }

                        override fun onSuccess(installedAppsData: List<String>) {
                            modifyList(installedAppsData)
                        }
                    })
            )
    }

    private fun setupSplitRoutingMode() {
        val mode = interactor.getAppPreferenceInterface().splitRoutingMode
        splitTunnelView.setSplitRoutingModeAdapter(splitTunnelView.splitRoutingModes, mode)
        if (mode == PreferencesKeyConstants.EXCLUSIVE_MODE) {
            splitTunnelView.setSplitModeTextView(mode, R.string.feature_tunnel_mode_exclusive)
        } else {
            splitTunnelView.setSplitModeTextView(mode, R.string.feature_tunnel_mode_inclusive)
        }
    }
}