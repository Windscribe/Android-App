package com.windscribe.vpn.services.sso

import android.content.Context
import android.content.Intent

class GoogleSignInManagerImpl(val context: Context) : GoogleSignInManager() {
    override fun getSignInIntent() = null
    override fun getToken(result: Intent, callback: (String?, String?) -> Unit) {
        callback(null,"Google sign in not available in F-Droid version")
    }
    override fun signOut(callback: () -> Unit) {
        callback()
    }
}