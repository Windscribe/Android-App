package com.windscribe.mobile.view.screen

sealed class Screen(val route: String) {
    object Start: Screen("start")
    object Login: Screen("login")
    object Signup: Screen("signup")
    object Home: Screen("home")
    object NoEmailAttention: Screen("no_email_attention")
    object Newsfeed: Screen("newsfeed")
    object EmergencyConnect: Screen("emergency_connect")
    object Web: Screen("web")
    object PowerWhitelist: Screen("power_whitelist")
    object ShareLink: Screen("share_link")
}