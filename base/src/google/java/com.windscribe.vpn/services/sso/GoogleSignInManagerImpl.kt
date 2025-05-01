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
            .requestIdToken("663054486699-d5m13v278rsjgtlsv962uva9s2gnqf37.apps.googleusercontent.com")
            .requestEmail()
            .build()
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