/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.backend.utils

import com.windscribe.common.tunnel.TunnelManager
import com.windscribe.vpn.Windscribe
import com.windscribe.vpn.apppreference.PreferencesHelper
import com.windscribe.vpn.apppreference.PreferencesKeyConstants.PROTO_IKev2
import com.windscribe.vpn.autoconnection.AutoConnectionManager
import com.windscribe.vpn.autoconnection.ProtocolConnectionStatus
import com.windscribe.vpn.autoconnection.ProtocolInformation
import com.windscribe.vpn.backend.VpnBackendHolder
import com.windscribe.vpn.backend.tunnel.VpnServiceGate
import com.windscribe.vpn.commonutils.WindUtilities
import com.windscribe.vpn.localdatabase.LocalDbInterface
import com.windscribe.vpn.repository.AdvanceParameterRepository
import com.windscribe.vpn.repository.EmergencyConnectRepository
import com.windscribe.vpn.repository.LocationRepository
import com.windscribe.vpn.repository.WgConfigRepository
import com.windscribe.vpn.serverlist.entity.Datacenter
import com.windscribe.vpn.serverlist.entity.DatacenterAndLocation
import com.windscribe.vpn.serverlist.entity.Server
import com.windscribe.vpn.state.ConnectionPolicyInputs
import com.windscribe.vpn.state.DeviceStateManager
import com.windscribe.vpn.state.VPNConnectionStateManager
import dagger.Lazy
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.slot
import io.mockk.unmockkObject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
class WindVpnControllerForceDisconnectTest {
    private lateinit var preferences: PreferencesHelper
    private lateinit var profileCreator: VPNProfileCreator
    private lateinit var vpnStateManager: VPNConnectionStateManager
    private lateinit var vpnBackendHolder: VpnBackendHolder
    private lateinit var locationRepository: LocationRepository
    private lateinit var wgConfigRepository: WgConfigRepository
    private lateinit var advanceParameterRepository: AdvanceParameterRepository
    private lateinit var advanceParameterRepositoryLazy: Lazy<AdvanceParameterRepository>
    private lateinit var autoConnectionManager: AutoConnectionManager
    private lateinit var emergencyConnectRepository: EmergencyConnectRepository
    private lateinit var localDb: LocalDbInterface
    private lateinit var deviceStateManager: DeviceStateManager
    private lateinit var excludedIpHolder: ExcludedIpHolder
    private lateinit var tunnelManagerLazy: Lazy<TunnelManager>
    private lateinit var serviceGate: VpnServiceGate
    private lateinit var policyInputs: ConnectionPolicyInputs

    @Before
    fun setUp() {
        preferences = mockk(relaxed = true)
        profileCreator = mockk(relaxed = true)
        vpnStateManager = mockk(relaxed = true)
        vpnBackendHolder = mockk(relaxed = true)
        locationRepository = mockk(relaxed = true)
        wgConfigRepository = mockk(relaxed = true)
        advanceParameterRepository = mockk(relaxed = true)
        advanceParameterRepositoryLazy =
            mockk {
                every { get() } returns advanceParameterRepository
            }
        autoConnectionManager = mockk(relaxed = true)
        emergencyConnectRepository = mockk(relaxed = true)
        localDb = mockk(relaxed = true)
        deviceStateManager = mockk(relaxed = true)
        excludedIpHolder = mockk(relaxed = true)
        tunnelManagerLazy =
            mockk {
                every { get() } returns mockk(relaxed = true)
            }
        serviceGate = mockk(relaxed = true)
        policyInputs = mockk(relaxed = true)

        Windscribe.appContext = mockk(relaxed = true)
        mockkObject(WindUtilities)
        every { WindUtilities.getSourceTypeBlocking() } returns SelectedLocationType.CityLocation
    }

    @After
    fun tearDown() {
        unmockkObject(WindUtilities)
    }

    private fun server(
        id: Int,
        hostname: String,
        weight: Int,
        forceDisconnect: Int,
    ): Server =
        Server(
            id = id,
            hostname = hostname,
            ip = "192.0.2.$id",
            ip2 = "198.51.100.$id",
            ip3 = "203.0.113.$id",
            datacenterId = 7,
            weight = weight,
            health = 10,
            forceDisconnect = forceDisconnect,
        )

    private fun TestScope.controller(): WindVpnController =
        object : WindVpnController(
            this,
            preferences,
            profileCreator,
            vpnStateManager,
            vpnBackendHolder,
            locationRepository,
            wgConfigRepository,
            advanceParameterRepositoryLazy,
            autoConnectionManager,
            emergencyConnectRepository,
            localDb,
            deviceStateManager,
            excludedIpHolder,
            tunnelManagerLazy,
            serviceGate,
            policyInputs,
        ) {
            override suspend fun launchVPNService(
                protocolInformation: ProtocolInformation,
                connectionId: UUID,
            ) = Unit
        }

    /**
     * The permission dialog can sit open for as long as the user leaves it, so the attempt that
     * asked for it may be long gone by the time they grant it.
     */
    @Test
    fun `a permission grant for an abandoned attempt does not start a backend`() =
        runTest {
            val controller = controller()

            controller.connectWithGrantedPermission(mockk(relaxed = true), UUID.randomUUID())

            coVerify(exactly = 0) { vpnBackendHolder.connect(any(), any()) }
        }

    @Test
    fun `new connection skips force disconnected node and selects eligible sibling`() =
        runTest {
            val datacenter =
                Datacenter().apply {
                    id = 7
                    nodeName = "Seattle"
                    nickName = "Cobain"
                    coordinates = "47.61,-122.33"
                }
            val datacenterAndLocation =
                DatacenterAndLocation().apply {
                    this.datacenter = datacenter
                    location = null
                }
            val draining = server(id = 1, hostname = "draining.example", weight = 1, forceDisconnect = 1)
            val eligible = server(id = 2, hostname = "eligible.example", weight = 0, forceDisconnect = 0)
            val protocol = ProtocolInformation(PROTO_IKev2, "500", "IKEv2", ProtocolConnectionStatus.Disconnected)
            val selectedParameters = slot<VPNParameters>()

            every { preferences.selectedCity } returns 7
            every { vpnBackendHolder.activeBackend } returns null
            every { advanceParameterRepository.getForceNode() } returns null
            every { localDb.getDatacenterAndLocation(7) } returns datacenterAndLocation
            coEvery { localDb.getServersByDatacenter(7) } returns listOf(draining, eligible)
            coEvery { localDb.getFavouritesAsync() } returns emptyList()
            coEvery { locationRepository.updateLocation() } returns 7
            every {
                profileCreator.createIkEV2Profile(
                    any(),
                    capture(selectedParameters),
                    protocol,
                )
            } returns "Seattle - Cobain"

            controller().connect(ConnectSource.User, protocolInformation = protocol)

            assertEquals("eligible.example", selectedParameters.captured.hostName)
        }
}
