package com.windscribe.vpn.workers.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.AcknowledgePurchaseResponseListener
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.QueryPurchasesParams
import com.windscribe.vpn.Windscribe
import com.windscribe.vpn.api.ApiCallManager
import com.windscribe.vpn.api.response.GenericSuccess
import com.windscribe.vpn.apppreference.PreferencesHelper
import com.windscribe.vpn.billing.PurchaseState
import com.windscribe.vpn.commonutils.Ext.result
import com.windscribe.vpn.exceptions.WindScribeException
import com.windscribe.vpn.repository.CallResult
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.suspendCancellableCoroutine
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicBoolean

@HiltWorker
class GooglePendingReceiptValidator
    @AssistedInject
    constructor(
        @Assisted appContext: Context,
        @Assisted params: WorkerParameters,
        private val apiManager: ApiCallManager,
        private val preferencesHelper: PreferencesHelper,
    ) : CoroutineWorker(appContext, params) {
        private val logger = LoggerFactory.getLogger("billing")
        private var billingClient: BillingClient? = null

        override suspend fun doWork(): Result {
            val state = preferencesHelper.purchaseFlowState
            if (state == PurchaseState.FINISHED.name) {
                return Result.success()
            }
            return try {
                val result =
                    initBillingClient().takeIf { true }.run {
                        logger.debug("Getting product history.")
                        getProductHistory()
                            .map {
                                logger.debug("Consuming product.")
                                tryToAcknowledgeProduct(it)
                            }.map {
                                logger.debug("Verifying product.")
                                verifyPayment(it)
                            }.toList()
                            .first { true }
                    }
                return if (result) {
                    logger.debug("Successfully verified purchase receipt")
                    preferencesHelper.purchaseFlowState = PurchaseState.FINISHED.name
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

        private suspend fun initBillingClient() =
            suspendCancellableCoroutine { continuation ->
                billingClient =
                    BillingClient
                        .newBuilder(applicationContext)
                        .setListener { _: BillingResult?, _: List<Purchase?>? -> logger.debug("Purchase flow: Purchases updated") }
                        .enablePendingPurchases(
                            PendingPurchasesParams.newBuilder().enableOneTimeProducts().build(),
                        ).build()
                val resumed = AtomicBoolean(false)
                billingClient?.startConnection(
                    object : BillingClientStateListener {
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
                                    continuation.resumeWith(
                                        kotlin.Result.failure(
                                            WindScribeException("Billing client setup failed code:" + billingResult.responseCode),
                                        ),
                                    )
                                }
                            }
                        }
                    },
                )
            }

        private suspend fun tryToAcknowledgeProduct(purchase: Purchase) =
            suspendCancellableCoroutine {
                val acknowledgePurchaseResponseListener =
                    AcknowledgePurchaseResponseListener { billingResult: BillingResult ->
                        if (BillingClient.BillingResponseCode.OK == billingResult.responseCode ||
                            BillingClient.BillingResponseCode.ITEM_NOT_OWNED == billingResult.responseCode
                        ) {
                            logger.debug("Purchase flow: Purchase was successfully consumed: ${billingResult.debugMessage}")
                            it.resumeWith(kotlin.Result.success(purchase))
                        } else {
                            it.resumeWith(
                                kotlin.Result.failure(WindScribeException("Acknowledgement Failure: ${billingResult.debugMessage}")),
                            )
                        }
                    }
                val acknowledgePurchaseParams =
                    AcknowledgePurchaseParams
                        .newBuilder()
                        .setPurchaseToken(purchase.purchaseToken)
                        .build()
                billingClient?.acknowledgePurchase(
                    acknowledgePurchaseParams,
                    acknowledgePurchaseResponseListener,
                )
            }

        private suspend fun getProductHistory() =
            suspendCancellableCoroutine {
                val params =
                    QueryPurchasesParams
                        .newBuilder()
                        .setProductType(BillingClient.ProductType.SUBS)
                        .build()
                billingClient?.queryPurchasesAsync(params) { billingResult: BillingResult, purchasesList: List<Purchase> ->
                    if (BillingClient.BillingResponseCode.OK == billingResult.responseCode) {
                        if (purchasesList.isEmpty()) {
                            preferencesHelper.purchaseFlowState = PurchaseState.FINISHED.name
                            it.resumeWith(kotlin.Result.failure(WindScribeException("No purchase history found.")))
                        } else {
                            it.resumeWith(kotlin.Result.success(purchasesList))
                        }
                    } else {
                        it.resumeWith(kotlin.Result.failure(WindScribeException(billingResult.debugMessage)))
                    }
                }
            }

        private suspend fun verifyPayment(itemPurchased: Purchase): Boolean {
            logger.info("Verifying payment for purchased item: " + itemPurchased.originalJson)
            return when (
                val result =
                    result<GenericSuccess> {
                        apiManager.verifyPurchaseReceipt(
                            itemPurchased.purchaseToken,
                            "com.windscribe.vpn",
                            itemPurchased.products[0],
                            "",
                            "",
                        )
                    }
            ) {
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
