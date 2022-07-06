package com.windscribe.vpn.mocks

import com.windscribe.vpn.ServiceInteractor
import com.windscribe.vpn.backend.VPNState
import com.windscribe.vpn.backend.VpnBackendHolder
import com.windscribe.vpn.backend.utils.ProtocolManager
import com.windscribe.vpn.backend.utils.VPNProfileCreator
import com.windscribe.vpn.backend.utils.WindVpnController
import com.windscribe.vpn.repository.LocationRepository
import com.windscribe.vpn.repository.UserRepository
import com.windscribe.vpn.repository.WgConfigRepository
import com.windscribe.vpn.state.VPNConnectionStateManager
import dagger.Lazy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class TestWindVpnController(
        scope: CoroutineScope,
        interactor: ServiceInteractor,
        vpnProfileCreator: VPNProfileCreator,
        private val vpnConnectionStateManager: VPNConnectionStateManager,
        vpnBackendHolder: VpnBackendHolder,
        locationRepository: LocationRepository,
        protocolManager: ProtocolManager,
        wgConfigRepository: WgConfigRepository,
        userRepository: Lazy<UserRepository>
) : WindVpnController(
        scope, interactor, vpnProfileCreator, vpnConnectionStateManager, vpnBackendHolder, locationRepository, protocolManager, wgConfigRepository,userRepository
) {
    var nextState: VPNState = VPNState(VPNState.Status.Connected)
    override fun launchVPNService() {
        scope.launch {
            vpnConnectionStateManager.setState(VPNState(VPNState.Status.Connecting))
            delay(5000)
            vpnConnectionStateManager.setState(nextState)
        }
    }
}