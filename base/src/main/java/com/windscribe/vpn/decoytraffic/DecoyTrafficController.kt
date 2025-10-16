package com.windscribe.vpn.decoytraffic

import com.windscribe.vpn.api.IApiCallManager
import com.windscribe.vpn.apppreference.PreferencesHelper
import com.windscribe.vpn.backend.VPNState
import com.windscribe.vpn.commonutils.Ext.result
import com.windscribe.vpn.repository.CallResult
import com.windscribe.vpn.state.VPNConnectionStateManager
import java.util.Arrays
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import org.slf4j.LoggerFactory

class DecoyTrafficController(val scope: CoroutineScope, val apiCallManager: IApiCallManager, val preferencesHelper: PreferencesHelper, val vpnConnectionStateManager: VPNConnectionStateManager) {

    private var mainJob: Job? = null
    private var fakeTrafficVolume = 0
    private var sendTrafficRequestInProgress = false
    private var sendTrafficIntervalInSeconds = 1
    private val logger = LoggerFactory.getLogger("decoy")
    private var _events = MutableStateFlow(preferencesHelper.isDecoyTrafficOn)
    val state: StateFlow<Boolean> = _events
    private var fakeTraffic = preferencesHelper.fakeTrafficVolume
    private val trafficTrend = TrafficTrend()

    init {
        load()
        scope.launch {
            vpnConnectionStateManager.state.collectLatest {
                if (it.status == VPNState.Status.Connected && preferencesHelper.isDecoyTrafficOn) {
                    start()
                } else {
                    stop()
                }
            }
        }
    }

    fun load() {
        trafficTrend.upperLimitMultiplier = preferencesHelper.fakeTrafficVolume.multiplier()
        fakeTrafficVolume = preferencesHelper.fakeTrafficVolume.toBytes()
        fakeTraffic = preferencesHelper.fakeTrafficVolume
        stop()
        if(preferencesHelper.isDecoyTrafficOn){
            start()
        }
    }

    private var lastRequestSendTime = System.currentTimeMillis()
    fun start() {
        stop()
        mainJob = scope.launch {
            _events.emit(true)
            delay(sendTrafficIntervalInSeconds * 1000L)
            while (true) {
                if (sendTrafficRequestInProgress.not()) {
                    sendTrafficRequestInProgress = true
                    val timeUsed = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - lastRequestSendTime)
                    if(timeUsed >= fakeTraffic.interval()){
                        sendTrafficIntervalInSeconds = fakeTraffic.interval()
                    }else {
                        sendTrafficIntervalInSeconds = (fakeTraffic.interval() - timeUsed).toInt()
                        if(sendTrafficIntervalInSeconds < 1){
                          sendTrafficIntervalInSeconds = 1
                        }
                        delay(sendTrafficIntervalInSeconds * 1000L)
                    }
                    lastRequestSendTime = System.currentTimeMillis()
                    val dataToSendPerMinute = trafficTrend.calculateTraffic(fakeTrafficVolume, trafficTrend.currentUploadTrend.attemptsToIncrease, true)
                    val dataToSendPerSecond = dataToSendPerMinute / 60 * sendTrafficIntervalInSeconds
                    val chars = CharArray(dataToSendPerSecond)
                    Arrays.fill(chars, 'a')
                    val fakeData = String(chars)
                    val dataToReceivePerMinute = trafficTrend.calculateTraffic(fakeTrafficVolume, trafficTrend.currentDownloadTrend.attemptsToIncrease, false)
                    val dataToReceivePerSecond = dataToReceivePerMinute / 60 * sendTrafficIntervalInSeconds
                    sendTraffic(fakeData, dataToReceivePerSecond.toString())
                }

            }
        }
    }

    private suspend fun sendTraffic(data: String, dataToReceiveString: String?) {
        try {
            val url = "http://10.255.255.1:8085"
            sendTrafficRequestInProgress = withTimeout(100_000) { // 100 seconds in milliseconds
                when (result<String> {
                    apiCallManager.sendDecoyTraffic(url, data, dataToReceiveString)
                }) {
                    is CallResult.Error -> {
                        false
                    }
                    is CallResult.Success -> {
                        false
                    }
                }
            }
        } catch (e: Exception) {
            logger.debug("Error sending traffic ${e.message}")
            lastRequestSendTime = System.currentTimeMillis()
            sendTrafficRequestInProgress = false
            logger.debug(e.toString())
        }
    }

    fun stop() {
        lastRequestSendTime = System.currentTimeMillis()
        sendTrafficRequestInProgress = false
        if(mainJob?.isActive == true){
            logger.debug("Stopping decoy traffic controller.")
            mainJob?.cancel()
        }
        scope.launch { _events.emit(false)}
    }
}