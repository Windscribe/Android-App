/*
 * Copyright © 2017-2023 WireGuard LLC. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.wireguard.android.backend;

import android.content.Context;
import android.util.Log;

import com.wireguard.android.backend.BackendException.Reason;
import com.wireguard.android.backend.Tunnel.State;
import com.wireguard.android.util.SharedLibraryLoader;
import com.wireguard.config.Config;
import com.wireguard.config.InetEndpoint;
import com.wireguard.config.Peer;
import com.wireguard.crypto.Key;
import com.wireguard.crypto.KeyFormatException;
import com.wireguard.util.NonNullForAll;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;
import java.util.Set;

import androidx.annotation.Nullable;
import androidx.collection.ArraySet;

/**
 * Runs wireguard-go over a descriptor it is handed.
 *
 * <p>An engine, not a service: the TUN belongs to the one service that owns it for the session —
 * see docs/architecture/VPN_BACKEND.md §2.
 *
 * <p>Its only coupling to a {@code VpnService} is two {@code protect()} calls per session, through
 * a {@link TunHandle} callback: {@code DisableSomeRoamingForBrokenMobileSemantics()} means the UDP
 * bind is never rebuilt.
 *
 * <p>The {@code customTun} flag threaded Java -> JNI -> Go is what lets wg-go run on a socketpair
 * instead of a real tun device; the amneziawg-go fork suppresses the tun-specific ioctls behind it.
 *
 * <h3>Descriptor ownership</h3>
 * wg-go <b>takes ownership of and closes</b> the descriptor passed to {@code wgTurnOn}. The caller
 * must therefore hand over a descriptor it will never touch again — {@link PacketMux} hands out a
 * dup for exactly this reason.
 */
@NonNullForAll
public final class GoBackend implements Backend {
    private static final int DNS_RESOLUTION_RETRIES = 10;
    private static final String TAG = "WireGuard/GoBackend";
    private final Context context;
    @Nullable private Config currentConfig;
    @Nullable private Tunnel currentTunnel;
    private int currentTunnelHandle = -1;
    private final Object tunnelStateLock = new Object();

    /**
     * Everything wg-go needs from the VPN service, without being able to see one.
     */
    public interface TunHandle {
        /**
         * The descriptor wg-go should run on. Ownership transfers to wg-go, which closes it, so
         * this must return a descriptor the caller has already given up.
         */
        int detachTunFd();

        /** Marks wg-go's UDP sockets so they bypass the tunnel. Called twice, once per session. */
        boolean protect(int socketFd);

        /** True when the descriptor is a socketpair rather than a real tun device. */
        boolean isCustomTun();
    }

    /**
     * Public constructor for GoBackend.
     *
     * @param context An Android {@link Context}
     */
    public GoBackend(final Context context) {
        SharedLibraryLoader.loadSharedLibrary(context, "wg-go");
        this.context = context;
        // The only place the library is loaded. Diagnostics only: a failure must not block a tunnel.
        if (wgSetCrashOutput() != 0)
            Log.w(TAG, "Could not install Go crash output");
    }

    @Nullable private static native String wgGetConfig(int handle);

    private static native int wgGetSocketV4(int handle);

    private static native int wgGetSocketV6(int handle);

    private static native void wgTurnOff(int handle);

    private static native int wgTurnOn(String ifName, int tunFd, String settings, boolean customTun);

    private static native String wgVersion();

    /**
     * Routes the Go runtime's fatal error line into the process abort message, so it reaches the
     * tombstone. libwg-go.so links three Go programs on one runtime, so without it every fatal
     * error reports identically: one frame in runtime.raise, with the reason only in logcat.
     */
    private static native int wgSetCrashOutput();

    /**
     * Method to get the names of running tunnels.
     *
     * @return A set of string values denoting names of running tunnels.
     */
    @Override
    public Set<String> getRunningTunnelNames() {
        if (currentTunnel != null) {
            final Set<String> runningTunnels = new ArraySet<>();
            runningTunnels.add(currentTunnel.getName());
            return runningTunnels;
        }
        return Collections.emptySet();
    }

    /**
     * Get the associated {@link State} for a given {@link Tunnel}.
     *
     * @param tunnel The tunnel to examine the state of.
     * @return {@link State} associated with the given tunnel.
     */
    @Override
    public State getState(final Tunnel tunnel) {
        return currentTunnel == tunnel ? State.UP : State.DOWN;
    }

