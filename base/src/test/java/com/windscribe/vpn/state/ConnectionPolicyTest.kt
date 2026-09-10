package com.windscribe.vpn.state

import com.windscribe.vpn.backend.VPNState
import com.windscribe.vpn.model.User
import com.windscribe.vpn.state.ConnectionPolicy.Effect
import com.windscribe.vpn.state.ConnectionPolicy.PolicyInput
import com.windscribe.vpn.state.ConnectionPolicy.ServiceMode
import com.windscribe.vpn.state.ConnectionPolicy.SessionEnd
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** The truth table for [ConnectionPolicy]. Pure functions, so no preferences or database here. */
class ConnectionPolicyTest {
    private fun input(
        isAutoSecureOn: Boolean? = true,
        isNetworkStoodDown: Boolean = false,
        isNetworkOptedIn: Boolean = false,
        autoConnect: Boolean = true,
        autoStartOnBoot: Boolean = false,
        globalUserConnectionPreference: Boolean = true,
        accountStatus: User.AccountStatus? = User.AccountStatus.Okay,
        status: VPNState.Status = VPNState.Status.Disconnected,
        serviceMode: ServiceMode? = null,
        canReadNetworkName: Boolean = true,
        isOnline: Boolean = true,
        isPreferredProtocolOn: Boolean = false,
        preferredProtocol: Pair<String, String>? = null,
        activeProtocol: Pair<String, String>? = null,
        isSystemLockdownEnabled: Boolean = false,
        isConnectingToConfigured: Boolean = false,
    ) = PolicyInput(
        isAutoSecureOn = isAutoSecureOn,
        isNetworkStoodDown = isNetworkStoodDown,
        isNetworkOptedIn = isNetworkOptedIn,
        autoConnect = autoConnect,
        autoStartOnBoot = autoStartOnBoot,
        globalUserConnectionPreference = globalUserConnectionPreference,
        accountStatus = accountStatus,
        status = status,
        serviceMode = serviceMode,
        canReadNetworkName = canReadNetworkName,
        isOnline = isOnline,
        isPreferredProtocolOn = isPreferredProtocolOn,
        preferredProtocol = preferredProtocol,
        activeProtocol = activeProtocol,
        isSystemLockdownEnabled = isSystemLockdownEnabled,
        isConnectingToConfigured = isConnectingToConfigured,
    )

    // --------------------------------------------------------------- shouldConnectOnStart

    private fun connectsOnBoot(
        autoConnect: Boolean,
        autoStartOnBoot: Boolean,
        globalUserConnectionPreference: Boolean,
        isAutoSecureOn: Boolean? = true,
    ) = ConnectionPolicy.shouldConnectOnStart(
        input(
            autoConnect = autoConnect,
            autoStartOnBoot = autoStartOnBoot,
            globalUserConnectionPreference = globalUserConnectionPreference,
            isAutoSecureOn = isAutoSecureOn,
        ),
        ConnectionPolicy.StartReason.DeviceBoot,
    )

    @Test
    fun `off means off - having been connected does not start on boot`() {
        // globalUserConnectionPreference is true for anyone who never disconnected, which would
        // make the switch a no-op in the off direction.
        assertFalse(
            connectsOnBoot(
                autoConnect = false,
                autoStartOnBoot = false,
                globalUserConnectionPreference = true,
            ),
        )
    }

    @Test
    fun `start on boot connects even if the user was not connected`() {
        assertTrue(
            connectsOnBoot(
                autoConnect = false,
                autoStartOnBoot = true,
                globalUserConnectionPreference = false,
            ),
        )
    }

    @Test
    fun `auto-connect outranks the boot switch`() {
        // The kill-switch promise does not lapse over a reboot.
        assertTrue(
            connectsOnBoot(
                autoConnect = true,
                autoStartOnBoot = false,
                globalUserConnectionPreference = false,
            ),
        )
    }

    @Test
    fun `auto-connect still yields to a network marked auto-secure off`() {
        assertFalse(
            connectsOnBoot(
                autoConnect = true,
                autoStartOnBoot = false,
                globalUserConnectionPreference = true,
                isAutoSecureOn = false,
            ),
        )
        // An unidentifiable network is not an objection.
        assertTrue(
            connectsOnBoot(
                autoConnect = true,
                autoStartOnBoot = false,
                globalUserConnectionPreference = false,
                isAutoSecureOn = null,
            ),
        )
    }

