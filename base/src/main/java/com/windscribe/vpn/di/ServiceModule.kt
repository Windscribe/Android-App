/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.vpn.di

import com.windscribe.vpn.ServiceInteractor
import com.windscribe.vpn.ServiceInteractorImpl
import com.windscribe.vpn.api.IApiCallManager
import com.windscribe.vpn.apppreference.PreferencesHelper
import com.windscribe.vpn.localdatabase.LocalDbInterface
import dagger.Module
import dagger.Provides

@Module
class ServiceModule {
    @Provides
    @PerService
    fun providesServiceInteractor(
            preferencesHelper: PreferencesHelper,
            apiCallManager: IApiCallManager,
            localDbInterface: LocalDbInterface
    ): ServiceInteractor {
        return ServiceInteractorImpl(preferencesHelper, apiCallManager, localDbInterface)
    }
}
