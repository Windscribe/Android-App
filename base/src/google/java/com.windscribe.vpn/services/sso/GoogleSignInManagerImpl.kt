package com.windscribe.vpn.services.sso

import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.windscribe.vpn.BuildConfig
import com.windscribe.vpn.Windscribe
import org.slf4j.LoggerFactory


class GoogleSignInManagerImpl(val context: Context) : GoogleSignInManager() {
    private val googleSignInClient: GoogleSignInClient
    private val logger = LoggerFactory.getLogger("sso")

    init {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(BuildConfig.WEB_CLIENT_ID)
            .requestEmail()
            .build()
        logger.info("Web client id: ${BuildConfig.WEB_CLIENT_ID}")
        val activity = (context as Windscribe).activeActivity
        googleSignInClient = GoogleSignIn.getClient(activity!!, gso)
    }

    override fun getSignInIntent() = googleSignInClient.signInIntent

    override fun getToken(result: Intent, callback: (String?, String?) -> Unit) {
        val task = GoogleSignIn.getSignedInAccountFromIntent(result)
        task.addOnCompleteListener { completedTask ->
            if (completedTask.isSuccessful) {
                val account = completedTask.result
                val idToken = account?.idToken
                if (!idToken.isNullOrEmpty()) {
                    callback(idToken, null)
                } else {
                    callback(null, "ID token is null or empty.")
                }
            } else {
                val exception = completedTask.exception
                callback(null, exception?.localizedMessage ?: "Google Sign-In failed.")
            }
        }
    }
}