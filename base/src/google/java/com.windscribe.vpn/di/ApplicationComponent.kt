/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.vpn.di

import android.content.Context
import com.windscribe.vpn.Windscribe
import com.windscribe.vpn.api.*
import com.windscribe.vpn.apppreference.PreferencesHelper
import com.windscribe.vpn.autoconnection.AutoConnectionManager
import com.windscribe.vpn.backend.TrafficCounter
import com.windscribe.vpn.backend.VpnBackendHolder
import com.windscribe.vpn.backend.ikev2.IKev2VpnBackend
import com.windscribe.vpn.backend.openvpn.OpenVPNBackend
import com.windscribe.vpn.backend.utils.WindNotificationBuilder
import com.windscribe.vpn.backend.utils.WindVpnController
import com.windscribe.vpn.backend.wireguard.WireguardBackend
import com.windscribe.vpn.billing.AmazonBillingManager
import com.windscribe.vpn.billing.GoogleBillingManager
import com.windscribe.vpn.decoytraffic.DecoyTrafficController
import com.windscribe.vpn.localdatabase.LocalDbInterface
import com.windscribe.vpn.mocklocation.MockLocationManager
import com.windscribe.vpn.repository.*
import com.windscribe.vpn.services.FirebaseManager
import com.windscribe.vpn.services.firebasecloud.WindscribeCloudMessaging
import com.windscribe.vpn.state.*
import com.windscribe.vpn.services.ReceiptValidator
import com.windscribe.vpn.workers.WindScribeWorkManager
import com.windscribe.vpn.workers.worker.*
import dagger.Component
import kotlinx.coroutines.CoroutineScope
import javax.inject.Named
import javax.inject.Singleton

@Singleton
@Component(modules = [ApplicationModule::class, NetworkModule::class, VPNModule::class, PersistentModule::class, BillingModule::class])
interface ApplicationComponent {
    //Main
    val coroutineScope: CoroutineScope

    @get:Named("ApplicationContext")
    val appContext: Context

    //Api
    val apiCallManager: IApiCallManager
    val windApiFactory: WindApiFactory
    val windCustomFactory: WindCustomApiFactory
    val authorizationGenerator: AuthorizationGenerator

    //Backup
    @get:Named("backupEndPoint")
    val backupEndpoint: String

    @get:Named("backupEndPointListForIp")
    val backupEndpointListForIp: List<String>

    @get:Named("accessIpList")
    val accessIpList: List<String>

    //Data
    val localDbInterface: LocalDbInterface
    val preferencesHelper: PreferencesHelper
    val preferenceChangeObserver: PreferenceChangeObserver

    //VPN
    val vpnBackendHolder: VpnBackendHolder
    val windNotificationBuilder: WindNotificationBuilder
    val wireguardBackend: WireguardBackend
    val iKev2VpnBackend: IKev2VpnBackend
    val openVPNBackend: OpenVPNBackend
    val vpnConnectionStateManager: VPNConnectionStateManager
    val autoConnectionManager: AutoConnectionManager

    //Managers
    val windScribeWorkManager: WindScribeWorkManager
    val deviceStateManager: DeviceStateManager
    val windVpnController: WindVpnController
    val mockLocationController: MockLocationManager
    val networkInfoManager: NetworkInfoManager
    val appLifeCycleObserver: AppLifeCycleObserver
    val decoyTrafficController: DecoyTrafficController
    val trafficCounter: TrafficCounter
    val shortcutStateManager: ShortcutStateManager
    val amazonBillingManager: AmazonBillingManager
    val googleBillingManager: GoogleBillingManager
    val receiptValidator: ReceiptValidator
    val firebaseManager: FirebaseManager

    //Repository
    val staticIpRepository: StaticIpRepository
    val serverListRepository: ServerListRepository
    val locationRepository: LocationRepository
    val connectionDataRepository: ConnectionDataRepository
    val notificationRepository: NotificationRepository
    val userRepository: UserRepository
    val latencyRepository: LatencyRepository
    val ipRepository: IpRepository
    val favouriteRepository: FavouriteRepository
    val emergencyConnectRepository: EmergencyConnectRepository
    val advanceParameterRepository: AdvanceParameterRepository

    //Inject
    fun inject(app: Windscribe)
    fun inject(windscribeCloudMessaging: WindscribeCloudMessaging)
    fun inject(serverListWorker: ServerListWorker)
    fun inject(credentialsWorker: CredentialsWorker)
    fun inject(staticListWorker: StaticIpWorker)
    fun inject(sessionWorker: SessionWorker)
    fun inject(notificationWorker: NotificationWorker)
    fun inject(robertSyncWorker: RobertSyncWorker)
    fun inject(googlePendingReceiptValidator: GooglePendingReceiptValidator)
    fun inject(amazonPendingReceiptValidator: AmazonPendingReceiptValidator)
    fun inject(latencyWorker: LatencyWorker)
}
