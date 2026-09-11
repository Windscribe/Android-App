/*
 * Copyright (c) 2026 Windscribe Limited.
 */

package com.windscribe.vpn.state

import com.windscribe.vpn.backend.VPNState
import com.windscribe.vpn.model.User

/**
 * Decides whether to connect and whether the service stays alive.
 *
 * Pure functions of [PolicyInput]. [ConnectionPolicyInputs] gathers the live values.
 */
object ConnectionPolicy {
    data class PolicyInput(
        /** The network's Auto-Secure toggle. Null when the network cannot be identified. */
        val isAutoSecureOn: Boolean?,
        /** The user disconnected on this network. Cleared when the network changes. */
        val isNetworkStoodDown: Boolean,
        /** The user connected on this network. The mirror of [isNetworkStoodDown]. */
        val isNetworkOptedIn: Boolean,
        /** Connection settings -> Auto-Connect. */
        val autoConnect: Boolean,
        /** Connection settings -> Start on boot. */
        val autoStartOnBoot: Boolean = false,
        /** The user's last manual action: true after a connect, false after a disconnect. */
        val globalUserConnectionPreference: Boolean,
        val accountStatus: User.AccountStatus?,
        val status: VPNState.Status,
        /** Idle mode, or null. Separate from [status] because a paused service reports Disconnected. */
        val serviceMode: ServiceMode?,
        /** Without the network name every per-network rule is unanswerable. */
        val canReadNetworkName: Boolean,
        /** Whether the device has a network at all. Nothing can connect without one. */
        val isOnline: Boolean = true,
        /** Android's own Always-on + "Block connections without VPN". Makes ours redundant. */
        val isSystemLockdownEnabled: Boolean = false,
        /** The network's "Preferred Protocol" toggle. */
        val isPreferredProtocolOn: Boolean = false,
        /** The protocol and port this network is configured to prefer, or null when unset. */
        val preferredProtocol: Pair<String, String>? = null,
        /** What we are actually connected with, or null when nothing is up. */
        val activeProtocol: Pair<String, String>? = null,
        /** The selected location is a config the user supplied, pointing at a third party. */
        val isConnectingToConfigured: Boolean = false,
    )

    /** Why the app is starting itself, for [shouldConnectOnStart]. */
    enum class StartReason {
        /** The user's device restarted. */
        DeviceBoot,

        /** We restarted: the package was replaced under a running session. */
        AppUpdate,
    }

    /**
     * Whether to connect when the app starts itself.
     *
     * Priority: Auto-Connect, then Start on boot. `globalUserConnectionPreference` does not trigger a
     * boot connect; it is true for anyone who has not explicitly disconnected, which would make Start
     * on boot meaningless when off.
     *
     * An app update restores the session it interrupted, so the boot switch does not apply.
     */
    fun shouldConnectOnStart(
        input: PolicyInput,
        reason: StartReason,
    ): Boolean =
        when {
            reason == StartReason.AppUpdate -> input.globalUserConnectionPreference
            input.autoConnect && input.isAutoSecureOn != false -> true
            else -> input.autoStartOnBoot
        }

    /** How a session ended, which decides whether the service outlives it. */
    enum class SessionEnd {
        UserPause,
        UserDisconnect,

        /** An error, a revoke, or Auto-Secure going off. */
        SystemDisconnect,

        /** Stop entirely - tunnel, watcher and notification. */
        UserTurnOff,

        /**
         * A connect gave up with nothing left to try: failover exhausted, or a single attempt with no
         * fallback. A preferred-protocol switch is the second kind, since the protocol was chosen.
         */
        ConnectFailed,

        /** There is no network to connect over, so nothing was attempted. */
        NoNetwork,

        /**
         * The tunnel stopped carrying traffic. Whether the location or the account is at fault is
         * not knowable here, so the session refresh decides; every producer of this end requests one.
         */
        AwaitingSession,

