/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn

import android.os.Build
import android.os.Build.VERSION
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleCoroutineScope
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.windscribe.vpn.R.*
import com.windscribe.vpn.Windscribe.Companion.appContext
import com.windscribe.vpn.api.IApiCallManager
import com.windscribe.vpn.api.response.*
import com.windscribe.vpn.apppreference.PreferencesHelper
import com.windscribe.vpn.autoconnection.AutoConnectionManager
import com.windscribe.vpn.backend.TrafficCounter
import com.windscribe.vpn.backend.openvpn.ProxyTunnelManager
import com.windscribe.vpn.backend.utils.WindVpnController
import com.windscribe.vpn.commonutils.ThemeUtils
import com.windscribe.vpn.commonutils.WindUtilities
import com.windscribe.vpn.constants.NetworkKeyConstants
import com.windscribe.vpn.constants.PreferencesKeyConstants
import com.windscribe.vpn.constants.RateDialogConstants
import com.windscribe.vpn.decoytraffic.DecoyTrafficController
import com.windscribe.vpn.encoding.encoders.Base64
import com.windscribe.vpn.exceptions.WindScribeException
import com.windscribe.vpn.localdatabase.LocalDbInterface
import com.windscribe.vpn.localdatabase.tables.*
import com.windscribe.vpn.model.User
import com.windscribe.vpn.repository.*
import com.windscribe.vpn.serverlist.entity.*
import com.windscribe.vpn.services.FirebaseManager
import com.windscribe.vpn.state.NetworkInfoManager
import com.windscribe.vpn.state.PreferenceChangeObserver
import com.windscribe.vpn.state.VPNConnectionStateManager
import com.windscribe.vpn.services.ReceiptValidator
import com.windscribe.vpn.workers.WindScribeWorkManager
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Single
import io.reactivex.SingleSource
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.observers.DisposableSingleObserver
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.CoroutineScope
import org.slf4j.LoggerFactory
import java.io.*
import java.nio.charset.Charset
import java.util.*
import java.util.concurrent.TimeUnit.DAYS
import java.util.concurrent.TimeUnit.MILLISECONDS

