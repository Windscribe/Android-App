/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.vpn.di

import com.windscribe.vpn.Windscribe
import com.windscribe.vpn.backend.AndroidDeviceIdentity
import com.windscribe.vpn.backend.AndroidDeviceIdentityImpl
import com.windscribe.vpn.services.FirebaseManager
import com.windscribe.vpn.services.ReceiptValidator
import com.windscribe.vpn.services.firebasecloud.FirebaseManagerImpl
import com.windscribe.vpn.services.sso.GoogleSignInManager
import com.windscribe.vpn.services.sso.GoogleSignInManagerImpl
import com.windscribe.vpn.workers.WindScribeWorkManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ApplicationModule {

    @Provides
    @Singleton
    fun provideReceiptValidator(app: Windscribe, manager: WindScribeWorkManager): ReceiptValidator {
        return ReceiptValidator(app, null, null)
    }

    @Provides
    @Singleton
    fun providesFirebaseManager(app: Windscribe): FirebaseManager {
        return FirebaseManagerImpl(app)
    }

    @Provides
    @Singleton
    fun provideAndroidIdentity(): AndroidDeviceIdentity {
        return AndroidDeviceIdentityImpl()
    }

    @Provides
    @Singleton
    fun providesGoogleSignInManager(app: Windscribe): GoogleSignInManager {
        return GoogleSignInManagerImpl(app)
    }
}
