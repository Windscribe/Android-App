/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.state

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import androidx.annotation.RequiresApi
import com.wsnet.lib.WSNet
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DNSStateManager @Inject constructor(
    private val scope: CoroutineScope
) {
    
    private val logger = LoggerFactory.getLogger("dns_state")
    private var connectivityManager: ConnectivityManager? = null
    private var networkCallback: ConnectivityManager.NetworkCallback? = null
    
    private var _dnsServers = MutableStateFlow<List<String>>(emptyList())

    @RequiresApi(Build.VERSION_CODES.M)
    fun init(context: Context) {
        connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        
        networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                updateDnsServers(network)
            }
            
            override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                updateDnsServers(network)
            }
            
            override fun onLinkPropertiesChanged(network: Network, linkProperties: android.net.LinkProperties) {
                updateDnsServers(network)
            }
        }
        
        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
            
        connectivityManager?.registerNetworkCallback(networkRequest, networkCallback!!)
        getCurrentDnsServers()
    }
    
    private fun updateDnsServers(network: Network) {
        scope.launch {
            try {
                val linkProperties = connectivityManager?.getLinkProperties(network)
                val dnsServersList = linkProperties?.dnsServers?.map { it.hostAddress } ?: emptyList()
                
                if (dnsServersList != _dnsServers.value) {
                    _dnsServers.emit(dnsServersList)
                    setDnsOnWSNet(dnsServersList)
                }
            } catch (e: Exception) {
                logger.error("Error updating DNS servers", e)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun getCurrentDnsServers() {
        scope.launch {
            try {
                val activeNetwork = connectivityManager?.activeNetwork
                val linkProperties = connectivityManager?.getLinkProperties(activeNetwork)
                val dnsServersList = linkProperties?.dnsServers?.map { it.hostAddress } ?: emptyList()
                
                _dnsServers.emit(dnsServersList)
                setDnsOnWSNet(dnsServersList)
            } catch (e: Exception) {
                logger.error("Error getting current DNS servers", e)
            }
        }
    }
    
    private fun setDnsOnWSNet(dnsServers: List<String>) {
        try {
            val ipv4Addresses = dnsServers.mapNotNull { dnsAddress ->
                when {
                    dnsAddress.matches(Regex("\\d+\\.\\d+\\.\\d+\\.\\d+:\\d+")) -> {
                        dnsAddress.substringBefore(":")
                    }
                    dnsAddress.matches(Regex("\\d+\\.\\d+\\.\\d+\\.\\d+")) -> {
                        dnsAddress
                    }
                    else -> null
                }
            }.toMutableList()
            
            ipv4Addresses.add("76.76.2.0")
            ipv4Addresses.add("1.1.1.1")
            
            WSNet.instance().dnsResolver().setDnsServers(ipv4Addresses.toTypedArray())
        } catch (e: Exception) {
            logger.error("Error setting DNS servers on WSNet", e)
        }
    }
    
    fun destroy() {
        networkCallback?.let { callback ->
            connectivityManager?.unregisterNetworkCallback(callback)
        }
        connectivityManager = null
        networkCallback = null
    }
}