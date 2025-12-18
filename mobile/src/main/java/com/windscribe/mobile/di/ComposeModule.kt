package com.windscribe.mobile.di

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.windscribe.mobile.ui.AppStartActivityViewModel
import com.windscribe.mobile.ui.AppStartActivityViewModelImpl
import com.windscribe.mobile.ui.auth.AppStartViewModel
import com.windscribe.mobile.ui.auth.AppStartViewModelImpl
import com.windscribe.mobile.ui.auth.EmergencyConnectViewModal
import com.windscribe.mobile.ui.auth.LoginViewModel
import com.windscribe.mobile.ui.auth.SignupViewModel
import com.windscribe.mobile.ui.connection.BridgeApiViewModel
import com.windscribe.mobile.ui.connection.BridgeApiViewModelImpl
import com.windscribe.mobile.ui.connection.ConnectionViewmodel
import com.windscribe.mobile.ui.connection.ConnectionViewmodelImpl
import com.windscribe.mobile.ui.home.HomeViewmodel
import com.windscribe.mobile.ui.home.HomeViewmodelImpl
import com.windscribe.mobile.ui.popup.EditCustomConfigViewmodel
import com.windscribe.mobile.ui.popup.EditCustomConfigViewmodelImpl
import com.windscribe.mobile.ui.popup.NewsfeedViewmodel
import com.windscribe.mobile.ui.popup.PowerWhitelistViewmodel
import com.windscribe.mobile.ui.popup.PowerWhitelistViewmodelImpl
import com.windscribe.mobile.ui.popup.SharedLinkViewmodel
import com.windscribe.mobile.ui.popup.SharedLinkViewmodelImpl
import com.windscribe.mobile.ui.preferences.account.AccountViewModel
import com.windscribe.mobile.ui.preferences.account.AccountViewModelImpl
import com.windscribe.mobile.ui.preferences.advance.AdvanceViewModel
import com.windscribe.mobile.ui.preferences.advance.AdvanceViewModelImpl
import com.windscribe.mobile.ui.preferences.connection.ConnectionViewModel
import com.windscribe.mobile.ui.preferences.connection.ConnectionViewModelImpl
import com.windscribe.mobile.ui.preferences.debug.DebugViewModel
import com.windscribe.mobile.ui.preferences.debug.DebugViewModelImpl
import com.windscribe.mobile.ui.preferences.email.EmailViewModel
import com.windscribe.mobile.ui.preferences.email.EmailViewModelImpl
import com.windscribe.mobile.ui.preferences.general.GeneralViewModel
import com.windscribe.mobile.ui.preferences.general.GeneralViewModelImpl
import com.windscribe.mobile.ui.preferences.help.HelpViewModel
import com.windscribe.mobile.ui.preferences.help.HelpViewModelImpl
import com.windscribe.mobile.ui.preferences.lipstick.LipstickViewmodel
import com.windscribe.mobile.ui.preferences.lipstick.LipstickViewmodelImpl
import com.windscribe.mobile.ui.preferences.main.MainMenuViewModel
import com.windscribe.mobile.ui.preferences.main.MainMenuViewModelImpl
import com.windscribe.mobile.ui.preferences.network_details.NetworkDetailViewModel
import com.windscribe.mobile.ui.preferences.network_details.NetworkDetailViewModelImpl
import com.windscribe.mobile.ui.preferences.network_options.NetworkOptionsViewModel
import com.windscribe.mobile.ui.preferences.network_options.NetworkOptionsViewModelImpl
import com.windscribe.mobile.ui.preferences.robert.RobertViewModel
import com.windscribe.mobile.ui.preferences.robert.RobertViewModelImpl
import com.windscribe.mobile.ui.preferences.split_tunnel.SplitTunnelViewModel
import com.windscribe.mobile.ui.preferences.split_tunnel.SplitTunnelViewModelImpl
import com.windscribe.mobile.ui.preferences.ticket.TicketViewModel
import com.windscribe.mobile.ui.preferences.ticket.TicketViewModelImpl
import com.windscribe.mobile.ui.serverlist.ConfigViewmodel
import com.windscribe.mobile.ui.serverlist.ConfigViewmodelImpl
import com.windscribe.mobile.ui.serverlist.ServerViewModel
import com.windscribe.mobile.ui.serverlist.ServerViewModelImpl
import com.windscribe.vpn.api.IApiCallManager
import com.windscribe.vpn.apppreference.PreferencesHelper
import com.windscribe.vpn.autoconnection.AutoConnectionManager
import com.windscribe.vpn.backend.ProxyDNSManager
import com.windscribe.vpn.backend.utils.WindVpnController
import com.windscribe.vpn.commonutils.ResourceHelper
import com.windscribe.vpn.decoytraffic.DecoyTrafficController
import com.windscribe.vpn.localdatabase.LocalDbInterface
import com.windscribe.vpn.repository.AdvanceParameterRepository
import com.windscribe.vpn.repository.BridgeApiRepository
import com.windscribe.vpn.repository.FavouriteRepository
import com.windscribe.vpn.repository.IpRepository
import com.windscribe.vpn.repository.LatencyRepository
import com.windscribe.vpn.repository.LocationRepository
import com.windscribe.vpn.repository.LogRepository
import com.windscribe.vpn.repository.NotificationRepository
import com.windscribe.vpn.repository.ServerListRepository
import com.windscribe.vpn.repository.StaticIpRepository
import com.windscribe.vpn.repository.UserRepository
import com.windscribe.vpn.services.FirebaseManager
import com.windscribe.vpn.services.sso.GoogleSignInManager
import com.windscribe.vpn.state.NetworkInfoManager
import com.windscribe.vpn.state.VPNConnectionStateManager
import com.windscribe.vpn.workers.WindScribeWorkManager
import dagger.Module
import dagger.Provides
import kotlinx.coroutines.CoroutineScope
import kotlin.jvm.java

