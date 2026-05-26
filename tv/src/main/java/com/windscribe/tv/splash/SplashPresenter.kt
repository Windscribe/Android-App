/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.tv.splash

import com.windscribe.tv.splash.SplashView
import kotlinx.coroutines.CoroutineScope

interface SplashPresenter {
    fun bind(view: SplashView, scope: CoroutineScope)
    fun checkNewMigration()
    fun onDestroy()
}
