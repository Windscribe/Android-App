package com.windscribe.vpn.services.firebasecloud

import com.windscribe.vpn.Windscribe
import com.windscribe.vpn.services.FirebaseManager

class FirebaseManagerImpl(private val context: Windscribe) : FirebaseManager {
    override fun initialise() {

    }

    override fun getFirebaseToken(callback: (String?) -> Unit) {
        callback(mutableMapOf())
    }

    override val isPlayStoreInstalled: Boolean
        get() = false
}