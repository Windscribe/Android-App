package com.windscribe.vpn.api

import android.util.Base64
import com.windscribe.vpn.BuildConfig
import kotlinx.coroutines.*
import org.conscrypt.Conscrypt
import java.io.IOException
import java.net.InetAddress
import java.net.Socket
import java.net.UnknownHostException
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory

/**
 * SSLSocketFactory that disables the DNS auto-fetch, then manually do DNS in the test.
 */
internal class ManualEchSSLSocketFactory(
    private val dohResolver: DohResolver,
    private val delegate: SSLSocketFactory
) : SSLSocketFactory() {
    private var host: String? = null
    private var sslSocket: SSLSocket? = null
    override fun getDefaultCipherSuites(): Array<String> {
        return delegate.defaultCipherSuites
    }

    override fun getSupportedCipherSuites(): Array<String> {
        return delegate.supportedCipherSuites
    }

    @Throws(IOException::class)
    override fun createSocket(socket: Socket, host: String, port: Int, autoClose: Boolean): Socket {
        this.host = host
        return setEchSettings(delegate.createSocket(socket, host, port, autoClose))!!
    }

    @Throws(IOException::class, UnknownHostException::class)
    override fun createSocket(host: String, port: Int): Socket {
        this.host = host
        return setEchSettings(delegate.createSocket(host, port))!!
    }

    @Throws(IOException::class, UnknownHostException::class)
    override fun createSocket(
        host: String, port: Int, localAddress: InetAddress, localPort: Int
    ): Socket {
        this.host = host
        return setEchSettings(delegate.createSocket(host, port, localAddress, localPort))!!
    }

    @Throws(IOException::class)
    override fun createSocket(address: InetAddress, port: Int): Socket {
        return setEchSettings(delegate.createSocket(address, port))!!
    }

    @Throws(IOException::class)
    override fun createSocket(
        address: InetAddress, port: Int, localAddress: InetAddress, localPort: Int
    ): Socket {
        return setEchSettings(delegate.createSocket(address, port, localAddress, localPort))!!
    }

    private fun setEchSettings(socket: Socket): Socket? {
        sslSocket = socket as SSLSocket
        Conscrypt.setUseEchGrease(sslSocket, false)
        Conscrypt.setCheckDnsForEch(sslSocket, false)
        setEchConfig(socket)
        return sslSocket
    }

    private fun setEchConfig(sslSocket: SSLSocket) {
        CoroutineScope(Dispatchers.IO).launch {
            dohResolver.getTxtAnswer(BuildConfig.ECH_DOMAIN)?.data?.let {
                val config = Base64.decode(it, Base64.DEFAULT)
                Conscrypt.setEchConfigList(sslSocket, config)
            }
        }
    }
}