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
            .requestIdToken("444711012498-5ftbuvd97d8vam8h93ef6l946t7p76kk.apps.googleusercontent.com")
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
                callback(null, exception?.localizedMessage ?: "Google Sign-In failed.")
            }
        }
    }

    override fun signOut(callback: () -> Unit) {
        googleSignInClient.signOut().addOnCompleteListener {
           callback()
        }
    }
}