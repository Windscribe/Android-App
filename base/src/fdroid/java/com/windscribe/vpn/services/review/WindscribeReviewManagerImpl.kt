package com.windscribe.vpn.services.review

import android.content.Context
import com.windscribe.vpn.apppreference.PreferencesHelper
import com.windscribe.vpn.repository.UserRepository
import com.windscribe.vpn.state.WindscribeReviewManager
import kotlinx.coroutines.CoroutineScope

class WindscribeReviewManagerImpl(
    val scope: CoroutineScope,
    val context: Context,
    val preferencesHelper: PreferencesHelper,
    val userRepository: UserRepository
) : WindscribeReviewManager {
    override fun handleAppReview() {}
}