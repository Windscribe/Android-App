package com.windscribe.common

import android.content.Context
import java.io.Serializable
import java.lang.Exception
import java.net.Inet4Address
import java.net.InetAddress
import java.net.MalformedURLException
import java.net.URL
import java.net.UnknownHostException

enum class DnsType {
    Tunnel, Plain, Proxy
}
data class DNSDetails(val address: String? = null, val ip: String? = null, val type: DnsType): Serializable {

    val getTypeValue: String
        get() {
            if (address == null) {
                return "legacy"
            }
            val dohPattern = "^https?://.*$".toRegex()
            val doh3Pattern = "^h3?://.*$".toRegex()
            val sdnsPattern = "^sdns?://.*$".toRegex()
            return when {
                dohPattern.matches(address) -> "doh"
                sdnsPattern.matches(address) -> "sdns"
                doh3Pattern.matches(address) -> "doh3"
                else -> "dot"
            }
        }
}
fun getDNSDetails(context: Context, customDNSEnabled: Boolean, address: String?): Result<DNSDetails> {
    if (!customDNSEnabled || address == null) {
        return Result.success(DNSDetails(type = DnsType.Tunnel))
    }
    val ipPattern = "^\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\$".toRegex()
    if (ipPattern.matches(address)){
        return Result.success(DNSDetails(address = address, ip = address, type = DnsType.Plain))
    }
    if(address.startsWith("sdns://")) {
        return Result.success(DNSDetails(address = address, null, type = DnsType.Proxy))
    }
    val bootstrapIp = CommonPreferences.getBootstrapIp(context, address)
    return try {
        var endpoint = address
        if (!address.startsWith("https://")) {
           endpoint = "https://$address".replace("h3://", "")
        }
        val url = URL(endpoint)
        val inetAddresses = InetAddress.getAllByName(url.host)
        val ipv4Address = inetAddresses.firstOrNull { it is Inet4Address } as? Inet4Address
        if (ipv4Address?.hostAddress != null) {
            CommonPreferences.saveBootstrapIp(context, address, ipv4Address.hostAddress)
            Result.success(DNSDetails(address = address, ip = ipv4Address.hostAddress, type = DnsType.Proxy))
        } else if(bootstrapIp != null) {
            Result.success(DNSDetails(address = address, ip = bootstrapIp, type = DnsType.Proxy))
        } else {
            Result.failure(Exception("Unable to resolve $address)"))
        }
    } catch (e: MalformedURLException) {
        Result.failure(Exception("Malformed DNS URL/IP"))
    } catch (e: UnknownHostException) {
        if(bootstrapIp != null) {
            Result.success(DNSDetails(address = address, ip = bootstrapIp, type = DnsType.Proxy))
        } else {
            Result.failure(Exception("Unable to resolve DNS Address"))
        }
    }
}