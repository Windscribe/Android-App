package com.windscribe.vpn.di

import com.windscribe.vpn.ServiceInteractor
import com.windscribe.vpn.autoconnection.AutoConnectionManager
import com.windscribe.vpn.backend.VpnBackendHolder
import com.windscribe.vpn.backend.utils.VPNProfileCreator
import com.windscribe.vpn.backend.utils.WindVpnController
import com.windscribe.vpn.repository.AdvanceParameterRepository
import com.windscribe.vpn.repository.EmergencyConnectRepository
import com.windscribe.vpn.repository.LocationRepository
import com.windscribe.vpn.repository.UserRepository
import com.windscribe.vpn.repository.WgConfigRepository
import com.windscribe.vpn.state.VPNConnectionStateManager
import dagger.Lazy
import dagger.Module
import dagger.Provides
import kotlinx.coroutines.CoroutineScope
import javax.inject.Singleton

@Module
class VPNModule {
    @Provides
    @Singleton
    fun provideWindVpnController(
        coroutineScope: CoroutineScope,
        serviceInteractor: ServiceInteractor,
        vpnProfileCreator: VPNProfileCreator,
        autoConnectionManager: AutoConnectionManager,
        vpnConnectionStateManager: VPNConnectionStateManager,
        vpnBackendHolder: VpnBackendHolder,
        locationRepository: LocationRepository,
        wgConfigRepository: WgConfigRepository,
        advanceParameterRepository: Lazy<AdvanceParameterRepository>,
        emergencyConnectRepository: EmergencyConnectRepository
    ): WindVpnController {
        return WindVpnController(
            coroutineScope,
            serviceInteractor,
            vpnProfileCreator,
            vpnConnectionStateManager,
            vpnBackendHolder,
            locationRepository,
            wgConfigRepository,
            advanceParameterRepository,
            autoConnectionManager,
            emergencyConnectRepository
        )
    }
}