package com.windscribe.vpn.api

import android.os.Build
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.windscribe.vpn.Windscribe
import com.windscribe.vpn.Windscribe.Companion.appContext
import com.windscribe.vpn.apppreference.PreferencesHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import okhttp3.Dns
import okio.ByteString.Companion.encodeUtf8
import java.net.Inet4Address
import java.net.InetAddress
import java.net.UnknownHostException

class WindscribeDnsResolver(private val mainScope: CoroutineScope, private val preferenceHelper: PreferencesHelper) : Dns {
    private var memoryCache = mutableMapOf<String, String>()

    fun addToCache(host: String, ip: String) {
        memoryCache[host] = ip
    }

    override fun lookup(hostname: String): List<InetAddress> {
        val key = hostname.encodeUtf8().md5().hex()
        try {
            if (memoryCache.containsKey(hostname)) {
                return listOf(InetAddress.getByName(memoryCache[hostname])).sortedWith { address1, address2 ->
                    return@sortedWith sortIpAddresses(hostname, address1, address2)
                }
            }
            val addresses = InetAddress.getAllByName(hostname).toList().sortedWith { address1, address2 ->
                return@sortedWith sortIpAddresses(hostname, address1, address2)
            }
            cacheDnsResponse(key, addresses)
            return addresses
        } catch (e: UnknownHostException) {
            return preferenceHelper.getResponseString(key)?.let {
                val type = object : TypeToken<List<InetAddress>>() {}.type
                return Gson().fromJson(it, type)
            } ?: throw UnknownHostException("Broken system behaviour for dns lookup of $hostname")
        } catch (e: Exception) {
            throw UnknownHostException("Broken system behaviour for dns lookup of $hostname").apply {
                initCause(e)
            }
        }
    }

    private fun preferIpv4(hostname: String): Boolean {
        val regex = Regex("(assets|api|checkip).[0-9a-f]{40}.com")
        return appContext.isRegionRestricted && regex.matches(hostname)
    }

    private fun sortIpAddresses(hostname: String, address1: InetAddress, address2: InetAddress): Int {
        val isIpv4Address1 = address1 is Inet4Address
        val isIpv4Address2 = address2 is Inet4Address
        val preferIpv4 = preferIpv4(hostname)
        return when {
            preferIpv4 && isIpv4Address1 && !isIpv4Address2 -> -1
            preferIpv4 && !isIpv4Address1 && isIpv4Address2 -> 1
            else -> 0
        }
    }

    private fun cacheDnsResponse(key: String, addresses: List<InetAddress>) {
        mainScope.launch {
            preferenceHelper.saveResponseStringData(key, Gson().toJson(addresses))
        }
    }
}