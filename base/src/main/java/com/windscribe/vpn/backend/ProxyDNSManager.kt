package com.windscribe.vpn.backend

import com.windscribe.common.DNSDetails
import com.windscribe.common.DnsType
import com.windscribe.vpn.Windscribe.Companion.appContext
import com.windscribe.vpn.apppreference.PreferencesHelper
import com.windscribe.vpn.apppreference.PreferencesKeyConstants
import com.windscribe.vpn.exceptions.WindScribeException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileOutputStream
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.SocketAddress
import java.security.SecureRandom
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

class ProxyDNSManager(
    val scope: CoroutineScope,
    val preferenceHelper: PreferencesHelper,
) {
    companion object {
        const val CONFIG_FILE = "config.toml"

        /**
         * Range the ctrld listener port is drawn from. Bounded below 32768 to stay clear of the
         * Linux ephemeral range (ip_local_port_range, 32768-60999 by default), so the kernel will
         * not hand the same port to an unrelated socket while ctrld is using it.
         */
        private const val PORT_RANGE_START = 10000
        private const val PORT_RANGE_END = 32767
        private const val PORT_SELECTION_ATTEMPTS = 20

        private const val READY_POLL_MS = 100L

        /**
         * 10s. Generous because ctrld only has to bind a local socket here - no upstream contact is
         * needed - but failure now aborts the connection, so a slow cold start on a weak device
         * must not be mistaken for a hijacked port.
         */
        private const val READY_MAX_ATTEMPTS = 100
    }

    /** Lazy so libwg-go loads on the connect coroutine rather than when the graph is built. */
    private val cdLib by lazy { CdLib() }
    private var controlDJob: Job? = null
    var dnsDetails: DNSDetails? = null
    var invalidConfig = false
    private var isRunning = AtomicBoolean(false)
    private val logger = LoggerFactory.getLogger("vpn")
    private val activePort = AtomicInteger(5355)
    private val portRandom = SecureRandom()

    /** Returns the port ctrld is currently configured to listen on. */
    fun getListenPort(): Int = activePort.get()

    private fun updateControlDConfig() {
        val configFile = File(appContext.filesDir, CONFIG_FILE)
        if (dnsDetails?.type == DnsType.Proxy) {
            if (!configFile.exists() || invalidConfig) {
                configFile.createNewFile()
            }
            val address = dnsDetails?.address ?: ""
            val upStreamInfo =
                "[upstream.0]\n" +
                    "bootstrap_ip = \"${dnsDetails?.ip ?: ""}\"\n" +
                    "endpoint = \"$address\"\n" +
                    "name = \"Custom DNS\"\n" +
                    "timeout = 5000\n" +
                    "type = \"${dnsDetails?.getTypeValue}\"\n" +
                    "ip_stack = \"v4\""
            val staticConfig =
                appContext.assets
                    .open("config.toml")
                    .bufferedReader()
                    .readText()
            val listenPort =
                findAvailablePort()
                    ?: throw WindScribeException("Unable to find port to start ControlD cli.")
            activePort.set(listenPort.toInt())
            val listenerInfo = staticConfig.replace("5355", listenPort)
            val configData = "$listenerInfo\n$upStreamInfo".encodeToByteArray()
            logger.debug("Configuring controlD on port $listenPort.")
            FileOutputStream(configFile).use {
                it.write(configData)
            }
        }
        invalidConfig = false
    }

    /**
     * Picks a free port for ctrld's listener, which binds UDP (and TCP) on 127.0.0.1 per
     * config.toml.
     *
     * Random rather than scanned upwards from a fixed port. Whichever process holds this port
     * receives every app's DNS while the tunnel is up, so a predictable one could be bound at boot
     * and waited on; a large range forces a guess and then the race below.
     *
     * Both protocols are probed on loopback specifically, since a TCP-only probe on the wildcard
     * address reports a port free whose UDP side is held. SO_REUSEADDR is disabled so the kernel
     * cannot grant a bind to a port already in use.
     *
     * A gap remains between this probe and ctrld's own bind, which needs the sockets released.
     * Closing it would need ctrld to accept an already-bound socket.
     */
    private fun findAvailablePort(): String? {
        val loopback = InetAddress.getByName("127.0.0.1")
        repeat(PORT_SELECTION_ATTEMPTS) {
            val port = PORT_RANGE_START + portRandom.nextInt(PORT_RANGE_END - PORT_RANGE_START + 1)
            try {
                DatagramSocket(null as SocketAddress?).use { udp ->
                    udp.reuseAddress = false
                    udp.bind(InetSocketAddress(loopback, port))
                    ServerSocket().use { tcp ->
                        tcp.reuseAddress = false
                        tcp.bind(InetSocketAddress(loopback, port))
                        return "$port"
                    }
                }
            } catch (e: Exception) {
                // Port is taken or unusable; draw another.
            }
        }
        return null
    }

    private fun shouldRunControlD(): Boolean =
        dnsDetails?.type == DnsType.Proxy &&
            preferenceHelper.dnsMode == PreferencesKeyConstants.DNS_MODE_CUSTOM &&
            preferenceHelper.dnsAddress != null

    suspend fun startControlDIfRequired() {
        if (shouldRunControlD() && isRunning.get().not()) {
            startControlD()
        } else if (!shouldRunControlD() && isRunning.get()) {
            stopControlD()
        }
    }

    private suspend fun startControlD() {
        updateControlDConfig()
        val logPath = ""
        val homeDir = appContext.filesDir.absolutePath
        if (controlDJob?.isActive == true) {
            logger.debug("Previous ControlD job is still running. Waiting for it to finish.")
            controlDJob?.join()
        }
        controlDJob =
            scope.launch {
                isRunning.set(true)
                // we are providing config file in home dir instead of UID
                // Pass device info directly to avoid JNI callbacks from Go goroutines
                cdLib.startCd(
                    "",
                    homeDir,
                    "doh",
                    logPath,
                    cdLib.getHostName(),
                    cdLib.getLanIP(),
                    cdLib.getMacAddress(),
                )
                logger.debug("ControlD stopped.")
                isRunning.set(false)
            }
        controlDJob?.start()
        waitForControlDReady()
    }

    /**
     * Waits until ctrld has taken its listen port, and fails the connection if it never does.
     *
     * A bind test, not a DNS query: while the VPN is still connecting ctrld cannot reach its
     * upstream resolver, so a real query would time out even though ctrld is correctly bound. A bind
     * needs no DNS traffic and no working tunnel — refused means the port is held.
     *
     * This establishes only that the port is occupied, not that ctrld is what occupies it. A local
     * app that won the race reads the same. Guards against nothing listening, not against a
     * deliberate local hijacker. See [findAvailablePort].
     */
    private suspend fun waitForControlDReady() {
        val port = activePort.get()
        for (i in 1..READY_MAX_ATTEMPTS) {
            if (isLoopbackUdpPortTaken(port) && cdLib.isCdRunning()) {
                logger.debug("ControlD is listening on port $port after ${i * READY_POLL_MS}ms.")
                return
            }
            delay(READY_POLL_MS)
        }
        // Fail closed. Nothing is confirmed on the port, so forwarding every app's DNS there would
        // either blackhole it or hand it to whatever else holds it. Both are unacceptable in a mode
        // the user turned on for DNS privacy, so refuse to connect and say why. ctrld is stopped
        // first so the next attempt starts from a clean state rather than skipping startup because
        // isRunning is still set.
        logger.error(
            "ControlD is not listening on port $port after " +
                "${READY_MAX_ATTEMPTS * READY_POLL_MS}ms. Failing the connection.",
        )
        stopControlD()
        throw WindScribeException("DNS proxy (ControlD) failed to start.")
    }

    /**
     * True if [port] cannot be bound on loopback UDP, i.e. some socket already holds it.
     *
     * SO_REUSEADDR is explicitly disabled before binding. Left enabled, the kernel may permit a
     * second bind to a UDP port that is already in use, which would report every port as free and
     * make this test meaningless.
     */
    private fun isLoopbackUdpPortTaken(port: Int): Boolean =
        try {
            DatagramSocket(null as SocketAddress?).use { socket ->
                socket.reuseAddress = false
                socket.bind(InetSocketAddress(InetAddress.getByName("127.0.0.1"), port))
            }
            false
        } catch (e: Exception) {
            true
        }

    suspend fun stopControlD() {
        if (isRunning.get()) {
            cdLib.stopCd(true, 0)
            controlDJob?.join()
            invalidConfig = false
        }
    }
}
