/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.tv.welcome.fragment

interface WelcomeActivityCallback {
    fun clearInputErrors() {}
    fun setLoginError(error: String) {}
    fun setPasswordError(error: String) {}
    fun setSecretCode(code: String) {}
    fun setUsernameError(error: String) {}
}
