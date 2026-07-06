package com.windscribe.common;

import static android.system.OsConstants.AF_UNIX;
import static android.system.OsConstants.SOCK_SEQPACKET;

import android.net.VpnService;
import android.os.ParcelFileDescriptor;
import android.system.ErrnoException;
import android.system.Os;
import android.util.Log;
import android.util.Pair;

import org.minidns.dnsmessage.DnsMessage;
import org.pcap4j.packet.IllegalRawDataException;
import org.pcap4j.packet.IpPacket;
import org.pcap4j.packet.IpSelector;
import org.pcap4j.packet.IpV4Packet;
import org.pcap4j.packet.IpV6Packet;
import org.pcap4j.packet.Packet;
import org.pcap4j.packet.UdpPacket;
import org.pcap4j.packet.UnknownPacket;
import org.pcap4j.packet.namednumber.UdpPort;
import org.pcap4j.packet.TcpPacket;
import org.pcap4j.packet.IcmpV4CommonPacket;
import org.pcap4j.packet.IcmpV6CommonPacket;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.FileChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class VPNTunnelWrapper {
    private static final String TAG = "VPNTunnelWrapper";

    private final boolean enablePacketLogging;

    private final FileChannel vpnInputChannel;
    private final FileChannel vpnOutputChannel;
    private final FileChannel socketInputChannel;
    private final FileChannel socketOutputChannel;
    private final ExecutorService threadPool;
    private final ByteBuffer vpnBuffer = ByteBuffer.allocateDirect(65536);
    private final ByteBuffer socketBuffer = ByteBuffer.allocateDirect(65536);
    private final ParcelFileDescriptor parcelFileDescriptor;
    private final BlockingQueue<Packet> dnsPackets = new LinkedBlockingQueue<>(300);
    private static final int DNS_READ_TIMEOUT_MS = 5000;
    private InetSocketAddress controlDAddress;
    private ParcelFileDescriptor socketFileDescriptor;
    private ParcelFileDescriptor detachFileDescriptor;
    private DatagramChannel controlDChannel;
    private Boolean byPassControlD = true;

    private static final long MAX_LOG_SIZE = 300 * 1024;
    private static final long TRUNCATE_TO_SIZE = 150 * 1024;
    private PrintWriter logWriter;
    private File logFile;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US);
    private final Object logLock = new Object();

    public VPNTunnelWrapper(ParcelFileDescriptor parcelFileDescriptor, VpnService vpnService) throws ErrnoException, IOException {
        this(parcelFileDescriptor, vpnService, 5355, false);
    }

    public VPNTunnelWrapper(ParcelFileDescriptor parcelFileDescriptor, VpnService vpnService, int controlDPort) throws ErrnoException, IOException {
        this(parcelFileDescriptor, vpnService, controlDPort, false);
    }

    public VPNTunnelWrapper(ParcelFileDescriptor parcelFileDescriptor, VpnService vpnService, int controlDPort, boolean enableLogging) throws ErrnoException, IOException {
        this.enablePacketLogging = enableLogging;
        this.controlDAddress = new InetSocketAddress("127.0.0.1", controlDPort);
        this.parcelFileDescriptor = parcelFileDescriptor;
        vpnInputChannel = new FileInputStream(parcelFileDescriptor.getFileDescriptor()).getChannel();
        vpnOutputChannel = new FileOutputStream(parcelFileDescriptor.getFileDescriptor()).getChannel();
        buildSocketPair();
        vpnService.protect(socketFileDescriptor.getFd());
        socketInputChannel = new FileInputStream(socketFileDescriptor.getFileDescriptor()).getChannel();
        socketOutputChannel = new FileOutputStream(socketFileDescriptor.getFileDescriptor()).getChannel();
        threadPool = Executors.newFixedThreadPool(3);
        initializeLogging(vpnService);
    }

    private void initializeLogging(VpnService vpnService) {
        try {
            logFile = new File(vpnService.getFilesDir(), "vpntunnel.log");

            if (logFile.exists() && logFile.length() > MAX_LOG_SIZE) {
                truncateLogFile();
            }

            logWriter = new PrintWriter(new FileWriter(logFile, true));
            logToFile("=== VPNTunnelWrapper initialized, port: " + controlDAddress.getPort() + " ===");
        } catch (IOException e) {
            Log.e(TAG, "Failed to initialize logging", e);
        }
    }

    private void logToFile(String message) {
        synchronized (logLock) {
            if (logWriter != null && logFile != null) {
                try {
                    // Write the message first
                    String timestamp = dateFormat.format(new Date());
                    logWriter.println("[VPNTunnel] " + timestamp + " " + message);
                    Log.i("tunnel", "[VPNTunnel] " + timestamp + " " + message);
                    logWriter.flush();

                    // Now check file size after flush to get accurate size
                    long currentSize = logFile.length();
                    if (currentSize > MAX_LOG_SIZE) {
                        Log.i(TAG, "Truncating log file: current size = " + currentSize + " bytes (" + (currentSize / 1024) + "KB), max = " + (MAX_LOG_SIZE / 1024) + "KB");
                        logWriter.close();
                        truncateLogFile();
                        logWriter = new PrintWriter(new FileWriter(logFile, true));
                        logWriter.println("[VPNTunnel] === File was " + (currentSize / 1024) + "KB, truncated to " + (logFile.length() / 1024) + "KB ===");
                        logWriter.flush();
                    }
                } catch (IOException e) {
                    Log.e(TAG, "Failed to write to log file", e);
                }
            }
        }
    }

    private void truncateLogFile() {
        try {
            if (logFile != null && logFile.exists()) {
                long fileSize = logFile.length();
                if (fileSize > TRUNCATE_TO_SIZE) {
                    try (java.io.RandomAccessFile raf = new java.io.RandomAccessFile(logFile, "r")) {
                        raf.seek(fileSize - TRUNCATE_TO_SIZE);
                        byte[] buffer = new byte[(int) TRUNCATE_TO_SIZE];
                        raf.readFully(buffer);

                        // Find the first complete line by looking for a newline followed by [VPNTunnel]
                        int startIndex = 0;
                        String bufferStr = new String(buffer, "UTF-8");
                        int searchLimit = Math.min(1000, buffer.length / 2); // Search up to 1KB or half the buffer

                        for (int i = 0; i < searchLimit; i++) {
                            if (buffer[i] == '\n') {
                                // Check if the next line starts with [VPNTunnel] to ensure it's a complete entry
                                if (i + 1 < buffer.length) {
                                    String nextPart = bufferStr.substring(i + 1, Math.min(i + 20, bufferStr.length()));
                                    if (nextPart.startsWith("[VPNTunnel]")) {
                                        startIndex = i + 1;
                                        break;
                                    }
                                }
                            }
                        }

                        try (FileWriter fw = new FileWriter(logFile, false)) {
                            fw.write("[VPNTunnel] ========================================\n");
                            fw.write("[VPNTunnel] === Log truncated at " + dateFormat.format(new Date()) + " ===\n");
                            fw.write("[VPNTunnel] === Keeping last " + (TRUNCATE_TO_SIZE / 1024) + "KB of " + (MAX_LOG_SIZE / 1024) + "KB ===\n");
                            fw.write("[VPNTunnel] ========================================\n");
                            fw.write(new String(buffer, startIndex, buffer.length - startIndex, "UTF-8"));
                        }
                    }
                } else {
                    try (FileWriter fw = new FileWriter(logFile, false)) {
                        fw.write("[VPNTunnel] === Log cleared at " + dateFormat.format(new Date()) + " ===\n");
                    }
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "Failed to truncate log file", e);
        }
    }

    private void logPacket(String message) {
        if (enablePacketLogging) {
            logToFile(message);
        }
    }

    void log(String message) {
        Log.i("VPNTunnelWrapper", message);
    }

    public ParcelFileDescriptor getParcelDescriptor() {
        return detachFileDescriptor;
    }

    public void start() {
        logToFile("Starting VPN tunnel wrapper with 3 threads");
        threadPool.submit(this::forwardSocketToVpn);
        threadPool.submit(this::forwardVpnToSocket);
        threadPool.submit(this::forwardToControlD);
        logToFile("All threads submitted to thread pool");
    }

    public void stop() {
        logToFile("Stopping VPN tunnel wrapper");
        try {
            parcelFileDescriptor.close();
            socketBuffer.clear();
            vpnBuffer.clear();
            vpnInputChannel.close();
            socketInputChannel.close();
            vpnOutputChannel.close();
            vpnInputChannel.close();
            controlDChannel.close();
            threadPool.shutdownNow();

            // Shutdown DNS executor services if they exist
            if (timeoutExecutor != null) {
                timeoutExecutor.shutdownNow();
            }
            if (dnsWorkerPool != null) {
                dnsWorkerPool.shutdownNow();
            }
        } catch (IOException e) {
            log(e.getMessage());
        }

        synchronized (logLock) {
            if (logWriter != null) {
                logToFile("=== VPNTunnelWrapper stopped ===");
                logWriter.close();
                logWriter = null;
            }
        }
    }

    private void buildSocketPair() throws ErrnoException, IOException {
        final FileDescriptor fd0 = new FileDescriptor();
        final FileDescriptor fd1 = new FileDescriptor();
        Os.socketpair(AF_UNIX, SOCK_SEQPACKET, 0, fd0, fd1);
        socketFileDescriptor = ParcelFileDescriptor.dup(fd0);
        detachFileDescriptor = ParcelFileDescriptor.dup(fd1);
    }

    private void forwardSocketToVpn() {
        try {
            logToFile("VPN→Apps thread started");
            int MAX_BATCH_SIZE = 1600;
            long totalBytesForwarded = 0;
            long packetCount = 0;
            long droppedCount = 0;
            while (true) {
                int bytesRead = socketInputChannel.read(socketBuffer);
                if (bytesRead > 0) {
                    packetCount++;

                    socketBuffer.flip();

                    byte[] firstPacketData = new byte[bytesRead];
                    socketBuffer.get(firstPacketData);
                    socketBuffer.rewind();


                    if (enablePacketLogging) {
                        String packetInfo = analyzePacket(firstPacketData, "VPN→Apps");
                        if (packetInfo != null) {
                            logPacket("[VPN→Apps #" + packetCount + "] " + packetInfo + " [" + bytesRead + " bytes]");
                        } else {
                            logPacket("[VPN→Apps #" + packetCount + "] Received " + bytesRead + " bytes from VPN");
                        }
                    }

                    int written = vpnOutputChannel.write(socketBuffer);
                    if (written > 0) {
                        totalBytesForwarded += written;
                    }
                    socketBuffer.clear();
                }
            }
        } catch (IOException e) {
            log("Forward VPN to Apps: " + e.getMessage());
            logToFile("VPN→Apps thread stopped: " + e.getMessage());
        }
    }

    private void forwardVpnToSocket() {
        try {
            logToFile("Apps→VPN thread started (intercepting DNS from apps)");
            long totalPackets = 0;

            while (true) {
                int bytesRead = vpnInputChannel.read(vpnBuffer);
                if (bytesRead > 0) {
                    totalPackets++;

                    try {
                        Pair<Packet, Boolean> packet = ipToDnsPacket(vpnBuffer);
                        if (packet != null) {
                            String packetDetails = enablePacketLogging ? getPacketDetails(packet.first) : null;

                            if (packet.second) {
                                if (enablePacketLogging) {
                                    logPacket("[Apps→ControlD #" + totalPackets + "] " + packetDetails + " (intercepted DNS)");
                                }
                                try {
                                    dnsPackets.put(packet.first);
                                } catch (InterruptedException ie) {
                                    Thread.currentThread().interrupt();
                                    break;
                                }
                            } else {
                                if (enablePacketLogging) {
                                    if (packetDetails != null && packetDetails.contains("→53")) {
                                        logPacket("[WARNING] DNS packet not intercepted: " + packetDetails);
                                    }
                                    logPacket("[Apps→VPN #" + totalPackets + "] " + packetDetails);

                                    if (packetDetails != null && packetDetails.contains("Protocol:Unknown")) {
                                        byte[] rawData = packet.first.getRawData();
                                        logPacket("  └─ Raw packet (first 64 bytes): " + bytesToHex(rawData, 64));
                                    }
                                }
                                int written = socketOutputChannel.write(ByteBuffer.wrap(packet.first.getRawData()));
                            }
                        } else {
                            if (enablePacketLogging) {
                                logPacket("Failed to parse packet #" + totalPackets + " from Apps (null packet)");
                            }
                        }
                    } catch (Exception e) {
                        logToFile("Error processing Apps packet #" + totalPackets + ": " + e.getMessage());
                    }
                }
                vpnBuffer.compact();
            }
        } catch (IOException e) {
            log("Forward Apps to VPN: " + e.getMessage());
            logToFile("Apps→VPN thread stopped: " + e.getMessage());
        }
    }

    // Non-blocking DNS handling
    private static class DnsQuery {
        final Packet packet;
        final long queryId;
        final long startTime;
        final String queryName;

        DnsQuery(Packet packet, long queryId) {
            this.packet = packet;
            this.queryId = queryId;
            this.startTime = System.currentTimeMillis();
            this.queryName = extractQueryName(packet);
        }

        private static String extractQueryName(Packet packet) {
            try {
                if (packet != null && packet.getPayload() instanceof UdpPacket udpPacket) {
                    byte[] dnsData = udpPacket.getPayload().getRawData();
                    DnsMessage message = new DnsMessage(dnsData);
                    if (message.questions != null && !message.questions.isEmpty()) {
                        return message.questions.get(0).name.toString();
                    }
                }
            } catch (Exception ignored) {}
            return "unknown";
        }
    }

    private final Map<Integer, DnsQuery> pendingQueries = new ConcurrentHashMap<>();
    private ScheduledExecutorService timeoutExecutor;
    private ExecutorService dnsWorkerPool;
    private long nextQueryId = 1;

    private void forwardToControlD() {
        connectToControlD();
        timeoutExecutor = Executors.newScheduledThreadPool(1);
        dnsWorkerPool = Executors.newFixedThreadPool(5);

        logToFile("DNS→ControlD thread started, listening on 127.0.0.1:" + controlDAddress.getPort());

        dnsWorkerPool.submit(this::readDnsResponses);

        while (true) {
            try {
                if (dnsPackets.remainingCapacity() < 100) {
                    logToFile("DNS queue low capacity, reconnecting to ControlD");
                    connectToControlD();
                }

                Packet packet = dnsPackets.take();
                if (packet != null) {
                    long queryId = nextQueryId++;
                    DnsQuery query = new DnsQuery(packet, queryId);

                    // Extract transaction ID from DNS packet for mapping
                    int transactionId = extractTransactionId(packet);
                    if (transactionId >= 0) {
                        pendingQueries.put(transactionId, query);

                        if (enablePacketLogging) {
                            String packetDetails = getPacketDetails(packet);
                            logPacket("[DNS Query #" + queryId + " TxID:" + transactionId + "] " + packetDetails);
                            logPacket("  └─ Query: " + query.queryName + " → ControlD");
                        }

                        // Send query without waiting for response
                        writeDNSRequestToControlD(packet);

                        // Schedule timeout check
                        timeoutExecutor.schedule(() -> handleTimeout(transactionId),
                                               DNS_READ_TIMEOUT_MS, TimeUnit.MILLISECONDS);
                    }
                }
            } catch (InterruptedException e) {
                log(e.getMessage());
                logToFile("DNS→ControlD interrupted: " + e.getMessage());
                break;
            }
        }

        // Cleanup
        timeoutExecutor.shutdown();
        dnsWorkerPool.shutdown();
    }

    private void readDnsResponses() {
        ByteBuffer responseBuffer = ByteBuffer.allocateDirect(1024);

        try {
            controlDChannel.configureBlocking(false);
            Selector selector = Selector.open();
            controlDChannel.register(selector, SelectionKey.OP_READ);

            while (!Thread.currentThread().isInterrupted()) {
                // Wait for responses with a short timeout
                if (selector.select(100) > 0) {
                    selector.selectedKeys().clear();

                    responseBuffer.clear();
                    int bytesRead = controlDChannel.read(responseBuffer);

                    if (bytesRead > 0) {
                        responseBuffer.flip();
                        byte[] responseData = new byte[responseBuffer.remaining()];
                        responseBuffer.get(responseData);

                        // Extract transaction ID from response
                        int transactionId = extractTransactionIdFromResponse(responseData);
                        DnsQuery query = pendingQueries.remove(transactionId);

                        if (query != null) {
                            long responseTime = System.currentTimeMillis() - query.startTime;

                            if (enablePacketLogging) {
                                logPacket("  └─ Response for TxID:" + transactionId + " (" + query.queryName +
                                        ") [" + bytesRead + " bytes, " + responseTime + "ms]");
                            }

                            // Send response back to apps
                            writeDNSResponseData(query.packet, responseData);
                        } else {
                            if (enablePacketLogging) {
                                logPacket("  └─ Unexpected response TxID:" + transactionId + " (no matching query)");
                            }
                        }
                    }
                }
            }
        } catch (IOException e) {
            logToFile("DNS response reader error: " + e.getMessage());
        }
    }

    private void handleTimeout(int transactionId) {
        DnsQuery query = pendingQueries.remove(transactionId);
        if (query != null) {
            long elapsed = System.currentTimeMillis() - query.startTime;
            if (enablePacketLogging) {
                logPacket("  └─ TIMEOUT TxID:" + transactionId + " (" + query.queryName +
                        ") after " + elapsed + "ms");
            }
        }
    }

    private int extractTransactionId(Packet packet) {
        try {
            if (packet != null && packet.getPayload() instanceof UdpPacket udpPacket) {
                byte[] dnsData = udpPacket.getPayload().getRawData();
                if (dnsData.length >= 2) {
                    return ((dnsData[0] & 0xFF) << 8) | (dnsData[1] & 0xFF);
                }
            }
        } catch (Exception e) {
            logToFile("Error extracting transaction ID: " + e.getMessage());
        }
        return -1;
    }

    private int extractTransactionIdFromResponse(byte[] responseData) {
        if (responseData != null && responseData.length >= 2) {
            return ((responseData[0] & 0xFF) << 8) | (responseData[1] & 0xFF);
        }
        return -1;
    }

    private void writeDNSResponseData(Packet requestPacket, byte[] responseData) {
        try {
            DnsMessage dnsResponse = new DnsMessage(responseData);
            IpPacket rebuiltIpPacket = dnsToIpPacket((IpPacket) requestPacket, dnsResponse.toArray());

            int written = vpnOutputChannel.write(ByteBuffer.wrap(rebuiltIpPacket.getRawData()));
            if (enablePacketLogging && written > 0) {
                logPacket("  └─ Written response to Apps: " + written + " bytes");
            }
        } catch (IOException e) {
            logToFile("  └─ ERROR: Failed to write DNS response: " + e.getMessage());
        }
    }


    private String analyzePacket(byte[] data, String direction) {
        try {
            Packet packet = IpSelector.newPacket(data, 0, data.length);
            return direction + ": " + getPacketDetails(packet);
        } catch (Exception e) {
            return null;
        }
    }

    private String bytesToHex(byte[] bytes, int limit) {
        StringBuilder sb = new StringBuilder();
        int count = Math.min(bytes.length, limit);
        for (int i = 0; i < count; i++) {
            sb.append(String.format("%02X ", bytes[i]));
            if ((i + 1) % 16 == 0) {
                sb.append("\n  ");
            }
        }
        if (bytes.length > limit) {
            sb.append("... (").append(bytes.length).append(" bytes total)");
        }
        return sb.toString();
    }


    private String getPacketDetails(Packet packet) {
        try {
            StringBuilder details = new StringBuilder();

            if (packet instanceof IpV4Packet ipv4) {
                details.append("IPv4 ");
                details.append(ipv4.getHeader().getSrcAddr().getHostAddress());
                details.append("→");
                details.append(ipv4.getHeader().getDstAddr().getHostAddress());
            } else if (packet instanceof IpV6Packet ipv6) {
                details.append("IPv6 ");
                details.append(ipv6.getHeader().getSrcAddr().getHostAddress());
                details.append("→");
                details.append(ipv6.getHeader().getDstAddr().getHostAddress());
            }

            if (packet.getPayload() instanceof UdpPacket udp) {
                details.append(" UDP:");
                details.append(udp.getHeader().getSrcPort().valueAsInt());
                details.append("→");
                details.append(udp.getHeader().getDstPort().valueAsInt());
                if (udp.getHeader().getDstPort().valueAsInt() == 53) {
                    details.append(" [DNS]");
                }
            } else if (packet.getPayload() instanceof TcpPacket tcp) {
                details.append(" TCP:");
                details.append(tcp.getHeader().getSrcPort().valueAsInt());
                details.append("→");
                details.append(tcp.getHeader().getDstPort().valueAsInt());
            } else if (packet.getPayload() instanceof IcmpV4CommonPacket) {
                details.append(" ICMPv4");
            } else if (packet.getPayload() instanceof IcmpV6CommonPacket) {
                details.append(" ICMPv6");
                if (packet instanceof IpV6Packet ipv6) {
                    String dstAddr = ipv6.getHeader().getDstAddr().getHostAddress();
                    if (dstAddr != null && dstAddr.startsWith("ff02::")) {
                        details.append(" [Multicast]");
                    }
                }
            } else {
                if (packet instanceof IpV6Packet ipv6) {
                    int nextHeader = ipv6.getHeader().getNextHeader().value().intValue();
                    switch (nextHeader) {
                        case 0:
                            details.append(" IPv6-HopByHop");
                            // Check if it's MLDv2 multicast (ff02::16)
                            String dstAddr = ipv6.getHeader().getDstAddr().getHostAddress();
                            if ("ff02::16".equals(dstAddr)) {
                                details.append(" [MLDv2 Multicast]");
                            } else if (dstAddr != null && dstAddr.startsWith("ff02::")) {
                                details.append(" [Multicast]");
                            }
                            break;
                        case 43:
                            details.append(" IPv6-Routing");
                            break;
                        case 44:
                            details.append(" IPv6-Fragment");
                            break;
                        case 60:
                            details.append(" IPv6-DestOptions");
                            break;
                        default:
                            details.append(" Protocol:Unknown");
                            details.append(" (NextHeader:").append(nextHeader).append(")");
                    }
                } else if (packet instanceof IpV4Packet ipv4) {
                    details.append(" Protocol:Unknown");
                    details.append(" (Proto:").append(ipv4.getHeader().getProtocol().valueAsString()).append(")");
                } else {
                    details.append(" Protocol:Unknown");
                }
            }

            return details.toString();
        } catch (Exception e) {
            return "Error parsing packet: " + e.getMessage();
        }
    }

    private void connectToControlD() {
        int MAX_RETRIES = 3;
        int retryDelay = 500;
        for (int i = 0; i < MAX_RETRIES; i++) {
            try {
                if (controlDChannel != null && controlDChannel.isOpen()) {
                    controlDChannel.close();
                }
                controlDChannel = DatagramChannel.open();
                controlDChannel.configureBlocking(true);
                controlDChannel.connect(controlDAddress);
                log("Connected to controlD");
                logToFile("Connected to ControlD at " + controlDAddress);
                return;
            } catch (IOException e) {
                log("Error connecting to proxy (attempt " + (i + 1) + "): " + e.getMessage());
                try {
                    Thread.sleep(retryDelay);
                } catch (InterruptedException ignored) {
                }
                retryDelay *= 2;
            }
        }
        log("Failed to connect to proxy after multiple attempts.");
    }

    public Pair<Packet, Boolean> ipToDnsPacket(ByteBuffer vpnBuffer) {
        vpnBuffer.flip();
        byte[] ipPacketData = new byte[vpnBuffer.remaining()];
        vpnBuffer.get(ipPacketData);
        Packet ipPacket;
        try {
            ipPacket = IpSelector.newPacket(ipPacketData, 0, ipPacketData.length);
            if ((ipPacket instanceof IpV4Packet || ipPacket instanceof IpV6Packet) && ipPacket.getPayload() instanceof UdpPacket requestUdpPacket) {
                if (requestUdpPacket.getHeader().getDstPort().value() == 53) {
                    try {
                        byte[] dnsQueryData = requestUdpPacket.getPayload().getRawData();
                        DnsMessage dnsMessage = new DnsMessage(dnsQueryData);
                        if (dnsMessage.questions != null && !dnsMessage.questions.isEmpty()) {
                            String domain = dnsMessage.questions.get(0).name.toString();
                            if (domain.endsWith("windscribe.com") && byPassControlD) {
                                byPassControlD = false;
                                return new Pair<>(ipPacket, false);
                            }
                        }
                    } catch (Exception ignored) { }
                    return new Pair<>(ipPacket, true);
                }
            }
            return new Pair<>(ipPacket, false);
        } catch (IllegalRawDataException e) {
            log(e.getMessage());
            return null;
        }
    }

    public IpPacket dnsToIpPacket(IpPacket requestPacket, byte[] responsePayload) {
        UdpPacket udpOutPacket = (UdpPacket) requestPacket.getPayload();
        UnknownPacket.Builder payloadBuilder = new UnknownPacket.Builder().rawData(responsePayload);
        UdpPacket.Builder udpBuilder = new UdpPacket.Builder(udpOutPacket).srcPort(UdpPort.getInstance(udpOutPacket.getHeader().getDstPort().value())).dstPort(UdpPort.getInstance(udpOutPacket.getHeader().getSrcPort().value())).srcAddr(requestPacket.getHeader().getDstAddr()).dstAddr(requestPacket.getHeader().getSrcAddr()).correctChecksumAtBuild(true).correctLengthAtBuild(true).payloadBuilder(payloadBuilder);
        if (requestPacket instanceof IpV4Packet) {
            return new IpV4Packet.Builder((IpV4Packet) requestPacket).srcAddr((Inet4Address) requestPacket.getHeader().getDstAddr()).dstAddr((Inet4Address) requestPacket.getHeader().getSrcAddr()).correctChecksumAtBuild(true).correctLengthAtBuild(true).payloadBuilder(udpBuilder).build();
        } else {
            return new IpV6Packet.Builder((IpV6Packet) requestPacket).srcAddr((Inet6Address) requestPacket.getHeader().getDstAddr()).dstAddr((Inet6Address) requestPacket.getHeader().getSrcAddr()).correctLengthAtBuild(true).payloadBuilder(udpBuilder).build();
        }
    }


    public void writeDNSRequestToControlD(Packet ipPacket) {
        UdpPacket requestUdpPacket = (UdpPacket) ipPacket.getPayload();
        ByteBuffer payLoadSendToProxy = ByteBuffer.wrap(requestUdpPacket.getPayload().getRawData());
        try {
            controlDChannel.write(payLoadSendToProxy);
        } catch (IOException e) {
            log(e.getMessage());
            logToFile("  └─ ERROR: Failed to send DNS request: " + e.getMessage());
            connectToControlD();
        }
    }

}