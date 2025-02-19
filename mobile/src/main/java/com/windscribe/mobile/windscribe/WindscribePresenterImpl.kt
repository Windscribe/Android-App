/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.mobile.windscribe

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.util.Pair
import android.view.View
import androidx.documentfile.provider.DocumentFile
import androidx.recyclerview.widget.RecyclerView
import com.google.common.io.CharStreams
import com.windscribe.mobile.R
import com.windscribe.mobile.adapter.ConfigAdapter
import com.windscribe.mobile.adapter.FavouriteAdapter
import com.windscribe.mobile.adapter.RegionsAdapter
import com.windscribe.mobile.adapter.StaticRegionAdapter
import com.windscribe.mobile.adapter.StreamingNodeAdapter
import com.windscribe.mobile.connectionui.ConnectedAnimationState
import com.windscribe.mobile.connectionui.ConnectedState
import com.windscribe.mobile.connectionui.ConnectingAnimationState
import com.windscribe.mobile.connectionui.ConnectingState
import com.windscribe.mobile.connectionui.ConnectionOptions
import com.windscribe.mobile.connectionui.ConnectionOptionsBuilder
import com.windscribe.mobile.connectionui.DisconnectedState
import com.windscribe.mobile.connectionui.FailedProtocol
import com.windscribe.mobile.connectionui.UnsecuredProtocol
import com.windscribe.mobile.listeners.ProtocolClickListener
import com.windscribe.mobile.utils.PermissionManager
import com.windscribe.mobile.utils.UiUtil.getDataRemainingColor
import com.windscribe.mobile.windscribe.WindscribeActivity.NetworkLayoutState
import com.windscribe.vpn.ActivityInteractor
import com.windscribe.vpn.ActivityInteractorImpl.PortMapLoadCallback
import com.windscribe.vpn.Windscribe.Companion.appContext
import com.windscribe.vpn.api.response.PortMapResponse
import com.windscribe.vpn.api.response.PushNotificationAction
import com.windscribe.vpn.autoconnection.ProtocolInformation
import com.windscribe.vpn.backend.Util
import com.windscribe.vpn.backend.Util.getSavedLocation
import com.windscribe.vpn.backend.VPNState
import com.windscribe.vpn.backend.openvpn.OpenVPNConfigParser
import com.windscribe.vpn.backend.utils.LastSelectedLocation
import com.windscribe.vpn.backend.utils.ProtocolConfig
import com.windscribe.vpn.backend.utils.SelectedLocationType
import com.windscribe.vpn.commonutils.FlagIconResource
import com.windscribe.vpn.commonutils.WindUtilities
import com.windscribe.vpn.constants.NetworkKeyConstants
import com.windscribe.vpn.constants.NetworkKeyConstants.NODE_STATUS_URL
import com.windscribe.vpn.constants.NetworkKeyConstants.getWebsiteLink
import com.windscribe.vpn.constants.PreferencesKeyConstants
import com.windscribe.vpn.constants.PreferencesKeyConstants.AZ_LIST_SELECTION_MODE
import com.windscribe.vpn.constants.PreferencesKeyConstants.LATENCY_LIST_SELECTION_MODE
import com.windscribe.vpn.constants.PreferencesKeyConstants.PROTO_WIRE_GUARD
import com.windscribe.vpn.constants.UserStatusConstants
import com.windscribe.vpn.constants.UserStatusConstants.ACCOUNT_STATUS_OK
import com.windscribe.vpn.errormodel.WindError.Companion.instance
import com.windscribe.vpn.exceptions.BackgroundLocationPermissionNotAvailable
import com.windscribe.vpn.exceptions.NoLocationPermissionException
import com.windscribe.vpn.exceptions.NoNetworkException
import com.windscribe.vpn.exceptions.WindScribeException
import com.windscribe.vpn.localdatabase.tables.NetworkInfo
import com.windscribe.vpn.localdatabase.tables.PopupNotificationTable
import com.windscribe.vpn.localdatabase.tables.WindNotification
import com.windscribe.vpn.model.User
import com.windscribe.vpn.repository.LatencyRepository
import com.windscribe.vpn.serverlist.entity.City
import com.windscribe.vpn.serverlist.entity.CityAndRegion
import com.windscribe.vpn.serverlist.entity.ConfigFile
import com.windscribe.vpn.serverlist.entity.Favourite
import com.windscribe.vpn.serverlist.entity.Group
import com.windscribe.vpn.serverlist.entity.PingTime
import com.windscribe.vpn.serverlist.entity.RegionAndCities
import com.windscribe.vpn.serverlist.entity.ServerListData
import com.windscribe.vpn.serverlist.entity.StaticRegion
import com.windscribe.vpn.serverlist.interfaces.ListViewClickListener
import com.windscribe.vpn.serverlist.sort.ByCityName
import com.windscribe.vpn.serverlist.sort.ByConfigName
import com.windscribe.vpn.serverlist.sort.ByLatency
import com.windscribe.vpn.serverlist.sort.ByRegionName
import com.windscribe.vpn.serverlist.sort.ByStaticRegionName
import com.windscribe.vpn.services.DeviceStateService.Companion.enqueueWork
import com.windscribe.vpn.state.NetworkInfoListener
import inet.ipaddr.AddressStringException
import inet.ipaddr.IPAddressString
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.observers.DisposableCompletableObserver
import io.reactivex.observers.DisposableSingleObserver
import io.reactivex.schedulers.Schedulers
import io.reactivex.subscribers.DisposableSubscriber
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import java.io.IOException
import java.io.InputStreamReader
import java.util.Collections
import java.util.Date
import java.util.concurrent.atomic.AtomicBoolean
import java.util.function.Consumer
import javax.inject.Inject


