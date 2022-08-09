/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.mobile.splash

interface SplashView {
    val isConnectedToNetwork: Boolean
    fun navigateToAccountSetUp()
    fun navigateToHome()
    fun navigateToLogin()
}