    /**
     * Get the associated {@link Statistics} for a given {@link Tunnel}.
     *
     * @param tunnel The tunnel to retrieve statistics for.
     * @return {@link Statistics} associated with the given tunnel.
     */
    @Override
    public Statistics getStatistics(final Tunnel tunnel) {
        final Statistics stats = new Statistics();
        if (tunnel != currentTunnel || currentTunnelHandle == -1)
            return stats;
        final String config = wgGetConfig(currentTunnelHandle);
        if (config == null)
            return stats;
        Key key = null;
        long rx = 0;
        long tx = 0;
        long latestHandshakeMSec = 0;
        for (final String line : config.split("\\n")) {
            if (line.startsWith("public_key=")) {
                if (key != null)
                    stats.add(key, rx, tx, latestHandshakeMSec);
                rx = 0;
                tx = 0;
                latestHandshakeMSec = 0;
                try {
                    key = Key.fromHex(line.substring(11));
                } catch (final KeyFormatException ignored) {
                    key = null;
                }
            } else if (line.startsWith("rx_bytes=")) {
                if (key == null)
                    continue;
                try {
                    rx = Long.parseLong(line.substring(9));
                } catch (final NumberFormatException ignored) {
                    rx = 0;
                }
            } else if (line.startsWith("tx_bytes=")) {
                if (key == null)
                    continue;
                try {
                    tx = Long.parseLong(line.substring(9));
                } catch (final NumberFormatException ignored) {
                    tx = 0;
                }
            } else if (line.startsWith("last_handshake_time_sec=")) {
                if (key == null)
                    continue;
                try {
                    latestHandshakeMSec += Long.parseLong(line.substring(24)) * 1000;
                } catch (final NumberFormatException ignored) {
                    latestHandshakeMSec = 0;
                }
            } else if (line.startsWith("last_handshake_time_nsec=")) {
                if (key == null)
                    continue;
                try {
                    latestHandshakeMSec += Long.parseLong(line.substring(25)) / 1000000;
                } catch (final NumberFormatException ignored) {
                    latestHandshakeMSec = 0;
                }
            }
        }
        if (key != null)
            stats.add(key, rx, tx, latestHandshakeMSec);
        return stats;
    }

    /**
     * Stats for whatever tunnel is up, or null when none is. The orchestrator holds no {@link Tunnel}
     * of its own to ask with.
     */
    @Nullable
    public Statistics getCurrentStatistics() {
        final Tunnel tunnel = currentTunnel;
        return tunnel == null ? null : getStatistics(tunnel);
    }

    /**
     * Get the version of the underlying wireguard-go library.
     *
     * @return {@link String} value of the version of the wireguard-go library.
     */
    @Override
    public String getVersion() {
        return wgVersion();
    }

    /**
     * Change the state of a given {@link Tunnel}, optionally applying a given {@link Config}.
     *
     * @param tunnel The tunnel to control the state of.
     * @param state  The new state for this tunnel. Must be {@code UP}, {@code DOWN}, or
     *               {@code TOGGLE}.
     * @param config The configuration for this tunnel, may be null if state is {@code DOWN}.
     * @return {@link State} of the tunnel after state changes are applied.
     * @throws Exception Exception raised while changing tunnel state.
     */
    /**
     * Not supported. wg-go cannot establish a TUN any more — it must be handed one.
     * Use {@link #setState(Tunnel, State, Config, TunHandle)}.
     */
    @Override
    public State setState(final Tunnel tunnel, final State state, @Nullable final Config config) {
        throw new UnsupportedOperationException(
                "GoBackend no longer owns a TUN; pass a TunHandle via setState(tunnel, state, config, tunHandle)");
    }

    /**
     * Brings the tunnel up on the descriptor supplied by {@code tunHandle}, or takes it down.
     *
     * @param tunHandle supplies the descriptor and the protect callback. May be null when taking
     *                  the tunnel down, since neither is needed then.
     */
    public State setState(final Tunnel tunnel, State state, @Nullable final Config config,
                          @Nullable final TunHandle tunHandle) throws Exception {
        final State originalState = getState(tunnel);

        if (state == State.TOGGLE)
            state = originalState == State.UP ? State.DOWN : State.UP;
        if (state == originalState && tunnel == currentTunnel && config == currentConfig)
            return originalState;
        if (state == State.UP) {
            if (tunHandle == null)
                throw new BackendException(Reason.UNABLE_TO_START_VPN);
            final Config originalConfig = currentConfig;
            final Tunnel originalTunnel = currentTunnel;
            if (originalTunnel != null)
                setStateInternal(originalTunnel, null, State.DOWN, null);
            try {
                setStateInternal(tunnel, config, state, tunHandle);
            } catch (final Exception e) {
                // Rolling back means bringing the previous tunnel up again, which needs a
                // descriptor. The old one belongs to wg-go and is closed, so the caller has to
                // re-attach; surface the original failure rather than pretend we recovered.
                Log.w(TAG, "Tunnel failed to come up; previous config was " + (originalConfig != null));
                if (originalTunnel != null)
                    originalTunnel.onStateChange(State.DOWN);
                throw e;
            }
        } else if (state == State.DOWN && tunnel == currentTunnel) {
            setStateInternal(tunnel, null, State.DOWN, null);
        }
        return getState(tunnel);
    }

