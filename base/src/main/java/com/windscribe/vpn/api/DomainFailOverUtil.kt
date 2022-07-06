package com.windscribe.vpn.api

import com.windscribe.vpn.Windscribe


object DomainFailOverUtil {

    private var failedStates = mutableMapOf<String, Boolean>()

    fun isAccessible(domainType: DomainType): Boolean{
        if(Windscribe.appContext.vpnConnectionStateManager.isVPNConnected()){
            return true
        }
       return failedStates[domainType.name] ?: true
    }

    fun reset(){
       failedStates.clear()
    }

    fun setDomainBlocked(domainType: DomainType){
        failedStates[domainType.name] = false
    }
}

enum class DomainType {
    Primary , Secondary , Hashed1, Hashed2, Hashed3, DirectIp1, DirectIp2
}