package com.windscribe.vpn.workers.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.AcknowledgePurchaseResponseListener
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchaseHistoryRecord
import com.windscribe.vpn.Windscribe
import com.windscribe.vpn.api.ApiCallManager
import com.windscribe.vpn.api.response.GenericSuccess
import com.windscribe.vpn.apppreference.PreferencesHelper
import com.windscribe.vpn.billing.PurchaseState
import com.windscribe.vpn.commonutils.Ext.result
import com.windscribe.vpn.exceptions.WindScribeException
import com.windscribe.vpn.repository.CallResult
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import kotlin.coroutines.suspendCoroutine

class GooglePendingReceiptValidator(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {
    private val logger = LoggerFactory.getLogger("billing")

    @Inject
    lateinit var apiManager: ApiCallManager
    @Inject
    lateinit var preferencesHelper: PreferencesHelper
    private var billingClient: BillingClient? = null

    init {
        Windscribe.appContext.applicationComponent.inject(this)
    }

    override suspend fun doWork(): Result {
        val state = preferencesHelper.purchaseFlowState
        if (state == PurchaseState.FINISHED.name) {
            return Result.success()
        }
        return try {
            val result = initBillingClient().takeIf { true }.run {
                logger.debug("Getting product history.")
                getProductHistory().map {
                    logger.debug("Consuming product.")
                    tryToAcknowledgeProduct(it)
                }.map {
                    logger.debug("Verifying product.")
                    verifyPayment(it)
                }.toList().first { true }
            }
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

    private suspend fun initBillingClient() = suspendCoroutine<Boolean> { continuation ->
        billingClient = BillingClient.newBuilder(applicationContext)
                .setListener { _: BillingResult?, _: List<Purchase?>? -> logger.debug("Purchase flow: Purchases updated") }
                .enablePendingPurchases().build()
        val resumed = AtomicBoolean(false)
        billingClient?.startConnection(object : BillingClientStateListener {
            override fun onBillingServiceDisconnected() {
                if (!resumed.getAndSet(true)) {
                    continuation.resumeWith(kotlin.Result.failure(WindScribeException("Billing client disconnected")))
                }
            }

            override fun onBillingSetupFinished(billingResult: BillingResult) {
                logger.debug("Billing client setup was successful.")
                if (BillingClient.BillingResponseCode.OK == billingResult.responseCode) {
                    logger.debug("Getting list of purchased products")
                    if (!resumed.getAndSet(true)) {
                        continuation.resumeWith(kotlin.Result.success(true))
                    }
                } else {
                    if (!resumed.getAndSet(true)) {
                        continuation.resumeWith(kotlin.Result.failure(WindScribeException("Billing client setup failed code:" + billingResult.responseCode)))
                    }
                }
            }
        })
    }

    private suspend fun tryToAcknowledgeProduct(purchase: PurchaseHistoryRecord) = suspendCoroutine<PurchaseHistoryRecord> {
        val acknowledgePurchaseResponseListener =
                AcknowledgePurchaseResponseListener { billingResult: BillingResult ->
                    if (BillingClient.BillingResponseCode.OK == billingResult.responseCode || BillingClient.BillingResponseCode.ITEM_NOT_OWNED == billingResult.responseCode) {
                        logger.debug("Purchase flow: Purchase was successfully consumed: ${billingResult.debugMessage}")
                        it.resumeWith(kotlin.Result.success(purchase))
                    } else {
                        it.resumeWith(kotlin.Result.failure(WindScribeException("Acknowledgement Failure: ${billingResult.debugMessage}")))
                    }
                }
        val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
                .setPurchaseToken(purchase.purchaseToken).build()
        billingClient?.acknowledgePurchase(acknowledgePurchaseParams, acknowledgePurchaseResponseListener)
    }

    private suspend fun getProductHistory() = suspendCoroutine<List<PurchaseHistoryRecord>> {
        billingClient?.queryPurchaseHistoryAsync(BillingClient.SkuType.SUBS) { billingResult: BillingResult, purchasesList: List<PurchaseHistoryRecord>? ->
            if (BillingClient.BillingResponseCode.OK == billingResult.responseCode) {
                if (purchasesList == null || purchasesList.isEmpty()) {
                    preferencesHelper.savePurchaseFlowState(PurchaseState.FINISHED.name)
                    it.resumeWith(kotlin.Result.failure(WindScribeException("No purchase history found.")))
                } else {
                    it.resumeWith(kotlin.Result.success(purchasesList))
                }
            } else {
                it.resumeWith(kotlin.Result.failure(WindScribeException(billingResult.debugMessage)))
            }
        }
    }


    private suspend fun verifyPayment(itemPurchased: PurchaseHistoryRecord): Boolean {
        logger.info("Verifying payment for purchased item: " + itemPurchased.originalJson)
        return when (val result = result<GenericSuccess> {
            apiManager.verifyPurchaseReceipt(itemPurchased.purchaseToken, "com.windscribe.vpn", itemPurchased.products[0], "", "")
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
