package com.windscribe.vpn.wsnet

import com.wsnet.lib.WSNet
import com.wsnet.lib.WSNetBridgeAPI
import com.wsnet.lib.WSNetPingManager
import com.wsnet.lib.WSNetServerAPI
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Thread-safe wrapper around WSNet that manages initialization state and provides safe access.
 *
 * Usage:
 * - Call initialize() to set up WSNet (can be called from background thread)
 * - Check isReady. Value or isInitialized() before accessing WSNet
 * - Use safe accessors (safePingManager, safeBridgeAPI, etc.) which return null if not ready
 * - All methods are thread-safe
 */
class WSNetWrapper {

    private val logger = LoggerFactory.getLogger("wsnet-wrapper")
    private val initialized = AtomicBoolean(false)
    private val _isReady = MutableStateFlow(false)

    @Volatile
    private var wsNetInstance: WSNet? = null

    /**
     * Observable state indicating if WSNet is fully initialized and ready to use.
     * Safe to collect from any thread.
     */
    val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

    /**
     * Initialize WSNet. This is a blocking call that should be called from a background thread.
     * Returns true if successful, false otherwise.
     */
    fun initialize(
        platformName: String,
        appVersion: String,
        deviceUuid: String,
        openVpnVersion: String,
        protocolStatus: String,
        isDev: Boolean,
        languageCode: String,
        persistentSettings: String?,
        ignoreTestDomains: Boolean,
        amneziaWgVersion: String
    ): Boolean {
        if (initialized.get()) {
            logger.warn("WSNet already initialized")
            return true
        }

        return try {
            logger.debug("Initializing WSNet...")
            WSNet.initialize(
                platformName,
                platformName,
                appVersion,
                deviceUuid,
                openVpnVersion,
                protocolStatus,
                isDev,
                languageCode,
                persistentSettings,
                { log -> logWsNetMessage(log) },
                ignoreTestDomains,
                amneziaWgVersion
            )
            wsNetInstance = WSNet.instance()
            initialized.set(true)
            updateReadyState()
            logger.debug("WSNet initialized successfully")
            true
        } catch (e: Exception) {
            logger.error("Failed to initialize WSNet: ${e.message}", e)
            false
        }
    }

    /**
     * Configure WSNet advanced parameters after initialization.
     * Should be called from Main thread to ensure proper JNI environment.
     */
    fun configureAdvancedParameters(
        countryOverride: String?,
        isProtocolTweaksEnabled: Boolean
    ) {
        withWSNet { wsNet ->
            try {
                wsNet.advancedParameters()?.let { params ->
                    countryOverride?.let { override ->
                        params.setCountryOverrideValue(override)
                    }
                    params.isAPIExtraTLSPadding = isProtocolTweaksEnabled
                }
            } catch (e: Exception) {
                logger.error("Failed to configure advanced parameters: ${e.message}", e)
            }
        }
    }

    /**
     * Check if WSNet is initialized and ready to use.
     * Thread-safe, non-blocking.
     */
    fun isInitialized(): Boolean {
        return initialized.get() && WSNet.isValid()
    }

    /**
     * Get the raw WSNet instance. Only use if you've checked isInitialized() first.
     * Prefer using the safe accessors instead.
     */
    fun getInstance(): WSNet? {
        return if (isInitialized()) wsNetInstance else null
    }

    /**
     * Safely get the ping manager, returns null if WSNet is not ready.
     */
    fun safePingManager(): WSNetPingManager? {
        val wsNet = wsNetInstance ?: return null
        if (!isInitialized()) return null
        return try {
            wsNet.pingManager()
        } catch (e: Exception) {
            logger.error("Failed to get pingManager: ${e.message}")
            null
        }
    }

    /**
     * Get the bridge API, blocking until WSNet is initialized if needed.
     * Should only be called through Dagger Lazy injection.
     */
    fun getBridgeAPI(): WSNetBridgeAPI {
        // Block until initialized (for Dagger providers)
        while (!isInitialized()) {
            Thread.sleep(50)
        }
        val wsNet = wsNetInstance ?: throw IllegalStateException("WSNet not initialized")
        return try {
            wsNet.bridgeAPI()
        } catch (e: Exception) {
            logger.error("Failed to get bridgeAPI: ${e.message}")
            throw e
        }
    }

    /**
     * Safely get the bridge API, returns null if WSNet is not ready.
     */
    fun safeBridgeAPI(): WSNetBridgeAPI? {
        val wsNet = wsNetInstance ?: return null
        if (!isInitialized()) return null
        return try {
            wsNet.bridgeAPI()
        } catch (e: Exception) {
            logger.error("Failed to get bridgeAPI: ${e.message}")
            null
        }
    }

    /**
     * Get the server API, blocking until WSNet is initialized if needed.
     * Should only be called through Dagger Lazy injection.
     */
    fun getServerAPI(): WSNetServerAPI {
        // Block until initialized (for Dagger providers)
        while (!isInitialized()) {
            Thread.sleep(50)
        }
        val wsNet = wsNetInstance ?: throw IllegalStateException("WSNet not initialized")
        return try {
            wsNet.serverAPI()
        } catch (e: Exception) {
            logger.error("Failed to get serverAPI: ${e.message}")
            throw e
        }
    }

    /**
     * Execute a block with WSNet only if it's ready.
     * Returns the result of the block, or null if WSNet is not ready.
     */
    fun <T> withWSNet(block: (WSNet) -> T): T? {
        val wsNet = wsNetInstance ?: return null
        if (!isInitialized()) return null
        return try {
            block(wsNet)
        } catch (e: Exception) {
            logger.error("WSNet operation failed: ${e.message}")
            null
        }
    }

    private fun updateReadyState() {
        val ready = initialized.get() && WSNet.isValid()
        _isReady.value = ready
        if (ready) {
            logger.debug("WSNet is ready")
        }
    }

    /**
     * Parses wsnet nested JSON log and extracts the actual message.
     * Input: {"tm":"...","lvl":"debug","mod":"wsnet","msg":"{"tm": "...", "lvl": "info", "mod": "wsnet", "msg": "actual message"}"}
     * Output: actual message
     */
    private fun logWsNetMessage(log: String) {
        try {
            if (!log.contains("6464/latency")) {
                val outerMsg = log.substringAfter("\"msg\":\"").substringBeforeLast("\"}")
                val unescaped = outerMsg.replace("\\\"", "\"")
                val actualMsg = unescaped.substringAfter("\"msg\": \"").substringBeforeLast("\"")
                if (actualMsg.isNotEmpty()) {
                    logger.debug(actualMsg)
                }
            }
        } catch (_: Exception) { }
    }
}