    @Test
    fun `an app update restores the session it interrupted, boot switch or not`() {
        val restored =
            ConnectionPolicy.shouldConnectOnStart(
                input(
                    autoConnect = false,
                    autoStartOnBoot = false,
                    globalUserConnectionPreference = true,
                ),
                ConnectionPolicy.StartReason.AppUpdate,
            )
        assertTrue("we interrupted it, so we put it back", restored)

        val notRestored =
            ConnectionPolicy.shouldConnectOnStart(
                input(
                    autoConnect = false,
                    autoStartOnBoot = false,
                    globalUserConnectionPreference = false,
                ),
                ConnectionPolicy.StartReason.AppUpdate,
            )
        assertFalse("nothing was running to put back", notRestored)
    }

    // ------------------------------------------------------------------ shouldAutoConnect

    @Test
    fun `connects on an auto-secure network with auto-connect on`() {
        assertTrue(ConnectionPolicy.shouldAutoConnect(input()))
    }

    @Test
    fun `does not connect when auto-secure is off for the network`() {
        assertFalse(ConnectionPolicy.shouldAutoConnect(input(isAutoSecureOn = false)))
    }

    @Test
    fun `an unnameable network is still secured`() {
        // Auto-Connect means connect, and a network we cannot name has not opted out of anything.
        assertTrue(
            ConnectionPolicy.shouldAutoConnect(
                input(canReadNetworkName = false, isAutoSecureOn = null),
            ),
        )
    }

    @Test
    fun `a network whose settings have not been read yet is left alone`() {
        // Null means two things; here the name is readable, so the row just has not arrived.
        assertFalse(
            ConnectionPolicy.shouldAutoConnect(
                input(canReadNetworkName = true, isAutoSecureOn = null),
            ),
        )
    }

    @Test
    fun `an unnameable network still honours a user disconnect`() {
        // Recorded per network name, so without one the last manual action stands in.
        assertFalse(
            ConnectionPolicy.shouldAutoConnect(
                input(
                    canReadNetworkName = false,
                    isAutoSecureOn = null,
                    globalUserConnectionPreference = false,
                ),
            ),
        )
    }

    @Test
    fun `does not connect when the device has no network`() {
        // Losing the network clears the stand-down, leaving this the only stop.
        assertFalse(ConnectionPolicy.shouldAutoConnect(input(isOnline = false, isAutoSecureOn = null)))
        assertFalse(ConnectionPolicy.shouldAutoConnect(input(isOnline = false)))
    }

    @Test
    fun `no network is its own state, not a failure`() {
        // Nothing was attempted, so "couldn't connect" would blame the VPN for a missing network.
        assertEquals(
            ServiceMode.NoNetwork,
            ConnectionPolicy.serviceModeAfterSession(input(), SessionEnd.NoNetwork),
        )
    }

    @Test
    fun `no network keeps the service up even with auto-connect off`() {
        // The user is owed the reason, and the service must be there to notice a network arriving.
        assertEquals(
            ServiceMode.NoNetwork,
            ConnectionPolicy.serviceModeAfterSession(input(autoConnect = false), SessionEnd.NoNetwork),
        )
    }

    @Test
    fun `an unnameable network reports the stand-down it actually applies`() {
        // The notification must not promise a reconnect shouldAutoConnect will refuse.
        val stoodDown =
            input(canReadNetworkName = false, globalUserConnectionPreference = false)
        assertFalse(ConnectionPolicy.shouldAutoConnect(stoodDown))
        assertEquals(
            ServiceMode.Watching(WatchReason.StoodDownByUser),
            ConnectionPolicy.serviceModeAfterSession(stoodDown, SessionEnd.UserDisconnect),
        )
    }

