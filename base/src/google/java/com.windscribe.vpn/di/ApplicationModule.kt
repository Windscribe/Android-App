/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.vpn.di

import com.windscribe.vpn.Windscribe
import com.windscribe.vpn.backend.AndroidDeviceIdentity
import com.windscribe.vpn.backend.AndroidDeviceIdentityImpl
import com.windscribe.vpn.services.FirebaseManager
import com.windscribe.vpn.services.ReceiptValidator
import com.windscribe.vpn.services.firebasecloud.FireBaseManagerImpl
import com.windscribe.vpn.services.sso.GoogleSignInManager
import com.windscribe.vpn.services.sso.GoogleSignInManagerImpl
import com.windscribe.vpn.workers.WindScribeWorkManager
import com.windscribe.vpn.workers.worker.AmazonPendingReceiptValidator
import com.windscribe.vpn.workers.worker.GooglePendingReceiptValidator
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
        return ReceiptValidator(
            windscribeApp,
            manager.createOneTimeWorkerRequest(AmazonPendingReceiptValidator::class.java),
            manager.createOneTimeWorkerRequest(GooglePendingReceiptValidator::class.java)
        )
    }

    @Provides
    @Singleton
    fun providesFirebaseManager(): FirebaseManager {
        return FireBaseManagerImpl(windscribeApp)
    }

    @Provides
    @Singleton
    fun providesGoogleSignInManager(): GoogleSignInManager {
        return GoogleSignInManagerImpl(windscribeApp)
    }

    @Provides
    @Singleton
    fun provideAndroidIdentity(): AndroidDeviceIdentity {
        return AndroidDeviceIdentityImpl()
    }
}