        /**
         * A server the user pointed us at rejected our credentials.
         *
         * Not [AwaitingSession]: no verdict is coming, since our session cannot speak for a third
         * party's credentials.
         */
        AuthFailed,
    }

    /**
     * What the service should be once a session ends, and the value published on
     * [VPNConnectionStateManager.serviceMode].
     */
    sealed class ServiceMode {
        object Off : ServiceMode()

        /** Foreground, no TUN, watching for a network worth connecting on. */
        data class Watching(
            val reason: WatchReason,
        ) : ServiceMode()

        /** Foreground, no TUN, and auto-connect stands down until the user resumes. */
        object Paused : ServiceMode()

        /**
         * Foreground, no TUN, every protocol tried without success.
         *
         * @param blocking true when the TUN is held rather than closed, so nothing leaves the device.
         */
        data class Failed(
            val blocking: Boolean = false,
        ) : ServiceMode()

        /**
         * Foreground, no TUN, and the device has no network to connect over.
         *
         * Distinct from [Failed]: nothing was attempted and nothing is wrong with the connection.
         * Saying "couldn't connect" here blames the VPN for the absence of a network, and with
         * blocking on it would claim traffic is blocked when no interface exists to block it.
         */
        object NoNetwork : ServiceMode()

        /**
         * Foreground, no TUN, waiting for the session refresh to say whose fault the drop was.
         *
         * A resting state, and can stand for as long as the refresh takes. The reconnect that
         * follows a verdict drops it; [AccountBlocked] replaces it.
         */
        object AwaitingSession : ServiceMode()

        /** Foreground, no TUN, and the server in the user's config rejected their credentials. */
        object AuthFailed : ServiceMode()

        /**
         * Foreground, no TUN, and this account cannot connect - expired, or banned.
         *
         * Auto-Connect is a kill switch, so traffic stays blocked. Turn off is the only action:
         * `ShortcutStateManager` refuses to load a session that is not Okay.
         */
        data class AccountBlocked(
            val status: User.AccountStatus,
        ) : ServiceMode()
    }

    /**
     * True when the account rules out connecting. Null means not read yet, which is not a verdict.
     * A config the user supplied points at a third party our account has no say over.
     */
    private fun accountCannotConnect(input: PolicyInput): Boolean =
        input.accountStatus != null &&
            input.accountStatus != User.AccountStatus.Okay &&
            !input.isConnectingToConfigured

    /** `globalUserConnectionPreference` is not consulted, except by [restoresOptedOutSession]. */
    fun shouldAutoConnect(input: PolicyInput): Boolean =
        networkAllowsAutoConnect(input) &&
            (input.autoConnect || restoresOptedOutSession(input)) &&
            // Losing connectivity clears the stand-down, so this is what stops an attempt with
            // nothing to attempt it over.
            input.isOnline &&
            !standsDown(input) &&
            // Strict, not [accountCannotConnect]: a status not yet read is no reason to connect.
            (input.accountStatus == User.AccountStatus.Okay || input.isConnectingToConfigured) &&
            input.status == VPNState.Status.Disconnected &&
            input.serviceMode !is ServiceMode.Paused &&
            // The watcher outlives a failure and every attempt re-emits networkInfo, so without
            // this it retries unboundedly. Cleared on a network change.
            input.serviceMode !is ServiceMode.Failed &&
            // A connect here would race the verdict, and blame the location for an account fault.
            input.serviceMode !is ServiceMode.AwaitingSession &&
            // Past the account gate, since `isConnectingToConfigured` is true here.
            input.serviceMode !is ServiceMode.AuthFailed

    /**
     * Whether this network may be connected on.
     *
     * `isAutoSecureOn` is null both when the network cannot be named and when its row is unread;
     * only the first is a decision, hence the fallback to [PolicyInput.canReadNetworkName].
     *
     * "Auto-Secure new networks" is already baked into `isAutoSecureOn` by `NetworkInfoManager`.
     */
    private fun networkAllowsAutoConnect(input: PolicyInput): Boolean = input.isAutoSecureOn ?: !input.canReadNetworkName

