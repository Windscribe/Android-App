/*
 * Copyright (c) 2026 Windscribe Limited.
 */
package com.windscribe.vpn.di

import android.app.Application
import com.windscribe.vpn.Windscribe
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Bridges Hilt's auto-bound [Application] to the [Windscribe] subtype that
 * [BaseApplicationModule] is parameterised on.
 */
@Module
@InstallIn(SingletonComponent::class)
object HiltAppBindingsModule {
    @Provides
    @Singleton
    fun provideWindscribe(app: Application): Windscribe = app as Windscribe
}
