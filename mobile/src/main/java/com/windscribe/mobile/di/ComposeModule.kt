package com.windscribe.mobile.di

import androidx.lifecycle.ViewModelProvider
import com.windscribe.mobile.lipstick.LipstickViewmodel
import com.windscribe.mobile.lipstick.LipstickViewmodelImpl
import com.windscribe.mobile.viewmodel.AppStartViewModel
import com.windscribe.mobile.viewmodel.ConfigViewmodel
import com.windscribe.mobile.viewmodel.ConfigViewmodelImpl
import com.windscribe.mobile.viewmodel.ConnectionViewmodel
import com.windscribe.mobile.viewmodel.ConnectionViewmodelImpl
import com.windscribe.mobile.viewmodel.EmergencyConnectViewModal
import com.windscribe.mobile.viewmodel.HomeViewmodel
import com.windscribe.mobile.viewmodel.HomeViewmodelImpl
import com.windscribe.mobile.viewmodel.LoginViewModel
import com.windscribe.mobile.viewmodel.NewsfeedViewmodel
import com.windscribe.mobile.viewmodel.PowerWhitelistViewmodel
import com.windscribe.mobile.viewmodel.PowerWhitelistViewmodelImpl
import com.windscribe.mobile.viewmodel.ServerViewModel
import com.windscribe.mobile.viewmodel.ServerViewModelImpl
import com.windscribe.mobile.viewmodel.SharedLinkViewmodel
import com.windscribe.mobile.viewmodel.SharedLinkViewmodelImpl
import com.windscribe.mobile.viewmodel.SignupViewModel
import com.windscribe.vpn.api.IApiCallManager
import com.windscribe.vpn.apppreference.PreferencesHelper
import com.windscribe.vpn.autoconnection.AutoConnectionManager
import com.windscribe.vpn.backend.utils.WindVpnController
import com.windscribe.vpn.localdatabase.LocalDbInterface
import com.windscribe.vpn.repository.FavouriteRepository
import com.windscribe.vpn.repository.IpRepository
import com.windscribe.vpn.repository.LatencyRepository
import com.windscribe.vpn.repository.LocationRepository
import com.windscribe.vpn.repository.NotificationRepository
import com.windscribe.vpn.repository.ServerListRepository
import com.windscribe.vpn.repository.StaticIpRepository
import com.windscribe.vpn.repository.UserRepository
import com.windscribe.vpn.services.FirebaseManager
import com.windscribe.vpn.state.NetworkInfoManager
import com.windscribe.vpn.state.VPNConnectionStateManager
import dagger.Module
import dagger.Provides
import kotlinx.coroutines.CoroutineScope

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
        userRepository: UserRepository
    ): ViewModelProvider.Factory {
        return object : ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(EmergencyConnectViewModal::class.java)) {
                    return EmergencyConnectViewModal(
                        scope,
                        windVpnController,
                        vpnConnectionStateManager
                    ) as T
                } else if (modelClass.isAssignableFrom(AppStartViewModel::class.java)) {
                    return AppStartViewModel(appPreferenceHelper, vpnConnectionStateManager) as T
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
                        serverListRepository
                    ) as T
                } else if (modelClass.isAssignableFrom(ConfigViewmodel::class.java)) {
                    return ConfigViewmodelImpl(localDbInterface, latencyRepository) as T
                } else if (modelClass.isAssignableFrom(LipstickViewmodel::class.java)) {
                    return LipstickViewmodelImpl(appPreferenceHelper, serverListRepository) as T
                } else if (modelClass.isAssignableFrom(PowerWhitelistViewmodel::class.java)) {
                    return PowerWhitelistViewmodelImpl(appPreferenceHelper) as T
                } else if (modelClass.isAssignableFrom(SharedLinkViewmodel::class.java)) {
                    return SharedLinkViewmodelImpl(appPreferenceHelper) as T
                } else if (modelClass.isAssignableFrom(HomeViewmodel::class.java)) {
                    return HomeViewmodelImpl(
                        vpnConnectionStateManager,
                        userRepository,
                        appPreferenceHelper
                    ) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class")
            }
        }
    }
}