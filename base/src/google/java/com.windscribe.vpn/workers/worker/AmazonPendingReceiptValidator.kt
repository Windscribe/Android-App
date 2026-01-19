package com.windscribe.vpn.workers.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.windscribe.vpn.Windscribe
import com.windscribe.vpn.api.IApiCallManager
import com.windscribe.vpn.api.response.GenericSuccess
import com.windscribe.vpn.apppreference.PreferencesHelper
import com.windscribe.vpn.billing.AmazonPurchase
import com.windscribe.vpn.billing.PurchaseState
import com.windscribe.vpn.commonutils.Ext.result
import com.windscribe.vpn.constants.BillingConstants
import com.windscribe.vpn.exceptions.WindScribeException
import com.windscribe.vpn.repository.CallResult
import org.slf4j.LoggerFactory
import javax.inject.Inject

class AmazonPendingReceiptValidator(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {
    private val logger = LoggerFactory.getLogger("billing")
    @Inject
    lateinit var apiManager: IApiCallManager
    @Inject
    lateinit var preferencesHelper: PreferencesHelper

    init {
        Windscribe.appContext.applicationComponent.inject(this)
    }

    override suspend fun doWork(): Result {
        val state = preferencesHelper.purchaseFlowState
        if (state == PurchaseState.FINISHED.name) {
            return Result.success()
        }
        return try {
            val result = verifyPayment(getPendingAmazonPurchase())
            return if (result) {
                logger.debug("Successfully verified purchase receipt")
                preferencesHelper.savePurchaseFlowState(PurchaseState.FINISHED.name)
                Windscribe.appContext.workManager.updateSession()
                Result.success()
            } else {
                logger.debug("Failure to verify receipt")
                Result.failure()
            }
        } catch (e: Exception) {
            logger.debug(e.message)
            Result.failure()
        }
    }

    private fun getPendingAmazonPurchase(): AmazonPurchase {
        val json = preferencesHelper.amazonPurchasedItem
                ?: throw WindScribeException("No amazon purchase found.")
        try {
            return Gson().fromJson(json, AmazonPurchase::class.java)
        } catch (jsonException: JsonSyntaxException) {
            throw WindScribeException("Fatal error: Invalid purchase response saved.")
        }
    }

    private suspend fun verifyPayment(amazonPurchase: AmazonPurchase): Boolean {
        logger.debug("Verifying amazon receipt.")
        return when (val result = result<GenericSuccess> {
            apiManager.verifyPurchaseReceipt(amazonPurchase.receiptId, "", "", BillingConstants.AMAZON_PURCHASE_TYPE, amazonPurchase.userId)
        }) {
            is CallResult.Error -> {
                logger.debug("Payment verification failed: ${result.errorMessage}")
                false
            }
            is CallResult.Success -> {
                logger.info("Payment verification successful.")
                true
            }
        }
    }
}