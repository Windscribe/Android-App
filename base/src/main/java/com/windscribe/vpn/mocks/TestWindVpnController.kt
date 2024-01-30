package com.windscribe.vpn.mocks

import com.windscribe.vpn.ServiceInteractor
import com.windscribe.vpn.autoconnection.AutoConnectionManager
import com.windscribe.vpn.autoconnection.ProtocolInformation
import com.windscribe.vpn.backend.VPNState
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import java.util.*

class TestWindVpnController(
        scope: CoroutineScope,
        interactor: ServiceInteractor,
        vpnProfileCreator: VPNProfileCreator,
        private val vpnConnectionStateManager: VPNConnectionStateManager,
        vpnBackendHolder: VpnBackendHolder,
        locationRepository: LocationRepository,
        autoConnectionManager: AutoConnectionManager,
        wgConfigRepository: WgConfigRepository,
        advanceParameterRepository: Lazy<AdvanceParameterRepository>,
        emergencyConnectRepository: EmergencyConnectRepository
) : WindVpnController(
    scope,
    interactor,
    vpnProfileCreator,
    vpnConnectionStateManager,
    vpnBackendHolder,
    locationRepository,
    wgConfigRepository,
    advanceParameterRepository,
    autoConnectionManager,
    emergencyConnectRepository
) {
    var mockState: VPNState = VPNState(VPNState.Status.Disconnected)
    override suspend fun launchVPNService(
        protocolInformation: ProtocolInformation,
        connectionId: UUID
    ) {
        delay(5000L)
        mockState.protocolInformation = protocolInformation
        vpnConnectionStateManager.setState(mockState)
    }
}