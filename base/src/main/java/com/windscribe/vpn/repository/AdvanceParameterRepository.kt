package com.windscribe.vpn.repository

import com.windscribe.vpn.apppreference.PreferencesHelper
import com.windscribe.vpn.constants.AdvanceParamKeys.FORCE_NODE
import com.windscribe.vpn.constants.AdvanceParamKeys.SERVER_LIST_COUNTRY_OVERRIDE
import com.windscribe.vpn.constants.AdvanceParamKeys.SHOW_STRONG_SWAN_LOG
import com.windscribe.vpn.constants.AdvanceParamKeys.TUNNEL_START_DELAY
import com.windscribe.vpn.constants.AdvanceParamKeys.TUNNEL_TEST_ATTEMPTS
import com.windscribe.vpn.constants.AdvanceParamKeys.TUNNEL_TEST_RETRY_DELAY
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

interface AdvanceParameterRepository {
    fun reload()
    fun getCountryOverride(): String?
    fun getForceNode(): String?
    fun showStrongSwanLog(): Boolean
    fun getTunnelStartDelay(): Long?
    fun getTunnelTestRetryDelay(): Long?
    fun getTunnelTestAttempts(): Long?
}

class AdvanceParameterRepositoryImpl(val scope: CoroutineScope, val preferencesHelper: PreferencesHelper) : AdvanceParameterRepository {
    private val _params = MutableStateFlow(mapOf<String, String>())
    val params: StateFlow<Map<String, String>> = _params

    init {
        reload()
    }

    override fun reload() {
        scope.launch {
            _params.emit(mapTextToAdvanceParams(preferencesHelper.advanceParamText))
        }
    }

    override fun getCountryOverride(): String? {
        return params.value[SERVER_LIST_COUNTRY_OVERRIDE]
    }

    override fun getForceNode(): String? {
        return params.value[FORCE_NODE]
    }

    override fun showStrongSwanLog(): Boolean {
        return params.value[SHOW_STRONG_SWAN_LOG].toBoolean()
    }

    override fun getTunnelStartDelay(): Long? {
        return params.value[TUNNEL_START_DELAY]?.toLongOrNull()
    }

    override fun getTunnelTestRetryDelay(): Long? {
        return params.value[TUNNEL_TEST_RETRY_DELAY]?.toLongOrNull()
    }

    override fun getTunnelTestAttempts(): Long? {
        return params.value[TUNNEL_TEST_ATTEMPTS]?.toLongOrNull()
    }

    private fun mapTextToAdvanceParams(text: String): HashMap<String, String> {
        val map = HashMap<String, String>()
        if (text.isNotEmpty() && text.split("\n".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray().isNotEmpty()) {
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