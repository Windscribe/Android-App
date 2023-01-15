/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.mobile.networksecurity.networkdetails

import android.content.Context
import com.windscribe.mobile.R
import com.windscribe.vpn.ActivityInteractor
import com.windscribe.vpn.ActivityInteractorImpl.PortMapLoadCallback
import com.windscribe.vpn.Windscribe.Companion.appContext
import com.windscribe.vpn.api.response.PortMapResponse
import com.windscribe.vpn.api.response.PortMapResponse.PortMap
import com.windscribe.vpn.constants.PreferencesKeyConstants
import com.windscribe.vpn.constants.PreferencesKeyConstants.PROTO_IKev2
import com.windscribe.vpn.localdatabase.tables.NetworkInfo
import com.windscribe.vpn.services.DeviceStateService
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.observers.DisposableSingleObserver
import io.reactivex.schedulers.Schedulers
import org.slf4j.LoggerFactory
import javax.inject.Inject

class NetworkDetailPresenterImp @Inject constructor(
    private val networkView: NetworkDetailView,
    private val interactor: ActivityInteractor
) : NetworkDetailPresenter {

    private val logger = LoggerFactory.getLogger("network_detail_p")

    override fun onDestroy() {
        if (!interactor.getCompositeDisposable().isDisposed) {
            interactor.getCompositeDisposable().dispose()
        }
    }

    override fun init(){
        networkView.setActivityTitle(interactor.getResourceString(R.string.network_options))
    }

    override fun onPortSelected(port: String) {
        val networkInfo = networkView.networkInfo
        networkInfo?.let {
            networkInfo.port = port
            interactor.getCompositeDisposable().add(
                interactor.saveNetwork(networkInfo)
                    .subscribeOn(Schedulers.io())
                    .flatMap { interactor.getNetwork(networkInfo.networkName) }
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeWith(object : DisposableSingleObserver<NetworkInfo?>() {
                        override fun onError(ignored: Throwable) {
                            networkView.showToast("Error Loading network.")
                        }

                        override fun onSuccess(updatedNetwork: NetworkInfo) {
                            networkView.networkInfo = updatedNetwork
                        }
                    })
            )
        }
    }

    override fun onProtocolSelected(protocol: String) {
        interactor.loadPortMap(object : PortMapLoadCallback {
            override fun onFinished(portMapResponse: PortMapResponse) {
                val networkInfo = networkView.networkInfo
                networkInfo?.let {
                    networkInfo.protocol =
                        getProtocolFromHeading(portMapResponse, protocol)
                    interactor.getCompositeDisposable()
                        .add(
                            interactor.saveNetwork(networkInfo)
                                .subscribeOn(Schedulers.io())
                                .flatMap { interactor.getNetwork(networkInfo.networkName) }
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribeWith(object : DisposableSingleObserver<NetworkInfo?>() {
                                    override fun onError(ignored: Throwable) {
                                        networkView.showToast("Error saving network information.")
                                    }

                                    override fun onSuccess(updatedNetwork: NetworkInfo) {
                                        networkView.networkInfo = updatedNetwork
                                        setPorts()
                                    }
                                })
                        )
                }
            }
        })
    }

    override fun removeNetwork(name: String) {
        interactor.getCompositeDisposable()
            .add(interactor.removeNetwork(name)
                .flatMap {
                    interactor.getNetworkInfoManager().reload(true)
                    return@flatMap Single.just(it)
                }.subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeWith(object : DisposableSingleObserver<Int?>() {
                        override fun onError(ignored: Throwable) {
                            networkView.showToast("Error deleting network")
                        }

                        override fun onSuccess(integer: Int) {
                            networkView.onNetworkDeleted()
                        }
                    })
            )
    }

    override fun setNetworkDetails(name: String) {
        networkView.setNetworkDetailError(false, null)
        interactor.getCompositeDisposable().add(
            interactor.getNetwork(name)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(object : DisposableSingleObserver<NetworkInfo?>() {
                    override fun onError(ignored: Throwable) {
                        networkView.setNetworkDetailError(true, "Network name not found.")
                    }

                    override fun onSuccess(networkInfo: NetworkInfo) {
                        val currentNetworkName =
                            interactor.getNetworkInfoManager().networkInfo?.networkName ?: ""
                        networkView.onNetworkDetailAvailable(
                            networkInfo,
                            currentNetworkName == networkInfo.networkName
                        )
                    }
                })
        )
    }

    fun setPorts() {
        interactor.loadPortMap(object : PortMapLoadCallback {
            override fun onFinished(portMapResponse: PortMapResponse) {
                networkView.networkInfo?.let {
                    val protocol = it.protocol
                    val savedPort = it.port
                    portMapResponse.let {
                        for (portMap in portMapResponse.portmap) {
                            if (portMap.protocol == protocol) {
                                networkView.setupPortMapAdapter(savedPort, portMap.ports)
                            }
                        }
                    }
                }
            }
        })
    }

    override fun setProtocols() {
        interactor.loadPortMap(object : PortMapLoadCallback {
            override fun onFinished(portMapResponse: PortMapResponse) {
                networkView.networkInfo?.let {
                    val savedProtocol = it.protocol
                    var selectedPortMap: PortMap? = null
                    val protocols: MutableList<String> = ArrayList()
                    portMapResponse.let {
                        for (portMap in portMapResponse.portmap) {
                            if (portMap.protocol == savedProtocol) {
                                selectedPortMap = portMap
                            }
                            protocols.add(portMap.heading)
                        }
                        selectedPortMap?.let { portMap ->
                            networkView.setupProtocolAdapter(
                                portMap.heading,
                                protocols.toTypedArray()
                            )
                            setPorts()
                        }
                    }
                }
            }
        })
    }

    override fun setTheme(context: Context) {
        val savedThem = interactor.getAppPreferenceInterface().selectedTheme
        if (savedThem == PreferencesKeyConstants.DARK_THEME) {
            context.setTheme(R.style.DarkTheme)
        } else {
            context.setTheme(R.style.LightTheme)
        }
    }

    override fun toggleAutoSecure() {
        interactor.getAppPreferenceInterface().whitelistOverride = false
        val networkInfo = networkView.networkInfo
        if (networkInfo == null) {
            networkView.showToast("Make sure location permission is set Allow all the time")
            return
        }
        networkInfo.isAutoSecureOn = !networkInfo.isAutoSecureOn
        logger.debug("Auto secure toggle: ${!networkInfo.isAutoSecureOn}")
        interactor.getCompositeDisposable().add(
            interactor.saveNetwork(networkInfo)
                .subscribeOn(Schedulers.io())
                .flatMap { interactor.getNetwork(networkInfo.networkName) }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(object : DisposableSingleObserver<NetworkInfo?>() {
                    override fun onError(ignored: Throwable) {
                        logger.debug("Auto secure toggle: ${!networkInfo.isAutoSecureOn}")
                        networkView.showToast("Failed to save network details.")
                    }

                    override fun onSuccess(updatedNetwork: NetworkInfo) {
                        logger.debug("SSID: ${networkInfo.networkName} AutoSecure: ${networkInfo.isAutoSecureOn} Preferred Protocols: ${networkInfo.isPreferredOn} ${networkInfo.protocol} ${networkInfo.port}")
                        networkView.networkInfo = updatedNetwork
                        networkView.setAutoSecureToggle(updatedNetwork.isAutoSecureOn)
                        logger.debug("Reloading network info.")
                        interactor.getNetworkInfoManager().reload(true)
                        DeviceStateService.enqueueWork(appContext)
                    }
                })
        )
    }

    override fun togglePreferredProtocol() {
        val networkInfo = networkView.networkInfo
        if (networkInfo == null) {
            networkView.showToast("Check network permissions.")
            return
        }
        networkInfo.isPreferredOn = !networkInfo.isPreferredOn
        interactor.getCompositeDisposable().add(
            interactor.saveNetwork(networkInfo)
                .subscribeOn(Schedulers.io())
                .flatMap { interactor.getNetwork(networkInfo.networkName) }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(object : DisposableSingleObserver<NetworkInfo?>() {
                    override fun onError(e: Throwable) {
                        networkView.showToast("Error...")
                    }

                    override fun onSuccess(updatedNetwork: NetworkInfo) {
                        networkView.networkInfo = updatedNetwork
                        networkView.setPreferredProtocolToggle(updatedNetwork.isPreferredOn)
                    }
                })
        )
    }

    private fun getProtocolFromHeading(portMapResponse: PortMapResponse, heading: String): String {
        for (map in portMapResponse.portmap) {
            if (map.heading == heading) {
                return map.protocol
            }
        }
        return PROTO_IKev2
    }

    override fun reloadNetworkOptions() {
        interactor.getNetworkInfoManager().reload(false)
    }
}