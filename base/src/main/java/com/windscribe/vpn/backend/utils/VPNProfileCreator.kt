/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.backend.utils

import android.content.Context
import android.net.DhcpInfo
import android.net.wifi.WifiManager
import android.util.Base64
import com.google.gson.Gson
import com.windscribe.vpn.Windscribe.Companion.appContext
import com.windscribe.vpn.api.response.ServerCredentialsResponse
import com.windscribe.vpn.apppreference.PreferencesHelper
import com.windscribe.vpn.backend.Util
import com.windscribe.vpn.backend.Util.saveProfile
import com.windscribe.vpn.backend.Util.saveSelectedLocation
import com.windscribe.vpn.backend.openvpn.WindStunnelUtility
import com.windscribe.vpn.backend.wireguard.WireGuardVpnProfile
import com.windscribe.vpn.commonutils.WindUtilities
import com.windscribe.vpn.commonutils.WindUtilities.ConfigType.WIRE_GUARD
import com.windscribe.vpn.constants.NetworkErrorCodes.ERROR_VALID_CONFIG_NOT_FOUND
import com.windscribe.vpn.constants.PreferencesKeyConstants
import com.windscribe.vpn.constants.VpnPreferenceConstants
import com.windscribe.vpn.exceptions.InvalidVPNConfigException
import com.windscribe.vpn.exceptions.WindScribeException
import com.windscribe.vpn.repository.CallResult
import com.windscribe.vpn.repository.WgConfigRepository
import com.windscribe.vpn.repository.WgRemoteParams
import com.windscribe.vpn.serverlist.entity.ConfigFile
import com.wireguard.config.Attribute
import com.wireguard.config.Config
import com.wireguard.config.Interface
import com.wireguard.config.Interface.Builder
import com.wireguard.config.Peer
import de.blinkt.openvpn.core.ConfigParser
import java.io.BufferedReader
import java.io.Reader
import java.io.StringReader
import java.math.BigInteger
import java.net.Inet4Address
import java.net.InetAddress
import java.net.NetworkInterface
import java.nio.ByteOrder
import java.util.SortedSet
import java.util.TreeSet
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton
import org.slf4j.LoggerFactory
import org.strongswan.android.data.VpnProfile
import org.strongswan.android.data.VpnProfile.SelectedAppsHandling.SELECTED_APPS_DISABLE
import org.strongswan.android.data.VpnProfile.SelectedAppsHandling.SELECTED_APPS_EXCLUDE
import org.strongswan.android.data.VpnProfile.SelectedAppsHandling.SELECTED_APPS_ONLY
import org.strongswan.android.data.VpnType

