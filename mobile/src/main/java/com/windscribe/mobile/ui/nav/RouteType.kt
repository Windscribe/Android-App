package com.windscribe.mobile.ui.nav

sealed class Route {
    data class Web(val url: String) : Route()
    data class Nav(val screen: Screen) : Route()
}