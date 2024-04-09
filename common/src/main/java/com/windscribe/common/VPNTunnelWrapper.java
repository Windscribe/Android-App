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

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.FileChannel;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

public class VPNTunnelWrapper {
    private final FileChannel vpnInputChannel;
    private final FileChannel vpnOutputChannel;
    private final FileChannel socketInputChannel;
    private final FileChannel socketOutputChannel;
    private final ExecutorService threadPool;
    private final ByteBuffer vpnBuffer = ByteBuffer.allocateDirect(6400);
    private final ByteBuffer socketBuffer = ByteBuffer.allocateDirect(6400);
    private final ParcelFileDescriptor parcelFileDescriptor;
    private final BlockingQueue<Packet> dnsPackets = new LinkedBlockingQueue<>(300);
    InetSocketAddress controlDAddress = new InetSocketAddress("127.0.0.1", 5354);
    private ParcelFileDescriptor socketFileDescriptor;
    private ParcelFileDescriptor detachFileDescriptor;
    private DatagramChannel controlDChannel;

    public VPNTunnelWrapper(ParcelFileDescriptor parcelFileDescriptor, VpnService vpnService) throws ErrnoException, IOException {
        this.parcelFileDescriptor = parcelFileDescriptor;
        vpnInputChannel = new FileInputStream(parcelFileDescriptor.getFileDescriptor()).getChannel();
        vpnOutputChannel = new FileOutputStream(parcelFileDescriptor.getFileDescriptor()).getChannel();
        buildSocketPair();
        vpnService.protect(socketFileDescriptor.getFd());
        socketInputChannel = new FileInputStream(socketFileDescriptor.getFileDescriptor()).getChannel();
        socketOutputChannel = new FileOutputStream(socketFileDescriptor.getFileDescriptor()).getChannel();
        threadPool = Executors.newFixedThreadPool(3);
    }

    void log(String message) {
        Log.i("VPNTunnelWrapper", message);
    }

    public ParcelFileDescriptor getParcelDescriptor() {
        return detachFileDescriptor;
    }

    public void start() {
        threadPool.submit(this::forwardSocketToVpn);
        threadPool.submit(this::forwardVpnToSocket);
        threadPool.submit(this::forwardToControlD);
    }

    public void stop() {
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
        } catch (IOException e) {
            log(e.getMessage());
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
            int MAX_BATCH_SIZE = 1600;
            while (true) {
                int bytesRead = socketInputChannel.read(socketBuffer);
                if (bytesRead > 0) {
                    socketBuffer.flip();
                    while (bytesRead > 0 && socketBuffer.remaining() > 0 && socketBuffer.remaining() >= MAX_BATCH_SIZE) {
                        bytesRead = socketInputChannel.read(socketBuffer);
                    }
                    vpnOutputChannel.write(socketBuffer);
                    socketBuffer.clear();
                }
            }
        } catch (IOException e) {
            log("Forward socket to vpn: " + e.getMessage());
        }
    }

    private void forwardVpnToSocket() {
        try {
            while (true) {
                int bytesRead = vpnInputChannel.read(vpnBuffer);
                if (bytesRead > 0) {
                    Pair<Packet, Boolean> packet = ipToDnsPacket(vpnBuffer);
                    if (packet.second) {
                        dnsPackets.put(packet.first);
                    } else {
                        socketOutputChannel.write(ByteBuffer.wrap(packet.first.getRawData()));
                    }
                }
                vpnBuffer.compact();
            }
        } catch (IOException | InterruptedException e) {
            log("Forward vpn to socket: " + e.getMessage());
        }
    }

    private void forwardToControlD(){
        connectToControlD();
        int BUFFER_SIZE = 1024;
        ByteBuffer controlDBuffer = ByteBuffer.allocateDirect(BUFFER_SIZE);
        while (true) {
            try {
                if (dnsPackets.remainingCapacity() < 100) {
                    connectToControlD();
                }
                Packet packet = dnsPackets.take();
                if (packet != null) {
                    writeDNSRequestToControlD(packet);
                    readDNSResponseFromControlD(controlDBuffer);
                    writeDNSResponseToVPN(packet, controlDBuffer);
                }
            } catch (InterruptedException e) {
               log(e.getMessage());
            }
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
        byte[] ipPacketData = new byte[vpnBuffer.limit() - vpnBuffer.position()];
        vpnBuffer.get(ipPacketData, vpnBuffer.position(), vpnBuffer.limit());
        Packet ipPacket;
        try {
            ipPacket = IpSelector.newPacket(ipPacketData, 0, ipPacketData.length);
            if ((ipPacket instanceof IpV4Packet || ipPacket instanceof IpV6Packet) && ipPacket.getPayload() instanceof UdpPacket requestUdpPacket) {
                if (requestUdpPacket.getHeader().getDstPort().value() == 53) {
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

    public void readDNSResponseFromControlD(ByteBuffer controlDBuffer) {
        controlDBuffer.clear();
        try {
            controlDChannel.read(controlDBuffer);
        } catch (IOException e) {
            log(e.getMessage());
        }
    }

    public void writeDNSRequestToControlD(Packet ipPacket) {
        UdpPacket requestUdpPacket = (UdpPacket) ipPacket.getPayload();
        ByteBuffer payLoadSendToProxy = ByteBuffer.wrap(requestUdpPacket.getPayload().getRawData());
        try {
            controlDChannel.write(payLoadSendToProxy);
        } catch (IOException e) {
            log(e.getMessage());
            connectToControlD();
        }
    }

    public void writeDNSResponseToVPN(Packet requestPacket, ByteBuffer controlDBuffer) {
        controlDBuffer.flip();
        try {
            if (controlDBuffer.limit() > 0) {
                byte[] buf = new byte[controlDBuffer.limit() - controlDBuffer.position()];
                controlDBuffer.get(buf, controlDBuffer.position(), controlDBuffer.limit());
                IpPacket rebuiltIpPacket = dnsToIpPacket((IpPacket) requestPacket, new DnsMessage(buf).toArray());
                int ignored = vpnOutputChannel.write(ByteBuffer.wrap(rebuiltIpPacket.getRawData()));
            }
        } catch (IOException e) {
            log(e.getMessage());
        }
    }
}