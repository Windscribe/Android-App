package com.windscribe.mobile.di

import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.windscribe.mobile.welcome.viewmodal.EmergencyConnectViewModal
import com.windscribe.vpn.state.VPNConnectionStateManager
import dagger.Module
import dagger.Provides

@Module
class EmergencyConnectModule(private val activity: AppCompatActivity) {

    @Provides
    fun providesEmergencyConnectViewModal(vpnConnectionStateManager: VPNConnectionStateManager): Lazy<EmergencyConnectViewModal> {
        return activity.viewModels {
            return@viewModels EmergencyConnectViewModal.provideFactory(vpnConnectionStateManager)
        }
    }
}