@Module
class ComposeModule {
    @Provides
    @PerCompose
    fun getViewModelFactory(
        scope: CoroutineScope,
        windVpnController: WindVpnController,
        vpnConnectionStateManager: VPNConnectionStateManager,
        apiCallManager: IApiCallManager,
        appPreferenceHelper: PreferencesHelper,
        firebaseManager: FirebaseManager,
        notificationRepository: NotificationRepository,
        serverListRepository: ServerListRepository,
        staticIpRepository: StaticIpRepository,
        favouriteRepository: FavouriteRepository,
        ipRepository: IpRepository,
        locationRepository: LocationRepository,
        localDbInterface: LocalDbInterface,
        networkInfoManager: NetworkInfoManager,
        autoConnectionManager: AutoConnectionManager,
        latencyRepository: LatencyRepository,
        userRepository: UserRepository,
        googleSignInManager: GoogleSignInManager,
        workManager: WindScribeWorkManager,
        advanceParameterRepository: AdvanceParameterRepository,
        proxyDNSManager: ProxyDNSManager,
        decoyTrafficController: DecoyTrafficController,
        portMapRepository: com.windscribe.vpn.repository.PortMapRepository,
        logRepository: LogRepository,
        bridgeApiRepository: BridgeApiRepository,
        resourceHelper: ResourceHelper
    ): ViewModelProvider.Factory {
        return object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(EmergencyConnectViewModal::class.java)) {
                    return EmergencyConnectViewModal(
                        scope,
                        windVpnController,
                        vpnConnectionStateManager
                    ) as T
                } else if (modelClass.isAssignableFrom(AppStartViewModel::class.java)) {
                    return AppStartViewModelImpl(
                        appPreferenceHelper,
                        apiCallManager,
                        vpnConnectionStateManager,
                        googleSignInManager,
                        firebaseManager,
                        userRepository
                    ) as T
                } else if (modelClass.isAssignableFrom(LoginViewModel::class.java)) {
                    return LoginViewModel(
                        apiCallManager,
                        appPreferenceHelper,
                        firebaseManager,
                        userRepository
                    ) as T
                } else if (modelClass.isAssignableFrom(SignupViewModel::class.java)) {
                    return SignupViewModel(
                        apiCallManager,
                        appPreferenceHelper,
                        firebaseManager,
                        userRepository
                    ) as T
                } else if (modelClass.isAssignableFrom(NewsfeedViewmodel::class.java)) {
                    return NewsfeedViewmodel(
                        scope,
                        notificationRepository,
                        appPreferenceHelper
                    ) as T
                } else if (modelClass.isAssignableFrom(ServerViewModel::class.java)) {
                    return ServerViewModelImpl(
                        serverListRepository,
                        favouriteRepository,
                        staticIpRepository,
                        localDbInterface,
                        appPreferenceHelper,
                        latencyRepository
                    ) as T
                } else if (modelClass.isAssignableFrom(ConnectionViewmodel::class.java)) {
                    return ConnectionViewmodelImpl(
                        scope,
                        vpnConnectionStateManager,
                        windVpnController,
                        ipRepository,
                        networkInfoManager,
                        locationRepository,
                        localDbInterface,
                        appPreferenceHelper,
                        autoConnectionManager,
                        userRepository,
                        serverListRepository,
                        decoyTrafficController,
                        resourceHelper
                    ) as T
                } else if (modelClass.isAssignableFrom(ConfigViewmodel::class.java)) {
                    return ConfigViewmodelImpl(localDbInterface, latencyRepository) as T
                } else if (modelClass.isAssignableFrom(PowerWhitelistViewmodel::class.java)) {
                    return PowerWhitelistViewmodelImpl(appPreferenceHelper) as T
                } else if (modelClass.isAssignableFrom(SharedLinkViewmodel::class.java)) {
                    return SharedLinkViewmodelImpl(userRepository) as T
                } else if (modelClass.isAssignableFrom(HomeViewmodel::class.java)) {
                    return HomeViewmodelImpl(
                        vpnConnectionStateManager,
                        userRepository,
                        appPreferenceHelper
                    ) as T
                } else if (modelClass.isAssignableFrom(EditCustomConfigViewmodel::class.java)) {
                    return EditCustomConfigViewmodelImpl(localDbInterface, windVpnController) as T
                } else if (modelClass.isAssignableFrom(AppStartActivityViewModel::class.java)) {
                    return AppStartActivityViewModelImpl(appPreferenceHelper, apiCallManager) as T
                } else if (modelClass.isAssignableFrom(MainMenuViewModel::class.java)) {
                    return MainMenuViewModelImpl(userRepository) as T
                } else if (modelClass.isAssignableFrom(GeneralViewModel::class.java)) {
                    return GeneralViewModelImpl(appPreferenceHelper, userRepository) as T
                } else if (modelClass.isAssignableFrom(AccountViewModel::class.java)) {
                    return AccountViewModelImpl(userRepository, apiCallManager, workManager, appPreferenceHelper) as T
                } else if (modelClass.isAssignableFrom(ConnectionViewModel::class.java)) {
                    return ConnectionViewModelImpl(preferencesHelper = appPreferenceHelper, api = apiCallManager, autoConnectionManager, vpnConnectionStateManager, proxyDNSManager, decoyTrafficController, portMapRepository) as T
                } else if (modelClass.isAssignableFrom(RobertViewModel::class.java)) {
                    return RobertViewModelImpl(apiCallManager, appPreferenceHelper) as T
                } else if (modelClass.isAssignableFrom(LipstickViewmodel::class.java)) {
                    return LipstickViewmodelImpl(appPreferenceHelper, serverListRepository) as T
                } else if (modelClass.isAssignableFrom(HelpViewModel::class.java)) {
                    return HelpViewModelImpl(userRepository,logRepository) as T
                } else if (modelClass.isAssignableFrom(TicketViewModel::class.java)) {
                    return TicketViewModelImpl(userRepository, apiCallManager) as T
                } else if (modelClass.isAssignableFrom(AdvanceViewModel::class.java)) {
                    return AdvanceViewModelImpl(appPreferenceHelper, advanceParameterRepository) as T
                } else if (modelClass.isAssignableFrom(DebugViewModel::class.java)) {
                    return DebugViewModelImpl(logRepository) as T
                } else if (modelClass.isAssignableFrom(NetworkOptionsViewModel::class.java)) {
                    return NetworkOptionsViewModelImpl(appPreferenceHelper, networkInfoManager, localDbInterface) as T
                } else if (modelClass.isAssignableFrom(NetworkDetailViewModel::class.java)) {
                    return NetworkDetailViewModelImpl(localDbInterface, apiCallManager, appPreferenceHelper, networkInfoManager, windVpnController, vpnConnectionStateManager, portMapRepository) as T
                } else if (modelClass.isAssignableFrom(SplitTunnelViewModel::class.java)) {
                    return SplitTunnelViewModelImpl(appPreferenceHelper) as T
                } else if (modelClass.isAssignableFrom(EmailViewModel::class.java)) {
                    return EmailViewModelImpl(apiCallManager, userRepository, workManager) as T
                } else if (modelClass.isAssignableFrom(BridgeApiViewModel::class.java)) {
                    return BridgeApiViewModelImpl(
                        bridgeApiRepository,
                        locationRepository,
                        localDbInterface,
                        apiCallManager,
                        ipRepository,
                        appPreferenceHelper,
                        resourceHelper
                    ) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class")
            }
        }
    }
}