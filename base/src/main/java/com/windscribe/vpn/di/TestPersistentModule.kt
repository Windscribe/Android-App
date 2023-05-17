package com.windscribe.vpn.di

import com.windscribe.vpn.apppreference.AppPreferenceHelper
import com.windscribe.vpn.apppreference.PreferencesHelper
import com.windscribe.vpn.apppreference.SecurePreferences
import com.windscribe.vpn.constants.PreferencesKeyConstants
import dagger.Module
import dagger.Provides
import net.grandcentrix.tray.AppPreferences
import javax.inject.Singleton

@Module
class TestPersistentModule {
    var testConfiguration: TestConfiguration? = null

    @Provides
    @Singleton
    fun providesPreferenceHelper(
        mPreference: AppPreferences, securePreferences: SecurePreferences
    ): AppPreferenceHelper {
        val preferencesHelper = AppPreferenceHelper(mPreference, securePreferences)
        testConfiguration?.update(preferencesHelper)
        return preferencesHelper
    }
}

data class TestConfiguration(
    var accountStatus: Int = 1,
    var premium: Int = 0,
    var lastAccountStatus: Int = 1,
    var theme: String = PreferencesKeyConstants.DARK_THEME,
    var language: String = PreferencesKeyConstants.DEFAULT_LANGUAGE
) {
    fun update(preferencesHelper: PreferencesHelper) {
        preferencesHelper.sessionHash = "xxxxxxxxx"
        preferencesHelper.setPreviousAccountStatus("test", lastAccountStatus)
        val session =
            "{\n    \"ignore_udp_tests\": 1,\n    \"username\": \"test\",\n    \"user_id\": \"test_id\",\n    \"traffic_used\": 700519742,\n    \"traffic_max\": -1,\n    \"status\": $accountStatus,\n    \"email\": \"test@gmail.com\",\n    \"email_status\": 3,\n    \"billing_plan_id\": 3,\n    \"is_premium\": 1,\n    \"rebill\": 1,\n    \"premium_expiry_date\": \"2022-02-26\",\n    \"reg_date\": 1508859394,\n    \"last_reset\": \"2022-01-15\",\n    \"sip\": {\n      \"count\": 1\n    },\n    \"loc_rev\": 7768,\n    \"loc_hash\": \"-x-x-x-x-x-x-x--x\"\n  }"
        preferencesHelper.alreadyShownShareAppLink = true
        if (theme == PreferencesKeyConstants.DARK_THEME) {
            preferencesHelper.selectedTheme = PreferencesKeyConstants.DARK_THEME
        } else {
            preferencesHelper.selectedTheme = "light"
        }
        preferencesHelper.saveResponseStringData(PreferencesKeyConstants.GET_SESSION, session)
    }
}