    @Test
    fun `an unnameable network the user did not stop still reconnects`() {
        val armed = input(canReadNetworkName = false, globalUserConnectionPreference = true)
        assertTrue(ConnectionPolicy.shouldAutoConnect(armed))
        assertEquals(
            ServiceMode.Watching(WatchReason.NetworkNotSecured),
            ConnectionPolicy.serviceModeAfterSession(armed, SessionEnd.UserDisconnect),
        )
    }

    @Test
    fun `a network with auto-secure off says so rather than promising a reconnect`() {
        // shouldAutoConnect refuses here, so "will connect automatically" would be a lie.
        val optedOut = input(isAutoSecureOn = false)
        assertFalse(ConnectionPolicy.shouldAutoConnect(optedOut))
        assertEquals(
            ServiceMode.Watching(WatchReason.NetworkOptedOut),
            ConnectionPolicy.serviceModeAfterSession(optedOut, SessionEnd.SystemDisconnect),
        )
    }

    @Test
    fun `a user disconnect outranks auto-secure being off`() {
        // Both are true here; the one the user just did is the one worth naming.
        assertEquals(
            ServiceMode.Watching(WatchReason.StoodDownByUser),
            ConnectionPolicy.serviceModeAfterSession(
                input(isAutoSecureOn = false, isNetworkStoodDown = true),
                SessionEnd.UserDisconnect,
            ),
        )
    }

    @Test
    fun `a dropped tunnel waits for the verdict rather than promising a reconnect`() {
        // Reporting it as ordinary watching promises a reconnect an expired account never gets.
        assertEquals(
            ServiceMode.AwaitingSession,
            ConnectionPolicy.serviceModeAfterSession(input(), SessionEnd.AwaitingSession),
        )
    }

    @Test
    fun `the wait keeps the service up even with auto-connect off`() {
        // A manually connected session is still owed a reconnect if the location was at fault.
        assertEquals(
            ServiceMode.AwaitingSession,
            ConnectionPolicy.serviceModeAfterSession(input(autoConnect = false), SessionEnd.AwaitingSession),
        )
    }

    /** The wait is entered only by a session that came up; a failed connect would loop through it. */
    @Test
    fun `a failed connect settles rather than waiting`() {
        assertEquals(
            ServiceMode.Failed(blocking = true),
            ConnectionPolicy.serviceModeAfterSession(input(), SessionEnd.ConnectFailed),
        )
    }

    @Test
    fun `the verdict against the account replaces the wait`() {
        assertEquals(
            ServiceMode.AccountBlocked(User.AccountStatus.Expired),
            ConnectionPolicy.serviceModeAfterSession(
                input(accountStatus = User.AccountStatus.Expired),
                SessionEnd.SystemDisconnect,
            ),
        )
    }

    /** Auto-Connect off means no kill switch and nothing to watch for. */
    @Test
    fun `a dead account stops the service outright with auto-connect off`() {
        assertEquals(
            ServiceMode.Off,
            ConnectionPolicy.serviceModeAfterSession(
                input(autoConnect = false, accountStatus = User.AccountStatus.Expired),
                SessionEnd.SystemDisconnect,
            ),
        )
    }

    /** No account state of ours blocks a config, or stops the service watching for one. */
    @Test
    fun `no account state blocks a custom config`() {
        for (status in listOf(User.AccountStatus.Expired, User.AccountStatus.Banned)) {
            val config = input(accountStatus = status, isConnectingToConfigured = true)
            assertEquals(
                ServiceMode.Watching(WatchReason.NetworkNotSecured),
                ConnectionPolicy.serviceModeAfterSession(config, SessionEnd.SystemDisconnect),
            )
            assertTrue(ConnectionPolicy.shouldWatch(config))
            assertTrue(ConnectionPolicy.shouldAutoConnect(config))
        }
    }

    /** An account we have not read yet is not a reason to connect on one of our own locations. */
    @Test
    fun `auto-connect waits for an account it has not read`() {
        assertFalse(ConnectionPolicy.shouldAutoConnect(input(accountStatus = null)))
    }

    /** No verdict is coming: the credentials are in the config and the server is not ours. */
    @Test
    fun `a config rejected by its own server says so`() {
        assertEquals(
            ServiceMode.AuthFailed,
            ConnectionPolicy.serviceModeAfterSession(input(), SessionEnd.AuthFailed),
        )
    }