    /**
     * True when the user disconnected here and we have not left since. `DeviceStateManager` records
     * it with or without a network name, never inferred from `globalUserConnectionPreference` -
     * that is equally false for someone who has never connected.
     */
    fun standsDown(input: PolicyInput): Boolean = input.isNetworkStoodDown

    /**
     * Fires regardless of Auto-Connect: Auto-Secure off is an instruction about this network.
     * Leaves alone a session the user started here; roaming clears that opt-in.
     */
    fun shouldAutoDisconnect(input: PolicyInput): Boolean =
        input.isAutoSecureOn == false &&
            !input.isNetworkOptedIn &&
            input.status == VPNState.Status.Connected

    /**
     * True when [shouldAutoDisconnect] took away a session the user asked for. The teardown ignores
     * Auto-Connect, so the undo does too; nothing observes the next network once the service stops.
     *
     * Excludes a stand-down, so a hand disconnect on an opted-out network still stops the service.
     */
    private fun awaitsSecuredNetwork(input: PolicyInput): Boolean =
        input.isAutoSecureOn == false &&
            input.globalUserConnectionPreference &&
            !standsDown(input) &&
            !accountCannotConnect(input)

    /**
     * Gives back what [awaitsSecuredNetwork] held. A connect clears the mode, so this fires once per
     * teardown rather than retrying a network that refuses.
     *
     * `isAutoSecureOn == true`, not the usual fallback: this overrides Auto-Connect being off, so it
     * needs a network we know we secure rather than one we cannot name.
     */
    private fun restoresOptedOutSession(input: PolicyInput): Boolean =
        input.globalUserConnectionPreference &&
            input.serviceMode == ServiceMode.Watching(WatchReason.NetworkOptedOut) &&
            input.isAutoSecureOn == true

    /**
     * True when nothing may leave the device outside the VPN while no tunnel is carrying it.
     *
     * Past it anyway: an Auto-Secure-off network, LAN under `lanByPass`, and our own package. An
     * expired account is not an exception; [ServiceMode.AccountBlocked] names the way out instead.
     */
    fun shouldBlockOutsideVpn(
        input: PolicyInput,
        end: SessionEnd,
    ): Boolean {
        val userAskedUsToStop =
            end == SessionEnd.UserDisconnect ||
                end == SessionEnd.UserTurnOff ||
                end == SessionEnd.UserPause
        return !userAskedUsToStop && blockingApplies(input)
    }

    /** Blocking in force, independent of a session end: a starting connect still needs covering. */
    fun blockingApplies(input: PolicyInput): Boolean =
        input.autoConnect &&
            input.isAutoSecureOn != false &&
            // Same instruction as Auto-Secure off, but held in memory rather than on the network.
            !standsDown(input) &&
            !input.isSystemLockdownEnabled &&
            // A pause is ended by hand; holding through one would leave the device dark.
            input.serviceMode !is ServiceMode.Paused

    /**
     * True when a live connection is on the wrong protocol for this network.
     *
     * Gated on Preferred Protocol alone; the switch is a handover, so no leak window opens. Only the
     * configured protocol is attempted, and a failure surfaces as [ServiceMode.Failed].
     */
    fun shouldSwitchProtocol(input: PolicyInput): Boolean {
        if (!input.isPreferredProtocolOn || input.isAutoSecureOn != true) {
            return false
        }
        val preferred = input.preferredProtocol ?: return false
        val active = input.activeProtocol ?: return false
        return input.status == VPNState.Status.Connected && preferred != active
    }

