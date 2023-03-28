package com.windscribe.mobile.welcome.viewmodal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.windscribe.vpn.state.VPNConnectionStateManager

class EmergencyConnectViewModal(vpnConnectionStateManager: VPNConnectionStateManager) :
    ViewModel() {


    companion object {
        fun provideFactory(vpnConnectionStateManager: VPNConnectionStateManager) =
            object : ViewModelProvider.NewInstanceFactory() {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    if (modelClass.isAssignableFrom(EmergencyConnectViewModal::class.java)) {
                        return EmergencyConnectViewModal(vpnConnectionStateManager) as T
                    }
                    return super.create(modelClass)
                }
            }
    }
}