/**
 * Implementation of ActivityInteractor
 * @see ActivityInteractor
*/
class ActivityInteractorImpl(
    private val activityScope: LifecycleCoroutineScope,
    private val mainScope: CoroutineScope,
    val preferenceHelper: PreferencesHelper,
    private val apiCallManager: IApiCallManager,
    val localDbInterface: LocalDbInterface,
    private val vpnConnectionStateManager: VPNConnectionStateManager,
    private val userRepository: UserRepository,
    private val networkInfoManager: NetworkInfoManager,
    private val locationRepository: LocationRepository,
    private val vpnController: WindVpnController,
    private val connectionDataRepository: ConnectionDataRepository,
    private val serverListRepository: ServerListRepository,
    private val staticListUpdate: StaticIpRepository,
    private val preferenceChangeObserver: PreferenceChangeObserver,
    private val notificationRepository: NotificationRepository,
    private val windScribeWorkManager: WindScribeWorkManager,
    private val decoyTrafficController: DecoyTrafficController,
    private val trafficCounter: TrafficCounter,
    private val autoConnectionManager: AutoConnectionManager,
    private val latencyRepository: LatencyRepository,
    private val receiptValidator: ReceiptValidator,
    private val firebaseManager: FirebaseManager,
    private val advanceParameterRepository: AdvanceParameterRepository
) : ActivityInteractor {

    interface PortMapLoadCallback {

        fun onFinished(portMapResponse: PortMapResponse)
    }

    private var compositeDisposable = CompositeDisposable()

    private val logger = LoggerFactory.getLogger("activity_i")

    private var mapResponse: PortMapResponse? = null

    override fun getWorkManager(): WindScribeWorkManager {
        return windScribeWorkManager
    }
    override fun getUserRepository(): UserRepository {
        return userRepository
    }

    override fun getVpnConnectionStateManager(): VPNConnectionStateManager {
        return vpnConnectionStateManager
    }

    override fun getNetworkInfoManager(): NetworkInfoManager {
        return networkInfoManager
    }

    override fun getLocationProvider(): LocationRepository {
        return locationRepository
    }

    override fun getVPNController(): WindVpnController {
        return vpnController
    }

    override fun getMainScope(): CoroutineScope {
        return mainScope
    }

    override fun getActivityScope(): LifecycleCoroutineScope {
        return activityScope
    }

    override fun getDecoyTrafficController(): DecoyTrafficController {
        return decoyTrafficController
    }

    override fun getServerListUpdater(): ServerListRepository {
        return serverListRepository
    }

    override fun getConnectionDataUpdater(): ConnectionDataRepository {
        return connectionDataRepository
    }

    override fun getStaticListUpdater(): StaticIpRepository {
        return staticListUpdate
    }

    override fun getPreferenceChangeObserver(): PreferenceChangeObserver {
        return preferenceChangeObserver
    }

    override fun getNotificationUpdater(): NotificationRepository {
        return notificationRepository
    }

    override fun getTrafficCounter(): TrafficCounter {
        return trafficCounter
    }

    override fun getAutoConnectionManager(): AutoConnectionManager {
        return autoConnectionManager
    }

    override fun getLatencyRepository(): LatencyRepository {
        return latencyRepository
    }

    override fun addConfigFile(configFile: ConfigFile): Completable {
        return this.localDbInterface.addConfig(configFile)
    }

    override fun addNetwork(networkInfo: NetworkInfo): SingleSource<Long> {
        return this.localDbInterface.addNetwork(networkInfo)
    }

    override fun addNetworkToKnown(networkName: String): Single<Long> {
        val networkInfo = NetworkInfo(
            networkName, true, false, PreferencesKeyConstants.PROTO_IKev2, "500"
        )
        return this.localDbInterface.addNetwork(networkInfo)
    }

    override fun addPing(pingTime: PingTime): Completable {
        return this.localDbInterface.addPing(pingTime)
    }

    override fun addToFavourites(favourite: Favourite): Single<Long> {
        return this.localDbInterface.addToFavourites(favourite)
    }

    override fun deleteConfigFile(id: Int): Completable {
        return this.localDbInterface.delete(id)
    }

    override fun deleteFavourite(favourite: Favourite) {
        this.localDbInterface.delete(favourite)
    }

    override fun getApiCallManager(): IApiCallManager {
        return this.apiCallManager
    }

    override fun getFireBaseManager(): FirebaseManager {
        return firebaseManager
    }

    override fun getAppPreferenceInterface(): PreferencesHelper {
        return this.preferenceHelper
    }

    override fun getColorResource(resourceId: Int): Int {
        return ContextCompat.getColor(Windscribe.appContext, resourceId)
    }

    override fun getCompositeDisposable(): CompositeDisposable {
        return compositeDisposable
    }

    override fun getAllCities(): Single<List<City>> {
        return this.localDbInterface.cities
    }

    override fun getCurrentUserStatusTable(userName: String): Flowable<UserStatusTable> {
        return this.localDbInterface.getUserStatus(userName)
    }

    override fun getDataLeftString(resourceId: Int, dataRemaining: Float): String {
        return Windscribe.appContext.resources.getString(resourceId, dataRemaining)
    }

    override fun getDebugFilePath(): String {
        return if (advanceParameterRepository.showStrongSwanLog()) {
            "${appContext.filesDir}/charon.log"
        } else {
            appContext.cacheDir.path + PreferencesKeyConstants.DEBUG_LOG_FILE_NAME
        }
    }

    @Throws(Exception::class)
    override fun getEncodedLog(): String {
        logger.info("Reading debug log file...")
        var logLine: String?
        val debugFilePath = getDebugFilePath()
        val logFile = Windscribe.appContext.resources.getString(
            string.log_file_header,
            VERSION.SDK_INT, Build.BRAND, Build.DEVICE, Build.MODEL, Build.MANUFACTURER,
            VERSION.RELEASE, WindUtilities.getVersionCode()
        )
        val builder = StringBuilder()
        builder.append(logFile)
        val file = File(debugFilePath)
        val bufferedReader = BufferedReader(FileReader(file))
        while (bufferedReader.readLine().also { logLine = it } != null) {
            builder.append(logLine)
            builder.append("\n")
        }
        val wsTunnelLog = File(appContext.filesDir, ProxyTunnelManager.PROXY_LOG)
        if (wsTunnelLog.exists()) {
            wsTunnelLog.bufferedReader().use { builder.append(it.readText()) }
        }
        bufferedReader.close()
        return String(Base64.encode(builder.toString().toByteArray(Charset.defaultCharset())))
    }

    private fun getHardCodedPortMap(): Single<PortMapResponse> {
        logger.debug("Using hardcoded port map.")
        return Single.fromCallable {
            val inputStream: InputStream = appContext.resources.openRawResource(raw.port_map)
            val sc = Scanner(inputStream)
            val sb = StringBuilder()
            while (sc.hasNext()) {
                sb.append(sc.nextLine())
            }
            inputStream.close()
            Gson().fromJson(sb.toString(), PortMapResponse::class.java)
        }
    }

    override fun getLastTimeUpdated(): String {
        return this.preferenceHelper.getResponseString(RateDialogConstants.LAST_UPDATE_TIME) ?: Date().time.toString()
    }

    override fun getAllConfigs(): Single<List<ConfigFile>> {
        return this.localDbInterface.allConfigs
    }

    override fun getAllPings(): Single<List<PingTime>> {
        return this.localDbInterface.allPingTimes
    }

    override fun getAllRegion(): Single<List<RegionAndCities>> {
        return this.localDbInterface.allRegion
    }

    override fun getNotifications(userName: String): Flowable<List<PopupNotificationTable>> {
        return this.localDbInterface.getPopupNotifications(userName)
    }

    private fun getPortMap(): Single<PortMapResponse> {
        return Single.fromCallable {
            val currentPortMap: Int = this.preferenceHelper.portMapVersion
            if (currentPortMap != NetworkKeyConstants.PORT_MAP_VERSION) {
                logger.debug("Outdated port map version.")
                throw WindScribeException("Port map version outdated")
            }
            val jsonString: String? = this.preferenceHelper.getResponseString(PreferencesKeyConstants.PORT_MAP)
            Gson().fromJson(jsonString, PortMapResponse::class.java)
        }.onErrorResumeNext {
            if (WindUtilities.isOnline()) {
                getPortMapFromApi()
            } else {
                logger.debug("No network available to get port map.")
                Single.error(Exception())
            }
        }.onErrorResumeNext {
            getHardCodedPortMap()
        }
    }

    private fun getPortMapFromApi(): Single<PortMapResponse> {
        logger.debug("Loading port map from api")
        return this.apiCallManager.getPortMap()
            .flatMap { responseClass: GenericResponseClass<PortMapResponse?, ApiErrorResponse?> ->
                Single
                    .fromCallable {
                        logger.debug(responseClass.dataClass.toString())
                        responseClass.dataClass?.let {
                            this.preferenceHelper
                                .savePortMapVersion(NetworkKeyConstants.PORT_MAP_VERSION)
                            this.preferenceHelper.saveResponseStringData(
                                PreferencesKeyConstants.PORT_MAP,
                                Gson().toJson(it)
                            )
                            return@fromCallable it
                        } ?: responseClass.errorClass?.let {
                            logger.debug(it.errorMessage)
                            throw WindScribeException(it.errorDescription)
                        }
                    }
            }
    }

    override fun getRateAppPreference(): Int {
        return this.preferenceHelper.getResponseInt(
                RateDialogConstants.CURRENT_STATUS_KEY,
                RateDialogConstants.STATUS_DEFAULT
            )
    }

    override fun getResourceString(resourceId: Int): String {
        return Windscribe.appContext.resources.getString(resourceId)
    }

    override fun getStringArray(resourceId: Int): Array<String> {
        return Windscribe.appContext.resources.getStringArray(resourceId)
    }

    override fun getUserAccountStatus(): Int {
        return userRepository.user.value?.accountStatusToInt ?: kotlin.run {
            return 1
        }
    }

    override fun getUserSessionData(): Single<UserSessionResponse> {
        return Single.fromCallable {
            this.preferenceHelper.getResponseString(
                PreferencesKeyConstants.GET_SESSION
            )
        }.flatMap { userSessionString: String? ->
            Single
                .fromCallable {
                    Gson().fromJson(
                        userSessionString,
                        UserSessionResponse::class.java
                    )
                }
        }
    }

    override fun getAllStaticRegions(): Single<List<StaticRegion>> {
        return this.localDbInterface.allStaticRegions
    }

    override fun isPremiumUser(): Boolean {
        return this.preferenceHelper.userStatus == 1
    }

    override fun isUserEligibleForRatingApp(userSessionResponse: UserSessionResponse): Boolean {
        val user = User(userSessionResponse)
        val days = user.daysRegisteredSince
        val experiencedUser = days >= RateDialogConstants.MINIMUM_DAYS_TO_START
        val dataUsed = user.dataUsed?.toFloat() ?: 0F
        val enoughDataUsed = dataUsed >= RateDialogConstants.MINIMUM_DATA_LIMIT
        val logDate: String = try {
            Date(getLastTimeUpdated().toLong()).toString()
        } catch (nm: NumberFormatException) {
            "No date available."
        }
        if (isPremiumUser() && enoughDataUsed && experiencedUser && lastShownDays(getLastTimeUpdated())) {
            logger
                .debug(
                    "Rate dialog check: IsPremiumUser:" + isPremiumUser() + ", Data Used:" + dataUsed + "GB" +
                        " Dialog data limit:" + RateDialogConstants.MINIMUM_DATA_LIMIT + "GB," +
                        " Registration(days):" + days + " Dialog days limit:" +
                        RateDialogConstants.MINIMUM_DAYS_TO_START + ", Last choice:" + getLastChoiceLog() +
                        " Last shown:" + logDate
                )
            return true
        }
        return false
    }

    override fun loadPortMap(callback: PortMapLoadCallback) {
        if (mapResponse != null) {
            mapResponse?.let { callback.onFinished(it) }
            return
        }
        compositeDisposable.add(
            getPortMap()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(object : DisposableSingleObserver<PortMapResponse>() {
                    override fun onError(e: Throwable) {
                        logger.debug(e.toString())
                    }

                    override fun onSuccess(portMapResponse: PortMapResponse) {
                        mapResponse = portMapResponse
                        callback.onFinished(portMapResponse)
                    }
                })
        )
    }

    override fun saveRateAppPreference(type: Int) {
        this.preferenceHelper.saveResponseIntegerData(RateDialogConstants.CURRENT_STATUS_KEY, type)
    }

    override fun setRateDialogUpdateTime() {
        this.preferenceHelper.saveResponseStringData(RateDialogConstants.LAST_UPDATE_TIME, Date().time.toString())
    }

    override fun getCityAndRegionByID(cityId: Int): Single<CityAndRegion> {
        return this.localDbInterface.getCityAndRegionByID(cityId)
    }

    private fun getLastChoiceLog(): String {
        return when (getRateAppPreference()) {
            RateDialogConstants.STATUS_ALREADY_ASKED -> "Already asked"
            RateDialogConstants.STATUS_ASK_LATER -> "Ask later"
            RateDialogConstants.STATUS_NEVER_ASK -> "Never Ask"
            else -> "Default"
        }
    }

    private fun lastShownDays(time: String): Boolean {
        return try {
            val difference = Date().time - time.toLong()
            val days = DAYS.convert(difference, MILLISECONDS)
            days > RateDialogConstants.MINIMUM_DAYS_TO_START
        } catch (e: NumberFormatException) {
            true
        }
    }

    override fun getCityByID(favIds: IntArray): Single<List<City>> {
        return this.localDbInterface.getCityByID(favIds)
    }

    override fun getConfigFile(id: Int): Single<ConfigFile> {
        return this.localDbInterface.getConfigFile(id)
    }

    override fun getFavourites(): Single<List<Favourite>> {
        return this.localDbInterface.favourites
    }

    override fun getLowestPingId(): Single<Int> {
        return this.localDbInterface.lowestPingId
    }

    override fun getMaxPrimaryKey(): Single<Int> {
        return this.localDbInterface.maxPrimaryKey
    }

    override fun getNetwork(networkName: String): Single<NetworkInfo> {
        return this.localDbInterface.getNetwork(networkName)
    }

    override fun getStaticRegionByID(staticId: Int): Single<StaticRegion> {
        return this.localDbInterface.getStaticRegionByID(staticId)
    }

    override fun insertOrUpdateUserStatus(userStatusTable: UserStatusTable): Completable {
        return this.localDbInterface.updateUserStatus(userStatusTable)
    }

    override fun updateNetwork(networkInfo: NetworkInfo): Single<Int> {
        return this.localDbInterface.updateNetwork(networkInfo)
    }

    override fun getWindNotifications(): Single<List<WindNotification>> {
        return this.localDbInterface.windNotifications
    }

    override fun getUserSessionDataFromStorage(): Single<UserSessionResponse> {
        return Single.fromCallable {
            preferenceHelper.getResponseString(
                PreferencesKeyConstants.GET_SESSION
            )
        }.flatMap {
            Single.fromCallable {
                Gson().fromJson(it, UserSessionResponse::class.java)
            }
        }
    }

    override fun getSavedConnectionMode(): String {
        return preferenceHelper.getResponseString(PreferencesKeyConstants.CONNECTION_MODE_KEY)
            ?: PreferencesKeyConstants.CONNECTION_MODE_AUTO
    }

    override fun getSavedProtocol(): String {
        return preferenceHelper.savedProtocol
    }

    override fun getSavedSTEALTHPort(): String {
        return preferenceHelper.savedSTEALTHPort
    }

    override fun getSavedWSTunnelPort(): String {
        return preferenceHelper.savedWSTunnelPort
    }

    override fun getSavedTCPPort(): String {
        return preferenceHelper.savedTCPPort
    }

    override fun getSavedUDPPort(): String {
        return preferenceHelper.savedUDPPort
    }

    override fun getThemeColor(resourceId: Int): Int {
        return ThemeUtils.getColor(Windscribe.appContext, resourceId, android.R.color.white)
    }

    override fun getWireGuardPort(): String {
        return preferenceHelper.wireGuardPort
    }

    override fun saveConnectionMode(connectionMode: String) {
        preferenceHelper.saveResponseStringData(
            PreferencesKeyConstants.CONNECTION_MODE_KEY,
            connectionMode
        )
    }

    override fun saveProtocol(protocol: String) {
        preferenceHelper.saveResponseStringData(PreferencesKeyConstants.PROTOCOL_KEY, protocol)
    }

    override fun saveSTEALTHPort(port: String) {
        preferenceHelper.saveResponseStringData(PreferencesKeyConstants.SAVED_STEALTH_PORT, port)
    }

    override fun saveWSTunnelPort(port: String) {
        preferenceHelper.saveResponseStringData(PreferencesKeyConstants.SAVED_WS_TUNNEL_PORT, port)
    }

    override fun saveTCPPort(port: String) {
        preferenceHelper.saveResponseStringData(PreferencesKeyConstants.SAVED_TCP_PORT, port)
    }

    override fun saveUDPPort(port: String) {
        preferenceHelper.saveResponseStringData(PreferencesKeyConstants.SAVED_UDP_PORT, port)
    }

    override fun getIKev2Port(): String {
        return preferenceHelper.iKEv2Port
    }

    override fun getSavedLanguage(): String {
        return preferenceHelper.savedLanguage
    }

    override fun getSavedSelection(): String {
        return preferenceHelper.selection
    }

    override fun saveSelectedLanguage(lang: String) {
        preferenceHelper.saveResponseStringData(PreferencesKeyConstants.USER_LANGUAGE, lang)
    }

    override fun saveSelection(selection: String) {
        preferenceHelper.saveSelection(selection)
    }

    override fun updateServerList(): Single<Boolean> {
        return Single.fromCallable {
            val username: String = preferenceHelper.userName
            localDbInterface.insertOrUpdateServerUpdateStatusTable(ServerStatusUpdateTable(username, 1))
            true
        }
    }

    override fun getLanguageList(): Array<String> {
        return Windscribe.appContext.resources.getStringArray(array.language)
    }

    override fun getCurrentUserStatus(): Int {
        return preferenceHelper.userStatus
    }

    override fun saveNetwork(networkInfo: NetworkInfo): Single<Int> {
        return localDbInterface.updateNetwork(networkInfo)
    }

    override fun removeNetwork(networkName: String): Single<Int> {
        return localDbInterface.deleteNetwork(networkName)
    }

    override fun getNetworkInfoUpdated(): Flowable<List<NetworkInfo>> {
        return localDbInterface.allNetworksWithUpdate
    }

    override fun getNotifications(): Single<List<WindNotification>> {
        return localDbInterface.windNotifications
    }

    override fun serverDataAvailable(): Single<Boolean> {
        return localDbInterface.city.flatMap {
            Single.fromCallable { true }
        }.onErrorReturnItem(false)
    }

    override fun updateUserData(): Completable {
        return Completable.fromAction {
            userRepository.reload()
        }
    }

    override fun updateServerData(): Completable {
        return localDbInterface.insertOrUpdateStatus(
            ServerStatusUpdateTable(
                preferenceHelper.userName,
                preferenceHelper.userStatus
            )
        )
    }

    override fun getSortList(): Array<String> {
        return Windscribe.appContext.resources.getStringArray(array.order_list)
    }

    override fun getServerStatus(): Single<ServerStatusUpdateTable> {
        return localDbInterface.getServerStatus(preferenceHelper.userName)
    }

    override fun updateServerList(serverStatus: Int): Completable {
        return localDbInterface.insertOrUpdateStatus(ServerStatusUpdateTable(preferenceHelper.userName, serverStatus))
    }

    override fun getPartialLog(): List<String> {
        return try {
            File(getDebugFilePath()).readLines()
        } catch (ignored: IOException) {
            emptyList()
        }
    }

    override fun getAllCities(regionId: Int): Single<List<City>> {
        return localDbInterface.getAllCities(regionId)
    }

    override fun getRegionAndCity(regionId: Int): Single<RegionAndCities> {
        return localDbInterface.getRegion(regionId)
    }

    override fun getAllStaticRegionsAsFlowAble(): Flowable<List<StaticRegion>> {
        return localDbInterface.allStaticRegionsFlowAble
    }

    override fun getFavouriteRegionAndCities(favourites: IntArray): Single<List<City>> {
        return localDbInterface.getCityByID(favourites)
    }

    override fun getPingResults(): Single<List<PingTestResults>> {
        return localDbInterface.allPings
    }

    override fun getFavoriteServerList(): Single<List<ServerNodeListOverLoaded>> {
        return Single.fromCallable {
            getFavoriteServers(preferenceHelper.getResponseString(PreferencesKeyConstants.FAVORITE_SERVER_LIST))
        }
    }

    private fun getFavoriteServers(jsonString: String?): List<ServerNodeListOverLoaded> {
        return Gson().fromJson(jsonString, object : TypeToken<List<ServerNodeListOverLoaded>>() {}.type)
    }

    override fun getReceiptValidator(): ReceiptValidator {
        return receiptValidator
    }
}