@Singleton
class VPNProfileCreator @Inject constructor(
        private val preferencesHelper: PreferencesHelper,
        private val wgConfigRepository: WgConfigRepository
) {

    private val logger = LoggerFactory.getLogger("profile_creator")
    var wgForceInit = AtomicBoolean(false)
    private val publicIpV4Array = arrayOf(
            "0.0.0.0/5", "8.0.0.0/7", "11.0.0.0/8", "12.0.0.0/6",
            "16.0.0.0/4", "32.0.0.0/3",
            "64.0.0.0/2", "128.0.0.0/3", "160.0.0.0/5", "168.0.0.0/6", "172.0.0.0/12",
            "172.32.0.0/11", "172.64.0.0/10", "172.128.0.0/9", "173.0.0.0/8", "174.0.0.0/7",
            "176.0.0.0/4", "192.0.0.0/9", "192.128.0.0/11", "192.160.0.0/13", "192.169.0.0/16",
            "192.170.0.0/15", "192.172.0.0/14", "192.176.0.0/12", "192.192.0.0/10",
            "193.0.0.0/8", "194.0.0.0/7", "196.0.0.0/6", "200.0.0.0/5", "208.0.0.0/4", "10.255.255.0/24"
    )

    fun createIkEV2Profile(
            location: LastSelectedLocation,
            vpnParameters: VPNParameters,
            config: ProtocolConfig
    ): String {
        logger.info("creating IKEv2 Profile.")
        // Vpn profile
        val profile = VpnProfile()
        profile.id = 1
        profile.uuid = UUID.randomUUID()
        profile.name = vpnParameters.hostName
        //Changing it to use altered ip instead of hostname.
        //profile.gateway = vpnParameters.hostName
        profile.gateway = vpnParameters.ikev2Ip
        profile.remoteId = vpnParameters.hostName
        profile.vpnType = VpnType.fromIdentifier("ikev2-eap")
        if (preferencesHelper.isKeepAliveModeAuto) {
            profile.natKeepAlive = 20
        } else {
            profile.natKeepAlive = Integer.valueOf(preferencesHelper.keepAlive)
        }
        preferencesHelper.selectedProtocol = config.protocol
        preferencesHelper.selectedPort = config.port
        preferencesHelper.selectedIp = vpnParameters.hostName

        // Mtu
        if (!preferencesHelper.isPackageSizeModeAuto && preferencesHelper.packetSize != -1) {
            profile.mtu = preferencesHelper.packetSize
        } else {
            profile.mtu = 1300
        }
        // Split tunnel
        setSplitMode(profile)
        // Lan bypass
        if (preferencesHelper.lanByPass) {
            val subNetBuilder = StringBuilder()
            subNetBuilder.append("255.255.255.255/32 ")
            subNetBuilder.append("224.0.0.0/24 ")
            val manager = appContext.applicationContext
                    .getSystemService(Context.WIFI_SERVICE) as WifiManager
            val wifInfo = manager.dhcpInfo
            val gatewayAddress = gatewayAddressAsString(wifInfo)
            val mask = subnetMask
            if (gatewayAddress != null && mask != null) {
                logger.info("Mask range:$mask")
                logger.info("Gateway address:$gatewayAddress")
                val range = "$gatewayAddress/$mask"
                subNetBuilder.append(range)
            } else {
                logger.info("Failed to set lan by pass for gateway ip")
            }
            logger.info("Excluded Subnet: $subNetBuilder")
            profile.excludedSubnets = subNetBuilder.toString()
        }

        val apps: SortedSet<String> = TreeSet(preferencesHelper.installedApps())
        profile.setSelectedApps(apps)
        val credentials = getIkev2Credentials()
        profile.username = credentials.first
        profile.password = credentials.second
        saveSelectedLocation(location)
        saveProfile(profile)
        return "$location"
    }

    fun createOpenVpnProfile(
            lastSelectedLocation: LastSelectedLocation,
            vpnParameters: VPNParameters,
            protocolConfig: ProtocolConfig
    ): String {
        logger.info("Creating open vpn profile.")
        // Create a new profile
        val profile = de.blinkt.openvpn.VpnProfile("Windscribe")
        profile.mUsePull = true
        profile.mUseTLSAuth = false
        profile.mCaFilename = "[inline]"
        profile.mAuthenticationType = de.blinkt.openvpn.VpnProfile.TYPE_USERPASS
        profile.mUseUdp = false

        // Lan by pass
        profile.mAllowLocalLAN = preferencesHelper.lanByPass

        // Split Routing
        if (preferencesHelper.splitTunnelToggle && (preferencesHelper.splitRoutingMode == PreferencesKeyConstants.EXCLUSIVE_MODE)) {
            preferencesHelper.lastConnectedUsingSplit = true
            profile.mAllowedAppsVpnAreDisallowed = true
        } else if (preferencesHelper.splitTunnelToggle && (preferencesHelper.splitRoutingMode == PreferencesKeyConstants.INCLUSIVE_MODE)) {
            preferencesHelper.lastConnectedUsingSplit = true
            profile.mAllowedAppsVpnAreDisallowed = false
        } else {
            preferencesHelper.lastConnectedUsingSplit = false
            if (profile.mAllowedAppsVpn != null) {
                profile.mAllowedAppsVpn.clear()
            }
            profile.mAllowedAppsVpnAreDisallowed = true
        }

        // MTU
        if (!preferencesHelper.isPackageSizeModeAuto && preferencesHelper.packetSize != -1) {
            profile.mTunMtu = preferencesHelper.packetSize
        }
        var port: String? = null
        var protocol: String? = null
        var serverConfig: String? = null
        var stunnelRoutingIp: String? = null
        var ip: String? = null
        try {
            if (PreferencesKeyConstants.PROTO_STEALTH == protocolConfig.protocol) {
                serverConfig = preferencesHelper.getOpenVPNServerConfig()
                port = VpnPreferenceConstants.STUNNEL_LOCAL_PORT
                protocol = VpnPreferenceConstants.STUNNEL_VPN_PROTOCOL
                stunnelRoutingIp = vpnParameters.stealthIp
                ip = VpnPreferenceConstants.STUNNEL_LOCAL_IP
            }
            if (PreferencesKeyConstants.PROTO_TCP == protocolConfig.protocol) {
                ip = vpnParameters.tcpIp
                protocol = "tcp"
                serverConfig = preferencesHelper.getOpenVPNServerConfig()
                port = protocolConfig.port
                stunnelRoutingIp = null
            }
            if (PreferencesKeyConstants.PROTO_UDP == protocolConfig.protocol) {
                ip = vpnParameters.udpIp
                protocol = "udp"
                serverConfig = preferencesHelper.getOpenVPNServerConfig()
                port = protocolConfig.port
                stunnelRoutingIp = null
            }
            if (serverConfig != null) {
                profile.writeConfigFile(
                        appContext,
                        serverConfig,
                        ip,
                        protocol,
                        port,
                        stunnelRoutingIp,
                        vpnParameters.ovpnX509
                )
            }else{
                throw InvalidVPNConfigException(CallResult.Error(ERROR_VALID_CONFIG_NOT_FOUND,"OpenVPN Server config not found."))
            }
        } catch (e: Exception) {
            logger.debug(e.toString())
        }
        val credentials = addOpenVpnCredentials()
        profile.mUsername = credentials.first
        profile.mPassword = credentials.second

        if (preferencesHelper.splitTunnelToggle) {
            profile.mAllowedAppsVpn = HashSet(preferencesHelper.installedApps())
        }
        if (protocolConfig.protocol == PreferencesKeyConstants.PROTO_STEALTH) {
            if (WindStunnelUtility.isStunnelRunning) {
                WindStunnelUtility.stopLocalTunFromAppContext(appContext)
            }
            preferencesHelper.selectedPort = protocolConfig.port
            WindUtilities.writeStunnelConfig(appContext, stunnelRoutingIp, protocolConfig.port)
            WindStunnelUtility.startLocalTun()
        }
        saveSelectedLocation(lastSelectedLocation)
        saveProfile(profile)
        return "$lastSelectedLocation"
    }

    fun createVpnProfileFromConfig(configFile: ConfigFile): Pair<String, ProtocolConfig> {
        val content = configFile.content
        return if (WindUtilities.getConfigType(content) == WIRE_GUARD) {
            Pair(createVpnProfileFromWireGuardConfig(configFile), Util.getProtocolConfigForWireguard(configFile.content))
        } else {
            Pair(createVpnProfileFromOpenVpnConfig(configFile), Util.getProtocolConfigForOpenVPN(configFile.content))
        }
    }

    private fun createVpnProfileFromOpenVpnConfig(configFile: ConfigFile): String {
        val configParser = ConfigParser()
        val reader = StringReader(configFile.content)
        try {
            configParser.parseConfig(reader)
        } catch (e: Exception) {
            throw e
        }
        logger.info("Writing config file options to profile.")
        val profile = configParser.convertProfile()
        profile.mUsername = configFile.username
        profile.mPassword = configFile.password
        logger.info("Adding application settings to profile.")
        // Lan by pass
        profile.mAllowLocalLAN = preferencesHelper.lanByPass

        // Split Routing
        if (preferencesHelper.splitTunnelToggle && (preferencesHelper.splitRoutingMode == PreferencesKeyConstants.EXCLUSIVE_MODE)) {
            preferencesHelper.lastConnectedUsingSplit = true
            profile.mAllowedAppsVpnAreDisallowed = true
        } else if (preferencesHelper.splitTunnelToggle && (preferencesHelper.splitRoutingMode == PreferencesKeyConstants.INCLUSIVE_MODE)) {
            preferencesHelper.lastConnectedUsingSplit = true
            profile.mAllowedAppsVpnAreDisallowed = false
        } else {
            preferencesHelper.lastConnectedUsingSplit = false
            if (profile.mAllowedAppsVpn != null) {
                profile.mAllowedAppsVpn.clear()
            }
            profile.mAllowedAppsVpnAreDisallowed = true
        }

        // MTU
        if (!preferencesHelper.isPackageSizeModeAuto && preferencesHelper.packetSize != -1) {
            profile.mTunMtu = preferencesHelper.packetSize
        }
        logger.info("Adding location meta data to profile.")
        val lastSelectedLocation =
                LastSelectedLocation(configFile.getPrimaryKey(), nickName = configFile.name)
        saveSelectedLocation(lastSelectedLocation)
        profile.writeConfigFile(appContext)
        profile.mAllowedAppsVpn = HashSet(preferencesHelper.installedApps())
        saveProfile(profile)
        return "Custom Config: ${profile.mServerName} ${profile.mServerPort}"
    }

    private fun createVpnProfileFromWireGuardConfig(configFile: ConfigFile): String {
        val mPreferencesHelper = appContext.preference
        val interFaceBuilder = Builder()
        if (preferencesHelper.splitTunnelToggle) {
            preferencesHelper.lastConnectedUsingSplit = true
            if (preferencesHelper.splitRoutingMode == PreferencesKeyConstants.INCLUSIVE_MODE) {
                interFaceBuilder.includeApplications(preferencesHelper.installedApps())
            } else {
                interFaceBuilder.excludeApplications(preferencesHelper.installedApps())
            }
        } else {
            preferencesHelper.lastConnectedUsingSplit = false
        }

        val reader: Reader = StringReader(configFile.content)
        val bufferedReader = BufferedReader(reader)
        val config: Config = bufferedReader.use {
            Config.parse(bufferedReader)
        }
        interFaceBuilder.parsePrivateKey(config.getInterface().keyPair.privateKey.toBase64())
        interFaceBuilder.addAddresses(config.getInterface().addresses)
        interFaceBuilder.addDnsServers(config.getInterface().dnsServers)
        if (!mPreferencesHelper.isPackageSizeModeAuto) {
            interFaceBuilder.setMtu(mPreferencesHelper.packetSize)
        }
        val configWithSettings = Config.Builder()
                .addPeers(config.peers)
                .setInterface(interFaceBuilder.build())
                .build()
        val lastSelectedLocation =
                LastSelectedLocation(configFile.getPrimaryKey(), nickName = configFile.name)
        saveSelectedLocation(lastSelectedLocation)
        saveProfile(WireGuardVpnProfile(configWithSettings.toWgQuickString()))
        return "Custom Config: ${configWithSettings.toWgQuickString()}"
    }

    suspend fun updateWireGuardConfig(config: Config): CallResult<Config> {
        val builder = Config.Builder()
        val serverPublicKey = config.peers[0].publicKey.toBase64()
        val hostName = config.`interface`.addresses.first().address.hostName
        val ip = config.peers[0].endpoint.get().host
        val port = config.peers[0].endpoint.get().port.toString()
        logger.debug("Requesting wg remote params.")
        when (val remoteParamsResponse = wgConfigRepository.getWgParams(hostName, serverPublicKey, wgForceInit.getAndSet(false), true)) {
            is CallResult.Success<WgRemoteParams> -> {
                logger.debug("Wg remote params successful.")
                val anInterface = createWireGuardInterface(remoteParamsResponse.data)
                builder.setInterface(anInterface)
                val peer = createWireGuardPeer(remoteParamsResponse.data, ip, port)
                builder.addPeer(peer)
                val content = builder.build().toWgQuickString()
                val profileLines = content.split(System.lineSeparator().toRegex()).toTypedArray()
                val stringBuilder = StringBuilder()
                for (logLine in profileLines) {
                    if (!logLine.startsWith("PrivateKey") && !logLine.startsWith("PreSharedKey") && !logLine.startsWith(
                                    "PublicKey"
                            )
                    ) {
                        stringBuilder.append(logLine).append(" ")
                    }
                }
                logger.debug(stringBuilder.toString())
                saveProfile(WireGuardVpnProfile(content))
                return CallResult.Success(WireGuardVpnProfile.createConfigFromString(content))
            }
            is CallResult.Error -> {
                logger.debug("Error getting Wg remote params.")
                return remoteParamsResponse
            }
        }
    }

    suspend fun createVpnProfileFromWireGuardProfile(
            lastSelectedLocation: LastSelectedLocation,
            vpnParameters: VPNParameters,
            config: ProtocolConfig
    ): String {
        val builder = Config.Builder()
        when (val remoteParamsResponse = wgConfigRepository.getWgParams(vpnParameters.hostName, vpnParameters.publicKey, wgForceInit.getAndSet(false))) {
            is CallResult.Success<WgRemoteParams> -> {
                val anInterface = createWireGuardInterface(remoteParamsResponse.data)
                builder.setInterface(anInterface)
                val peer = createWireGuardPeer(remoteParamsResponse.data, vpnParameters.stealthIp, config.port)
                builder.addPeer(peer)

                val content = builder.build().toWgQuickString()
                val profileLines = content.split(System.lineSeparator().toRegex()).toTypedArray()
                val stringBuilder = StringBuilder()
                for (logLine in profileLines) {
                    if (!logLine.startsWith("PrivateKey") && !logLine.startsWith("PreSharedKey") && !logLine.startsWith(
                                    "PublicKey"
                            )
                    ) {
                        stringBuilder.append(logLine).append(" ")
                    }
                }
                logger.debug(stringBuilder.toString())
                saveSelectedLocation(lastSelectedLocation)
                saveProfile(WireGuardVpnProfile(content))
                return "$lastSelectedLocation"
            }
            is CallResult.Error -> {
                throw InvalidVPNConfigException(remoteParamsResponse)
            }
            else -> {
                throw WindScribeException("Unexpected Error creating Wg profile")
            }
        }
    }

    private fun createWireGuardInterface(wgRemoteParams: WgRemoteParams): Interface {
        val builder = Builder()
        builder.parsePrivateKey(wgRemoteParams.privateKey)
        builder.parseAddresses(wgRemoteParams.address)
        builder.parseDnsServers(wgRemoteParams.dns)
        if (!preferencesHelper.isPackageSizeModeAuto && preferencesHelper.packetSize != -1) {
            builder.setMtu(preferencesHelper.packetSize)
        }
        if(preferencesHelper.isDecoyTrafficOn){
            builder.setMtu(100)
        }
        if (preferencesHelper.splitTunnelToggle) {
            preferencesHelper.lastConnectedUsingSplit = true
            if (preferencesHelper.splitRoutingMode == PreferencesKeyConstants.INCLUSIVE_MODE) {
                builder.includeApplications(preferencesHelper.installedApps())
            } else {
                builder.excludeApplications(preferencesHelper.installedApps())
            }
        } else {
            preferencesHelper.lastConnectedUsingSplit = false
        }
        return builder.build()
    }

    private fun createWireGuardPeer(wgRemoteParams: WgRemoteParams, endpoint: String, port: String): Peer {
        val builder = Peer.Builder()
        builder.parsePublicKey(wgRemoteParams.serverPublicKey)
        val lanByPass = preferencesHelper.lanByPass
        val modifiedAllowedIps = modifyAllowedIps(
                wgRemoteParams.allowedIPs,
                wgRemoteParams.dns
        )
        builder.parseAllowedIPs(
                if (lanByPass) modifiedAllowedIps else wgRemoteParams.allowedIPs
        )
        val sb = endpoint +
                ":" +
                port
        builder.parseEndpoint(sb)
        builder.setPersistentKeepalive(25)
        builder.parsePreSharedKey(wgRemoteParams.preSharedKey)
        return builder.build()
    }

    private fun getIkev2Credentials(): Pair<String, String> {
        val serverCredentials = getServerCredentials(true)
        val mUsername: String
        val mPassword: String
        if (preferencesHelper.isConnectingToStaticIp) {
            mUsername = serverCredentials.userNameEncoded
            mPassword = serverCredentials.passwordEncoded
        } else {
            mUsername = String(
                    Base64
                            .decode(serverCredentials.userNameEncoded, Base64.DEFAULT)
            )
            mPassword = String(
                    Base64
                            .decode(serverCredentials.passwordEncoded, Base64.DEFAULT)
            )
        }
        return Pair(mUsername, mPassword)
    }

    private fun addOpenVpnCredentials(): Pair<String, String> {
        val credentials = getServerCredentials(false)
        val username: String
        val password: String
        if (preferencesHelper.isConnectingToStaticIp) {
            username = credentials.userNameEncoded
            password = credentials.passwordEncoded
        } else {
            username = String(Base64.decode(credentials.userNameEncoded, Base64.DEFAULT))
            password = String(Base64.decode(credentials.passwordEncoded, Base64.DEFAULT))
        }
        return Pair(username, password)
    }

    private fun gatewayAddressAsString(wifiInfo: DhcpInfo): String? {
        return try {
            val ipAddress: Int = if (ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN) {
                Integer.reverseBytes(wifiInfo.gateway)
            } else {
                wifiInfo.gateway
            }
            var ipByteArray = BigInteger.valueOf(ipAddress.toLong()).toByteArray()
            logger.info("Gateway address :$ipAddress")
            if (ipByteArray.size > 16) {
                logger.info("Fixing illegal length")
                val bytes2 = ByteArray(16)
                System.arraycopy(ipByteArray, 1, bytes2, 0, bytes2.size)
                ipByteArray = bytes2
            }
            InetAddress.getByAddress(ipByteArray).hostAddress
        } catch (e: Exception) {
            logger.info("Failed to get Gateway address: $e")
            null
        }
    }

    private fun getServerCredentials(
            ikEV2: Boolean
    ): ServerCredentialsResponse {
        return when {
            preferencesHelper.isConnectingToStaticIp -> {
               preferencesHelper.getCredentials(PreferencesKeyConstants.STATIC_IP_CREDENTIAL)
            }
            ikEV2 -> {
                preferencesHelper.getCredentials(PreferencesKeyConstants.IKEV2_CREDENTIALS)?: kotlin.run {
                    preferencesHelper.setUserAccountUpdateRequired(true)
                    null
                }
            }
            else -> {
               preferencesHelper.getCredentials(PreferencesKeyConstants.OPEN_VPN_CREDENTIALS)
            }
        }?:throw InvalidVPNConfigException(CallResult.Error(ERROR_VALID_CONFIG_NOT_FOUND, "valid server credential not found."))
    }

    private val subnetMask: String?
        get() {
            try {
                val interfaces = NetworkInterface.getNetworkInterfaces()
                while (interfaces.hasMoreElements()) {
                    val networkInterface = interfaces.nextElement()
                    if (networkInterface.isLoopback) {
                        continue
                    }
                    logger.info(
                            "Interface Name:" + networkInterface.displayName + "  Addresses: " + networkInterface
                                    .interfaceAddresses.toString()
                    )
                    for (interfaceAddress in networkInterface.interfaceAddresses) {
                        val broadcast = interfaceAddress.broadcast
                        if (broadcast is Inet4Address) {
                            logger.info("chosen address: $interfaceAddress")
                            return interfaceAddress.networkPrefixLength.toString()
                        }
                    }
                }
            } catch (ex: Exception) {
                logger.info("Failed to get Subnet mask: $ex")
                return null
            }
            logger.info("No interface found...")
            return null
        }

    private fun getUserCredentials(jsonString: String?): ServerCredentialsResponse {
        return Gson().fromJson(jsonString, ServerCredentialsResponse::class.java)
    }

    private fun modifyAllowedIps(allowedIps: String, dnsRoutes: String): String {
        val ipv4PublicNetworks = HashSet(listOf(*publicIpV4Array))
        val ipv4Wildcard = "0.0.0.0/0"
        val allNetworks = HashSet(listOf(ipv4Wildcard))
        val input: Collection<String> = HashSet(listOf(*Attribute.split(allowedIps)))
        val outputSize = input.size - allNetworks.size + ipv4PublicNetworks.size
        val output: MutableCollection<String?> = LinkedHashSet(outputSize)
        var replaced = false
        for (network in input) {
            if (allNetworks.contains(network)) {
                if (!replaced) {
                    for (replacement in ipv4PublicNetworks) {
                        if (!output.contains(replacement)) {
                            output.add(replacement)
                        }
                    }
                    replaced = true
                }
            } else if (!output.contains(network)) {
                output.add(network)
            }
        }
        output.addAll(listOf(*Attribute.split(dnsRoutes)))
        return Attribute.join(output)
    }

    private fun setSplitMode(profile: VpnProfile) {
        if (preferencesHelper.splitTunnelToggle && (preferencesHelper.splitRoutingMode == PreferencesKeyConstants.EXCLUSIVE_MODE)) {
            preferencesHelper.lastConnectedUsingSplit = true
            profile.selectedAppsHandling = SELECTED_APPS_EXCLUDE
        } else if (preferencesHelper.splitTunnelToggle && (preferencesHelper.splitRoutingMode == PreferencesKeyConstants.INCLUSIVE_MODE)) {
            preferencesHelper.lastConnectedUsingSplit = true
            profile.selectedAppsHandling = SELECTED_APPS_ONLY
        } else {
            preferencesHelper.lastConnectedUsingSplit = false
            profile.selectedAppsHandling = SELECTED_APPS_DISABLE
        }
    }
}