    /** However the session ended, an account that cannot connect is the fact worth reporting. */
    @Test
    fun `an account that cannot connect outranks how the session ended`() {
        for (end in listOf(SessionEnd.SystemDisconnect, SessionEnd.ConnectFailed, SessionEnd.NoNetwork, SessionEnd.AwaitingSession)) {
            val mode = ConnectionPolicy.serviceModeAfterSession(input(accountStatus = User.AccountStatus.Banned), end)
            assertEquals(User.AccountStatus.Banned, (mode as ServiceMode.AccountBlocked).status)
        }
    }

    /** The user asked for these, so the account is not news. */
    @Test
    fun `turning off and pausing still win over a dead account`() {
        assertEquals(
            ServiceMode.Off,
            ConnectionPolicy.serviceModeAfterSession(input(accountStatus = User.AccountStatus.Expired), SessionEnd.UserTurnOff),
        )
        assertEquals(
            ServiceMode.Paused,
            ConnectionPolicy.serviceModeAfterSession(input(accountStatus = User.AccountStatus.Expired), SessionEnd.UserPause),
        )
    }

    /** A connect here would race the verdict and record a location failure for an account fault. */
    @Test
    fun `auto-connect does not race the verdict`() {
        assertFalse(ConnectionPolicy.shouldAutoConnect(input(serviceMode = ServiceMode.AwaitingSession)))
    }

    @Test
    fun `an explicit opt-out still stops auto-connect`() {
        assertFalse(ConnectionPolicy.shouldAutoConnect(input(isAutoSecureOn = false)))
    }

    /** Auto-Connect off must stop it, whatever else is true. */
    @Test
    fun `does not connect when auto-connect is off`() {
        assertFalse(ConnectionPolicy.shouldAutoConnect(input(autoConnect = false)))
    }

    @Test
    fun `does not connect on a network the user disconnected from`() {
        assertFalse(ConnectionPolicy.shouldAutoConnect(input(isNetworkStoodDown = true)))
    }

    @Test
    fun `does not connect for an account that cannot connect`() {
        assertFalse(ConnectionPolicy.shouldAutoConnect(input(accountStatus = User.AccountStatus.Expired)))
        assertFalse(ConnectionPolicy.shouldAutoConnect(input(accountStatus = User.AccountStatus.Banned)))
        assertFalse(ConnectionPolicy.shouldAutoConnect(input(accountStatus = null)))
    }

    @Test
    fun `does not connect while connecting or already connected`() {
        listOf(
            VPNState.Status.Connecting,
            VPNState.Status.Connected,
            VPNState.Status.UnsecuredNetwork,
        ).forEach {
            assertFalse("status=$it", ConnectionPolicy.shouldAutoConnect(input(status = it)))
        }
    }

    /** A paused service reports Disconnected, so only the service-mode check keeps Pause held. */
    @Test
    fun `does not connect while paused`() {
        assertFalse(ConnectionPolicy.shouldAutoConnect(input(serviceMode = ServiceMode.Paused)))
    }

    /** The watcher outlives a failed connect and every emission re-runs it, so the retry is unbounded. */
    @Test
    fun `does not connect after a failed attempt on this network`() {
        assertFalse(ConnectionPolicy.shouldAutoConnect(input(serviceMode = ServiceMode.Failed())))
    }

    @Test
    fun `a failed connect ends the session as a failure the user can see`() {
        assertEquals(
            ServiceMode.Failed(blocking = true),
            ConnectionPolicy.serviceModeAfterSession(input(), SessionEnd.ConnectFailed),
        )
        // Even with Auto-Connect off: the user is owed the outcome, and nothing is blocked.
        assertEquals(
            ServiceMode.Failed(blocking = false),
            ConnectionPolicy.serviceModeAfterSession(input(autoConnect = false), SessionEnd.ConnectFailed),
        )
    }

    /** Watching is the state we are *supposed* to connect out of. */
    @Test
    fun `connects while watching for a secured network`() {
        assertTrue(
            ConnectionPolicy.shouldAutoConnect(
                input(serviceMode = ServiceMode.Watching(WatchReason.NetworkNotSecured)),
            ),
        )
    }

