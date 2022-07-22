/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.vpn.di

import android.content.Context
import com.windscribe.vpn.Windscribe
import com.windscribe.vpn.api.AuthorizationGenerator
import com.windscribe.vpn.api.IApiCallManager
import com.windscribe.vpn.api.WindApiFactory
import com.windscribe.vpn.api.WindCustomApiFactory
import com.windscribe.vpn.apppreference.PreferencesHelper
import com.windscribe.vpn.backend.VpnBackendHolder
import com.windscribe.vpn.backend.ikev2.IKev2VpnBackend
import com.windscribe.vpn.backend.openvpn.OpenVPNBackend
import com.windscribe.vpn.backend.utils.ProtocolManager
import com.windscribe.vpn.backend.utils.WindNotificationBuilder
import com.windscribe.vpn.backend.utils.WindVpnController
import com.windscribe.vpn.backend.wireguard.WireguardBackend
import com.windscribe.vpn.decoytraffic.DecoyTrafficController
import com.windscribe.vpn.localdatabase.LocalDbInterface
import com.windscribe.vpn.mocklocation.MockLocationManager
import com.windscribe.vpn.repository.ConnectionDataRepository
import com.windscribe.vpn.repository.LocationRepository
import com.windscribe.vpn.repository.NotificationRepository
import com.windscribe.vpn.repository.ServerListRepository
import com.windscribe.vpn.repository.StaticIpRepository
import com.windscribe.vpn.repository.UserRepository
import com.windscribe.vpn.services.firebasecloud.WindscribeCloudMessaging
import com.windscribe.vpn.state.AppLifeCycleObserver
import com.windscribe.vpn.state.DeviceStateManager
import com.windscribe.vpn.state.NetworkInfoManager
import com.windscribe.vpn.state.PreferenceChangeObserver
import com.windscribe.vpn.state.VPNConnectionStateManager
import com.windscribe.vpn.workers.WindScribeWorkManager
import com.windscribe.vpn.workers.worker.*
import dagger.Component
import javax.inject.Named
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope

@Singleton
@Component(modules = [ApplicationModule::class])
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
    @get:Named("backupEndPointList")
    val backupEndpointList: List<String>

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

    //Managers
    val windScribeWorkManager: WindScribeWorkManager
    val deviceStateManager: DeviceStateManager
    val windVpnController: WindVpnController
    val protocolManager: ProtocolManager
    val mockLocationController: MockLocationManager
    val networkInfoManager: NetworkInfoManager
    val appLifeCycleObserver: AppLifeCycleObserver
    val decoyTrafficController: DecoyTrafficController

    //Repository
    val staticIpRepository: StaticIpRepository
    val serverListRepository: ServerListRepository
    val locationRepository: LocationRepository
    val connectionDataRepository: ConnectionDataRepository
    val notificationRepository: NotificationRepository
    val userRepository: UserRepository

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
}
