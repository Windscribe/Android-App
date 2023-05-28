package com.windscribe.vpn.di

import com.windscribe.vpn.apppreference.AppPreferenceHelper
import com.windscribe.vpn.apppreference.SecurePreferences
import dagger.Module
import dagger.Provides
import net.grandcentrix.tray.AppPreferences
import javax.inject.Singleton

@Module
class PersistentModule {
    @Provides
    @Singleton
    fun providesPreferenceHelper(
        mPreference: AppPreferences, securePreferences: SecurePreferences
    ): AppPreferenceHelper {
        return AppPreferenceHelper(mPreference, securePreferences)
    }
}