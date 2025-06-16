package com.windscribe.vpn.services.sso

import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.windscribe.vpn.BuildConfig
import org.slf4j.LoggerFactory


class GoogleSignInManagerImpl(val context: Context) : GoogleSignInManager() {
    private val googleSignInClient: GoogleSignInClient
    private val logger = LoggerFactory.getLogger("sso")

    init {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(BuildConfig.WEB_CLIENT_ID)
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(context, gso)
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
                if (exception is com.google.android.gms.common.api.ApiException) {
                    val statusCode = exception.statusCode
                    logger.error("Google Sign-In failed with status code: $statusCode")
                    callback(null, "Google Sign-In failed with status code: $statusCode")
                    return@addOnCompleteListener
                } else {
                    logger.error("Google Sign-In failed with exception: $exception")
                    callback(null, "Google Sign-In failed.")
                    return@addOnCompleteListener
                }
            }
        }
    }

    override fun signOut(callback: () -> Unit) {
        googleSignInClient.signOut().addOnCompleteListener {
           callback()
        }
    }
}