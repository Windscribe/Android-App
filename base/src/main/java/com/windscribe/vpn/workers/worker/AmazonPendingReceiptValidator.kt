package com.windscribe.vpn.workers.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.windscribe.vpn.ServiceInteractor
import com.windscribe.vpn.Windscribe
import com.windscribe.vpn.api.response.GenericSuccess
import com.windscribe.vpn.billing.AmazonPurchase
import com.windscribe.vpn.billing.PurchaseState
import com.windscribe.vpn.commonutils.Ext.result
import com.windscribe.vpn.constants.BillingConstants
import com.windscribe.vpn.exceptions.WindScribeException
import com.windscribe.vpn.repository.CallResult
import org.slf4j.LoggerFactory
import javax.inject.Inject

class AmazonPendingReceiptValidator(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {
    private val logger = LoggerFactory.getLogger("amazon_receipt_w")
    @Inject
    lateinit var interactor: ServiceInteractor

    init {
        Windscribe.appContext.applicationComponent.inject(this)
    }

    override suspend fun doWork(): Result {
        val state = interactor.preferenceHelper.purchaseFlowState
        if (state == PurchaseState.FINISHED.name) {
            return Result.success()
        }
        return try {
            val result = verifyPayment(getPendingAmazonPurchase())
            return if (result) {
                logger.debug("Successfully verified purchase receipt")
                interactor.preferenceHelper.savePurchaseFlowState(PurchaseState.FINISHED.name)
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
        val json = interactor.preferenceHelper.getResponseString(BillingConstants.AMAZON_PURCHASED_ITEM)
                ?: throw WindScribeException("No amazon purchase found.")
        try {
            return Gson().fromJson(json, AmazonPurchase::class.java)
        } catch (jsonException: JsonSyntaxException) {
            throw WindScribeException("Fatal error: Invalid purchase response saved.")
        }
    }

    private suspend fun verifyPayment(amazonPurchase: AmazonPurchase): Boolean {
        logger.debug("Verifying amazon receipt.")
        val purchaseMap = HashMap<String, String>()
        purchaseMap[BillingConstants.PURCHASE_TOKEN] = amazonPurchase.receiptId
        purchaseMap[BillingConstants.PURCHASE_TYPE] = BillingConstants.AMAZON_PURCHASE_TYPE
        purchaseMap[BillingConstants.AMAZON_USER_ID] = amazonPurchase.userId
        logger.info(purchaseMap.toString())
        return when (val result = interactor.apiManager.verifyPurchaseReceipt(purchaseMap).result<GenericSuccess>()) {
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