    /** Auto-connect acts without a manual action, so the flag must not be a precondition. */
    @Test
    fun `connects even though the user last disconnected manually`() {
        assertTrue(ConnectionPolicy.shouldAutoConnect(input(globalUserConnectionPreference = false)))
    }

    // ------------------------------------------------------------------ shouldAutoDisconnect

    @Test
    fun `disconnects when auto-secure is off and a tunnel is up`() {
        assertTrue(ConnectionPolicy.shouldAutoDisconnect(input(isAutoSecureOn = false, status = VPNState.Status.Connected)))
    }

    /**
     * Only on Connected: firing on Connecting tears down a manual connect on an Auto-Secure-off
     * network milliseconds after it starts. Auto-connect cannot produce that state, so a
     * connect in progress there is always something the user asked for by hand.
     */
    @Test
    fun `does not cancel a manual connect on an auto-secure-off network`() {
        assertFalse(ConnectionPolicy.shouldAutoDisconnect(input(isAutoSecureOn = false, status = VPNState.Status.Connecting)))
    }

    /** Auto-Secure off is an instruction about this network, not a preference about automation. */
    @Test
    fun `disconnects on an auto-secure-off network even with auto-connect off`() {
        assertTrue(
            ConnectionPolicy.shouldAutoDisconnect(
                input(isAutoSecureOn = false, autoConnect = false, status = VPNState.Status.Connected),
            ),
        )
    }

    /**
     * Regression: waking the device re-emits the same NetworkInfo with no network change at all, so
     * a hand-started connection on an Auto-Secure-off network was torn down a minute or two later.
     */
    @Test
    fun `does not disconnect a connection the user started on this network`() {
        assertFalse(
            ConnectionPolicy.shouldAutoDisconnect(
                input(isAutoSecureOn = false, isNetworkOptedIn = true, status = VPNState.Status.Connected),
            ),
        )
    }

    /** Roaming clears the opt-in, so Auto-Secure still applies on the network moved to. */
    @Test
    fun `disconnects after roaming onto an auto-secure-off network`() {
        assertTrue(
            ConnectionPolicy.shouldAutoDisconnect(
                input(isAutoSecureOn = false, isNetworkOptedIn = false, status = VPNState.Status.Connected),
            ),
        )
    }

    @Test
    fun `does not disconnect when there is no tunnel`() {
        assertFalse(ConnectionPolicy.shouldAutoDisconnect(input(isAutoSecureOn = false, status = VPNState.Status.Disconnected)))
    }

    @Test
    fun `does not disconnect on a secured or unidentifiable network`() {
        assertFalse(ConnectionPolicy.shouldAutoDisconnect(input(isAutoSecureOn = true, status = VPNState.Status.Connected)))
        assertFalse(ConnectionPolicy.shouldAutoDisconnect(input(isAutoSecureOn = null, status = VPNState.Status.Connected)))
    }

    // ------------------------------------------------------------------ serviceModeAfterSession

    @Test
    fun `a pause always keeps the service`() {
        assertEquals(ServiceMode.Paused, ConnectionPolicy.serviceModeAfterSession(input(), SessionEnd.UserPause))
        // Even with nothing to watch for: the user asked to come back.
        assertEquals(
            ServiceMode.Paused,
            ConnectionPolicy.serviceModeAfterSession(input(autoConnect = false), SessionEnd.UserPause),
        )
    }

    @Test
    fun `turning off always stops the service`() {
        assertEquals(ServiceMode.Off, ConnectionPolicy.serviceModeAfterSession(input(), SessionEnd.UserTurnOff))
    }

    /** The decision the user made: with Auto-Connect on we stay up and say why. */
    @Test
    fun `a user disconnect with auto-connect on keeps watching, stood down`() {
        assertEquals(
            ServiceMode.Watching(WatchReason.StoodDownByUser),
            ConnectionPolicy.serviceModeAfterSession(input(isNetworkStoodDown = true), SessionEnd.UserDisconnect),
        )
    }