class WindscribePresenterImpl @Inject constructor(
    private var windscribeView: WindscribeView,
    private var interactor: ActivityInteractor,
    private val permissionManager: PermissionManager
) : WindscribePresenter, ListViewClickListener, ProtocolClickListener, NetworkInfoListener {

    // Adapters
    private var adapter: RegionsAdapter? = null
    private var configAdapter: ConfigAdapter? = null
    private var favouriteAdapter: FavouriteAdapter? = null
    private var staticRegionAdapter: StaticRegionAdapter? = null
    private var streamingNodeAdapter: StreamingNodeAdapter? = null

    // Connection
    private var lastVPNState = VPNState.Status.Disconnected
    private var selectedLocation: LastSelectedLocation? = null
    private val flagIcons: Map<String, Int> = FlagIconResource.flagIcons
    private var networkInformation: NetworkInfo? = null
    private val onUserDataUpdate = AtomicBoolean()
    private val logger = LoggerFactory.getLogger("basic")
    private var connectingFromServerList = false

    override fun onStart() {}

    override fun onDestroy() {
        interactor.getNetworkInfoManager().removeNetworkInfoListener(this)
        if (!interactor.getCompositeDisposable().isDisposed) {
            interactor.getCompositeDisposable().dispose()
        }
        streamingNodeAdapter = null
        favouriteAdapter = null
        staticRegionAdapter = null
        adapter = null
    }

    override fun observeUserData(windscribeActivity: WindscribeActivity) {
        interactor.getUserRepository().user.observe(windscribeActivity) {
            setAccountStatus(it)
            setUserStatus(it)
        }
    }

    override suspend fun observeDecoyTrafficState() {
        interactor.getDecoyTrafficController().state.collectLatest {
            if (interactor.getVpnConnectionStateManager().isVPNActive()) {
                if (it) {
                    windscribeView.setDecoyTrafficInfoVisibility(View.VISIBLE)
                } else {
                    windscribeView.setDecoyTrafficInfoVisibility(View.GONE)
                }
            }
        }
    }

    override fun addToFavourite(
        cityId: Int,
        position: Int,
        adapter: RecyclerView.Adapter<RecyclerView.ViewHolder>
    ) {
        val favourite = Favourite()
        favourite.id = cityId
        interactor.getCompositeDisposable()
            .add(interactor.addToFavourites(favourite).flatMap { interactor.getFavourites() }
                .subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
                .subscribe({ favourites: List<Favourite> ->
                    resetAdapters(
                        favourites,
                        interactor.getResourceString(R.string.added_to_favourites),
                        position,
                        adapter
                    )
                }) { throwable: Throwable ->
                    logger.debug(
                        String.format(
                            "Failed to add to favourites. : %s", throwable.localizedMessage
                        )
                    )
                    windscribeView.showToast("Failed to add to favourites.")
                })
    }

    override val lastSelectedTabIndex: Int
        get() = interactor.getAppPreferenceInterface().lastSelectedTabIndex

    override fun deleteConfigFile(configFile: ConfigFile) {
        interactor.getCompositeDisposable().add(
            interactor.deleteConfigFile(configFile.getPrimaryKey())
                .observeOn(AndroidSchedulers.mainThread()).subscribeOn(Schedulers.io())
                .subscribeWith(object : DisposableCompletableObserver() {
                    override fun onComplete() {
                        interactor.getPreferenceChangeObserver().postConfigListChange()
                        logger.debug("Config deleted successfully")
                        windscribeView.showToast("Config deleted successfully")
                    }

                    override fun onError(e: Throwable) {
                        logger.error(e.toString())
                        windscribeView.showToast("Error deleting config file.")
                    }
                })
        )
    }

    override fun editConfigFile(file: ConfigFile) {
        windscribeView.openEditConfigFileDialog(file)
    }

    val connectionOptions: ConnectionOptions = ConnectionOptionsBuilder().build()

    override val selectedPort: String
        get() = interactor.getAppPreferenceInterface().selectedPort
    override val selectedProtocol: String
        get() = interactor.getAppPreferenceInterface().selectedProtocol

    override fun handlePushNotification(extras: Bundle?) {
        if (extras != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                extras.keySet().forEach(Consumer { s: String ->
                    logger.debug("$s " + extras.getString(s, "----"))
                })
            }
        }
        if (extras != null && extras.containsKey("type") && "promo" == extras.getString("type")) {
            val pushNotificationAction = PushNotificationAction(
                extras.getString("pcpid")!!,
                extras.getString("promo_code")!!,
                extras.getString("type")!!
            )
            appContext.appLifeCycleObserver.pushNotificationAction = pushNotificationAction
            logger.debug("App Launch by push notification with promo action. Taking user to upgrade")
            windscribeView.openUpgradeActivity()
        }
    }

    override fun init() {
        interactor.getAppPreferenceInterface().isReconnecting = false
        // User data
        onUserDataUpdate.set(false)
        // Set ip from local Storage
        setIpFromLocalStorage()
        // Config locations
        interactor.getPreferenceChangeObserver().postConfigListChange()
        // Notifications
        updateNotificationCount()
        windscribeView.setIpBlur(interactor.getAppPreferenceInterface().blurIp)
        windscribeView.setNetworkNameBlur(interactor.getAppPreferenceInterface().blurNetworkName)
        addNotificationChangeListener()
        calculateFlagDimensions()
        interactor.getUserRepository().user.value?.let {
            setUserStatus(it)
            setAccountStatus(it)
        }
    }

    override fun setAdapters() {
        adapter?.let { windscribeView.setAdapter(it) }
        favouriteAdapter?.let { windscribeView.setFavouriteAdapter(it) }
        streamingNodeAdapter?.let { windscribeView.setStreamingNodeAdapter(it) }
        staticRegionAdapter?.let { windscribeView.setStaticRegionAdapter(it) }
        configAdapter?.let { windscribeView.setConfigLocListAdapter(it) }
        if (staticRegionAdapter == null) {
            windscribeView.showStaticIpAdapterLoadError(
                "No Static IP's", interactor.getResourceString(R.string.add_static_ip), ""
            )
        }
    }

    override val isConnectedOrConnecting: Boolean
        get() {
            val status = interactor.getVpnConnectionStateManager().state.value.status
            return status === VPNState.Status.Connected || status === VPNState.Status.Connecting
        }
    override val isHapticFeedbackEnabled: Boolean
        get() = interactor.getAppPreferenceInterface().isHapticFeedbackEnabled

    override fun loadConfigLocations() {
        val serverListData = ServerListData()
        interactor.getCompositeDisposable().add(
            interactor.getAllPings().flatMap { pingTestResults: List<PingTime> ->
                serverListData.pingTimes = pingTestResults
                interactor.getAllConfigs()
            }.onErrorResumeNext(
                interactor.getAllConfigs()
            ).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(object : DisposableSingleObserver<List<ConfigFile>>() {
                    override fun onError(e: Throwable) {
                        windscribeView.hideRecyclerViewProgressBar()
                        windscribeView.setConfigLocListAdapter(null)
                        logger.debug("Error getting config locations..")
                        windscribeView.showConfigLocAdapterLoadError(
                            interactor.getResourceString(R.string.no_custom_configs), 0
                        )
                    }

                    override fun onSuccess(configFiles: List<ConfigFile>) {
                        val selection = interactor.getAppPreferenceInterface().selection
                        if (selection == LATENCY_LIST_SELECTION_MODE) {
                            Collections.sort(configFiles) { o1: ConfigFile, o2: ConfigFile ->
                                serverListData.pingTimes
                                getPingTimeFromCity(
                                    o1.getPrimaryKey(), serverListData
                                ) - getPingTimeFromCity(
                                    o2.getPrimaryKey(), serverListData
                                )
                            }
                        } else if (selection == AZ_LIST_SELECTION_MODE) {
                            Collections.sort(configFiles, ByConfigName())
                        }
                        serverListData.setShowLatencyInMs(interactor.getAppPreferenceInterface().showLatencyInMS)
                        serverListData.setShowLocationHealth(
                            interactor.getAppPreferenceInterface().isShowLocationHealthEnabled
                        )
                        serverListData.flags = flagIcons
                        serverListData.isProUser =
                            interactor.getAppPreferenceInterface().userStatus == 1
                        if (configFiles.isNotEmpty()) {
                            configAdapter = ConfigAdapter(
                                configFiles, serverListData, this@WindscribePresenterImpl
                            )
                            windscribeView.setConfigLocListAdapter(configAdapter!!)
                            windscribeView.showConfigLocAdapterLoadError(
                                "", configFiles.size
                            )
                        } else {
                            windscribeView.setConfigLocListAdapter(null)
                            configAdapter = null
                            windscribeView.showConfigLocAdapterLoadError(
                                interactor.getResourceString(R.string.no_custom_configs), 0
                            )
                        }
                        windscribeView.hideRecyclerViewProgressBar()
                    }
                })
        )
    }

    override suspend fun observeAllLocations() {
        interactor.getServerListUpdater().regions.collectLatest {
            val updatedServerListHash = interactor.getAppPreferenceInterface().locationHash
            if (adapter?.serverListData?.serverListHash != updatedServerListHash) {
                if (it.isNotEmpty()) {
                    loadServerList(it.toMutableList(), updatedServerListHash)
                }
            }
        }
    }

    private val latencyAtomic = AtomicBoolean(true)
    override suspend fun observeLatency() {
        interactor.getLatencyRepository().latencyEvent.collectLatest {
            if (latencyAtomic.getAndSet(false)) return@collectLatest
            when (it.second) {
                LatencyRepository.LatencyType.Servers -> {
                    interactor.getServerListUpdater().invalidateServerListUI()
                }

                LatencyRepository.LatencyType.StaticIp -> {
                    interactor.getStaticListUpdater().load()
                }

                LatencyRepository.LatencyType.Config -> {
                    loadConfigLocations()
                }
            }
        }
    }

    private fun loadServerList(regions: MutableList<RegionAndCities>, serverListHash: String?) {
        windscribeView.showRecyclerViewProgressBar()
        val serverListData = ServerListData()
        val oneTimeCompositeDisposable = CompositeDisposable()
        oneTimeCompositeDisposable.add(
            interactor.getAllPings().onErrorReturnItem(ArrayList()).flatMap {
                serverListData.pingTimes = it
                interactor.getFavourites()
            }.onErrorReturnItem(ArrayList()).flatMap {
                serverListData.favourites = it
                interactor.getLocationProvider().bestLocation
            }.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(object : DisposableSingleObserver<CityAndRegion?>() {
                    override fun onError(e: Throwable) {
                        windscribeView.hideRecyclerViewProgressBar()
                        val error =
                            if (e is WindScribeException) e.message else "Unknown error loading while loading server list."
                        windscribeView.showReloadError(error!!)
                        if (!oneTimeCompositeDisposable.isDisposed) {
                            oneTimeCompositeDisposable.dispose()
                        }
                    }

                    override fun onSuccess(cityAndRegion: CityAndRegion) {
                        if (selectedLocation == null) {
                            val coordinatesArray =
                                cityAndRegion.city.coordinates.split(",".toRegex()).toTypedArray()
                            selectedLocation = LastSelectedLocation(
                                cityAndRegion.city.getId(),
                                cityAndRegion.city.nodeName,
                                cityAndRegion.city.nickName,
                                cityAndRegion.region.countryCode,
                                coordinatesArray[0],
                                coordinatesArray[1]
                            )
                        }
                        updateLocationUI(selectedLocation, true)
                        serverListData.setShowLatencyInMs(interactor.getAppPreferenceInterface().showLatencyInMS)
                        serverListData.setShowLocationHealth(
                            interactor.getAppPreferenceInterface().isShowLocationHealthEnabled
                        )
                        serverListData.serverListHash = serverListHash
                        serverListData.flags = flagIcons
                        serverListData.bestLocation = cityAndRegion
                        serverListData.isProUser =
                            interactor.getAppPreferenceInterface().userStatus == 1
                        logger.debug(if (serverListData.isProUser) "Setting server list for pro user" else "Setting server list for free user")
                        setAllServerView(regions, serverListData)
                        setFavouriteServerView(serverListData)
                        if (!oneTimeCompositeDisposable.isDisposed) {
                            oneTimeCompositeDisposable.dispose()
                        }
                    }
                })
        )
    }

    override suspend fun observeStaticRegions() {
        interactor.getStaticListUpdater().regions.collectLatest {
            loadStaticServers(it.toMutableList())
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun loadStaticServers(regions: MutableList<StaticRegion>) {
        interactor.getCompositeDisposable()
            .add(
                interactor.getAllPings().onErrorReturnItem(ArrayList()).flatMap {
                    val dataDetails = ServerListData()
                    dataDetails.pingTimes = it
                    dataDetails.setShowLatencyInMs(interactor.getAppPreferenceInterface().showLatencyInMS)
                    dataDetails.setShowLocationHealth(
                        interactor.getAppPreferenceInterface().isShowLocationHealthEnabled
                    )
                    dataDetails.flags = flagIcons
                    dataDetails.isProUser = interactor.getAppPreferenceInterface().userStatus == 1
                    Single.fromCallable { dataDetails }
                }.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
                    .subscribeWith(object : DisposableSingleObserver<ServerListData?>() {
                        override fun onError(e: Throwable) {
                            logger.debug("Error loading static server list:$e")
                        }

                        override fun onSuccess(serverListData: ServerListData) {
                            val selection = interactor.getAppPreferenceInterface().selection
                            if (selection == LATENCY_LIST_SELECTION_MODE) {
                                regions.sortWith { o1: StaticRegion, o2: StaticRegion ->
                                    serverListData.pingTimes
                                    getPingTimeFromCity(
                                        o1.id, serverListData
                                    ) - getPingTimeFromCity(
                                        o2.id, serverListData
                                    )
                                }
                            } else if (selection == AZ_LIST_SELECTION_MODE) {
                                Collections.sort(regions, ByStaticRegionName())
                            }
                            if (regions.size > 0) {
                                logger.debug("Setting static ip adapter with " + regions.size + " items.")
                                staticRegionAdapter = StaticRegionAdapter(
                                    regions, serverListData, this@WindscribePresenterImpl
                                )
                                staticRegionAdapter?.let {
                                    windscribeView.setStaticRegionAdapter(it)
                                }
                                var deviceName = ""
                                if (regions[0].deviceName != null) {
                                    deviceName = regions[0].deviceName
                                }
                                windscribeView.showStaticIpAdapterLoadError(
                                    "",
                                    interactor.getResourceString(R.string.add_static_ip),
                                    deviceName
                                )
                            } else {
                                staticRegionAdapter?.let { staticRegionAdapter ->
                                    staticRegionAdapter.setStaticIpList(null)
                                    staticRegionAdapter.notifyDataSetChanged()
                                }
                                logger.debug(if (staticRegionAdapter != null) "Removing static ip adapter." else "Setting no static ip error.")
                                windscribeView.showStaticIpAdapterLoadError(
                                    "No Static IP's",
                                    interactor.getResourceString(R.string.add_static_ip),
                                    ""
                                )
                            }
                        }
                    })
            )
    }

    override fun logoutFromCurrentSession() {
        logger.debug("Logging user out of current session.")
        interactor.getAppPreferenceInterface().clearAllData()
        if (interactor.getVpnConnectionStateManager().isVPNActive()) {
            logger.info("VPN is active, stopping the current connection...")
            interactor.getMainScope().launch { interactor.getVPNController().disconnectAsync() }
            windscribeView.gotoLoginRegistrationActivity()
        } else {
            windscribeView.gotoLoginRegistrationActivity()
        }
    }

    override suspend fun observeNextProtocolToConnect() {
        interactor.getAutoConnectionManager().nextInLineProtocol.collectLatest { protocol ->
            setProtocolAndPortOptions(protocol)
        }
    }

    private fun setCustomConfigPortAndProtocol() {
        selectedLocation?.cityId?.let {
            interactor.getConfigFile(it).flatMap {
                return@flatMap Single.fromCallable {
                    if (WindUtilities.getConfigType(it.content) == WindUtilities.ConfigType.OpenVPN) {
                        Util.getProtocolInformationFromOpenVPNConfig(it.content)
                    } else {
                        Util.getProtocolInformationFromWireguardConfig(it.content)
                    }
                }
            }.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
                .subscribe { protocolInfo, error ->
                    if (error != null) {
                        logger.debug("Unable to get Protocol info from custom config. ${error.message}")
                    } else if (protocolInfo != null) {
                        windscribeView.setPortAndProtocol(
                            Util.getProtocolLabel(protocolInfo.protocol), protocolInfo.port
                        )
                    }
                }
        }
    }

    override suspend fun observeConnectedProtocol() {
        interactor.getAutoConnectionManager().connectedProtocol.collectLatest { protocol ->
            if (interactor.getVpnConnectionStateManager().isVPNActive()) {
                protocol?.let {
                    updatePreferredProtocol(it)
                    windscribeView.setPortAndProtocol(Util.getProtocolLabel(it.protocol), it.port)
                }
            }
        }
    }

    private fun updatePreferredProtocol(protocol: ProtocolInformation) {
        connectionOptions.isPreferred = isPreferred(protocol)
        windscribeView.uiConnectionState?.let { state ->
            state.connectionOptions = connectionOptions
            windscribeView.setLastConnectionState(state)
        }
    }

    private fun isPreferred(selectedProtocol: ProtocolInformation): Boolean {
        return interactor.getNetworkInfoManager().networkInfo?.let {
            return if (WindUtilities.getSourceTypeBlocking() == SelectedLocationType.CustomConfiguredProfile) {
                false
            } else {
                (it.isPreferredOn && selectedProtocol.protocol == it.protocol && selectedProtocol.port == it.port)
            }
        } ?: false
    }

    override suspend fun observeVPNState() {
        selectedLocation = Util.getLastSelectedLocation(appContext)
        interactor.getVpnConnectionStateManager().state.collectLatest { vpnState ->
            if (vpnState.status == VPNState.Status.Disconnected && interactor.getAppPreferenceInterface().isReconnecting && interactor.getAppPreferenceInterface().globalUserConnectionPreference) {
                return@collectLatest
            }
            lastVPNState = vpnState.status
            when (vpnState.status) {
                VPNState.Status.Connected -> {
                    windscribeView.setRefreshLayout(false)
                    vpnState.ip?.let {
                        onVpnIpReceived(it)
                    } ?: kotlin.run {
                        onVpnIpReceived("--.--.--.--")
                    }
                }

                VPNState.Status.Connecting -> onVPNConnecting()
                VPNState.Status.Disconnected -> onVPNDisconnected()
                VPNState.Status.Disconnecting -> onVPNDisconnecting()
                VPNState.Status.RequiresUserInput -> onVpnRequiresUserInput()
                VPNState.Status.InvalidSession -> windscribeView.gotoLoginRegistrationActivity()
                VPNState.Status.ProtocolSwitch -> {}
                VPNState.Status.UnsecuredNetwork -> onUnsecuredNetwork()
            }
        }
    }

    override fun onAddConfigLocation() {
        windscribeView.openFileChooser()
    }

    override fun onAddStaticIPClicked() {
        logger.info("Opening static ip URL...")
        windscribeView.openStaticIPUrl(getWebsiteLink(NetworkKeyConstants.URL_ADD_STATIC_IP))
    }

    override fun onAutoSecureInfoClick() {
        windscribeView.showDialog(interactor.getResourceString(R.string.auto_secure_description))
    }

    override fun onAutoSecureToggleClick() {
        interactor.saveWhiteListedNetwork(true)
        networkInformation?.let {
            it.isAutoSecureOn = !it.isAutoSecureOn
            interactor.getNetworkInfoManager().updateNetworkInfo(it)
        }
    }

    override fun onCheckNodeStatusClick() {
        windscribeView.openNodeStatusPage(getWebsiteLink(NODE_STATUS_URL))
    }

    override fun onCityClick(cityId: Int) {
        windscribeView.exitSearchLayout()
        logger.debug("User clicked on city.")
        selectedLocation?.cityId?.let {
            if (it == cityId && (interactor.getVpnConnectionStateManager()
                    .isVPNActive() || connectingFromServerList)
            ) {
                return@let
            }
            connectingFromServerList = true
            connectToCity(cityId)
        }
    }

    override fun onConfigFileClicked(configFile: ConfigFile) {
        if (configFile.username == null && WindUtilities.getConfigType(configFile.content) == WindUtilities.ConfigType.OpenVPN) {
            windscribeView.openProvideUsernameAndPasswordDialog(configFile)
        } else {
            connectToConfiguredLocation(configFile.getPrimaryKey())
        }
    }

    override fun onConfigFileContentReceived(
        name: String, content: String, username: String, password: String
    ) {
        val configFile = ConfigFile(0, name, content, username, password, true)
        addConfigFileToDatabase(configFile)
    }

    override fun onConnectClicked() {
        logger.debug(
            "Connection UI State: {} Last connection State: {}",
            windscribeView.uiConnectionState?.javaClass?.simpleName,
            lastVPNState
        )
        interactor.getAutoConnectionManager().stop()
        when (windscribeView.uiConnectionState) {
            is ConnectingState -> {
                stopVpnFromUI()
            }

            is ConnectedState -> {
                stopVpnFromUI()
            }

            is ConnectedAnimationState -> {}
            is ConnectingAnimationState -> {}
            is FailedProtocol -> {
                logger.debug("Stopping protocol switch service.")
                interactor.getMainScope().launch {
                    interactor.getVPNController().disconnectAsync()
                }
            }

            is UnsecuredProtocol -> {
                logger.debug("Stopping standby network service.")
                stopVpnFromUI()
            }

            else -> {
                selectedLocation?.let {
                    logger.debug("Starting Connection.")
                    val sourceType = WindUtilities.getSourceTypeBlocking()
                    if (sourceType != null) {
                        when (sourceType) {
                            SelectedLocationType.StaticIp -> connectToStaticIp(
                                it.cityId
                            )

                            SelectedLocationType.CustomConfiguredProfile -> connectToConfiguredLocation(
                                it.cityId
                            )

                            SelectedLocationType.CityLocation -> connectToCity(it.cityId)
                        }
                    }
                } ?: kotlin.run {
                    logger.debug("No saved location found. wait for server list to refresh.")
                    windscribeView.showToast("Server list is not ready.")
                }
            }
        }
    }

    override fun onConnectedAnimationCompleted() {
        selectedLocation?.let {
            windscribeView.setupLayoutConnected(
                ConnectedState(it, connectionOptions, appContext)
            )
        }
    }

    override fun onConnectingAnimationCompleted() {
        selectedLocation?.let {
            windscribeView.setupLayoutConnecting(
                ConnectingState(
                    it, connectionOptions, appContext
                )
            )
        }
    }

    override fun onConnectingAnimationCancelled() {
        selectedLocation?.let {
            windscribeView.setCountryFlag(FlagIconResource.getFlag(it.countryCode))
            windscribeView.setupLayoutConnecting(
                ConnectingState(
                    it, connectionOptions, appContext
                )
            )
        }
    }

    override fun onDisconnectIntentReceived() {
        stopVpnFromUI()
    }

    override fun onHotStart() {
        // setConnectionLayout();
        checkLoginStatus()

        // Update Notification count
        updateNotificationCount()

        interactor.getPreferenceChangeObserver().postConfigListChange()
    }

    override fun onIpClicked() {
        val blurIp = !interactor.getAppPreferenceInterface().blurIp
        interactor.getAppPreferenceInterface().blurIp = blurIp
        windscribeView.setIpBlur(blurIp)
    }

    override fun onNetworkNameClick() {
        permissionManager.withForegroundLocationPermission { error ->
            if (error != null) {
                logger.debug(error)
            } else {
                val blurNetworkName = !interactor.getAppPreferenceInterface().blurNetworkName
                interactor.getAppPreferenceInterface().blurNetworkName = blurNetworkName
                windscribeView.setNetworkNameBlur(blurNetworkName)
            }
        }
    }

    override fun onLanguageChanged() {
    }

    // UI Items onClick Methods
    override fun onMenuButtonClicked() {
        windscribeView.performButtonClickHapticFeedback()
        if (windscribeView.networkLayoutState != NetworkLayoutState.CLOSED) {
            windscribeView.setNetworkLayout(networkInformation, NetworkLayoutState.CLOSED, true)
        }
        logger.debug("Opening main menu activity...")
        windscribeView.openMenuActivity()
    }

    override fun onNetworkInfoUpdate(networkInfo: NetworkInfo?, userReload: Boolean) {
        networkInformation = networkInfo
        if (networkInformation != null && userReload) {
            if (networkInformation?.isAutoSecureOn != true) {
                logger.debug("Setting closed Preferred layout.")
                windscribeView.setNetworkLayout(
                    networkInformation, NetworkLayoutState.OPEN_1, false
                )
            } else if (networkInformation?.isPreferredOn != true) {
                logger.debug("Setting open 2 Preferred layout.")
                windscribeView.setNetworkLayout(
                    networkInformation, NetworkLayoutState.OPEN_2, false
                )
            } else {
                logger.debug("Setting open 3 Preferred layout.")
                windscribeView.setNetworkLayout(
                    networkInformation, NetworkLayoutState.OPEN_3, false
                )
            }
        } else {
            logger.debug("Setting Closed Preferred layout.")
            windscribeView.setNetworkLayout(networkInformation, NetworkLayoutState.CLOSED, true)
        }
        setProtocolAndPortOptions(interactor.getAutoConnectionManager().nextInLineProtocol.value)
    }

    private fun setProtocolAndPortOptions(protocol: ProtocolInformation?) {
        protocol?.let {
            if (interactor.getVpnConnectionStateManager().isVPNActive().not()) {
                updatePreferredProtocol(it)
                if (WindUtilities.getSourceTypeBlocking() == SelectedLocationType.CustomConfiguredProfile) {
                    setCustomConfigPortAndProtocol()
                } else {
                    windscribeView.setPortAndProtocol(
                        Util.getProtocolLabel(it.protocol), it.port
                    )
                }
            }
        }
    }

    override fun onNetworkLayoutCollapsed(checkForReconnect: Boolean) {
        if (checkForReconnect) {
            logger.debug("Network Layout collapsed.")
            val connectionPreference =
                interactor.getAppPreferenceInterface().globalUserConnectionPreference
            if (networkInformation != null && connectionPreference && WindUtilities.getSourceTypeBlocking() !== SelectedLocationType.CustomConfiguredProfile) {
                if (isNetworkInfoChanged && (networkInformation?.isAutoSecureOn == true) && networkInformation?.isPreferredOn == true) {
                    if (interactor.getVpnConnectionStateManager().isVPNConnected()) {
                        interactor.getAppPreferenceInterface().globalUserConnectionPreference = true
                        logger.debug("Preferred protocol and port info change Now connecting.")
                        interactor.getMainScope().launch {
                            interactor.getAutoConnectionManager().connectInForeground()
                        }
                    }
                }
            }
        }
        enqueueWork(appContext)
    }

    /*
     * On every network change
     * Add network to database
     * Set Ui based on network
     * */
    override fun onNetworkStateChanged() {
        if (WindUtilities.isOnline() && !interactor.getVpnConnectionStateManager()
                .isVPNActive() && !interactor.getAppPreferenceInterface().isReconnecting
        ) {
            interactor.getWorkManager().updateNodeLatencies()
        }
        setIpFromLocalStorage()
    }

    override fun onNewsFeedItemClick() {
        logger.debug("Opening news feed activity...")
        windscribeView.openNewsFeedActivity(false, -1)
    }

    override fun onPortSelected(port: String) {
        networkInformation?.let {
            it.port = port
            interactor.getNetworkInfoManager().updateNetworkInfo(it)
        }
    }

    override fun onPreferredProtocolInfoClick() {
        windscribeView.showDialog(interactor.getResourceString(R.string.preferred_protocol_description))
    }

    override fun onPreferredProtocolToggleClick() {
        networkInformation?.let {
            it.isPreferredOn = !it.isPreferredOn
            interactor.getNetworkInfoManager().updateNetworkInfo(it)
        }
    }

    override fun onProtocolSelected(protocol: String) {
        interactor.loadPortMap(object : PortMapLoadCallback {
            override fun onFinished(portMapResponse: PortMapResponse) {
                portMapResponse.let {
                    for (portMap in portMapResponse.portmap) {
                        if (protocol == portMap.heading) {
                            networkInformation?.let {
                                it.protocol = portMap.protocol
                                windscribeView.setupPortMapAdapter(
                                    it.port, portMap.ports
                                )
                                interactor.getNetworkInfoManager().updateNetworkInfo(it)
                            }
                        }
                    }
                }
            }
        })
    }

    override fun onProtocolSelected(protocolConfig: ProtocolConfig?) {
        protocolConfig?.let {
            interactor.getVPNController().connectAsync()
        }
    }

    override fun onRefreshPingsForAllServers() {
        if (canNotUpdatePings()) {
            return
        }
        logger.debug("Starting ping testing for all nodes.")
        interactor.getActivityScope().launch {
            withContext(interactor.getMainScope().coroutineContext) {
                return@withContext interactor.getLatencyRepository().updateAllServerLatencies()
            }
            windscribeView.setRefreshLayout(false)
            logger.debug("Ping testing finished successfully.")
        }
    }

    override fun onRefreshPingsForConfigServers() {
        logger.debug("Starting ping testing for custom nodes.")
        interactor.getActivityScope().launch {
            withContext(interactor.getMainScope().coroutineContext) {
                return@withContext interactor.getLatencyRepository().updateConfigLatencies()
            }
            windscribeView.setRefreshLayout(false)
            logger.debug("Ping testing finished successfully.")
        }
    }

    override fun onRefreshPingsForFavouritesServers() {
        if (canNotUpdatePings()) {
            return
        }
        logger.debug("Starting ping testing for favourite nodes.")
        interactor.getActivityScope().launch {
            withContext(interactor.getMainScope().coroutineContext) {
                return@withContext interactor.getLatencyRepository().updateFavouriteCityLatencies()
            }
            windscribeView.setRefreshLayout(false)
            logger.debug("Ping testing finished successfully.")
        }
    }

    override fun onRefreshPingsForStaticServers() {
        if (canNotUpdatePings()) {
            return
        }
        logger.debug("Starting ping testing for static nodes.")
        interactor.getActivityScope().launch {
            withContext(interactor.getMainScope().coroutineContext) {
                return@withContext interactor.getLatencyRepository().updateStaticIpLatency()
            }
            windscribeView.setRefreshLayout(false)
            logger.debug("Ping testing finished successfully.")
        }
    }

    override fun onRefreshPingsForStreamingServers() {
        if (canNotUpdatePings()) {
            return
        }
        logger.debug("Starting ping testing for streaming nodes.")
        interactor.getActivityScope().launch {
            withContext(interactor.getMainScope().coroutineContext) {
                return@withContext interactor.getLatencyRepository()
                    .updateStreamingServerLatencies()
            }
            windscribeView.setRefreshLayout(false)
            logger.debug("Ping testing finished successfully.")
        }
    }

    /*
     During destructive migration if application had no internet user can use reload the server list.
     */
    override fun onReloadClick() {
        logger.debug("User clicked on reload server list.")
        windscribeView.showRecyclerViewProgressBar()
        interactor.getMainScope().launch { interactor.getVPNController().disconnectAsync() }
        interactor.getAppPreferenceInterface().setUserAccountUpdateRequired(true)
        interactor.getCompositeDisposable().add(
            interactor.getConnectionDataUpdater().update()
                .andThen(interactor.getServerListUpdater().update())
                .andThen(Completable.fromAction { interactor.getUserRepository().reload() })
                .subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(object : DisposableCompletableObserver() {
                    override fun onComplete() {
                        windscribeView.hideRecyclerViewProgressBar()
                        logger.debug("Server list, connection data and static ip data is updated successfully.")
                        windscribeView.showToast("Updated successfully.")
                        interactor.getAppPreferenceInterface().migrationRequired = false
                        interactor.getAppPreferenceInterface().setUserAccountUpdateRequired(false)
                    }

                    override fun onError(e: Throwable) {
                        windscribeView.hideRecyclerViewProgressBar()
                        logger.error("Server list update failed.$e")
                        windscribeView.showToast("Check your internet connection.")
                        windscribeView.showReloadError("Error loading server list")
                    }
                })
        )
    }

    override fun onRenewPlanClicked() {
        when (interactor.getUserAccountStatus()) {
            ACCOUNT_STATUS_OK -> {
                logger.info("Account status okay, opening upgrade activity...")
                windscribeView.openUpgradeActivity()
            }

            UserStatusConstants.ACCOUNT_STATUS_BANNED -> {
                logger.info("Account status banned!")
                windscribeView.showToast("(OnClick) Placeholder for learning more")
            }

            UserStatusConstants.ACCOUNT_STATUS_EXPIRED -> {
                logger.info("Account status is expired, opening upgrade activity...")
                windscribeView.openUpgradeActivity()
            }
        }
    }

    override fun onSearchButtonClicked() {
        adapter?.let { adapter ->
            adapter.groups?.let {
                val searchGroups = adapter.groupsList
                streamingNodeAdapter?.groupsList?.let { groupsList ->
                    searchGroups.addAll(groupsList)
                }
                windscribeView.setupSearchLayout(
                    searchGroups, adapter.serverListData, this@WindscribePresenterImpl
                )
            }
        }
    }

    override fun onShowAllServerListClicked() {
        windscribeView.showListBarSelectTransition(R.id.img_server_list_all)
    }

    override fun onShowConfigLocListClicked() {
        windscribeView.showListBarSelectTransition(R.id.img_config_loc_list)
        resetConfigEditState()
    }

    override fun onShowFavoritesClicked() {
        windscribeView.showListBarSelectTransition(R.id.img_server_list_favorites)
    }

    override fun onShowFlixListClicked() {
        windscribeView.showListBarSelectTransition(R.id.img_server_list_flix)
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun resetConfigEditState() {
        var itemsBeingEdited = 0
        configAdapter?.configFiles?.filter {
            it.type == 2
        }?.forEach {
            it.type = 1
            itemsBeingEdited++
        }
        if (itemsBeingEdited > 0) {
            configAdapter?.notifyDataSetChanged()
        }
    }

    override fun onShowStaticIpListClicked() {
        windscribeView.showListBarSelectTransition(R.id.img_static_ip_list)
    }

    /*
     * Connect to static IP
     * @param StaticIpID
     * */
    override fun onStaticIpClick(staticIpId: Int) {
        logger.debug("User clicked on static ip from list")
        connectToStaticIp(staticIpId)
    }

    override fun onUnavailableRegion() {
        windscribeView.exitSearchLayout()
        windscribeView.setUpLayoutForNodeUnderMaintenance()
    }

    override fun onUpgradeClicked() {
        logger.debug("Opening upgrade activity...")
        windscribeView.openUpgradeActivity()
    }

    private fun onVPNConnecting() {
        windscribeView.setRefreshLayout(false)
        selectedLocation?.let {
            if (windscribeView.uiConnectionState !is ConnectingAnimationState) {
                logger.debug("Changing UI state to connecting.")
                windscribeView.startVpnConnectingAnimation(
                    ConnectingAnimationState(
                        it, connectionOptions, appContext
                    )
                )
            } else {
                updateLocationUI(it, true)
            }
        }
    }

    private fun onUnsecuredNetwork() {
        selectedLocation?.let {
            windscribeView.setupLayoutUnsecuredNetwork(
                UnsecuredProtocol(
                    it, connectionOptions, appContext
                )
            )
        }
    }

    private fun onVPNDisconnected() {
        connectingFromServerList = false
        if (interactor.getAppPreferenceInterface().isReconnecting) return
        if (windscribeView.uiConnectionState is ConnectedState) {
            windscribeView.performConfirmConnectionHapticFeedback()
        }
        if (windscribeView.uiConnectionState !is DisconnectedState) {
            logger.debug("Changing UI state to Disconnected")
            selectedLocation?.let {
                windscribeView.clearConnectingAnimation()
                windscribeView.setupLayoutDisconnected(
                    DisconnectedState(
                        it, connectionOptions, appContext
                    )
                )
                setIpAddress()
                updateLocationUI(it, false)
            }
        }
    }

    private fun onVPNDisconnecting() {
        windscribeView.setupLayoutDisconnecting(
            interactor.getResourceString(R.string.disconnecting),
            interactor.getColorResource(R.color.colorLightBlue)
        )
    }

    private fun onVpnIpReceived(ip: String) {
        logger.info("Connection with the server is established.")
        connectingFromServerList = false
        interactor.getAppPreferenceInterface().isReconnecting = false
        windscribeView.setIpAddress(ip.trim { it <= ' ' })
        windscribeView.performConfirmConnectionHapticFeedback()
        interactor.getCompositeDisposable().add(getSavedLocation().filter {
            interactor.getVpnConnectionStateManager().isVPNActive()
        }.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
            .subscribe({ location: LastSelectedLocation -> onLastSelectedLocationLoaded(location) }) { throwable: Throwable ->
                onLastSelectedLocationLoadFailed(
                    throwable
                )
            })
    }

    private fun onVpnRequiresUserInput() {
        val locationSourceType = WindUtilities.getSourceTypeBlocking()
        if (locationSourceType === SelectedLocationType.CustomConfiguredProfile) {
            val cityId = interactor.getLocationProvider().selectedCity.value
            interactor.getCompositeDisposable().add(
                interactor.getConfigFile(cityId).subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeWith(object : DisposableSingleObserver<ConfigFile?>() {
                        override fun onError(e: Throwable) {}
                        override fun onSuccess(configFile: ConfigFile) {
                            windscribeView.openProvideUsernameAndPasswordDialog(configFile)
                        }
                    })
            )
        }
    }

    override fun registerNetworkInfoListener() {
        interactor.getNetworkInfoManager().addNetworkInfoListener(this)
        interactor.getNetworkInfoManager().reload()
    }

    override fun reloadNetworkInfo() {
        interactor.getNetworkInfoManager().reload(true)
    }

    /*
     * Remove from favourite list
     * @Param cityID
     * */
    override fun removeFromFavourite(
        cityId: Int,
        position: Int,
        adapter: RecyclerView.Adapter<RecyclerView.ViewHolder>
    ) {
        val favourite = Favourite()
        favourite.id = cityId
        interactor.getCompositeDisposable()
            .add(Completable.fromAction { interactor.deleteFavourite(favourite) }
                .andThen(interactor.getFavourites()).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ favourites: List<Favourite> ->
                    resetAdapters(
                        favourites,
                        interactor.getResourceString(R.string.remove_from_favourites),
                        position,
                        adapter
                    )
                }) { throwable: Throwable ->
                    logger.debug(
                        String.format(
                            "Failed to remove from favourites. : %s", throwable.localizedMessage
                        )
                    )
                    windscribeView.showToast("Failed to remove from favourites.")
                })
    }

    override fun saveLastSelectedTabIndex(index: Int) {
        interactor.getAppPreferenceInterface().saveLastSelectedServerTabIndex(index)
    }

    override fun saveRateDialogPreference(type: Int) {
        interactor.saveRateAppPreference(type)
        interactor.setRateDialogUpdateTime()
    }

    override fun setMainCustomConstraints() {
        val customAppBackground = interactor.getAppPreferenceInterface().isCustomBackground
        windscribeView.setMainConstraints(customAppBackground)
    }

    override fun setProtocolAdapter(protocol: String) {
        interactor.loadPortMap(object : PortMapLoadCallback {
            override fun onFinished(portMapResponse: PortMapResponse) {
                portMapResponse.let {
                    val protocols: MutableList<String> = ArrayList()
                    var heading: String? = null
                    for (portMap in it.portmap) {
                        protocols.add(portMap.heading)
                        if (protocol == portMap.protocol) {
                            heading = portMap.heading
                        }
                    }
                    heading?.let {
                        windscribeView.setupProtocolAdapter(
                            heading, protocols.toTypedArray()
                        )
                    }
                }
            }
        })
    }

    override fun setScrollTo(scrollTo: Int) {
        windscribeView.scrollTo(scrollTo)
    }

    override suspend fun observerSelectedLocation() {
        interactor.getCompositeDisposable().add(Single.fromCallable {
            return@fromCallable Util.getLastSelectedLocation(appContext)
                ?: throw Exception("No saved location found")
        }.onErrorResumeNext(interactor.getLocationProvider().bestLocation.flatMap {
            val coordinatesArray = it.city.coordinates.split(",".toRegex()).toTypedArray()
            val location = LastSelectedLocation(
                it.city.id,
                it.city.nodeName,
                it.city.nickName,
                it.region.countryCode,
                coordinatesArray[0],
                coordinatesArray[1]
            )
            Util.saveSelectedLocation(location)
            return@flatMap Single.fromCallable { location }
        }).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe({
            selectedLocation = it
            updateLocationUI(selectedLocation, true)
        }, {})
        )
    }

    /*
     * Set theme Dark/Light
     * */
    override fun setTheme(context: Context) {
        val savedThem = interactor.getAppPreferenceInterface().selectedTheme
        logger.debug("Setting theme to $savedThem")
        if (savedThem == PreferencesKeyConstants.DARK_THEME) {
            context.setTheme(R.style.DarkTheme)
        } else {
            context.setTheme(R.style.LightTheme)
        }
    }

    private fun setPreferredNetworkLayout() {
        if (windscribeView.networkLayoutState === NetworkLayoutState.CLOSED) {
            if (networkInformation?.isAutoSecureOn != true) {
                windscribeView.setNetworkLayout(
                    networkInformation, NetworkLayoutState.OPEN_1, false
                )
            } else if (networkInformation?.isPreferredOn != true) {
                windscribeView.setNetworkLayout(
                    networkInformation, NetworkLayoutState.OPEN_2, false
                )
            } else {
                windscribeView.setNetworkLayout(
                    networkInformation, NetworkLayoutState.OPEN_3, false
                )
            }
        } else {
            windscribeView.setNetworkLayout(
                networkInformation, NetworkLayoutState.CLOSED, false
            )
        }
    }

    override fun onCollapseExpandIconClick() {
        try {
            WindUtilities.getNetworkName()
            setPreferredNetworkLayout()
        } catch (e: WindScribeException) {
            logger.debug(e.message)
            when (e) {
                is NoNetworkException -> {
                    windscribeView.setNetworkLayout(null, NetworkLayoutState.CLOSED, false)
                    windscribeView.showToast("No Network")
                }

                is BackgroundLocationPermissionNotAvailable, is NoLocationPermissionException -> {
                    if (!isGPSEnabled(appContext)) {
                        windscribeView.showToast("Location service is disabled. Enable it to use preferred protocols.")
                    }
                    windscribeView.setNetworkLayout(null, NetworkLayoutState.CLOSED, false)
                    permissionManager.withForegroundLocationPermission { error ->
                        if (error != null) {
                            logger.debug(error)
                        } else {
                            interactor.getNetworkInfoManager().reload(true)
                        }
                    }
                }

                else -> {
                    logger.info("Unknown error.")
                }
            }
        } catch (e: Exception) {
            logger.info(e.toString())
        }
    }

    private fun isGPSEnabled(context: Context): Boolean {
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return lm.isProviderEnabled(LocationManager.GPS_PROVIDER)
    }

    override fun updateConfigFile(configFile: ConfigFile) {
        interactor.getCompositeDisposable().add(
            interactor.addConfigFile(configFile).observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribeWith(object : DisposableCompletableObserver() {
                    @SuppressLint("NotifyDataSetChanged")
                    override fun onComplete() {
                        windscribeView.showToast("Updated profile")
                        configAdapter?.notifyDataSetChanged()
                    }

                    override fun onError(e: Throwable) {
                        logger.error(e.toString())
                        windscribeView.showToast("Error updating config file.")
                    }
                })
        )
    }

    override fun updateConfigFileConnect(configFile: ConfigFile) {
        interactor.getCompositeDisposable().add(
            interactor.addConfigFile(configFile).observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribeWith(object : DisposableCompletableObserver() {
                    override fun onComplete() {
                        connectToConfiguredLocation(configFile.getPrimaryKey())
                        interactor.getPreferenceChangeObserver().postConfigListChange()
                    }

                    override fun onError(e: Throwable) {
                        logger.error(e.toString())
                        windscribeView.showToast("Error updating config file.")
                    }
                })
        )
    }

    override fun updateLatency() {
        if (adapter == null) {
            return
        }
        interactor.getCompositeDisposable()
            .add(
                interactor.getAllPings().flatMap { pingTimes: List<PingTime> ->
                    interactor.getLocationProvider().bestLocation.flatMap { cityAndRegion: CityAndRegion ->
                        Single.fromCallable {
                            Pair(
                                pingTimes, cityAndRegion
                            )
                        }
                    }
                }.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
                    .subscribeWith(object :
                        DisposableSingleObserver<Pair<List<PingTime>, CityAndRegion>>() {
                        override fun onError(e: Throwable) {}
                        override fun onSuccess(pair: Pair<List<PingTime>, CityAndRegion>) {
                            adapter?.let {
                                val serverListData = it.serverListData
                                serverListData.pingTimes = pair.first
                                serverListData.bestLocation = pair.second
                                updateServerListData(serverListData)
                            }
                        }
                    })
            )
    }

    override fun userHasAccess(): Boolean {
        return interactor.getAppPreferenceInterface().sessionHash != null
    }

    private fun validIpAddress(str: String?): Boolean {
        val addressString = IPAddressString(str)
        return try {
            addressString.toAddress()
            true
        } catch (e: AddressStringException) {
            logger.debug(e.localizedMessage)
            false
        }
    }

    private fun addConfigFileToDatabase(configFile: ConfigFile) {
        windscribeView.showRecyclerViewProgressBar()
        interactor.getCompositeDisposable()
            .add(
                interactor.getMaxPrimaryKey().onErrorReturnItem(20000)
                    .flatMapCompletable { max: Int ->
                        configFile.setPrimaryKey(max + 1)
                        interactor.addConfigFile(configFile)
                    }.observeOn(AndroidSchedulers.mainThread()).subscribeOn(Schedulers.io())
                    .subscribeWith(object : DisposableCompletableObserver() {
                        override fun onComplete() {
                            logger.debug("Config added successfully to database.")
                            interactor.getActivityScope().launch {
                                withContext(interactor.getMainScope().coroutineContext) {
                                    interactor.getLatencyRepository().updateConfigLatencies()
                                }
                                windscribeView.showToast(interactor.getResourceString(R.string.config_added))
                                interactor.getPreferenceChangeObserver().postConfigListChange()
                            }
                        }

                        override fun onError(e: Throwable) {
                            windscribeView.hideRecyclerViewProgressBar()
                            logger.error(e.toString())
                            windscribeView.showToast("Error adding config file.")
                        }
                    })
            )
    }

    private fun addNotificationChangeListener() {
        interactor.getCompositeDisposable().add(
            interactor.getNotifications(interactor.getAppPreferenceInterface().userName)
                .subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(object : DisposableSubscriber<List<PopupNotificationTable>>() {
                    override fun onComplete() {
                        logger.debug("Registering notification listener finishing.")
                    }

                    override fun onError(t: Throwable) {
                        logger.debug(
                            "Error reading popup notification table. StackTrace: " + instance.convertThrowableToString(
                                t
                            )
                        )
                    }

                    override fun onNext(popupNotificationTables: List<PopupNotificationTable>) {
                        updateNotificationCount()
                        checkForPopNotification(popupNotificationTables)
                    }
                })
        )
    }

    private fun calculateFlagDimensions() {
        interactor.getAppPreferenceInterface().flagViewWidth = windscribeView.flagViewWidth
        interactor.getAppPreferenceInterface().flagViewHeight = windscribeView.flagViewHeight
    }

    private fun canNotUpdatePings(): Boolean {
        if (!WindUtilities.isOnline()) {
            windscribeView.setRefreshLayout(false)
            windscribeView.showToast("No network available")
            return true
        } else if (interactor.getVpnConnectionStateManager().isVPNActive()) {
            windscribeView.setRefreshLayout(false)
            windscribeView.showToast("Disconnect from VPN")
            return true
        }
        return false
    }

    /*
     * Check if we can connect
     * */
    private fun checkEligibility(isPro: Int, isStaticIp: Boolean, serverStatus: Int): Boolean {
        // Check Internet
        if (!windscribeView.isConnectedToNetwork) {
            logger.info("Error: no internet available.")
            windscribeView.showToast(interactor.getResourceString(R.string.no_internet))
            return false
        }

        // Does user own this location
        if (interactor.getAppPreferenceInterface().userStatus != UserStatusConstants.USER_STATUS_PREMIUM && isPro == 1 && !isStaticIp) {
            logger.info("Location is pro but user is not. Opening upgrade activity.")
            windscribeView.openUpgradeActivity()
            return false
        }

        // User account status
        if (interactor.getUserAccountStatus() == UserStatusConstants.ACCOUNT_STATUS_EXPIRED && !isStaticIp) {
            logger.info("Error: account status is expired.")
            val resetDate = interactor.getUserRepository().user.value?.nextResetDate() ?: ""
            windscribeView.setupAccountStatusExpired(resetDate)
            return false
        }
        if (interactor.getUserAccountStatus() == UserStatusConstants.ACCOUNT_STATUS_BANNED) {
            logger.info("Error: account status is banned.")
            windscribeView.setupAccountStatusBanned()
            return false
        }

        // Set Static status
        interactor.getAppPreferenceInterface().setConnectingToStaticIP(isStaticIp)
        interactor.getAppPreferenceInterface().setConnectingToConfiguredLocation(false)

        // Check Network security
        val whiteListOverride = interactor.getAppPreferenceInterface().whiteListedNetwork
        networkInformation?.let {
            if (!it.isAutoSecureOn && whiteListOverride != null) {
                interactor.saveWhiteListedNetwork(true)
            }
        }
        if (serverStatus == NetworkKeyConstants.SERVER_STATUS_TEMPORARILY_UNAVAILABLE) {
            logger.info("Error: Server is temporary unavailable.")
            windscribeView.showToast("Location temporary unavailable.")
            return false
        }
        return true
    }

    private fun checkForPopNotification(popupNotificationTables: List<PopupNotificationTable>) {
        for (popupNotification in popupNotificationTables) {
            val alreadySeen = interactor.getAppPreferenceInterface()
                .isNotificationAlreadyShown(popupNotification.notificationId.toString())
            if (!alreadySeen && popupNotification.popUpStatus == 1) {
                logger.info("New popup notification received, showing notification...")
                interactor.getAppPreferenceInterface()
                    .saveNotificationId(popupNotification.notificationId.toString())
                windscribeView.openNewsFeedActivity(true, popupNotification.notificationId)
                break
            }
        }
    }

    private fun checkLoginStatus() {
        val session = interactor.getAppPreferenceInterface().sessionHash
        if (session == null) {
            logoutFromCurrentSession()
        }
    }

    /*
     * Gets city node
     * Check if we can connect
     * start connection
     * @Param ID
     * */
    private fun connectToCity(cityId: Int) {
        interactor.getCompositeDisposable().add(
            interactor.getCityAndRegionByID(cityId).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(object : DisposableSingleObserver<CityAndRegion?>() {
                    override fun onError(e: Throwable) {
                        logger.debug("Could not find selected location in database.")
                        windscribeView.showToast("Error")
                    }

                    override fun onSuccess(cityAndRegion: CityAndRegion) {
                        val serverStatus = cityAndRegion.region.status
                        val eligibleToConnect = checkEligibility(
                            cityAndRegion.city.pro, false, serverStatus
                        )
                        if (eligibleToConnect) {
                            interactor.getAppPreferenceInterface().globalUserConnectionPreference =
                                true
                            interactor.getAppPreferenceInterface().setConnectingToStaticIP(false)
                            interactor.getAppPreferenceInterface()
                                .setConnectingToConfiguredLocation(false)
                            val coordinatesArray =
                                cityAndRegion.city.coordinates.split(",".toRegex()).toTypedArray()
                            selectedLocation = LastSelectedLocation(
                                cityAndRegion.city.getId(),
                                cityAndRegion.city.nodeName,
                                cityAndRegion.city.nickName,
                                cityAndRegion.region.countryCode,
                                coordinatesArray[0],
                                coordinatesArray[1]
                            )
                            updateLocationUI(selectedLocation, false)
                            logger.debug("Attempting to connect")
                            interactor.getMainScope().launch {
                                interactor.getAutoConnectionManager().connectInForeground()
                            }
                        } else {
                            logger.error("User can not connect to location right now.")
                        }
                    }
                })
        )
    }

    private fun connectToConfiguredLocation(id: Int) {
        interactor.getCompositeDisposable().add(
            interactor.getConfigFile(id).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(object : DisposableSingleObserver<ConfigFile?>() {
                    override fun onError(e: Throwable) {
                        windscribeView.showToast("Error connecting to location")
                    }

                    override fun onSuccess(configFile: ConfigFile) {
                        interactor.getLocationProvider().setSelectedCity(configFile.getPrimaryKey())
                        selectedLocation = LastSelectedLocation(
                            configFile.getPrimaryKey(), "Custom Config", configFile.name, "", "", ""
                        )
                        updateLocationUI(selectedLocation, false)
                        interactor.getAppPreferenceInterface().globalUserConnectionPreference = true
                        interactor.getAppPreferenceInterface()
                            .setConnectingToConfiguredLocation(true)
                        interactor.getAppPreferenceInterface().setConnectingToStaticIP(false)
                        interactor.getVPNController().connectAsync()
                    }
                })
        )
    }

    private fun connectToStaticIp(staticId: Int) {
        interactor.getCompositeDisposable().add(
            interactor.getStaticRegionByID(staticId).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(object : DisposableSingleObserver<StaticRegion?>() {
                    override fun onError(e: Throwable) {
                        logger.debug("Could not find static ip in database")
                        windscribeView.showToast("Error connecting to Location")
                    }

                    override fun onSuccess(staticRegion: StaticRegion) {
                        val eligibleToConnect = checkEligibility(1, true, 1)
                        if (eligibleToConnect) {
                            interactor.getAppPreferenceInterface().globalUserConnectionPreference =
                                true
                            interactor.getAppPreferenceInterface().setConnectingToStaticIP(true)
                            interactor.getAppPreferenceInterface()
                                .setConnectingToConfiguredLocation(false)
                            selectedLocation = LastSelectedLocation(
                                staticRegion.id,
                                staticRegion.cityName,
                                staticRegion.staticIp,
                                staticRegion.countryCode,
                                "",
                                ""
                            )
                            updateLocationUI(selectedLocation, false)
                            logger.debug("Attempting to connect..")
                            interactor.getMainScope().launch {
                                interactor.getAutoConnectionManager().connectInForeground()
                            }
                        } else {
                            logger.error("User can not connect to location right now.")
                        }
                    }
                })
        )
    }

    private fun elapsedOneDayAfterLogin(): Boolean {
        val milliSeconds1 = interactor.getAppPreferenceInterface().loginTime?.time ?: Date().time
        val milliSeconds2 = Date().time
        val periodSeconds = (milliSeconds2 - milliSeconds1) / 1000
        val elapsedDays = periodSeconds / 60 / 60 / 24
        return elapsedDays > 0
    }

    private fun setIpAddress() {
        if (windscribeView.isConnectedToNetwork) {
            interactor.getCompositeDisposable().add(
                interactor.getApiCallManager().checkConnectivityAndIpAddress()
                    .subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
                    .subscribe({ response ->
                        response.dataClass?.let {
                            if (validIpAddress(it.userIp)) {
                                windscribeView.setIpAddress(getModifiedIpAddress(it.userIp))
                            }
                        }
                        response.errorClass?.let {
                            logger.error("Server returned error response when getting user ip.")
                            windscribeView.setIpAddress("---.---.---.---")
                        }
                    }, {
                        logger.error("Network call to get ip failed ${it.message}")
                        windscribeView.setIpAddress("---.---.---.---")
                    })
            )
        } else {
            logger.debug("Network is not available. Ip update failed...")
            windscribeView.setIpAddress("---.---.---.---")
        }
    }

    private fun getModifiedIpAddress(ipResponse: String): String {
        var ipAddress: String?
        if (ipResponse.length >= 32) {
            logger.info("Ipv6 address. Truncating and saving ip data...")
            ipAddress = ipResponse.replace("0000".toRegex(), "0")
            ipAddress = ipAddress.replace("000".toRegex(), "")
            ipAddress = ipAddress.replace("00".toRegex(), "")
        } else {
            ipAddress = ipResponse
        }
        interactor.getAppPreferenceInterface()
            .saveResponseStringData(PreferencesKeyConstants.USER_IP, ipAddress)
        return ipAddress
    }

    private fun getPingTimeFromCity(id: Int, serverListData: ServerListData): Int {
        return serverListData.pingTimes.let {
            return@let it.firstOrNull { ping -> ping.id == id }
        }?.pingTime ?: -1
    }

    private fun getTotal(cities: List<City>, serverListData: ServerListData): Int {
        var total = 0
        var index = 0
        for (city in cities) {
            for (pingTime in serverListData.pingTimes) {
                if (pingTime.id == city.getId()) {
                    total += pingTime.getPingTime()
                    index++
                }
            }
        }
        if (index == 0) {
            return 2000
        }
        val average = total / index
        return if (average == -1) {
            2000
        } else average
    }

    private val isNetworkInfoChanged: Boolean
        get() {
            logger.debug(interactor.getAppPreferenceInterface().selectedProtocol)
            logger.debug(networkInformation.toString())
            return (interactor.getAppPreferenceInterface().selectedProtocol != networkInformation?.protocol) or (interactor.getAppPreferenceInterface().selectedPort != networkInformation?.port)
        }

    private fun onLastSelectedLocationLoadFailed(throwable: Throwable) {
        logger.error(
            "Error getting connected profile.StackTrace: " + instance.convertThrowableToString(
                throwable
            )
        )
        selectedLocation?.let {
            windscribeView.startVpnConnectedAnimation(
                ConnectedAnimationState(it, connectionOptions, appContext)
            )
            updateLocationUI(it, true)
        }
    }

    private fun onLastSelectedLocationLoaded(location: LastSelectedLocation) {
        selectedLocation = location
        selectedLocation?.let {
            windscribeView.startVpnConnectedAnimation(
                ConnectedAnimationState(
                    it, connectionOptions, appContext
                )
            )
        }
        interactor.getCompositeDisposable().add(Single.fromCallable {
            interactor.getLocationProvider().setSelectedCity(location.cityId)
            return@fromCallable interactor.getAppPreferenceInterface().connectedFlagPath ?: ""
        }.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
            .subscribe { flagPath: String ->
                if (interactor.getAppPreferenceInterface().isCustomBackground) {
                    if (flagPath.isEmpty()) {
                        windscribeView.setCountryFlag(R.drawable.dummy_flag)
                    } else {
                        windscribeView.setupLayoutForCustomBackground(flagPath)
                    }
                }
                windscribeView.updateLocationName(location.nodeName, location.nickName)
            })
    }

    private fun onNotificationResponse(windNotifications: List<WindNotification>) {
        var count = 0
        for ((notificationId) in windNotifications) {
            if (!interactor.getAppPreferenceInterface().isNotificationAlreadyShown(
                    notificationId.toString()
                )
            ) {
                count++
            }
        }
        windscribeView.showNotificationCount(count)
    }

    private fun onNotificationResponseError() {
        logger.error("Error updating notification count: setting notification count to 0")
        windscribeView.showNotificationCount(0)
    }

    /*
     * Reset adapters on data change
     * */
    @SuppressLint("NotifyDataSetChanged")
    private fun resetAdapters(
        favourites: List<Favourite>,
        message: String,
        position: Int,
        changedAdapter: RecyclerView.Adapter<RecyclerView.ViewHolder>
    ) {
        logger.debug(message)
        windscribeView.showToast(message)
        logger.debug("Resetting list adapters.")
        adapter?.serverListData?.favourites = favourites
        streamingNodeAdapter?.serverListData?.favourites = favourites
        changedAdapter.notifyItemChanged(position)
        if (changedAdapter !is RegionsAdapter) {
            adapter?.notifyDataSetChanged()
        }
        if (changedAdapter !is StreamingNodeAdapter) {
            streamingNodeAdapter?.notifyDataSetChanged()
        }
        adapter?.serverListData?.let {
            setFavouriteServerView(it)
        }
        adapter?.let {
            windscribeView.updateSearchAdapter(it.serverListData)
        }
    }

    private fun setAllServerView(
        regionAndCities: List<RegionAndCities>, serverListData: ServerListData
    ) {
        // All Server list
        val normalGroups: MutableList<Group> = ArrayList()
        // Streaming server list
        val streamingGroups: MutableList<Group> = ArrayList()

        // Populate normal and streaming regions
        for (regionAndCity in regionAndCities) {
            val total = getTotal(regionAndCity.cities, serverListData)
            Collections.sort(regionAndCity.cities, ByCityName())
            if (regionAndCity.region != null && (regionAndCity.region.locationType == "streaming")) {
                streamingGroups.add(
                    Group(
                        regionAndCity.region.name, regionAndCity.region, regionAndCity.cities, total
                    )
                )
            } else if (regionAndCity.region != null) {
                normalGroups.add(
                    Group(
                        regionAndCity.region.name, regionAndCity.region, regionAndCity.cities, total
                    )
                )
            }
        }

        // Sort Normal regions
        val selection = interactor.getAppPreferenceInterface().selection
        if (selection == LATENCY_LIST_SELECTION_MODE) {
            Collections.sort(normalGroups, ByLatency())
        } else if (selection == AZ_LIST_SELECTION_MODE) {
            Collections.sort(normalGroups, ByRegionName())
        }
        if (selection == LATENCY_LIST_SELECTION_MODE) {
            Collections.sort(streamingGroups, ByLatency())
        } else if (selection == AZ_LIST_SELECTION_MODE) {
            Collections.sort(streamingGroups, ByRegionName())
        }
        // Add best location to normal region adapter
        normalGroups.add(0, Group("Best Location", null, null, 0))

        // Normal region adapter
        adapter = RegionsAdapter(normalGroups, serverListData, this)
        windscribeView.setAdapter(adapter!!)
        // Streaming Adapter
        streamingNodeAdapter = StreamingNodeAdapter(streamingGroups, serverListData, this)
        windscribeView.setStreamingNodeAdapter(streamingNodeAdapter!!)
        // Check for errors.
        if (normalGroups.size <= 1) {
            windscribeView.showReloadError("Error loading server list")
        }
        windscribeView.hideRecyclerViewProgressBar()
    }

    private fun setFavouriteServerView(serverListData: ServerListData) {
        val favIds = IntArray(serverListData.favourites.size)
        for (i in serverListData.favourites.indices) {
            favIds[i] = serverListData.favourites[i].id
        }
        interactor.getCompositeDisposable().add(
            interactor.getCityByID(favIds).observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribeWith(object : DisposableSingleObserver<List<City>?>() {
                    override fun onError(e: Throwable) {
                        logger.error("Error setting favourite adapter.")
                        windscribeView.setFavouriteAdapter(null)
                        windscribeView.showFavouriteAdapterLoadError(
                            interactor.getResourceString(R.string.no_favourites)
                        )
                    }

                    override fun onSuccess(cities: List<City>) {
                        // Sort Normal regions
                        val selection = interactor.getAppPreferenceInterface().selection
                        if (selection == LATENCY_LIST_SELECTION_MODE) {

                            Collections.sort(cities) { o1: City, o2: City ->
                                serverListData.pingTimes
                                getPingTimeFromCity(
                                    o1.getId(), serverListData
                                ) - getPingTimeFromCity(
                                    o2.getId(), serverListData
                                )
                            }
                        } else if (selection == AZ_LIST_SELECTION_MODE) {
                            Collections.sort(cities, ByCityName())
                        }
                        if (cities.isNotEmpty()) {
                            favouriteAdapter = FavouriteAdapter(
                                cities, serverListData, this@WindscribePresenterImpl
                            )
                            windscribeView.setFavouriteAdapter(favouriteAdapter!!)
                        } else {
                            favouriteAdapter = null
                            windscribeView.setFavouriteAdapter(null)
                            windscribeView.showFavouriteAdapterLoadError(
                                interactor.getResourceString(R.string.no_favourites)
                            )
                        }
                    }
                })
        )
    }

    private fun setIpFromLocalStorage() {
        val ipAddress = interactor.getAppPreferenceInterface()
            .getResponseString(PreferencesKeyConstants.USER_IP)
        if (ipAddress != null && interactor.getVpnConnectionStateManager().isVPNActive()) {
            windscribeView.setIpAddress(ipAddress)
        }
        if (!interactor.getVpnConnectionStateManager().isVPNActive() || ipAddress == null) {
            windscribeView.setIpAddress("---.---.---.---")
            setIpAddress()
        }
    }


    var disconnectJob: Job? = null
    private fun stopVpnFromUI() {
        logger.debug("Disconnecting using connect button.")
        disconnectJob = interactor.getMainScope().launch {
            interactor.saveWhiteListedNetwork(true)
            interactor.getAppPreferenceInterface().globalUserConnectionPreference = false
            interactor.getAppPreferenceInterface().isReconnecting = false
            interactor.getVPNController().disconnectAsync()
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun updateServerListData(serverListData: ServerListData) {
        if (adapter != null) {
            adapter?.serverListData = serverListData
            adapter?.notifyDataSetChanged()
        }
        if (favouriteAdapter != null) {
            favouriteAdapter?.setDataDetails(serverListData)
            favouriteAdapter?.notifyDataSetChanged()
        }
        if (streamingNodeAdapter != null) {
            streamingNodeAdapter?.serverListData = serverListData
            streamingNodeAdapter?.notifyDataSetChanged()
        }
        if (staticRegionAdapter != null) {
            staticRegionAdapter?.setDataDetails(serverListData)
            staticRegionAdapter?.notifyDataSetChanged()
        }
    }

    private fun updateLocationUI(lastSelectedLocation: LastSelectedLocation?, updateFlag: Boolean) {
        if (lastSelectedLocation != null) {
            // Save city and update location
            interactor.getLocationProvider().setSelectedCity(lastSelectedLocation.cityId)
            windscribeView.updateLocationName(
                lastSelectedLocation.nodeName, lastSelectedLocation.nickName
            )
            // Custom flag
            val customBackground = interactor.getAppPreferenceInterface().isCustomBackground
            if (customBackground) {
                val path = if (interactor.getVpnConnectionStateManager()
                        .isVPNActive()
                ) interactor.getAppPreferenceInterface().connectedFlagPath else interactor.getAppPreferenceInterface().disConnectedFlagPath
                path?.let {
                    windscribeView.setupLayoutForCustomBackground(path)
                } ?: kotlin.run {
                    windscribeView.setCountryFlag(R.drawable.dummy_flag)
                }
            } else {
                // Country flag
                if (updateFlag && flagIcons.containsKey(lastSelectedLocation.countryCode)) {
                    flagIcons[lastSelectedLocation.countryCode]?.let {
                        windscribeView.setCountryFlag(it)
                    }
                }
            }
            // Rebuild state if not available.
            if (windscribeView.uiConnectionState == null) {
                windscribeView.setLastConnectionState(
                    DisconnectedState(
                        lastSelectedLocation, connectionOptions, appContext
                    )
                )
            }
        }
    }

    private fun updateNotificationCount() {
        interactor.getCompositeDisposable()
            .add(interactor.getWindNotifications().subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ windNotifications: List<WindNotification> ->
                    onNotificationResponse(
                        windNotifications
                    )
                }) { onNotificationResponseError() })
    }

    private fun setAccountStatus(user: User) {
        when (user.accountStatus) {
            User.AccountStatus.Okay -> {
                windscribeView.setupAccountStatusOkay()
            }

            User.AccountStatus.Banned -> {
                if (interactor.getVpnConnectionStateManager().isVPNActive()) {
                    interactor.getMainScope()
                        .launch { interactor.getVPNController().disconnectAsync() }
                }
                windscribeView.setupAccountStatusBanned()
            }

            else -> {
                val previousAccountStatus =
                    interactor.getAppPreferenceInterface().getPreviousAccountStatus(user.userName)
                if (user.accountStatusToInt != previousAccountStatus) {
                    interactor.getAppPreferenceInterface()
                        .setPreviousAccountStatus(user.userName, user.accountStatusToInt)
                    if (user.accountStatus == User.AccountStatus.Expired) {
                        setUserStatus(user)
                        if (interactor.getVpnConnectionStateManager().isVPNActive()) {
                            interactor.getMainScope()
                                .launch { interactor.getVPNController().disconnectAsync() }
                        }
                        val resetDate = interactor.getUserRepository().user.value?.nextResetDate() ?: ""
                        windscribeView.setupAccountStatusExpired(resetDate)
                    }
                }
            }
        }
    }

    private fun setUserStatus(user: User) {
        logger.debug("{}", user)
        if (user.maxData != -1L) {
            user.dataLeft.let {
                val dataRemaining = interactor.getDataLeftString(R.string.data_left, it)
                windscribeView.setupLayoutForFreeUser(
                    dataRemaining,
                    interactor.getResourceString(R.string.get_more_data),
                    getDataRemainingColor(it, user.maxData)
                )
            }
        } else {
            windscribeView.setupLayoutForProUser()
        }
    }

    override fun loadConfigFile(data: Intent) {
        try {
            val fileUri = data.data
            val inputStream = appContext.contentResolver.openInputStream(fileUri!!)
            inputStream?.use {
                val documentFile = DocumentFile.fromSingleUri(appContext, fileUri)
                val fileName = validatedConfigFileName(documentFile) ?: return
                val content = CharStreams.toString(InputStreamReader(inputStream))
                var username = ""
                var password = ""
                try {
                    val configParser = OpenVPNConfigParser()
                    username = configParser.getEmbeddedUsername(InputStreamReader(inputStream))
                    password = configParser.getEmbeddedPassword(InputStreamReader(inputStream))
                } catch (ignored: Exception) {
                }
                logger.info("Successfully read file.")
                onConfigFileContentReceived(
                    fileName, content, username, password
                )
            }

        } catch (e: IOException) {
            logger.info(e.toString())
        }
    }

    private fun validatedConfigFileName(documentFile: DocumentFile?): String? {
        if (documentFile == null) {
            windscribeView.showToast("Choose a valid config file")
            return null
        }
        if (documentFile.length() > 1024 * 12) {
            windscribeView.showToast("File is larger than 12KB")
            return null
        }
        val fileName = documentFile.name
        val existingFile = configAdapter?.configFiles?.firstOrNull { it.name == fileName }
        if (existingFile != null) {
            windscribeView.showToast("A file with same name already exists")
            return null
        }
        if (fileName != null && fileName.length > 35) {
            windscribeView.showToast("File name is too long. Maximum 35 characters allowed.")
            return null
        }
        if (fileName != null && fileName.endsWith(".conf") or fileName.endsWith(".ovpn")) {
            return fileName
        }
        windscribeView.showToast("Choose valid .ovpn or .conf file.")
        return null
    }

    override suspend fun showShareLinkDialog() {
        interactor.getUserRepository().user.value?.let {
            delay(4000)
            if (it.isGhost.not() && it.isPro.not() && it.daysRegisteredSince > 30 && interactor.getAppPreferenceInterface().getConnectionCount() >= 10 && interactor.getAppPreferenceInterface().alreadyShownShareAppLink.not()) {
                windscribeView.showShareLinkDialog()
            }
        }
    }

    override fun onDecoyTrafficClick() {
        if (interactor.getVpnConnectionStateManager().isVPNConnected()) {
            if (interactor.getAppPreferenceInterface().isDecoyTrafficOn) {
                windscribeView.openConnectionActivity()
            }
        }
    }

    override fun onProtocolChangeClick() {
        if (WindUtilities.getSourceTypeBlocking() == SelectedLocationType.CustomConfiguredProfile) {
            windscribeView.showToast(interactor.getResourceString(R.string.protocol_change_is_not_available_for_custom_config))
        } else if (interactor.getVpnConnectionStateManager().isVPNConnected()) {
            interactor.getMainScope().launch {
                interactor.getAutoConnectionManager().changeProtocolInForeground()
            }
        }
    }

    override fun onLocationSettingsChanged() {
        selectedLocation?.let {
            updateLocationUI(it, true)
        }
    }

    /**
     * Check for user ip when app resumes if connected to Wg.
     * Dynamic wg may change ip on network changes.
     */
    override fun checkForWgIpChange() {
        if (interactor.getVpnConnectionStateManager()
                .isVPNConnected() && interactor.getAppPreferenceInterface().selectedProtocol == PROTO_WIRE_GUARD
        ) {
            logger.debug("Checking dynamic wg ip change.")
            interactor.getCompositeDisposable()
                .add(interactor.getApiCallManager().checkConnectivityAndIpAddress()
                    .subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
                    .subscribe { response, _ ->
                        response?.dataClass?.let { ip ->
                            if (validIpAddress(ip.userIp)) {
                                val updatedIpAddress = getModifiedIpAddress(ip.userIp)
                                interactor.getAppPreferenceInterface().saveResponseStringData(
                                    PreferencesKeyConstants.USER_IP, updatedIpAddress
                                )
                                logger.debug("Updating ip address to $updatedIpAddress")
                                windscribeView.setIpAddress(updatedIpAddress)
                            } else {
                                logger.error("Invalid ip returned from Api $ip")
                            }
                        } ?: kotlin.run {
                            logger.error("Failed to get ip from API.")
                        }
                    })
        }
    }

    override fun checkPendingAccountUpgrades() {
        interactor.getReceiptValidator().checkPendingAccountUpgrades()
    }

    override fun onAntiCensorShipStatusChanged() {
        if (interactor.getAppPreferenceInterface().isAntiCensorshipOn) {
            windscribeView.setCensorShipIconVisibility(View.VISIBLE)
        } else {
            windscribeView.setCensorShipIconVisibility(View.GONE)
        }
    }

    override suspend fun observeLocationUIInvalidation() {
        interactor.getServerListUpdater().locationUIInvalidation.collectLatest {
            if (adapter?.serverListData != null) {
                adapter?.serverListData?.serverListHash = ""
                interactor.getServerListUpdater().load()
            }
        }
    }

    override suspend fun observeConnectionCount() {
        interactor.getVpnConnectionStateManager().connectionCount
            .filter { count ->
                val showCount = interactor.getAppPreferenceInterface().getPowerWhiteListDialogCount()
                count > 1 && !isIgnoringBatteryOptimizations(appContext) && showCount < 3
            }.collectLatest {
                if (!isIgnoringBatteryOptimizations(appContext) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    windscribeView.launchBatteryOptimizationActivity()
                }
            }
    }

    private fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        val manager =
            context.applicationContext.getSystemService(Context.POWER_SERVICE) as PowerManager
        val name = context.applicationContext.packageName
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return manager.isIgnoringBatteryOptimizations(name)
        }
        return true
    }

    override fun neverAskPowerWhiteListPermissionAgain() {
        interactor.getAppPreferenceInterface().setPowerWhiteListDialogCount(3)
    }

    override fun askPowerWhiteListPermissionLater() {
        val count = interactor.getAppPreferenceInterface().getPowerWhiteListDialogCount()
        interactor.getAppPreferenceInterface().setPowerWhiteListDialogCount( count + 1)
    }
}