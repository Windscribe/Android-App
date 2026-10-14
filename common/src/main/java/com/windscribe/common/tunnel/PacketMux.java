/*
 * Copyright (c) 2026 Windscribe Limited.
 */

package com.windscribe.common.tunnel;

import static android.system.OsConstants.AF_UNIX;
import static android.system.OsConstants.EAGAIN;
import static android.system.OsConstants.EBADF;
import static android.system.OsConstants.ECONNRESET;
import static android.system.OsConstants.EINTR;
import static android.system.OsConstants.EPIPE;
import static android.system.OsConstants.POLLIN;
import static android.system.OsConstants.SOCK_SEQPACKET;

import android.os.ParcelFileDescriptor;
import android.system.ErrnoException;
import android.system.Os;
import android.system.StructPollfd;
import android.util.Log;

import androidx.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileDescriptor;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Owns the packet path between the TUN and whichever engine is currently attached.
 *
 * <p>Detaching one engine and attaching another must not disturb the TUN, which constrains the
 * implementation:
 *
 * <ul>
 *   <li>{@code Os.poll}, not NIO, which cannot {@code select()} on an arbitrary fd. Each pump polls
 *       {@code {source, wakeupPipe}} so it wakes without closing what it reads.</li>
 *   <li>The wakeup pipe is mandatory: {@code dup2} leaves a thread already in {@code read()}
 *       holding the old {@code struct file}.</li>
 *   <li>State and descriptors move together in one immutable {@link Snapshot}, so they cannot be
 *       read torn. Buffers are per-pump for the same reason.</li>
 *   <li>DROP actively drains; a queue nothing reads backs up into apps' {@code send()}.</li>
 *   <li>Every descriptor written to is non-blocking, so a congested peer costs a packet rather than
 *       parking a pump where {@link #quiesce} cannot recall it.</li>
 *   <li>Pumps are supervised and restartable.</li>
 *   <li>DNS is a separate failure domain: if the stage dies, the mux keeps forwarding.</li>
 * </ul>
 *
 * <p>Ownership: the host owns the TUN and the mux never closes it. The mux owns its side of each
 * engine socketpair and closes it on detach; the engine owns the side it is given.
 */
public final class PacketMux {

    private static final String TAG = "PacketMux";
    /** Session lifecycle and hard failures. Per-packet paths stay on {@link #TAG} alone. */
    private static final Logger log = LoggerFactory.getLogger("mux");

    static {
        try {
            System.loadLibrary("wscommon");
        } catch (UnsatisfiedLinkError e) {
            // Host JVM tests drive the wiring without descriptors; only nativeSetNonBlocking needs
            // the library, and it still throws if a descriptor path is reached without it.
            log.warn("wscommon unavailable: {}", e.getMessage());
        }
    }

    /** Bounded wait for both pumps to release the old descriptors during a swap. */
    private static final long QUIESCE_TIMEOUT_MS = 250;
    /** Consecutive pump failures tolerated before the mux gives up and reports. */
    private static final int MAX_PUMP_RESTARTS = 5;
    private static final long PUMP_RESTART_BACKOFF_MS = 50;
    /** A pump that ran this long before failing is healthy; its restart budget resets. */
    private static final long HEALTHY_RUN_NANOS = 30L * 1_000_000_000L;
    /** Headroom over the MTU so an oversized frame is read rather than silently truncated. */
    private static final int BUFFER_HEADROOM = 512;

    public enum State {
        /** Packets move between the TUN and the attached engine. */
        FORWARD,
        /** No engine is carrying traffic. Egress is read and discarded so apps do not block. */
        DROP
    }

    /**
     * What the packet path has carried, as one reading. Plaintext at the TUN boundary: DNS the stage
     * answered locally is counted in neither direction.
     */
    public static final class Traffic {
        public final State state;
        public final long egressPackets;
        public final long ingressPackets;

        Traffic(State state, long egressPackets, long ingressPackets) {
            this.state = state;
            this.egressPackets = egressPackets;
            this.ingressPackets = ingressPackets;
        }
    }

    /** Notified when the mux can no longer do its job. Never called from a pump's hot path. */
    public interface Listener {
        void onMuxFailure(String detail);
    }

    /** Somewhere to write a packet. Given to the DNS stage so it can answer into the TUN. */
    public interface PacketSink {
        void write(byte[] data, int offset, int length);
    }

    /** DNS interception, kept in its own failure domain. See ControlDDnsStage. */
    public interface DnsStage {
        /**
         * @param towardApps  writes an IP packet into the live TUN (a DNS answer).
         * @param towardEngine writes an IP packet to the attached engine — used when the stage
         *                     accepted a packet and then decided not to intercept it after all.
         */
        void start(PacketSink towardApps, PacketSink towardEngine);

        void stop();

        /** A connect is starting. Lets the stage reset what is scoped to a connect, not to itself. */
        default void onEngineAttached() {
        }

        /**
         * @return true if this stage took ownership of the packet; false to pass it through to the
         * engine. Must never block: backpressure is a drop plus a counter, never a wait.
         */
        boolean offer(byte[] packet, int offset, int length);
    }

    /** State and descriptors in one object, so a pump can never observe a torn pair. */
    private static final class Snapshot {
        final State state;
        @Nullable final FileDescriptor tun;
        @Nullable final FileDescriptor engine;
        final int epoch;
        final int mtu;

        Snapshot(State state, @Nullable FileDescriptor tun, @Nullable FileDescriptor engine,
                 int epoch, int mtu) {
            this.state = state;
            this.tun = tun;
            this.engine = engine;
            this.epoch = epoch;
            this.mtu = mtu;
        }
    }

    private enum Direction {
        /** TUN -> engine. Apps' egress. Classifies DNS. */
        EGRESS,
        /** engine -> TUN. Apps' ingress. */
        INGRESS
    }

    private final AtomicReference<Snapshot> snapshot;
    private final Listener listener;
    /** Volatile: the egress pump reads it per packet. Replaceable — see {@link #setDnsStage}. */
    @Nullable private volatile DnsStage dnsStage;
    private final boolean packetLogging;

    /** One wakeup pipe per pump. A shared pipe would be racy — one pump would eat the other's byte. */
    @Nullable private ParcelFileDescriptor[] egressWakeup;
    @Nullable private ParcelFileDescriptor[] ingressWakeup;

    @Nullable private volatile CountDownLatch quiesceLatch;
    private volatile boolean running;
    @Nullable private Thread egressPump;
    @Nullable private Thread ingressPump;

    /** Mux's own end of the live engine socketpair. Closed only after a successful quiesce. */
    @Nullable private ParcelFileDescriptor engineMuxSide;

    /** Per-pump scratch for wakeup bytes; each pump only ever touches its own pipe. */
    private final byte[] wakeupSink = new byte[64];

    private final AtomicLong droppedInDropState = new AtomicLong();
    /** Queries the stage could not take, passed to the engine rather than dropped. */
    private final AtomicLong dnsQueueFullPassThrough = new AtomicLong();
    private final AtomicLong forwardedEgress = new AtomicLong();
    private final AtomicLong forwardedIngress = new AtomicLong();

    public PacketMux(Listener listener, @Nullable DnsStage dnsStage, boolean packetLogging) {
        this.listener = listener;
        this.dnsStage = dnsStage;
        this.packetLogging = packetLogging;
        this.snapshot = new AtomicReference<>(new Snapshot(State.DROP, null, null, 0, 1280));
    }

    // ---------------------------------------------------------------- lifecycle

    /**
     * Replaces the DNS stage without dropping the interface. Cleared before the old one is stopped:
     * a moment of pass-through is recoverable, a query handed to a dead listener is not.
     */
    public synchronized void setDnsStage(@Nullable DnsStage stage) {
        DnsStage previous = dnsStage;
        if (previous == stage) {
            return;
        }
        dnsStage = null;
        if (previous != null) {
            try {
                previous.stop();
            } catch (Throwable t) {
                log.warn("DNS stage stop failed", t);
            }
        }
        if (stage == null || !running) {
            return;
        }
        try {
            stage.start(this::writeToTun, this::writeToEngine);
            dnsStage = stage;
        } catch (Throwable t) {
            // A dead DNS stage must never stop traffic.
            log.error("DNS stage failed to start; continuing in pass-through", t);
        }
    }

    /**
     * Starts the pumps against [tun]. The mux does not own [tun] and will never close it.
     */
    public synchronized void start(FileDescriptor tun, int mtu) throws ErrnoException, IOException {
        if (running) {
            throw new IllegalStateException("PacketMux already started");
        }
        try {
            egressWakeup = nonBlockingPipe();
            ingressWakeup = nonBlockingPipe();
            publish(new Snapshot(State.DROP, tun, null, 1, mtu));
            running = true;
            if (dnsStage != null) {
                try {
                    dnsStage.start(this::writeToTun, this::writeToEngine);
                } catch (Throwable t) {
                    // A dead DNS stage must never stop traffic.
                    log.error("DNS stage failed to start; continuing in pass-through", t);
                }
            }
            egressPump = startPump(Direction.EGRESS);
            ingressPump = startPump(Direction.INGRESS);
        } catch (ErrnoException | IOException | RuntimeException | Error e) {
            abandonPartialStart();
            throw e;
        }
        log.info("started, mtu=" + mtu);
    }

    /** Releases what a failed {@link #start} took. {@link #stop} early-returns before {@code running}. */
    private void abandonPartialStart() {
        if (running) {
            stop();
            return;
        }
        if (egressWakeup != null) {
            closeQuietly(egressWakeup[0]);
            closeQuietly(egressWakeup[1]);
            egressWakeup = null;
        }
        if (ingressWakeup != null) {
            closeQuietly(ingressWakeup[0]);
            closeQuietly(ingressWakeup[1]);
            ingressWakeup = null;
        }
    }

    /**
     * Replaces the TUN the pumps read and write, for the cases {@link TunConfig#coversSameInterfaceAs}
     * has already ruled out reusing. The old descriptor stays the host's to close, after this returns.
     */
    public synchronized void setTun(FileDescriptor tun, int mtu) {
        Snapshot cur = snapshot.get();
        quiesce(new Snapshot(State.DROP, null, cur.engine, cur.epoch + 1, mtu), "setTun");
        publishAndWake(new Snapshot(cur.state, tun, cur.engine, snapshot.get().epoch + 1, mtu));
        // WindscribeVpnService and TunnelManager log these at info, with the detail these lack.
        log.debug("TUN replaced, mtu=" + mtu);
    }

    /**
     * Creates a fresh socketpair, hands the engine its end, and attaches the other end here.
     *
     * <p>Any previously attached engine is detached first, with a full quiesce, so no pump can hold
     * a descriptor when it is closed and read a recycled one.
     *
     * @return the descriptor to hand to {@link VpnEngine#start}. The engine owns and closes it.
     */
    public synchronized ParcelFileDescriptor attachEngine() throws ErrnoException, IOException {
        detachEngine();

        FileDescriptor rawMux = new FileDescriptor();
        FileDescriptor rawEngine = new FileDescriptor();
        Os.socketpair(AF_UNIX, SOCK_SEQPACKET, 0, rawMux, rawEngine);

        ParcelFileDescriptor muxSide = null;
        ParcelFileDescriptor engineSide = null;
        try {
            muxSide = ParcelFileDescriptor.dup(rawMux);
            engineSide = ParcelFileDescriptor.dup(rawEngine);
            // Our side only: socketpair() returns two independent open file descriptions, so the
            // engine keeps the blocking end it expects.
            setNonBlocking(muxSide);
        } catch (ErrnoException | IOException e) {
            closeQuietly(muxSide);
            closeQuietly(engineSide);
            throw e;
        } finally {
            // Left open these leak two descriptors per attach, and neither side can observe EOF.
            closeRaw(rawMux);
            closeRaw(rawEngine);
        }

        engineMuxSide = muxSide;
        if (dnsStage != null) {
            try {
                dnsStage.onEngineAttached();
            } catch (Throwable t) {
                log.warn("DNS stage rejected the attach notice", t);
            }
        }
        Snapshot cur = snapshot.get();
        publishAndWake(new Snapshot(State.FORWARD, cur.tun, muxSide.getFileDescriptor(),
                cur.epoch + 1, cur.mtu));
        log.debug("engine attached, forwarding");
        return engineSide;
    }

    /**
     * Detaches the current engine and enters DROP. The TUN stays established and egress keeps being
     * drained, so apps see a black hole rather than a hang or a leak.
     */
    public synchronized void detachEngine() {
        Snapshot cur = snapshot.get();
        if (cur.engine == null && engineMuxSide == null) {
            return;
        }
        quiesce(new Snapshot(State.DROP, cur.tun, null, cur.epoch + 1, cur.mtu), "detachEngine");
        // Safe now: both pumps have acknowledged the epoch and released the descriptor.
        closeQuietly(engineMuxSide);
        engineMuxSide = null;
        log.debug("engine detached, dropping");
    }

    public State state() {
        return snapshot.get().state;
    }

    /**
     * Not a consistent snapshot: the pumps keep running, so callers work in deltas. Ingress is read
     * last, so a straddled reading over-reports it rather than showing silence that was not there.
     */
    public Traffic traffic() {
        State state = snapshot.get().state;
        long egressPackets = forwardedEgress.get();
        long ingressPackets = forwardedIngress.get();
        return new Traffic(state, egressPackets, ingressPackets);
    }

    public synchronized void stop() {
        if (!running) {
            return;
        }
        running = false;
        Snapshot cur = snapshot.get();
        publishAndWake(new Snapshot(State.DROP, null, null, cur.epoch + 1, cur.mtu));

        joinQuietly(egressPump);
        joinQuietly(ingressPump);
        egressPump = null;
        ingressPump = null;

        if (dnsStage != null) {
            try {
                dnsStage.stop();
            } catch (Throwable t) {
                log.warn("DNS stage stop failed", t);
            }
        }
        // Pumps are joined, so nothing can be holding these.
        closeQuietly(engineMuxSide);
        engineMuxSide = null;
        if (egressWakeup != null) {
            closeQuietly(egressWakeup[0]);
            closeQuietly(egressWakeup[1]);
            egressWakeup = null;
        }
        if (ingressWakeup != null) {
            closeQuietly(ingressWakeup[0]);
            closeQuietly(ingressWakeup[1]);
            ingressWakeup = null;
        }
        log.info("stopped. egress=" + forwardedEgress.get() + " ingress=" + forwardedIngress.get()
                + " droppedInDrop=" + droppedInDropState.get()
                + " dnsQueueFullPassThrough=" + dnsQueueFullPassThrough.get());
    }

    // ---------------------------------------------------------------- swap machinery

    /**
     * Publishes [target], wakes both pumps and waits, bounded, for both to acknowledge the new epoch
     * and so release the previous descriptors. Proceeds on timeout: blocking a disconnect forever is
     * worse than a late write to a descriptor about to close.
     */
    private void quiesce(Snapshot target, String why) {
        CountDownLatch ack = new CountDownLatch(2);
        // Order matters: the latch must exist before the epoch is visible, or a pump can observe
        // the new epoch, find no latch, and never acknowledge.
        quiesceLatch = ack;
        publishAndWake(target);
        boolean settled = false;
        try {
            settled = ack.await(QUIESCE_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            quiesceLatch = null;
        }
        if (!settled) {
            log.error("quiesce(" + why + ") TIMED OUT after " + QUIESCE_TIMEOUT_MS
                    + "ms; proceeding. A pump was likely blocked writing to a full queue.");
        }
    }

    private void publish(Snapshot s) {
        snapshot.set(s);
    }

    private void publishAndWake(Snapshot s) {
        snapshot.set(s);
        ParcelFileDescriptor[] eg = egressWakeup;
        ParcelFileDescriptor[] in = ingressWakeup;
        if (eg != null) {
            poke(eg[1].getFileDescriptor());
        }
        if (in != null) {
            poke(in[1].getFileDescriptor());
        }
    }

    /** A wakeup pipe that cannot park a thread: a blocking one stalls the swap doing the waking. */
    private static ParcelFileDescriptor[] nonBlockingPipe() throws ErrnoException, IOException {
        ParcelFileDescriptor[] pipe = ParcelFileDescriptor.createPipe();
        setNonBlocking(pipe[0]);
        setNonBlocking(pipe[1]);
        return pipe;
    }

    /** Non-blocking, so a congested peer costs a dropped packet rather than a parked pump. */
    private static void setNonBlocking(ParcelFileDescriptor fd) throws ErrnoException {
        int errno = nativeSetNonBlocking(fd.getFd());
        if (errno != 0) {
            throw new ErrnoException("fcntl", errno);
        }
    }

    /**
     * Sets O_NONBLOCK through libc. {@code Os.fcntlInt} is public SDK only from API 30, and below
     * that the hidden-API check denies the call.
     *
     * @return errno, or 0 on success.
     */
    private static native int nativeSetNonBlocking(int fd);

    private void poke(@Nullable FileDescriptor writeEnd) {
        if (writeEnd == null) {
            return;
        }
        try {
            Os.write(writeEnd, new byte[]{1}, 0, 1);
        } catch (ErrnoException e) {
            // A full pipe still wakes. Anything else means a pump that cannot be quiesced.
            if (e.errno != EAGAIN) {
                log.warn("wakeup poke failed: " + e.getMessage());
            }
        } catch (IOException e) {
            log.warn("wakeup poke failed: " + e.getMessage());
        }
    }

    /**
     * Consumes pending wakeup bytes in exactly one read: the pipe is blocking, so draining it to
     * empty would park the pump inside its own wakeup channel. Later bytes just fire another poll.
     */
    private void drainWakeup(@Nullable FileDescriptor readEnd) {
        if (readEnd == null) {
            return;
        }
        try {
            Os.read(readEnd, wakeupSink, 0, wakeupSink.length);
        } catch (ErrnoException | IOException ignored) {
        }
    }

    // ---------------------------------------------------------------- pumps

    private Thread startPump(Direction dir) {
        Thread t = new Thread(() -> supervise(dir), "PacketMux-" + dir);
        t.setDaemon(true);
        t.start();
        return t;
    }

    /** Keeps a pump alive across faults; one that dies silently means "connected, passing nothing". */
    private void supervise(Direction dir) {
        int restarts = 0;
        while (running) {
            final long startedAt = System.nanoTime();
            try {
                pump(dir);
                return; // clean exit: mux is stopping
            } catch (Throwable t) {
                if (!running) {
                    return;
                }
                // Only failures in quick succession count: the budget catches a pump that cannot
                // run, not one-off faults accumulated over a long session.
                if (System.nanoTime() - startedAt > HEALTHY_RUN_NANOS) {
                    restarts = 0;
                }
                restarts++;
                log.error(dir + " pump failed (restart " + restarts + "/" + MAX_PUMP_RESTARTS + ")", t);
                if (restarts >= MAX_PUMP_RESTARTS) {
                    listener.onMuxFailure(dir + " pump failed " + restarts + "x: " + t.getMessage());
                    return;
                }
                try {
                    Thread.sleep(PUMP_RESTART_BACKOFF_MS * restarts);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
    }

    private void pump(Direction dir) throws ErrnoException, IOException {
        final ParcelFileDescriptor[] wakeupPair = dir == Direction.EGRESS ? egressWakeup : ingressWakeup;
        if (wakeupPair == null) {
            return;
        }
        final FileDescriptor wakeupRead = wakeupPair[0].getFileDescriptor();
        int localEpoch = -1;
        byte[] buf = null;

        StructPollfd srcPfd = new StructPollfd();
        StructPollfd wakePfd = new StructPollfd();
        wakePfd.fd = wakeupRead;
        wakePfd.events = (short) POLLIN;
        StructPollfd[] pfds = new StructPollfd[]{srcPfd, wakePfd};

        while (running) {
            Snapshot s = snapshot.get();

            if (s.epoch != localEpoch) {
                localEpoch = s.epoch;
                // Everything from the previous epoch is released by virtue of re-reading here.
                if (buf == null || buf.length < s.mtu + BUFFER_HEADROOM) {
                    buf = new byte[s.mtu + BUFFER_HEADROOM];
                }
                CountDownLatch ack = quiesceLatch;
                if (ack != null) {
                    ack.countDown();
                }
            }

            FileDescriptor src = dir == Direction.EGRESS ? s.tun : s.engine;
            FileDescriptor dst = dir == Direction.EGRESS ? s.engine : s.tun;

            if (src == null) {
                // Nothing to read from in this epoch. Park on the wakeup pipe only.
                pfds = new StructPollfd[]{wakePfd};
                int ready = pollInterruptibly(pfds, 200);
                if (ready > 0 && (wakePfd.revents & POLLIN) != 0) {
                    drainWakeup(wakeupRead);
                }
                pfds = new StructPollfd[]{srcPfd, wakePfd};
                continue;
            }

            srcPfd.fd = src;
            srcPfd.events = (short) POLLIN;
            srcPfd.revents = 0;
            wakePfd.revents = 0;

            int ready = pollInterruptibly(pfds, 200);
            if (ready <= 0) {
                continue;
            }
            if ((wakePfd.revents & POLLIN) != 0) {
                drainWakeup(wakeupRead);
                continue; // re-read the snapshot before touching any descriptor
            }
            if ((srcPfd.revents & POLLIN) == 0) {
                continue;
            }

            int n;
            try {
                n = Os.read(src, buf, 0, buf.length);
            } catch (ErrnoException e) {
                if (e.errno == EINTR || e.errno == EAGAIN) {
                    continue;
                }
                if (e.errno == EBADF) {
                    // The descriptor moved under us; the snapshot is authoritative.
                    continue;
                }
                if (e.errno == ECONNRESET || e.errno == EPIPE) {
                    // The normal shape of an engine going away during a handover, not a fault: it
                    // closes the descriptor it owns and we find out by reading. Re-read and carry on.
                    Thread.yield();
                    continue;
                }
                throw e;
            }
            if (n <= 0) {
                // EOF. Yield rather than hot-spinning on a core.
                Thread.yield();
                continue;
            }

            if (dir == Direction.EGRESS) {
                handleEgress(s, dst, buf, n);
            } else {
                writeTo(dst, buf, n);
                forwardedIngress.incrementAndGet();
            }
        }
    }

    private void handleEgress(Snapshot s, @Nullable FileDescriptor dst, byte[] buf, int n) {
        if (s.state == State.DROP || dst == null) {
            // Ahead of the DNS stage: the hold excludes our own package, so resolving here would go
            // out on the physical interface. Discarded rather than left to back up into send().
            droppedInDropState.incrementAndGet();
            return;
        }
        if (dnsStage != null && isUdpPort53(buf, n)) {
            try {
                if (dnsStage.offer(buf, 0, n)) {
                    return;
                }
                dnsQueueFullPassThrough.incrementAndGet();
            } catch (Throwable t) {
                // The DNS stage must never be able to stop traffic. Fall through to pass-through.
                Log.w(TAG, "DNS stage threw; passing packet through", t);
            }
        }
        writeTo(dst, buf, n);
        forwardedEgress.incrementAndGet();
    }

    private void writeTo(@Nullable FileDescriptor dst, byte[] buf, int len) {
        if (dst == null) {
            return;
        }
        int off = 0;
        while (off < len) {
            try {
                int w = Os.write(dst, buf, off, len - off);
                if (w <= 0) {
                    return;
                }
                off += w;
            } catch (ErrnoException e) {
                if (e.errno == EINTR) {
                    continue;
                }
                if (e.errno == EAGAIN) {
                    return; // drop rather than spin; the peer is congested
                }
                if (packetLogging) {
                    Log.v(TAG, "write failed: " + e.getMessage());
                }
                return;
            } catch (IOException e) {
                return;
            }
        }
    }

    /** Used by the DNS stage to answer an app. Always targets the live TUN. */
    private void writeToTun(byte[] data, int offset, int length) {
        Snapshot s = snapshot.get();
        if (s.state != State.FORWARD) {
            // In flight when the hold began; the app it answers is being held.
            droppedInDropState.incrementAndGet();
            return;
        }
        writeTo(s.tun, sliceIfNeeded(data, offset, length), length);
    }

    /**
     * Used by the DNS stage to pass a packet through after declining to intercept it. Resolves the
     * engine from the live snapshot, so a packet in flight during a swap lands on the new engine or
     * is dropped — never on a descriptor that has been closed.
     */
    private void writeToEngine(byte[] data, int offset, int length) {
        Snapshot s = snapshot.get();
        if (s.state != State.FORWARD) {
            droppedInDropState.incrementAndGet();
            return;
        }
        writeTo(s.engine, sliceIfNeeded(data, offset, length), length);
    }

    private static byte[] sliceIfNeeded(byte[] data, int offset, int length) {
        if (offset == 0) {
            return data;
        }
        byte[] out = new byte[length];
        System.arraycopy(data, offset, out, 0, length);
        return out;
    }

    private int pollInterruptibly(StructPollfd[] pfds, int timeoutMs) throws ErrnoException {
        while (true) {
            try {
                return Os.poll(pfds, timeoutMs);
            } catch (ErrnoException e) {
                if (e.errno == EINTR) {
                    continue;
                }
                throw e;
            }
        }
    }

    // ---------------------------------------------------------------- classifier

    /**
     * Five-byte inline prefilter for "is this UDP to port 53".
     *
     * <p>Avoids a full pcap4j parse on the data path. {@code getRawData()} is an unconditional
     * defensive copy, so parsing here would cost roughly six allocations and six full
     * copies per egress packet in order to test one 16-bit field (P2). This tests the same field
     * with no allocation, and keeps pcap4j out of the path for >99.9% of packets.
     */
    static boolean isUdpPort53(byte[] p, int len) {
        if (len < 8) {
            return false;
        }
        int version = (p[0] & 0xF0) >> 4;
        if (version == 4) {
            int ihl = (p[0] & 0x0F) * 4;
            if (ihl < 20 || len < ihl + 4) {
                return false;
            }
            if ((p[9] & 0xFF) != 17) { // IPPROTO_UDP
                return false;
            }
            int dstPort = ((p[ihl + 2] & 0xFF) << 8) | (p[ihl + 3] & 0xFF);
            return dstPort == 53;
        }
        if (version == 6) {
            if (len < 48) {
                return false;
            }
            if ((p[6] & 0xFF) != 17) { // next header == UDP; extension headers are not followed
                return false;
            }
            int dstPort = ((p[42] & 0xFF) << 8) | (p[43] & 0xFF);
            return dstPort == 53;
        }
        return false;
    }

    // ---------------------------------------------------------------- util

    private static void closeQuietly(@Nullable ParcelFileDescriptor pfd) {
        if (pfd == null) {
            return;
        }
        try {
            pfd.close();
        } catch (IOException ignored) {
        }
    }

    private static void closeRaw(@Nullable FileDescriptor fd) {
        if (fd == null || !fd.valid()) {
            return;
        }
        try {
            Os.close(fd);
        } catch (ErrnoException ignored) {
        }
    }

    private static void joinQuietly(@Nullable Thread t) {
        if (t == null) {
            return;
        }
        try {
            t.join(QUIESCE_TIMEOUT_MS * 4);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        if (t.isAlive()) {
            log.error(t.getName() + " did not exit; descriptors may be held");
        }
    }
}
