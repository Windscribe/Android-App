package com.windscribe.vpn.api

import com.windscribe.vpn.Windscribe
import com.windscribe.vpn.apppreference.PreferencesHelper
import com.windscribe.vpn.backend.wireguard.WireguardBackend
import com.windscribe.vpn.exceptions.WindScribeException
import com.windscribe.vpn.localdatabase.LocalDbInterface
import com.windscribe.vpn.repository.LocationRepository
import com.windscribe.vpn.repository.UserRepository
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.Socket
import javax.inject.Singleton
import javax.net.SocketFactory
import kotlinx.coroutines.CoroutineScope
import org.slf4j.LoggerFactory

class CustomSocketFactory : SocketFactory() {
    private var logger = LoggerFactory.getLogger("socket_factory")
    override fun createSocket(): Socket {
        val socket = Socket(Proxy.NO_PROXY)
        try {
            socket.bind(InetSocketAddress(0))
            val activeBackend = Windscribe.appContext.vpnController.vpnBackendHolder.activeBackend
            if (activeBackend is WireguardBackend && activeBackend.active) {
                val protected = activeBackend.service?.protect(socket)
                if(protected == false){
                    logger.debug("Failed to protect socket from wg tunnel")
                }
            }
        } catch (e: Exception) {
            logger.debug("Error creating socket: ${e.message}")
        }
        return socket
    }

    override fun createSocket(host: String?, port: Int): Socket {
        throw WindScribeException("Not supported")
    }

    override fun createSocket(host: String?, port: Int, localHost: InetAddress?, localPort: Int): Socket {
        throw WindScribeException("Not supported")
    }

    override fun createSocket(host: InetAddress?, port: Int): Socket {
        throw WindScribeException("Not supported")
    }

    override fun createSocket(address: InetAddress?, port: Int, localAddress: InetAddress?, localPort: Int): Socket {
        throw WindScribeException("Not supported")
    }
}