    @Test
    fun `a system disconnect with auto-connect on keeps the service watching`() {
        // Auto-Secure off names itself; anything else is armed and says so.
        assertEquals(
            ServiceMode.Watching(WatchReason.NetworkOptedOut),
            ConnectionPolicy.serviceModeAfterSession(input(isAutoSecureOn = false), SessionEnd.SystemDisconnect),
        )
        assertEquals(
            ServiceMode.Watching(WatchReason.NetworkNotSecured),
            ConnectionPolicy.serviceModeAfterSession(input(), SessionEnd.SystemDisconnect),
        )
    }

    @Test
    fun `a disconnect with auto-connect off stops the service`() {
        assertEquals(
            ServiceMode.Off,
            ConnectionPolicy.serviceModeAfterSession(input(autoConnect = false), SessionEnd.UserDisconnect),
        )
    }

    @Test
    fun `an unreadable network name does not stop the service watching`() {
        // Auto-Connect is a global switch, and the next network to appear defaults to secured.
        assertTrue(
            ConnectionPolicy.serviceModeAfterSession(
                input(canReadNetworkName = false),
                SessionEnd.UserDisconnect,
            ) is ServiceMode.Watching,
        )
    }

    @Test
    fun `nothing to watch for without a usable account`() {
        assertEquals(
            ServiceMode.Off,
            ConnectionPolicy.serviceModeAfterSession(
                input(accountStatus = User.AccountStatus.Expired),
                SessionEnd.UserDisconnect,
            ),
        )
    }

    // ------------------------------------------------------------------ shouldWatch

    /** The service comes up at boot before the session is read, so an unloaded user cannot refuse it. */
    @Test
    fun `watches with auto-connect on, including before the user has loaded`() {
        assertTrue(ConnectionPolicy.shouldWatch(input()))
        assertTrue(ConnectionPolicy.shouldWatch(input(accountStatus = null)))
    }

    @Test
    fun `does not watch with auto-connect off`() {
        assertFalse(ConnectionPolicy.shouldWatch(input(autoConnect = false)))
    }

    // ------------------------------------------------------------------ shouldSwitchProtocol

    private fun preferred(
        on: Boolean = true,
        want: Pair<String, String>? = "wstunnel" to "443",
        have: Pair<String, String>? = "wg" to "443",
        status: VPNState.Status = VPNState.Status.Connected,
        autoConnect: Boolean = false,
        isAutoSecureOn: Boolean? = true,
    ) = input(
        isAutoSecureOn = isAutoSecureOn,
        autoConnect = autoConnect,
        status = status,
        isPreferredProtocolOn = on,
        preferredProtocol = want,
        activeProtocol = have,
    )

    /** Preferred Protocol is its own per-network opt-in; it must not depend on Auto-Connect. */
    @Test
    fun `switches to the network's preferred protocol regardless of auto-connect`() {
        assertTrue(ConnectionPolicy.shouldSwitchProtocol(preferred(autoConnect = false)))
        assertTrue(ConnectionPolicy.shouldSwitchProtocol(preferred(autoConnect = true)))
    }

    @Test
    fun `does not switch when already on the preferred protocol`() {
        assertFalse(ConnectionPolicy.shouldSwitchProtocol(preferred(have = "wstunnel" to "443")))
    }

    /** A differing port is a switch too: the preference is protocol *and* port. */
    @Test
    fun `switches when only the port differs`() {
        assertFalse(
            ConnectionPolicy.shouldSwitchProtocol(
                preferred(want = "wg" to "1194", have = "wg" to "1194"),
            ),
        )
        assertTrue(
            ConnectionPolicy.shouldSwitchProtocol(
                preferred(want = "wg" to "1194", have = "wg" to "443"),
            ),
        )
    }

    @Test
    fun `does not switch when the network has no preference`() {
        assertFalse(ConnectionPolicy.shouldSwitchProtocol(preferred(on = false)))
        assertFalse(ConnectionPolicy.shouldSwitchProtocol(preferred(want = null)))
    }

    @Test
    fun `does not switch on a network we are not securing`() {
        assertFalse(ConnectionPolicy.shouldSwitchProtocol(preferred(isAutoSecureOn = false)))
        assertFalse(ConnectionPolicy.shouldSwitchProtocol(preferred(isAutoSecureOn = null)))
    }