    /**
     * True when the service is worth keeping up with no tunnel. With Auto-Connect off there is
     * nothing to react to.
     *
     * Deliberately does not require the network name: Auto-Connect is a global switch, and a network
     * we cannot name still needs securing. Nor the current network's Auto-Secure — that is about this
     * network, while the next one to appear defaults to secured.
     */
    fun shouldWatch(input: PolicyInput): Boolean = input.autoConnect && !accountCannotConnect(input)

    /** What the service should be made to do once an input it never observes has changed. */
    enum class Effect {
        /** This network qualifies, so connect now rather than wait for the next one. */
        Connect,

        /** Nothing to connect to here, but the service should be up for the network that follows. */
        StartWatching,

        /** Nothing left for an idle service to be up for. Also releases a TUN held to block. */
        StopService,

        None,
    }

    /**
     * What to do about the inputs nothing observes. Decides on what it is given: the caller writes
     * the new values and clears whatever the change overrides first.
     */
    fun reconcile(input: PolicyInput): Effect =
        when {
            // Off does not end a live session: that one is the user's doing, not Auto-Connect's.
            !input.autoConnect ->
                if (input.status == VPNState.Status.Disconnected) Effect.StopService else Effect.None

            shouldAutoConnect(input) -> Effect.Connect

            // An idle service is already in a mode of its own; Watching would take that away.
            !shouldWatch(input) ||
                input.serviceMode != null ||
                input.status != VPNState.Status.Disconnected -> Effect.None

            else -> Effect.StartWatching
        }

    /** Ends the user asked for, where the reason for no tunnel is not news to them. */
    private val USER_INTENT_ENDS = setOf(SessionEnd.UserTurnOff, SessionEnd.UserPause, SessionEnd.UserDisconnect)

    fun serviceModeAfterSession(
        input: PolicyInput,
        end: SessionEnd,
    ): ServiceMode {
        // The account outranks how the session ended, except for ends the user asked for.
        if (end !in USER_INTENT_ENDS && accountCannotConnect(input)) {
            // No kill switch and nothing to watch for, so no state to keep the user in.
            if (!input.autoConnect) {
                return ServiceMode.Off
            }
            return ServiceMode.AccountBlocked(input.accountStatus!!)
        }
        return when (end) {
            SessionEnd.UserTurnOff -> ServiceMode.Off
            SessionEnd.UserPause -> ServiceMode.Paused
            // The next three ignore Auto-Connect: the user asked to connect and is owed an
            // outcome, and the service stays up to deliver it.
            SessionEnd.ConnectFailed -> ServiceMode.Failed(blocking = shouldBlockOutsideVpn(input, end))
            SessionEnd.NoNetwork -> ServiceMode.NoNetwork
            SessionEnd.AwaitingSession -> ServiceMode.AwaitingSession
            SessionEnd.AuthFailed -> ServiceMode.AuthFailed
            SessionEnd.UserDisconnect, SessionEnd.SystemDisconnect ->
                when {
                    // Nothing else here outlives Auto-Connect being off.
                    !shouldWatch(input) && !awaitsSecuredNetwork(input) -> ServiceMode.Off
                    // Staying up makes the stand-down visible rather than silent.
                    standsDown(input) -> ServiceMode.Watching(WatchReason.StoodDownByUser)
                    // Nothing is waiting to happen here, so the copy must not promise a connect.
                    input.isAutoSecureOn == false -> ServiceMode.Watching(WatchReason.NetworkOptedOut)
                    else -> ServiceMode.Watching(WatchReason.NetworkNotSecured)
                }
        }
    }
}

/** Why the service is alive with no tunnel. Picks the notification's copy. */
enum class WatchReason {
    /** Nothing is stopping a reconnect; the next network that needs securing gets one. */
    NetworkNotSecured,

    /**
     * Auto-Secure is off for this network, so we deliberately will not connect on it. Also marks a
     * session Auto-Secure took away, for the next secured network to give back.
     */
    NetworkOptedOut,

    /** The user disconnected here. */
    StoodDownByUser,
}
