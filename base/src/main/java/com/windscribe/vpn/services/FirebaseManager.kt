package com.windscribe.vpn.services

interface FirebaseManager {
    fun initialise()
    fun getFirebaseToken(callback: (MutableMap<String, String>) -> Unit)
    val isPlayStoreInstalled: Boolean
}