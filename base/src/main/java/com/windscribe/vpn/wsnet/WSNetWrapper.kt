package com.windscribe.vpn.wsnet

import com.wsnet.lib.WSNet
import com.wsnet.lib.WSNetAdvancedParameters
import com.wsnet.lib.WSNetBridgeAPI
import com.wsnet.lib.WSNetEmergencyConnect
import com.wsnet.lib.WSNetServerAPI
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Thread-safe wrapper around WSNet that manages initialization state and provides safe access.
 *
 * Usage:
 * - Call initialize() to set up WSNet (can be called from background thread)
 * - Check isReady. Value or isInitialized() before accessing WSNet
 * - Use safe accessors (safeBridgeAPI, safeAdvancedParameters, etc.) which return null if not ready
 * - All methods are thread-safe
 */
class WSNetWrapper {
    private val logger = LoggerFactory.getLogger("wsnet-wrapper")
    private val initialized = AtomicBoolean(false)
    private val _isReady = MutableStateFlow(false)

    @Volatile
    private var wsNetInstance: WSNet? = null

    /**
     * Cached, strongly-held references to the native sub-API peers.
     *
     * Each sub-API is a singleton on the native (scapix) side, and the native side keeps only a
     * JNI *weak* global reference to its Java peer. Calling wsNet.serverAPI()/bridgeAPI() returns
     * that peer, but if nothing on the Java side holds a strong reference the GC can collect it;
     * the next native->Java handoff then decodes a stale weak global and ART aborts the process
     * (SetLongField on the collected "ptr" field).
     *
     * Up to 4.1.0 the @Singleton Dagger providers (providesWsNetServerApi / providesBridgeApi)
     * called wrapper.getServerAPI()/getBridgeAPI() once and held the result for the app lifetime,
     * so the peer was never collected. The Hilt refactor switched callers to
     * awaitServerAPI()/awaitBridgeAPI(), which re-fetched on every call and retained nothing,
     * reintroducing the GC race. Caching the peer here restores that guarantee: fetch once, then
     * always hand back the same strongly-referenced instance.
     */
    @Volatile
    private var serverAPIInstance: WSNetServerAPI? = null

    @Volatile
    private var bridgeAPIInstance: WSNetBridgeAPI? = null

    @Volatile
    private var emergencyConnectInstance: WSNetEmergencyConnect? = null

    @Volatile
    private var advancedParametersInstance: WSNetAdvancedParameters? = null

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
        amneziaWgVersion: String,
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
                amneziaWgVersion,
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
        isProtocolTweaksEnabled: Boolean,
    ) {
        try {
            safeAdvancedParameters()?.let { params ->
                countryOverride?.let { override ->
                    params.setCountryOverrideValue(override)
                }
                params.isAPIExtraTLSPadding = isProtocolTweaksEnabled
            }
        } catch (e: Exception) {
            logger.error("Failed to configure advanced parameters: ${e.message}", e)
        }
    }

    /**
     * Check if WSNet is initialized and ready to use.
     * Thread-safe, non-blocking.
     */
    fun isInitialized(): Boolean = initialized.get() && WSNet.isValid()

    /**
     * Get the raw WSNet instance. Only use if you've checked isInitialized() first.
     * Prefer using the safe accessors instead.
     */
    fun getInstance(): WSNet? = if (isInitialized()) wsNetInstance else null

    /**
     * Suspend until WSNet is initialized, then return the bridge API.
     * Safe to call from the main thread.
     */
    suspend fun awaitBridgeAPI(): WSNetBridgeAPI {
        if (!_isReady.value) {
            logger.debug("awaitBridgeAPI: waiting for WSNet init")
        }
        isReady.first { it }
        val wsNet = wsNetInstance ?: throw IllegalStateException("WSNet not initialized")
        return bridgeAPIInstance ?: wsNet.bridgeAPI().also { bridgeAPIInstance = it }
    }

    /**
     * Safely get the bridge API, returns null if WSNet is not ready.
     */
    fun safeBridgeAPI(): WSNetBridgeAPI? {
        val wsNet = wsNetInstance ?: return null
        if (!isInitialized()) return null
        return try {
            bridgeAPIInstance ?: wsNet.bridgeAPI().also { bridgeAPIInstance = it }
        } catch (e: Exception) {
            logger.error("Failed to get bridgeAPI: ${e.message}")
            null
        }
    }

    /**
     * Suspend until WSNet is initialized, then return the server API.
     * Safe to call from the main thread.
     */
    suspend fun awaitServerAPI(): WSNetServerAPI {
        if (!_isReady.value) {
            logger.debug("awaitServerAPI: waiting for WSNet init")
        }
        isReady.first { it }
        val wsNet = wsNetInstance ?: throw IllegalStateException("WSNet not initialized")
        return serverAPIInstance ?: wsNet.serverAPI().also { serverAPIInstance = it }
    }

    /**
     * Safely get the emergency connect API, returns null if WSNet is not ready.
     */
    fun safeEmergencyConnect(): WSNetEmergencyConnect? {
        val wsNet = wsNetInstance ?: return null
        if (!isInitialized()) return null
        return try {
            emergencyConnectInstance ?: wsNet
                .emergencyConnect()
                .also { emergencyConnectInstance = it }
        } catch (e: Exception) {
            logger.error("Failed to get emergencyConnect: ${e.message}")
            null
        }
    }

    /**
     * Safely get the advanced parameters, returns null if WSNet is not ready.
     */
    fun safeAdvancedParameters(): WSNetAdvancedParameters? {
        val wsNet = wsNetInstance ?: return null
        if (!isInitialized()) return null
        return try {
            advancedParametersInstance ?: wsNet
                .advancedParameters()
                ?.also { advancedParametersInstance = it }
        } catch (e: Exception) {
            logger.error("Failed to get advancedParameters: ${e.message}")
            null
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
        } catch (_: Exception) {
        }
    }
}
