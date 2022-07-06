/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.services.verify

import android.content.Context
import android.content.Intent
import androidx.core.app.JobIntentWorkAroundService
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.AcknowledgePurchaseResponseListener
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClient.BillingResponseCode
import com.android.billingclient.api.BillingClient.SkuType
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchaseHistoryRecord
import com.windscribe.vpn.ServiceInteractor
import com.windscribe.vpn.Windscribe.Companion.appContext
import com.windscribe.vpn.api.response.ApiErrorResponse
import com.windscribe.vpn.api.response.GenericResponseClass
import com.windscribe.vpn.billing.PurchaseState.FINISHED
import com.windscribe.vpn.constants.BillingConstants
import io.reactivex.observers.DisposableSingleObserver
import io.reactivex.schedulers.Schedulers
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import org.json.JSONException
import org.json.JSONObject
import org.slf4j.LoggerFactory

class VerifyGooglePurchaseService : JobIntentWorkAroundService() {

    @Inject
    lateinit var interactor: ServiceInteractor
    private var client: BillingClient? = null
    private val stateBoolean = AtomicBoolean()
    private val logger = LoggerFactory.getLogger(TAG)
    override fun onCreate() {
        super.onCreate()
        stateBoolean.set(true)
        appContext.serviceComponent.inject(this)
    }

    override fun onHandleWork(intent: Intent) {
        initBillingClient()
    }

    private fun initBillingClient() {
        val state = interactor.preferenceHelper.purchaseFlowState
        if (state == FINISHED.name) {
            // No pending purchase  flow
            stopSelf()
            return
        }
        client = BillingClient.newBuilder(this)
                .setListener { _: BillingResult?, _: List<Purchase?>? -> logger.debug("Purchase flow: Purchases updated") }
                .enablePendingPurchases().build()
        client?.startConnection(object : BillingClientStateListener {
            override fun onBillingServiceDisconnected() {
                logger.debug("Purchase flow: Billing client disconnected")
                stopSelf()
            }

            override fun onBillingSetupFinished(billingResult: BillingResult) {
                logger.debug("Purchase flow: Billing client setup was successful.")
                if (BillingResponseCode.OK == billingResult.responseCode) {
                    logger.debug("Purchase flow: Getting list of purchased products")
                    // Get list of latest purchases
                    historyProducts
                } else {
                    logger.debug(
                            "Purchase flow: Billing client setup failed with code:" + billingResult
                                    .responseCode
                    )
                    stopSelf()
                }
            }
        })
    }

    private fun consumeProduct(purchase: PurchaseHistoryRecord) {
        val acknowledgePurchaseResponseListener =
                AcknowledgePurchaseResponseListener { billingResult: BillingResult ->
                    if (BillingResponseCode.OK == billingResult.responseCode || BillingResponseCode.ITEM_NOT_OWNED == billingResult.responseCode) {
                        logger.debug(
                                "Purchase flow: Purchase was successfully consumed for tokenId:" + purchase
                                        .purchaseToken + " " + billingResult.responseCode
                        )
                        verifyPurchase(purchase)
                    } else {
                        // Any other reason reset state
                        interactor.preferenceHelper
                                .savePurchaseFlowState(FINISHED.name)
                        logger
                                .debug(
                                        "Purchase flow: Purchase failed to consume for tokenId:" + purchase.purchaseToken +
                                                " with code:" + billingResult.responseCode
                                )
                        stopSelf()
                    }
                }
        val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
                .setPurchaseToken(purchase.purchaseToken).build()
        client?.acknowledgePurchase(acknowledgePurchaseParams, acknowledgePurchaseResponseListener)
    } // Try to consume these purchase