    /** Nothing to switch mid-connect; the attempt already picks the preferred protocol. */
    @Test
    fun `does not switch unless a connection is up`() {
        assertFalse(ConnectionPolicy.shouldSwitchProtocol(preferred(status = VPNState.Status.Connecting)))
        assertFalse(ConnectionPolicy.shouldSwitchProtocol(preferred(status = VPNState.Status.Disconnected)))
        assertFalse(ConnectionPolicy.shouldSwitchProtocol(preferred(have = null)))
    }

    // ------------------------------------------------------------------ shouldBlockOutsideVpn

    private fun blocking(
        autoConnect: Boolean = true,
        isAutoSecureOn: Boolean? = true,
        serviceMode: ServiceMode? = null,
        accountStatus: User.AccountStatus? = User.AccountStatus.Okay,
        isSystemLockdownEnabled: Boolean = false,
    ) = input(
        autoConnect = autoConnect,
        isAutoSecureOn = isAutoSecureOn,
        serviceMode = serviceMode,
        accountStatus = accountStatus,
        isSystemLockdownEnabled = isSystemLockdownEnabled,
    )

    @Test
    fun `blocks between connections when the mode is on`() {
        assertTrue(ConnectionPolicy.shouldBlockOutsideVpn(blocking(), SessionEnd.SystemDisconnect))
        assertTrue(ConnectionPolicy.shouldBlockOutsideVpn(blocking(), SessionEnd.ConnectFailed))
    }

    @Test
    fun `does not block when the mode is off`() {
        assertFalse(ConnectionPolicy.shouldBlockOutsideVpn(blocking(autoConnect = false), SessionEnd.ConnectFailed))
    }

    /** Blocking someone who asked us to stop would trap them with no way back. */
    @Test
    fun `does not block when the user asked us to stop`() {
        listOf(SessionEnd.UserDisconnect, SessionEnd.UserTurnOff, SessionEnd.UserPause).forEach {
            assertFalse("$it", ConnectionPolicy.shouldBlockOutsideVpn(blocking(), it))
        }
    }

    /** The exclusion has to mean something: "do not secure me here" cannot mean "block me here". */
    @Test
    fun `does not block on a network the user excluded`() {
        assertFalse(
            ConnectionPolicy.shouldBlockOutsideVpn(blocking(isAutoSecureOn = false), SessionEnd.ConnectFailed),
        )
    }

    /** An unidentifiable network is not an excluded one, so the guarantee still holds. */
    @Test
    fun `blocks on a network it cannot identify`() {
        assertTrue(
            ConnectionPolicy.shouldBlockOutsideVpn(blocking(isAutoSecureOn = null), SessionEnd.ConnectFailed),
        )
    }

    @Test
    fun `does not block while paused`() {
        assertFalse(
            ConnectionPolicy.shouldBlockOutsideVpn(blocking(serviceMode = ServiceMode.Paused), SessionEnd.ConnectFailed),
        )
    }

    /** Android is already blocking; ours would be redundant. */
    @Test
    fun `stands down under the system killswitch`() {
        assertFalse(
            ConnectionPolicy.shouldBlockOutsideVpn(blocking(isSystemLockdownEnabled = true), SessionEnd.ConnectFailed),
        )
    }

    /** Auto-Connect is a kill switch, and expiry is not a reason to put held traffic on the wire. */
    @Test
    fun `keeps blocking an account that cannot connect`() {
        assertTrue(
            ConnectionPolicy.shouldBlockOutsideVpn(
                blocking(accountStatus = User.AccountStatus.Expired),
                SessionEnd.ConnectFailed,
            ),
        )
    }

    /** The way back out of the block, and the only action the two blocked states offer. */
    @Test
    fun `turning off releases the block on a dead account`() {
        assertFalse(
            ConnectionPolicy.shouldBlockOutsideVpn(
                blocking(accountStatus = User.AccountStatus.Expired),
                SessionEnd.UserTurnOff,
            ),
        )
    }