    private void setStateInternal(final Tunnel tunnel, @Nullable final Config config, final State state,
                                  @Nullable final TunHandle tunHandle)
            throws Exception {
        // Perform DNS resolution BEFORE acquiring lock to prevent ANR
        // DNS lookups can be slow and should not block other operations
        if (state == State.UP && config != null) {
            dnsRetry: for (int i = 0; i < DNS_RESOLUTION_RETRIES; ++i) {
                // Pre-resolve IPs so they're cached when building the userspace string
                for (final Peer peer : config.getPeers()) {
                    final InetEndpoint ep = peer.getEndpoint().orElse(null);
                    if (ep == null)
                        continue;
                    if (ep.getResolved().orElse(null) == null) {
                        if (i < DNS_RESOLUTION_RETRIES - 1) {
                            Log.w(TAG, "DNS host \"" + ep.getHost() + "\" failed to resolve; trying again");
                            Thread.sleep(1000);
                            continue dnsRetry;
                        } else
                            throw new BackendException(Reason.DNS_RESOLUTION_FAILURE, ep.getHost());
                    }
                }
                break;
            }
        }

        synchronized (tunnelStateLock) {
            setStateInternalLocked(tunnel, config, state, tunHandle);
        }
    }

    private void setStateInternalLocked(final Tunnel tunnel, @Nullable final Config config, final State state,
                                        @Nullable final TunHandle tunHandle)
            throws Exception {
        Log.i(TAG, "Bringing tunnel " + tunnel.getName() + ' ' + state);

        if (state == State.UP) {
            if (config == null)
                throw new BackendException(Reason.TUNNEL_MISSING_CONFIG);
            if (tunHandle == null)
                throw new BackendException(Reason.UNABLE_TO_START_VPN);

            if (currentTunnelHandle != -1) {
                Log.w(TAG, "Tunnel already up");
                return;
            }

            // DNS resolution already completed in setStateInternal() before acquiring the lock.
            final String goConfig = config.toWgUserspaceString();

            // No Builder, no establish(): the descriptor arrives ready. Ownership transfers to
            // wg-go, which closes it, so we never touch it again after this call.
            final int tunFd = tunHandle.detachTunFd();
            if (tunFd < 0)
                throw new BackendException(Reason.TUN_CREATION_ERROR);

            Log.d(TAG, "Go backend " + wgVersion());
            currentTunnelHandle = wgTurnOn(tunnel.getName(), tunFd, goConfig, tunHandle.isCustomTun());

            if (currentTunnelHandle < 0)
                throw new BackendException(Reason.GO_ACTIVATION_ERROR_CODE, currentTunnelHandle);

            currentTunnel = tunnel;
            currentConfig = config;

            // Once per session. DisableSomeRoamingForBrokenMobileSemantics() means the UDP bind
            // is never rebuilt, so these do not need repeating on a roam.
            tunHandle.protect(wgGetSocketV4(currentTunnelHandle));
            tunHandle.protect(wgGetSocketV6(currentTunnelHandle));
        } else {
            if (currentTunnelHandle == -1) {
                Log.w(TAG, "Tunnel already down");
                return;
            }
            int handleToClose = currentTunnelHandle;
            currentTunnel = null;
            currentTunnelHandle = -1;
            currentConfig = null;
            // Nothing else to do. The service is not ours to stop and the TUN is not ours to
            // close — both outlive this engine by design.
            wgTurnOff(handleToClose);
        }

        tunnel.onStateChange(state);
    }

    /**
     * Forces the tunnel down without going through {@link #setState}. Used when the session is
     * ending abruptly — a revoked VPN consent, or the owning service being destroyed — where there
     * is no point negotiating state.
     */
    public void forceDown() {
        synchronized (tunnelStateLock) {
            final Tunnel tunnel = currentTunnel;
            if (tunnel == null) {
                return;
            }
            if (currentTunnelHandle != -1) {
                wgTurnOff(currentTunnelHandle);
            }
            currentTunnel = null;
            currentTunnelHandle = -1;
            currentConfig = null;
            tunnel.onStateChange(State.DOWN);
        }
    }
}