    // No purchase history found. Reset state
    private val historyProducts: Unit
        get() {
            client?.queryPurchaseHistoryAsync(SkuType.SUBS) { billingResult: BillingResult, purchasesList: List<PurchaseHistoryRecord>? ->
                if (BillingResponseCode.OK == billingResult.responseCode) {
                    if (purchasesList == null || purchasesList.isEmpty()) {
                        // No purchase history found. Reset state
                        logger.debug("Purchase flow: no recent purchase found, resetting state.")
                        interactor.preferenceHelper
                                .savePurchaseFlowState(FINISHED.name)
                        stopSelf()
                    } else {
                        // Try to consume these purchase
                        logger.debug("Purchase flow: token found:" + purchasesList.size)
                        for (i in purchasesList.indices) {
                            consumeProduct(purchasesList[i])
                        }
                    }
                } else {
                    logger
                            .debug(
                                    "Purchase flow: Error occurred during history request with code:" + billingResult
                                            .responseCode
                            )
                    stopSelf()
                }
            }
        }

    private fun verifyPurchase(itemPurchased: PurchaseHistoryRecord) {
        logger.debug(
                "Purchase flow: Starting verify purchase service for orderId:" + itemPurchased
                        .purchaseToken
        )
        logger.info("Purchase flow: Verifying payment for purchased item: " + itemPurchased.originalJson)
        val purchaseMap = HashMap<String, String>()
        purchaseMap[BillingConstants.GP_PACKAGE_NAME] = "com.windscribe.vpn"
        purchaseMap[BillingConstants.GP_PRODUCT_ID] = itemPurchased.sku
        purchaseMap[BillingConstants.PURCHASE_TOKEN] = itemPurchased.purchaseToken
        interactor.compositeDisposable.add(
                interactor.apiManager
                        .verifyPayment(purchaseMap)
                        .subscribeOn(Schedulers.io())
                        .observeOn(Schedulers.io())
                        .subscribeWith(
                                object : DisposableSingleObserver<GenericResponseClass<String?, ApiErrorResponse?>?>() {
                                    override fun onError(e: Throwable) {
                                        logger
                                                .debug("Purchase flow: Payment verification failed. resetting state")
                                        interactor.preferenceHelper.savePurchaseFlowState(
                                                FINISHED.name
                                        )
                                        stopSelf()
                                    }

                                    override fun onSuccess(
                                            paymentVerificationResponse: GenericResponseClass<String?, ApiErrorResponse?>
                                    ) {
                                        if (paymentVerificationResponse.dataClass != null) {

                                            // Note: change this for check for http error code 400
                                            // Check for token already verified
                                            try {
                                                val res = JSONObject(paymentVerificationResponse.dataClass!!)
                                                logger.debug(res.toString())
                                                val error = res.getInt("errorCode")
                                                if (error == 4005) {
                                                    logger
                                                            .debug("Purchase flow: Token was already verified once. Ignore")
                                                    interactor.preferenceHelper.savePurchaseFlowState(
                                                            FINISHED.name
                                                    )
                                                    return
                                                }
                                            } catch (e: JSONException) {
                                                e.printStackTrace()
                                            }

                                            // Now its time to upgrade.
                                            logger
                                                    .info("Purchase flow: Payment verification successful. ")
                                            interactor.preferenceHelper.savePurchaseFlowState(
                                                    FINISHED.name
                                            )
                                            appContext.workManager.updateSession()
                                            stopSelf()
                                        } else if (paymentVerificationResponse.errorClass != null) {
                                            logger
                                                    .debug(
                                                            "Purchase flow: Payment verification failed. Server error response..." +
                                                                    paymentVerificationResponse.errorClass
                                                                            .toString()
                                                    )
                                            interactor.preferenceHelper.savePurchaseFlowState(
                                                    FINISHED.name
                                            )
                                            stopSelf()
                                        }
                                    }
                                })
        )
    }

    companion object {

        const val TAG = "verify_purchase"
        private const val PURCHASE_JOB_ID = 4221

        @JvmStatic
        fun enqueueWork(context: Context, intent: Intent) {
            try {
                enqueueWork(context, VerifyGooglePurchaseService::class.java, PURCHASE_JOB_ID, intent)
            } catch (ignored: IllegalArgumentException) {
                // Lava Device exception!!
            }
        }
    }
}
