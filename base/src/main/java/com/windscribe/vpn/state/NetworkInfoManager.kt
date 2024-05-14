/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.state

import com.windscribe.vpn.apppreference.PreferencesHelper
import com.windscribe.vpn.commonutils.WindUtilities
import com.windscribe.vpn.constants.PreferencesKeyConstants
import com.windscribe.vpn.localdatabase.LocalDbInterface
import com.windscribe.vpn.localdatabase.tables.NetworkInfo
import com.windscribe.vpn.state.DeviceStateManager.DeviceStateListener
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import java.util.concurrent.Callable
import java.util.concurrent.ConcurrentLinkedQueue
import javax.inject.Singleton

@Singleton
class NetworkInfoManager(private val preferencesHelper: PreferencesHelper, private val localDbInterface: LocalDbInterface, deviceStateManager: DeviceStateManager) :
        DeviceStateListener {

    private val compositeDisposable = CompositeDisposable()
    var networkInfo: NetworkInfo? = null
        private set
    private val listeners = ConcurrentLinkedQueue<NetworkInfoListener>()
    fun addNetworkInfoListener(networkInfoListener: NetworkInfoListener) {
        listeners.add(networkInfoListener)
    }

    private fun addNetworkToKnown(networkName: String): Single<Long> {
        val networkInfo = NetworkInfo(
                networkName, preferencesHelper.isAutoSecureOn, false, PreferencesKeyConstants.PROTO_IKev2, PreferencesKeyConstants.DEFAULT_IKEV2_PORT
        )
        return localDbInterface.addNetwork(networkInfo)
    }

    override fun onNetworkStateChanged() {
        reloadCurrentNetwork(false)
    }

    fun reload(userReload: Boolean = false) {
        reloadCurrentNetwork(userReload)
    }

    fun removeNetworkInfoListener(networkInfoListener: NetworkInfoListener) {
        listeners.remove(networkInfoListener)
    }

    fun updateNetworkInfo(networkInfo: NetworkInfo) {
        compositeDisposable.add(
                localDbInterface.updateNetwork(networkInfo).doOnSuccess { reloadCurrentNetwork(true) }
                        .subscribeOn(Schedulers.io())
                        .subscribe()
        )
    }

    private fun reloadCurrentNetwork(userReload: Boolean) {
        compositeDisposable.add(
                Single.fromCallable(Callable { WindUtilities.getNetworkName() } as Callable<String>)
                        .flatMap { name ->
                            localDbInterface
                                    .getNetwork(name).onErrorResumeNext(
                                            addNetworkToKnown(name)
                                                    .flatMap { localDbInterface.getNetwork(name) }
                                    )
                        }
                        .observeOn(AndroidSchedulers.mainThread())
                        .doOnSuccess {
                            this.networkInfo = it
                            for (listener in listeners) {
                                listener.onNetworkInfoUpdate(networkInfo, userReload)
                            }
                        }.doOnError {
                            networkInfo = null
                            for (listener in listeners) {
                                listener.onNetworkInfoUpdate(null, userReload)
                            }
                        }.subscribeOn(Schedulers.io()).subscribe({}, {})
        )
    }

    init {
        deviceStateManager.addListener(this)
    }
}

interface NetworkInfoListener {

    fun onNetworkInfoUpdate(networkInfo: NetworkInfo?, userReload: Boolean)
}
