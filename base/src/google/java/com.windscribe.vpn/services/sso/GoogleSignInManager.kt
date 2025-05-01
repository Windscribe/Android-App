package com.windscribe.vpn.services.sso

import android.content.Intent

abstract class GoogleSignInManager {
    abstract fun getSignInIntent(): Intent?
    abstract fun getToken(result: Intent, callback: (String?, String?) -> Unit)
}