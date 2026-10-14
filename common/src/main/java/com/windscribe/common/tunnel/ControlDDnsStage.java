/*
 * Copyright (c) 2026 Windscribe Limited.
 */

package com.windscribe.common.tunnel;

import android.util.Log;

import androidx.annotation.Nullable;

import org.minidns.dnsmessage.DnsMessage;
import org.pcap4j.packet.IpPacket;
import org.pcap4j.packet.IpSelector;
import org.pcap4j.packet.IpV4Packet;
import org.pcap4j.packet.IpV6Packet;
import org.pcap4j.packet.UdpPacket;
import org.pcap4j.packet.UnknownPacket;
import org.pcap4j.packet.namednumber.UdpPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketTimeoutException;
import java.security.SecureRandom;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Intercepts UDP/53 from apps and resolves it through the local ctrld listener.
 *
 * <p>Not on the data path: {@link PacketMux} classifies with an inline prefilter and only DNS
 * packets reach this class. If the stage dies, backs up, or throws, the mux keeps forwarding.
 *
 * <p>The queue is bounded and {@link #offer} never blocks. Responses are forwarded byte-for-byte
 * with only the two ID bytes patched: re-encoding through minidns would destroy name compression,
 * unknown RRs and DNSSEC byte-exactness.
 *
 * <p>Known limitation: TCP/53 is not intercepted. The prefilter matches UDP only, so a TC=1 retry
 * over TCP, and DoT/DoH, reach the engine unmodified and bypass ctrld.
 */
public final class ControlDDnsStage implements PacketMux.DnsStage {

    private static final String TAG = "ControlDDnsStage";
    /** Lifecycle and failures that change resolution, so the log shows ctrld or pass-through. */
    private static final Logger log = LoggerFactory.getLogger("dns");

    private static final int QUEUE_DEPTH = 512;
    private static final int RESPONSE_BUFFER_BYTES = 4096;
    private static final int RECEIVE_TIMEOUT_MS = 200;
    private static final long QUERY_TIMEOUT_MS = 5000;
    /** Bounds memory and transaction-ID exhaustion when an app floods queries. */
    private static final int MAX_PENDING = 4096;
    private static final int CONNECT_ATTEMPTS = 3;

    /** Bounded and non-blocking by contract. Backpressure is a drop, never a wait. */
    private final BlockingQueue<byte[]> inbound = new ArrayBlockingQueue<>(QUEUE_DEPTH);

    /**
     * Keyed by the upstream transaction ID <em>this stage</em> assigned, never the app's: two apps
     * that picked the same ID would be indistinguishable, and could get each other's answers.
     */
    private final Map<Integer, PendingQuery> pending = new ConcurrentHashMap<>();
    private final AtomicInteger nextUpstreamId = new AtomicInteger(new SecureRandom().nextInt(0x10000));

    private final AtomicLong queueDrops = new AtomicLong();
    private final AtomicLong answered = new AtomicLong();
    private final AtomicLong timedOut = new AtomicLong();

    private final InetSocketAddress resolver;
    private final boolean packetLogging;
    private final FailureListener failureListener;

    @Nullable private volatile DatagramSocket socket;
    @Nullable private ScheduledExecutorService timeouts;
    @Nullable private Thread txThread;
    @Nullable private Thread rxThread;
    private volatile boolean running;

    @Nullable private PacketMux.PacketSink towardApps;
    @Nullable private PacketMux.PacketSink towardEngine;

    /**
     * Lets one lookup under windscribe.com skip ctrld, so the connectivity test resolves without
     * waiting for ctrld to be ready.
     *
     * <p>Per-connect, not per-stage: the stage spans the whole session, so a stage-lifetime flag
     * would be spent by the first query. {@link #armBypass()} re-arms it on every engine attach.
     */
    private volatile boolean windscribeBypassAvailable = true;

    public interface FailureListener {
        /** DNS is no longer working. The mux stays in pass-through; the caller decides what to do. */
        void onDnsStageFailure(String detail);
    }

    public ControlDDnsStage(int controlDPort, boolean packetLogging, FailureListener failureListener) {
        this.resolver = new InetSocketAddress("127.0.0.1", controlDPort);
        this.packetLogging = packetLogging;
        this.failureListener = failureListener;
    }

    // ---------------------------------------------------------------- lifecycle

    @Override
    public synchronized void start(PacketMux.PacketSink towardApps, PacketMux.PacketSink towardEngine) {
        if (running) {
            return;
        }
        this.towardApps = towardApps;
        this.towardEngine = towardEngine;
        DatagramSocket s = openSocket();
        if (s == null) {
            // Never proceed with a null socket. Report and stay down; the mux keeps forwarding.
            failureListener.onDnsStageFailure("could not reach ctrld at " + resolver);
            return;
        }
        socket = s;
        running = true;
        timeouts = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "dns-timeout");
            t.setDaemon(true);
            return t;
        });
        txThread = spawn("dns-tx", this::txLoop);
        rxThread = spawn("dns-rx", this::rxLoop);
        log.info("started, resolver=" + resolver);
    }

    @Override
    public synchronized void stop() {
        if (!running) {
            closeSocket();
            return;
        }
        running = false;
        // Closing the socket is what unblocks rx; interrupting is what unblocks tx's queue take.
        closeSocket();
        interruptQuietly(txThread);
        interruptQuietly(rxThread);
        joinQuietly(txThread);
        joinQuietly(rxThread);
        txThread = null;
        rxThread = null;
        ScheduledExecutorService t = timeouts;
        if (t != null) {
            t.shutdownNow();
            timeouts = null;
        }
        pending.clear();
        inbound.clear();
        log.info("stopped. answered=" + answered.get() + " timedOut=" + timedOut.get()
                + " queueDrops=" + queueDrops.get());
    }

    /**
     * Called on the mux's egress pump thread, so it does no parsing and never blocks — it copies
     * the bytes and hands off. All DNS work happens on this stage's own threads.
     */
    @Override
    public boolean offer(byte[] packet, int offset, int length) {
        if (!running) {
            return false;
        }
        byte[] copy = new byte[length];
        System.arraycopy(packet, offset, copy, 0, length);
        if (!inbound.offer(copy)) {
            long n = queueDrops.incrementAndGet();
            if (n == 1 || n % 100 == 0) {
                log.warn("DNS queue full, dropped " + n + " queries (traffic unaffected)");
            }
            return false;
        }
        return true;
    }

    // ---------------------------------------------------------------- tx

    private void txLoop() {
        while (running) {
            byte[] raw;
            try {
                raw = inbound.poll(200, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
            if (raw == null) {
                continue;
            }
            try {
                handleQuery(raw);
            } catch (Throwable t) {
                // One malformed query must never take the stage down.
                if (packetLogging) {
                    Log.v(TAG, "query handling failed: " + t.getMessage());
                }
            }
        }
    }

    private void handleQuery(byte[] raw) {
        IpPacket ip;
        try {
            ip = (IpPacket) IpSelector.newPacket(raw, 0, raw.length);
        } catch (Exception e) {
            // An unparseable packet is passed through, never silently dropped.
            passThrough(raw);
            return;
        }
        if (!(ip.getPayload() instanceof UdpPacket)) {
            passThrough(raw);
            return;
        }
        UdpPacket udp = (UdpPacket) ip.getPayload();
        byte[] dnsPayload = udp.getPayload() == null ? null : udp.getPayload().getRawData();
        if (dnsPayload == null || dnsPayload.length < 2) {
            passThrough(raw);
            return;
        }

        if (shouldBypass(dnsPayload)) {
            passThrough(raw);
            return;
        }

        int appTransactionId = ((dnsPayload[0] & 0xFF) << 8) | (dnsPayload[1] & 0xFF);
        String srcAddr = ip.getHeader().getSrcAddr().getHostAddress();
        int srcPort = udp.getHeader().getSrcPort().valueAsInt();
        QueryKey key = new QueryKey(appTransactionId, srcAddr, srcPort);

        if (pending.size() >= MAX_PENDING) {
            Log.w(TAG, "DNS in-flight limit reached, dropping query from " + key);
            return;
        }
        Integer upstreamId = allocateUpstreamId();
        if (upstreamId == null) {
            Log.w(TAG, "no free upstream DNS transaction ID, dropping query from " + key);
            return;
        }

        // Copy before rewriting: the pending entry keeps the original, and pcap4j raw data is shared.
        byte[] upstream = dnsPayload.clone();
        writeTransactionId(upstream, upstreamId);

        PendingQuery q = new PendingQuery(ip, appTransactionId, key, questionName(dnsPayload));
        pending.put(upstreamId, q);

        DatagramSocket s = socket;
        if (s == null || s.isClosed()) {
            pending.remove(upstreamId);
            return;
        }
        try {
            s.send(new DatagramPacket(upstream, upstream.length, resolver));
        } catch (IOException e) {
            pending.remove(upstreamId);
            // No reconnect: a send failure is not evidence the socket needs rebuilding, and doing
            // it under a live receiver kills DNS permanently.
            Log.w(TAG, "failed to send DNS query: " + e.getMessage());
            return;
        }
        if (packetLogging) {
            Log.v(TAG, "query " + key + " upstreamId=" + upstreamId + " name=" + q.name);
        }
        ScheduledExecutorService t = timeouts;
        if (t != null && !t.isShutdown()) {
            final int id = upstreamId;
            try {
                t.schedule(() -> expire(id), QUERY_TIMEOUT_MS, TimeUnit.MILLISECONDS);
            } catch (Exception ignored) {
            }
        }
    }

    /** Re-arms the one-shot bypass on engine attach, when a connectivity test is about to run. */
    @Override
    public void onEngineAttached() {
        armBypass();
    }

    void armBypass() {
        windscribeBypassAvailable = true;
    }

    /** Exactly one windscribe.com lookup is allowed to skip ctrld, per connect. */
    private boolean shouldBypass(byte[] dnsPayload) {
        if (!windscribeBypassAvailable) {
            return false;
        }
        String name = questionName(dnsPayload);
        // Label boundary: a bare endsWith also matches evilwindscribe.com.
        if (name != null && (name.equals("windscribe.com") || name.endsWith(".windscribe.com"))) {
            windscribeBypassAvailable = false;
            return true;
        }
        return false;
    }

    private void passThrough(byte[] raw) {
        PacketMux.PacketSink sink = towardEngine;
        if (sink != null) {
            sink.write(raw, 0, raw.length);
        }
    }

    // ---------------------------------------------------------------- rx

    private void rxLoop() {
        byte[] buf = new byte[RESPONSE_BUFFER_BYTES];
        while (running) {
            DatagramSocket s = socket;
            if (s == null || s.isClosed()) {
                running = false;
                return;
            }
            DatagramPacket dp = new DatagramPacket(buf, buf.length);
            try {
                s.receive(dp);
            } catch (SocketTimeoutException e) {
                continue;
            } catch (IOException e) {
                if (running) {
                    // offer() claims queries while this holds, and this is the only reader.
                    running = false;
                    Log.w(TAG, "DNS receive failed: " + e.getMessage());
                    failureListener.onDnsStageFailure("receive failed: " + e.getMessage());
                }
                return;
            }
            try {
                deliver(dp.getData(), dp.getOffset(), dp.getLength());
            } catch (Throwable t) {
                if (packetLogging) {
                    Log.v(TAG, "response handling failed: " + t.getMessage());
                }
            }
        }
    }

    private void deliver(byte[] data, int offset, int length) {
        if (length < 2) {
            return;
        }
        // Own copy: the receive buffer is reused on the next iteration.
        byte[] response = new byte[length];
        System.arraycopy(data, offset, response, 0, length);

        int upstreamId = ((response[0] & 0xFF) << 8) | (response[1] & 0xFF);
        PendingQuery q = pending.remove(upstreamId);
        if (q == null) {
            if (packetLogging) {
                Log.v(TAG, "unmatched DNS response, upstreamId=" + upstreamId);
            }
            return;
        }
        if (!questionMatches(response, q)) {
            // Ungated, and logcat is readable outside the app: the key identifies it, the name would name it.
            Log.w(TAG, "dropped DNS response for " + q.key + ": question mismatch");
            if (packetLogging) {
                Log.v(TAG, "expected question " + q.name);
            }
            return;
        }

        // Hand back the app's own ID and forward every other byte untouched. No re-encode.
        writeTransactionId(response, q.appTransactionId);

        IpPacket reply;
        try {
            reply = rebuild(q.request, response);
        } catch (Exception e) {
            Log.w(TAG, "failed to rebuild DNS reply for " + q.key + ": " + e.getMessage());
            return;
        }
        PacketMux.PacketSink sink = towardApps;
        if (sink == null) {
            return;
        }
        byte[] out = reply.getRawData();
        sink.write(out, 0, out.length);
        answered.incrementAndGet();
        if (packetLogging) {
            Log.v(TAG, "answered " + q.key + " (" + q.name + ") in "
                    + (System.currentTimeMillis() - q.startedAt) + "ms");
        }
    }

    private void expire(int upstreamId) {
        PendingQuery q = pending.remove(upstreamId);
        if (q != null) {
            timedOut.incrementAndGet();
            if (packetLogging) {
                Log.v(TAG, "timeout " + q.key + " (" + q.name + ")");
            }
        }
    }

    // ---------------------------------------------------------------- packet building

    /** Swaps addresses and ports and carries the response payload back to the asking app. */
    private static IpPacket rebuild(IpPacket request, byte[] responsePayload) {
        UdpPacket requestUdp = (UdpPacket) request.getPayload();
        UnknownPacket.Builder payload = new UnknownPacket.Builder().rawData(responsePayload);
        UdpPacket.Builder udp = new UdpPacket.Builder(requestUdp)
                .srcPort(UdpPort.getInstance(requestUdp.getHeader().getDstPort().value()))
                .dstPort(UdpPort.getInstance(requestUdp.getHeader().getSrcPort().value()))
                .srcAddr(request.getHeader().getDstAddr())
                .dstAddr(request.getHeader().getSrcAddr())
                .correctChecksumAtBuild(true)
                .correctLengthAtBuild(true)
                .payloadBuilder(payload);
        if (request instanceof IpV4Packet) {
            return new IpV4Packet.Builder((IpV4Packet) request)
                    .srcAddr((Inet4Address) request.getHeader().getDstAddr())
                    .dstAddr((Inet4Address) request.getHeader().getSrcAddr())
                    .correctChecksumAtBuild(true)
                    .correctLengthAtBuild(true)
                    .payloadBuilder(udp)
                    .build();
        }
        return new IpV6Packet.Builder((IpV6Packet) request)
                .srcAddr((Inet6Address) request.getHeader().getDstAddr())
                .dstAddr((Inet6Address) request.getHeader().getSrcAddr())
                .correctLengthAtBuild(true)
                .payloadBuilder(udp)
                .build();
    }

    @Nullable
    private static String questionName(byte[] dnsPayload) {
        try {
            DnsMessage m = new DnsMessage(dnsPayload);
            if (m.questions != null && !m.questions.isEmpty()) {
                return m.questions.get(0).name.toString();
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    /**
     * True when the response answers the name we asked. The upstream ID already fixes which app the
     * answer belongs to; this additionally rejects a resolver answering a different name. When
     * neither side yields a parseable question there is nothing to compare, so the ID match stands.
     */
    private static boolean questionMatches(byte[] response, PendingQuery q) {
        String name = questionName(response);
        if (name == null) {
            return q.name == null;
        }
        return name.equalsIgnoreCase(q.name);
    }

    private static void writeTransactionId(byte[] dns, int id) {
        if (dns != null && dns.length >= 2) {
            dns[0] = (byte) ((id >> 8) & 0xFF);
            dns[1] = (byte) (id & 0xFF);
        }
    }

    /**
     * Reserves a 16-bit ID that is not in flight, or null if the whole space is in use. Only the tx
     * thread allocates, and getAndIncrement hands out distinct candidates regardless.
     */
    @Nullable
    private Integer allocateUpstreamId() {
        for (int attempt = 0; attempt < 0x10000; attempt++) {
            int candidate = nextUpstreamId.getAndIncrement() & 0xFFFF;
            if (!pending.containsKey(candidate)) {
                return candidate;
            }
        }
        return null;
    }

    // ---------------------------------------------------------------- socket

    @Nullable
    private DatagramSocket openSocket() {
        long backoff = 250;
        for (int attempt = 1; attempt <= CONNECT_ATTEMPTS; attempt++) {
            try {
                DatagramSocket s = new DatagramSocket();
                s.setSoTimeout(RECEIVE_TIMEOUT_MS);
                // Connect so the kernel filters replies to the resolver, and so send() needs no
                // address. Loopback, so no protect() is required.
                s.connect(resolver);
                return s;
            } catch (Exception e) {
                log.warn("ctrld connect attempt " + attempt + " failed: " + e.getMessage());
                try {
                    Thread.sleep(backoff);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return null;
                }
                backoff *= 2;
            }
        }
        return null;
    }

    private void closeSocket() {
        DatagramSocket s = socket;
        socket = null;
        if (s != null && !s.isClosed()) {
            s.close();
        }
    }

    // ---------------------------------------------------------------- helpers

    private Thread spawn(String name, Runnable body) {
        Thread t = new Thread(() -> {
            try {
                body.run();
            } catch (Throwable th) {
                log.error(name + " died", th);
                if (running) {
                    running = false;
                    failureListener.onDnsStageFailure(name + " died: " + th.getMessage());
                }
            }
        }, name);
        t.setDaemon(true);
        t.start();
        return t;
    }

    private static void interruptQuietly(@Nullable Thread t) {
        if (t != null) {
            t.interrupt();
        }
    }

    private static void joinQuietly(@Nullable Thread t) {
        if (t == null) {
            return;
        }
        try {
            t.join(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static final class PendingQuery {
        final IpPacket request;
        final int appTransactionId;
        final QueryKey key;
        @Nullable final String name;
        final long startedAt = System.currentTimeMillis();

        PendingQuery(IpPacket request, int appTransactionId, QueryKey key, @Nullable String name) {
            this.request = request;
            this.appTransactionId = appTransactionId;
            this.key = key;
            this.name = name;
        }
    }

    /** Identifies the asking app. Used for logging and diagnostics, never for response matching. */
    private static final class QueryKey {
        final int transactionId;
        final String srcAddr;
        final int srcPort;

        QueryKey(int transactionId, String srcAddr, int srcPort) {
            this.transactionId = transactionId;
            this.srcAddr = srcAddr;
            this.srcPort = srcPort;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof QueryKey)) {
                return false;
            }
            QueryKey that = (QueryKey) o;
            return transactionId == that.transactionId
                    && srcPort == that.srcPort
                    && Objects.equals(srcAddr, that.srcAddr);
        }

        @Override
        public int hashCode() {
            return Objects.hash(transactionId, srcAddr, srcPort);
        }

        @Override
        public String toString() {
            return "[txId=" + transactionId + ", src=" + srcAddr + ":" + srcPort + "]";
        }
    }
}
