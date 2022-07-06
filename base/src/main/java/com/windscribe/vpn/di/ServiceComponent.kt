/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.vpn.di

import com.windscribe.vpn.backend.ikev2.CharonVpnServiceWrapper
import com.windscribe.vpn.backend.openvpn.OpenVPNWrapperService
import com.windscribe.vpn.backend.wireguard.WireGuardWrapperService
import com.windscribe.vpn.bootreceiver.BootSessionService
import com.windscribe.vpn.services.DeviceStateService
import com.windscribe.vpn.services.DisconnectService
import com.windscribe.vpn.services.NetworkWhiteListService
import com.windscribe.vpn.services.VpnTileService
import com.windscribe.vpn.services.ping.PingTestService
import com.windscribe.vpn.services.verify.VerifyAmazonPurchaseService
import com.windscribe.vpn.services.verify.VerifyGooglePurchaseService
import dagger.Component

@PerService
@Component(dependencies = [ApplicationComponent::class], modules = [ServiceModule::class])
interface ServiceComponent {
    fun inject(wireGuardService: WireGuardWrapperService)
    fun inject(noNetworkService: NetworkWhiteListService)
    fun inject(disconnectService: DisconnectService)
    fun inject(openVPNWrapperService: OpenVPNWrapperService)
    fun inject(deviceStateService: DeviceStateService)
    fun inject(pingTestService: PingTestService)
    fun inject(bootSessionService: BootSessionService)
    fun inject(tileService: VpnTileService)
    fun inject(purchaseService: VerifyGooglePurchaseService)
    fun inject(amazonPurchaseService: VerifyAmazonPurchaseService)
    fun inject(charonVpnServiceWrapper: CharonVpnServiceWrapper)
}
