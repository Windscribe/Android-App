/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.mobile.networksecurity

import android.content.Context
import com.windscribe.mobile.R
import com.windscribe.vpn.ActivityInteractor
import com.windscribe.vpn.constants.PreferencesKeyConstants
import com.windscribe.vpn.errormodel.WindError.Companion.instance
import com.windscribe.vpn.localdatabase.tables.NetworkInfo
import com.windscribe.vpn.state.NetworkInfoListener
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import io.reactivex.subscribers.DisposableSubscriber
import org.slf4j.LoggerFactory
import javax.inject.Inject

class NetworkSecurityPresenterImpl @Inject constructor(
    private val networkSecurityView: NetworkSecurityView,
    private val interactor: ActivityInteractor
) : NetworkSecurityPresenter, NetworkInfoListener {
    private val logger = LoggerFactory.getLogger("basic")
    override fun onDestroy() {
        interactor.getNetworkInfoManager().removeNetworkInfoListener(this)
        //Dispose any observer
        if (interactor.getCompositeDisposable().isDisposed.not()) {
            logger.info("Disposing observer...")
            interactor.getCompositeDisposable().dispose()
        }
    }

    override fun init() {
        interactor.getNetworkInfoManager().networkInfo?.let {
            networkSecurityView.setupCurrentNetwork(it)
        }
        interactor.getNetworkInfoManager().addNetworkInfoListener(this)
        networkSecurityView.setAutoSecureToggle(if (interactor.getAppPreferenceInterface().isAutoSecureOn) R.drawable.ic_toggle_button_on else R.drawable.ic_toggle_button_off)
    }

    override val savedLocale: String
        get() {
            val selectedLanguage = interactor.getAppPreferenceInterface().savedLanguage
            return selectedLanguage.substring(
                selectedLanguage.indexOf("(") + 1,
                selectedLanguage.indexOf(")")
            )
        }

    override fun onAdapterSet() {
        networkSecurityView.hideProgress()
    }

    override fun onNetworkSecuritySelected(networkInfo: NetworkInfo) {
        networkSecurityView.openNetworkSecurityDetails(networkInfo.networkName)
    }

    override fun setTheme(context: Context) {
        val savedThem = interactor.getAppPreferenceInterface().selectedTheme
        if (savedThem == PreferencesKeyConstants.DARK_THEME) {
            context.setTheme(R.style.DarkTheme)
        } else {
            context.setTheme(R.style.LightTheme)
        }
    }

    override fun setupNetworkListAdapter() {
        logger.info("Setting up network list adapter...")
        networkSecurityView.showProgress(interactor.getResourceString(R.string.loading_network_list))
        interactor.getCompositeDisposable().add(
            interactor.getNetworkInfoUpdated()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(object : DisposableSubscriber<List<NetworkInfo>>() {
                    override fun onComplete() {
                        networkSecurityView.setAdapter(null)
                    }

                    override fun onError(e: Throwable) {
                        networkSecurityView.setAdapter(null)
                        logger.debug(
                            "Error reading network list data..." +
                                    instance.convertThrowableToString(e)
                        )
                        networkSecurityView.onAdapterLoadFailed(
                            interactor.getResourceString(R.string.no_saved_network_list)
                        )
                        networkSecurityView.hideProgress()
                    }

                    override fun onNext(networks: List<NetworkInfo>?) {
                        logger.info("Reading network list data successful...")
                        val networkList = networks?.toMutableList() ?: mutableListOf()
                        val activeNetworkInfo = interactor.getNetworkInfoManager().networkInfo
                        if (activeNetworkInfo != null) {
                            networkList.removeIf { networkInfo: NetworkInfo -> networkInfo.networkName == activeNetworkInfo.networkName }
                        }
                        networkSecurityView.setAdapter(networkList)
                    }
                })
        )
    }

    override fun onNetworkInfoUpdate(networkInfo: NetworkInfo?, userReload: Boolean) {
        if(networkInfo != null){
            networkSecurityView.setupCurrentNetwork(networkInfo)
        } else {
            networkSecurityView.hideCurrentNetwork()
        }
        setupNetworkListAdapter()
    }

    override fun onCurrentNetworkClick() {
        val networkInfo = interactor.getNetworkInfoManager().networkInfo
        if (networkInfo != null) {
            networkSecurityView.openNetworkSecurityDetails(networkInfo.networkName)
        }
    }

    override fun onAutoSecureToggleClick() {
        if (interactor.getAppPreferenceInterface().isAutoSecureOn) {
            interactor.getAppPreferenceInterface().isAutoSecureOn = false
            networkSecurityView.setAutoSecureToggle(R.drawable.ic_toggle_button_off)
        } else {
            interactor.getAppPreferenceInterface().isAutoSecureOn = true
            networkSecurityView.setAutoSecureToggle(R.drawable.ic_toggle_button_on)
        }
    }
}