    /** A failure that blocks and one that merely leaves you unprotected are different states. */
    @Test
    fun `a failed connect records whether it is blocking`() {
        assertEquals(
            ServiceMode.Failed(blocking = true),
            ConnectionPolicy.serviceModeAfterSession(blocking(), SessionEnd.ConnectFailed),
        )
        assertEquals(
            ServiceMode.Failed(blocking = false),
            ConnectionPolicy.serviceModeAfterSession(blocking(isAutoSecureOn = false), SessionEnd.ConnectFailed),
        )
    }

    /** Asked while a connect starts, where there is no session end and no tunnel to hold. */
    @Test
    fun `blocking applies while a connect is starting`() {
        assertTrue(ConnectionPolicy.blockingApplies(blocking()))
        assertFalse(ConnectionPolicy.blockingApplies(blocking(autoConnect = false)))
        assertFalse(ConnectionPolicy.blockingApplies(blocking(isAutoSecureOn = false)))
        assertFalse(ConnectionPolicy.blockingApplies(blocking(serviceMode = ServiceMode.Paused)))
        assertFalse(ConnectionPolicy.blockingApplies(blocking(isSystemLockdownEnabled = true)))
    }

    // --------------------------------------------------------------- reconcile

    /** The whole point: the switch is owed something now, not at the next network change. */
    @Test
    fun `turning auto-connect on connects when this network qualifies`() {
        assertEquals(Effect.Connect, ConnectionPolicy.reconcile(input()))
    }

    /** Nothing to connect to here, but the next network needs a service to notice it. */
    @Test
    fun `turning auto-connect on watches when this network is opted out`() {
        assertEquals(
            Effect.StartWatching,
            ConnectionPolicy.reconcile(input(isAutoSecureOn = false)),
        )
    }

    /** The idle service is in a mode of its own, and settling it into Watching would erase it. */
    @Test
    fun `turning auto-connect on leaves a paused or failed service alone`() {
        assertEquals(
            Effect.None,
            ConnectionPolicy.reconcile(input(isAutoSecureOn = false, serviceMode = ServiceMode.Paused)),
        )
        assertEquals(
            Effect.None,
            ConnectionPolicy.reconcile(input(isAutoSecureOn = false, serviceMode = ServiceMode.Failed())),
        )
    }

    /** A session is already running; there is nothing for the switch to start. */
    @Test
    fun `turning auto-connect on does nothing while connected`() {
        assertEquals(
            Effect.None,
            ConnectionPolicy.reconcile(input(status = VPNState.Status.Connected)),
        )
    }

    /** Left running it promises a reconnect [ConnectionPolicy.shouldAutoConnect] now refuses. */
    @Test
    fun `turning auto-connect off stops an idle service`() {
        assertEquals(
            Effect.StopService,
            ConnectionPolicy.reconcile(
                input(autoConnect = false, serviceMode = ServiceMode.Watching(WatchReason.NetworkNotSecured)),
            ),
        )
        assertEquals(
            Effect.StopService,
            ConnectionPolicy.reconcile(
                input(autoConnect = false, serviceMode = ServiceMode.Failed(blocking = true)),
            ),
        )
    }

    /** The live session is the user's doing, not Auto-Connect's, so the switch does not end it. */
    @Test
    fun `turning auto-connect off leaves a live session alone`() {
        assertEquals(
            Effect.None,
            ConnectionPolicy.reconcile(input(autoConnect = false, status = VPNState.Status.Connected)),
        )
    }

    /** The caller clears the block first, so this decides on a clean slate. */
    @Test
    fun `an account that can connect again reconnects`() {
        assertEquals(Effect.Connect, ConnectionPolicy.reconcile(input(serviceMode = null)))
    }

    /** Recovering does not undo Auto-Connect being off - it stops the service the block left up. */
    @Test
    fun `an account that can connect again stops the service when auto-connect is off`() {
        assertEquals(Effect.StopService, ConnectionPolicy.reconcile(input(autoConnect = false)))
    }

    /** Nothing to connect to here, but the block comes off and the next network needs watching. */
    @Test
    fun `an account that can connect again watches on an opted-out network`() {
        assertEquals(
            Effect.StartWatching,
            ConnectionPolicy.reconcile(input(isAutoSecureOn = false, serviceMode = null)),
        )
    }
}
