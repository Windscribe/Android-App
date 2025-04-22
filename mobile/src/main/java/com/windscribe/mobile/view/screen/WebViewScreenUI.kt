package com.windscribe.mobile.view.screen

import android.annotation.SuppressLint
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import java.io.Serializable
import java.util.UUID

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebViewScreenUI(navController: NavController) {
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.javaScriptCanOpenWindowsAutomatically = true
                settings.setSupportMultipleWindows(true)

                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(
                        view: WebView?,
                        request: WebResourceRequest
                    ): Boolean {
                        val url = request.url.toString()
                        if (url.startsWith("com.windscribe.vpn://callback")) {
                            val uri = Uri.parse(url)
                            val code = uri.getQueryParameter("code")
                            val error = uri.getQueryParameter("error")
                            val state = uri.getQueryParameter("state")

                            val data = if (code != null) {
                                WebViewResult("success", state ?: "", code)
                            } else {
                                WebViewResult("error", "", error ?: "Unknown error")
                            }

                            val bundle = Bundle().apply {
                                putSerializable("data", data)
                            }
                            navController.previousBackStackEntry?.savedStateHandle?.set(
                                "result",
                                bundle
                            )
                            navController.popBackStack()
                            return true
                        }
                        return super.shouldOverrideUrlLoading(view, request)
                    }
                }
                Log.i("WebView", getAuthUrl())
                loadUrl(getAuthUrl())
            }
        }
    )
}

private fun getAuthUrl(): String {
    val state = UUID.randomUUID().toString()
    return Uri.parse("https://appleid.apple.com/auth/authorize")
        .buildUpon()
        .apply {
            appendQueryParameter("response_type", "code")
            appendQueryParameter("v", "1.1.6")
            appendQueryParameter("client_id", "com.windscribe.applelogin")
            appendQueryParameter(
                "redirect_uri",
                "https://3a95-135-0-84-99.ngrok-free.app/callback.php"
            )
            appendQueryParameter("scope", "email name")
            appendQueryParameter("state", state)
            appendQueryParameter("response_mode", "form_post")
        }
        .build()
        .toString()
}

data class WebViewResult(val status: String, val state: String, val message: String) : Serializable