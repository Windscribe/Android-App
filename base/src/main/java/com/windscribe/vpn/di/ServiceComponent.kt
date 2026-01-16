/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.vpn.di

import com.windscribe.vpn.backend.ikev2.CharonVpnServiceWrapper
import com.windscribe.vpn.backend.openvpn.OpenVPNWrapperService
import com.windscribe.vpn.backend.wireguard.WireGuardWrapperService
import com.windscribe.vpn.bootreceiver.BootSessionService
import com.windscribe.vpn.services.AutoConnectService
import com.windscribe.vpn.services.DeviceStateService
import com.windscribe.vpn.services.DisconnectService
import com.windscribe.vpn.services.VpnTileService
import dagger.Component

@PerService
@Component(dependencies = [ApplicationComponent::class])
interface ServiceComponent {
    fun inject(wireGuardService: WireGuardWrapperService)
    fun inject(disconnectService: DisconnectService)
    fun inject(openVPNWrapperService: OpenVPNWrapperService)
    fun inject(deviceStateService: DeviceStateService)
    fun inject(bootSessionService: BootSessionService)
    fun inject(tileService: VpnTileService)
    fun inject(charonVpnServiceWrapper: CharonVpnServiceWrapper)
    fun inject(autoConnectService: AutoConnectService)
}
