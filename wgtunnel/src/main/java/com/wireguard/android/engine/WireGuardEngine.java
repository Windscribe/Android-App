/*
 * Copyright (c) 2026 Windscribe Limited.
 */

package com.wireguard.android.engine;

import android.os.ParcelFileDescriptor;

import androidx.annotation.Nullable;

import com.windscribe.common.tunnel.EngineProfile;
import com.windscribe.common.tunnel.VpnEngine;
import com.windscribe.common.tunnel.VpnHost;
import com.wireguard.android.backend.GoBackend;
import com.wireguard.android.backend.Tunnel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * WireGuard as a plain object.
 *
 * <p>wg-go's whole coupling to a {@code VpnService} is two {@code protect()} calls
 * (docs/architecture/VPN_BACKEND.md §2).
 *
 * <p><b>Always a socketpair</b>, never the real TUN, which is what makes the engine swappable
 * without touching the interface. The cost is the mux's copy and thread hop on every path.
 */
public final class WireGuardEngine implements VpnEngine {

    private static final Logger log = LoggerFactory.getLogger("wireguard");

    /** Reports wg-go's own tunnel state so the orchestrator can drive connection state. */
    public interface StateCallback {
        void onWireGuardState(Tunnel.State state);
    }

    private final GoBackend backend;
    private final StateCallback stateCallback;
    @Nullable private EngineTunnel tunnel;

    public WireGuardEngine(final GoBackend backend, final StateCallback stateCallback) {
        this.backend = backend;
        this.stateCallback = stateCallback;
    }

    @Override
    public String getId() {
        return WireGuardEngineProfile.PROTOCOL;
    }

    /**
     * Resolves the peer endpoints so {@link #start} finds them cached. The slow part of bringing
     * wg-go up: ten retries a second apart, each with getaddrinfo's own timeout on top.
     */
    @Override
    public void prepare(final EngineProfile profile) throws Exception {
        if (!(profile instanceof WireGuardEngineProfile)) {
            throw new IllegalArgumentException("not a WireGuard profile: " + profile.getProtocol());
        }
        backend.resolveEndpoints(((WireGuardEngineProfile) profile).getConfig());
    }

    @Override
    public void start(final EngineProfile profile, final VpnHost host,
                      final ParcelFileDescriptor engineFd) throws Exception {
        if (!(profile instanceof WireGuardEngineProfile)) {
            throw new IllegalArgumentException("not a WireGuard profile: " + profile.getProtocol());
        }
        final WireGuardEngineProfile wgProfile = (WireGuardEngineProfile) profile;
        final EngineTunnel t = new EngineTunnel(wgProfile.getTunnelName(), stateCallback);
        tunnel = t;

        // wg-go takes ownership of what detachTunFd() returns and closes it: detach once, never touch.
        final DetachOnce handoff = new DetachOnce(engineFd, host);
        try {
            backend.setState(t, Tunnel.State.UP, wgProfile.getConfig(), handoff);
        } catch (final Exception e) {
            // If wg-go never took the descriptor we still own it and must not leak it.
            handoff.releaseIfUntaken();
            tunnel = null;
            throw e;
        }
        log.info("wg-go up on a mux descriptor");
    }

    @Override
    public void stop() {
        final EngineTunnel t = tunnel;
        tunnel = null;
        if (t == null) {
            return;
        }
        try {
            backend.setState(t, Tunnel.State.DOWN, null, null);
        } catch (final Exception e) {
            log.warn("graceful down failed, forcing", e);
            backend.forceDown();
        }
    }

    @Override
    public void onRevoked() {
        log.warn("VPN consent revoked");
        tunnel = null;
        backend.forceDown();
    }

    /**
     * Hands the descriptor over exactly once, and knows whether it did — so a failed start can
     * close a descriptor wg-go never took, without risking a double close of one it did.
     */
    private static final class DetachOnce implements GoBackend.TunHandle {
        private final ParcelFileDescriptor pfd;
        private final VpnHost host;
        private boolean taken;

        DetachOnce(final ParcelFileDescriptor pfd, final VpnHost host) {
            this.pfd = pfd;
            this.host = host;
        }

        @Override
        public int detachTunFd() {
            if (taken) {
                throw new IllegalStateException("descriptor already handed to wg-go");
            }
            taken = true;
            return pfd.detachFd();
        }

        @Override
        public boolean protect(final int socketFd) {
            return host.protect(socketFd);
        }

        @Override
        public boolean isCustomTun() {
            // Always. The engine never sees a real tun device in this architecture, and this flag
            // is what tells the amneziawg-go fork to skip every tun-specific ioctl.
            return true;
        }

        void releaseIfUntaken() {
            if (taken) {
                return;
            }
            try {
                pfd.close();
            } catch (final IOException ignored) {
            }
        }
    }

    /** Minimal {@link Tunnel} that forwards wg-go's state changes to the orchestrator. */
    private static final class EngineTunnel implements Tunnel {
        private final String name;
        private final StateCallback callback;

        EngineTunnel(final String name, final StateCallback callback) {
            this.name = name;
            this.callback = callback;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public void onStateChange(final State newState) {
            callback.onWireGuardState(newState);
        }
    }
}
