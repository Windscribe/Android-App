package com.windscribe.vpn.services.review

import android.content.Context
import com.google.android.play.core.review.ReviewManagerFactory
import com.windscribe.vpn.Windscribe.Companion.appContext
import com.windscribe.vpn.apppreference.PreferencesHelper
import com.windscribe.vpn.constants.RateDialogConstants
import com.windscribe.vpn.repository.UserRepository
import com.windscribe.vpn.state.WindscribeReviewManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import java.util.Date
import java.util.concurrent.TimeUnit

class WindscribeReviewManagerImpl(
    val scope: CoroutineScope,
    val context: Context,
    val preferencesHelper: PreferencesHelper,
    val userRepository: UserRepository
) : WindscribeReviewManager {
    private val reviewManager = ReviewManagerFactory.create(context)
    private val logger = LoggerFactory.getLogger("state")
    override fun handleAppReview() {
        scope.launch {
            userRepository.userInfo.collectLatest {
                delay(3000)
                val dataUsed = it.dataUsed.toDouble() / (1024 * 1024 * 1024)
                logger.debug(
                    "Data Used {}, Days since login {} Request app review: {}",
                    dataUsed,
                    daysSinceLogin(),
                    notAlreadyShown()
                )
                if (dataUsed > 1.0 && daysSinceLogin() >= 2 && notAlreadyShown()) {
                    reviewManager.requestReviewFlow().addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val reviewInfo = task.result
                            logger.debug("Launching review flow.")
                            appContext.activeActivity?.let { activity ->
                                val flow = reviewManager.launchReviewFlow(activity, reviewInfo)
                                flow.addOnCompleteListener { _ ->
                                    preferencesHelper.rateDialogLastUpdateTime = Date().time.toString()
                                    logger.debug("Review flow completed.")
                                }
                            }

                        } else {
                            logger.error("Error requesting review flow", task.exception)
                        }
                    }
                }
            }
        }
    }

    private fun daysSinceLogin(): Long {
        val milliSeconds1 = preferencesHelper.loginTime?.time ?: Date().time
        val milliSeconds2 = Date().time
        val periodSeconds = (milliSeconds2 - milliSeconds1) / 1000
        return periodSeconds / 60 / 60 / 24
    }

    private fun notAlreadyShown(): Boolean {
        val time = preferencesHelper.rateDialogLastUpdateTime ?: return true
        return try {
            val difference = Date().time - time.toLong()
            val days = TimeUnit.DAYS.convert(difference, TimeUnit.MILLISECONDS)
            return days > 30
        } catch (e: NumberFormatException) {
            true
        }
    }
}