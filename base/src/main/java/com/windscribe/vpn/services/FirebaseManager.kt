package com.windscribe.vpn.services

interface FirebaseManager {
    fun initialise()
    fun getFirebaseToken(callback: (String?) -> Unit)
    val isPlayStoreInstalled: Boolean
}