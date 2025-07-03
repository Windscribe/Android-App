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
<<<<<<< HEAD
=======
import com.windscribe.vpn.services.sso.GoogleSignInManager
import com.windscribe.vpn.services.sso.GoogleSignInManagerImpl
>>>>>>> origin/develop
import com.windscribe.vpn.workers.WindScribeWorkManager
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

/**
 * Application module provides production dependencies
 * In future plan is break this module in to smaller modules
 * to ease swapping of modules for testing.
 * */
@Module
class ApplicationModule(override var windscribeApp: Windscribe) : BaseApplicationModule() {
    @Provides
    @Singleton
    fun provideReceiptValidator(manager: WindScribeWorkManager): ReceiptValidator {
        return ReceiptValidator(windscribeApp, null, null)
    }
    @Provides
    @Singleton
    fun providesFirebaseManager(): FirebaseManager {
        return FirebaseManagerImpl(windscribeApp)
    }

    @Provides
    @Singleton
    fun provideAndroidIdentity(): AndroidDeviceIdentity {
        return AndroidDeviceIdentityImpl()
    }
<<<<<<< HEAD
=======

    @Provides
    @Singleton
    fun providesGoogleSignInManager(): GoogleSignInManager {
        return GoogleSignInManagerImpl(windscribeApp)
    }
>>>>>>> origin/develop
}