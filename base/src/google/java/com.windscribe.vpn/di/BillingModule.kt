package com.windscribe.vpn.di

import com.windscribe.vpn.Windscribe
import com.windscribe.vpn.billing.AmazonBillingManager
import com.windscribe.vpn.billing.GoogleBillingManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class BillingModule {
    @Provides
    @Singleton
    fun provideAmazonBillingManager(app: Windscribe): AmazonBillingManager = AmazonBillingManager(app)

    @Provides
    @Singleton
    fun provideGoogleBillingManager(app: Windscribe): GoogleBillingManager = GoogleBillingManager(app)
}
