package com.windscribe.vpn.workers.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.windscribe.vpn.apppreference.PreferencesHelper
import com.windscribe.vpn.billing.AmazonPurchase
import com.windscribe.vpn.billing.PurchaseManager
import com.windscribe.vpn.billing.ReceiptParams
import com.windscribe.vpn.constants.BillingConstants
import com.windscribe.vpn.exceptions.WindScribeException
import com.windscribe.vpn.repository.UserDataState
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import org.slf4j.LoggerFactory

@HiltWorker
class AmazonPendingReceiptValidator
    @AssistedInject
    constructor(
        @Assisted appContext: Context,
        @Assisted params: WorkerParameters,
        private val purchaseManager: PurchaseManager,
        private val preferencesHelper: PreferencesHelper,
    ) : CoroutineWorker(appContext, params) {
        private val logger = LoggerFactory.getLogger("billing")

        override suspend fun doWork(): Result =
            try {
                val amazonPurchase = getPendingAmazonPurchase()
                val verified = verifyPurchase(amazonPurchase)

                if (verified) {
                    logger.debug("Amazon purchase verified successfully")
                    Result.success()
                } else {
                    logger.debug("Amazon purchase verification failed")
                    Result.failure()
                }
            } catch (e: Exception) {
                logger.debug(e.message)
                Result.failure()
            }

        private suspend fun verifyPurchase(amazonPurchase: AmazonPurchase): Boolean {
            logger.debug("Verifying amazon receipt: ${amazonPurchase.receiptId}")

            val receipt =
                ReceiptParams(
                    purchaseToken = amazonPurchase.receiptId,
                    type = BillingConstants.AMAZON_PURCHASE_TYPE,
                    amazonUserId = amazonPurchase.userId,
                )

            // Use PurchaseManager - single source of truth!
            var success = false
            purchaseManager.completePurchase(receipt).collect { state ->
                when (state) {
                    is UserDataState.Success -> {
                        logger.info("Amazon purchase verified successfully")
                        success = true
                    }
                    is UserDataState.Error -> {
                        logger.debug("Amazon verification failed: ${state.error}")
                        success = false
                    }
                    else -> {}
                }
            }

            return success
        }

        private fun getPendingAmazonPurchase(): AmazonPurchase {
            val json =
                preferencesHelper.amazonPurchasedItem
                    ?: throw WindScribeException("No amazon purchase found.")
            try {
                return Gson().fromJson(json, AmazonPurchase::class.java)
            } catch (jsonException: JsonSyntaxException) {
                throw WindScribeException("Fatal error: Invalid purchase response saved.")
            }
        }
    }
