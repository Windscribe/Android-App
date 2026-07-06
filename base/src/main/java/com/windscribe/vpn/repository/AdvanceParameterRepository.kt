package com.windscribe.vpn.repository

import com.windscribe.vpn.apppreference.PreferencesHelper
import com.windscribe.vpn.constants.AdvanceParamKeys.FORCE_NODE
import com.windscribe.vpn.constants.AdvanceParamKeys.SERVER_LIST_COUNTRY_OVERRIDE
import com.windscribe.vpn.constants.AdvanceParamKeys.SHOW_CD_LOG
import com.windscribe.vpn.constants.AdvanceParamKeys.SHOW_STRONG_SWAN_LOG
import com.windscribe.vpn.constants.AdvanceParamKeys.SHOW_WG_LOG
import com.windscribe.vpn.constants.AdvanceParamKeys.TUNNEL_START_DELAY
import com.windscribe.vpn.constants.AdvanceParamKeys.TUNNEL_TEST_ATTEMPTS
import com.windscribe.vpn.constants.AdvanceParamKeys.TUNNEL_TEST_RETRY_DELAY
import com.windscribe.vpn.constants.AdvanceParamKeys.USE_ICMP_PINGS
import com.windscribe.vpn.wsnet.WSNetWrapper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

interface AdvanceParameterRepository {
    fun reload()

    fun getCountryOverride(): String?

    fun getForceNode(): String?

    fun showStrongSwanLog(): Boolean

    fun showWgLog(): Boolean

    fun showCdLog(): Boolean

    fun getTunnelStartDelay(): Long?

    fun getTunnelTestRetryDelay(): Long?

    fun getTunnelTestAttempts(): Long?

    fun pingType(): Int
}

class AdvanceParameterRepositoryImpl(
    val scope: CoroutineScope,
    val preferencesHelper: PreferencesHelper,
    val wsNetWrapper: WSNetWrapper,
) : AdvanceParameterRepository {
    private val _params = MutableStateFlow(mapOf<String, String>())
    val params: StateFlow<Map<String, String>> = _params

    init {
        reload()
        // Apply advanced parameters when WSNet is ready
        scope.launch {
            wsNetWrapper.isReady.collect { isReady ->
                if (isReady) {
                    withContext(Dispatchers.Main) {
                        applyAdvancedParameters()
                    }
                }
            }
        }
    }

    private fun applyAdvancedParameters() {
        wsNetWrapper.configureAdvancedParameters(
            getCountryOverride(),
            preferencesHelper.extraTlsPaddingEnabled,
        )
    }

    override fun reload() {
        scope.launch {
            _params.emit(mapTextToAdvanceParams(preferencesHelper.advanceParamText))
        }
    }

    override fun getCountryOverride(): String? = params.value[SERVER_LIST_COUNTRY_OVERRIDE]

    override fun getForceNode(): String? = params.value[FORCE_NODE]

    override fun showStrongSwanLog(): Boolean = params.value[SHOW_STRONG_SWAN_LOG].toBoolean()

    override fun showWgLog(): Boolean = params.value[SHOW_WG_LOG].toBoolean()

    override fun showCdLog(): Boolean = params.value[SHOW_CD_LOG].toBoolean()

    override fun getTunnelStartDelay(): Long? = params.value[TUNNEL_START_DELAY]?.toLongOrNull()

    override fun getTunnelTestRetryDelay(): Long? = params.value[TUNNEL_TEST_RETRY_DELAY]?.toLongOrNull()

    override fun getTunnelTestAttempts(): Long? = params.value[TUNNEL_TEST_ATTEMPTS]?.toLongOrNull()

    override fun pingType(): Int {
        val useIcmp = params.value[USE_ICMP_PINGS]?.toBoolean()
        return if (useIcmp == true) {
            1
        } else {
            0
        }
    }

    private fun mapTextToAdvanceParams(text: String): HashMap<String, String> {
        val map = HashMap<String, String>()
        if (text.isNotEmpty() &&
            text
                .split("\n".toRegex())
                .dropLastWhile { it.isEmpty() }
                .toTypedArray()
                .isNotEmpty()
        ) {
            val lines = text.split("\n".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            for (line in lines) {
                val kv = line.split("=".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                if (kv.size == 2) {
                    map[kv[0]] = kv[1]
                }
            }
        }
        return map
    }
}
