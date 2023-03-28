package com.windscribe.mobile.di

import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LifecycleOwner
import com.windscribe.mobile.welcome.viewmodal.EmergencyConnectViewModal
import com.windscribe.vpn.state.VPNConnectionStateManager
import dagger.Module
import dagger.Provides

@Module
class EmergencyConnectModule {
    lateinit var lifeowner: LifecycleOwner
    lateinit var activty: AppCompatActivity

    constructor()
    constructor(lifecycleOwner: LifecycleOwner) {
        this.lifeowner = lifeowner
    }

    @Provides
    fun providesEmergencyConnectViewModal(vpnConnectionStateManager: VPNConnectionStateManager): EmergencyConnectViewModal {
        activty.viewModelStore.return EmergencyConnectViewModal.provideFactory(
            vpnConnectionStateManager
        ).create(EmergencyConnectViewModal::class.java)
    }
}