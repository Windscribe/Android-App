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
import com.windscribe.vpn.apppreference.PreferencesHelper
import com.windscribe.vpn.billing.PurchaseManager
import com.windscribe.vpn.billing.ReceiptParams
import com.windscribe.vpn.exceptions.WindScribeException
import com.windscribe.vpn.repository.UserDataState
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
        private val purchaseManager: PurchaseManager,
        private val preferencesHelper: PreferencesHelper,
    ) : CoroutineWorker(appContext, params) {
        private val logger = LoggerFactory.getLogger("billing")
        private var billingClient: BillingClient? = null

        override suspend fun doWork(): Result =
            try {
                initBillingClient()
                logger.debug("Getting product history.")
                val purchases = getProductHistory()
                val allVerified =
                    purchases.all { purchase ->
                        verifyPurchase(purchase)
                    }

                if (allVerified) {
                    logger.debug("All purchases verified successfully")
                    Result.success()
                } else {
                    logger.debug("Some purchases failed verification")
                    Result.failure()
                }
            } catch (e: Exception) {
                logger.debug(e.message)
                Result.failure()
            }

        private suspend fun verifyPurchase(purchase: Purchase): Boolean {
            logger.info("Verifying purchase: ${purchase.purchaseToken}")
            tryToAcknowledgeProduct(purchase)
            val receipt =
                ReceiptParams(
                    purchaseToken = purchase.purchaseToken,
                    gpPackageName = "com.windscribe.vpn",
                    gpProductId = purchase.products[0],
                )
            var success = false
            purchaseManager.completePurchase(receipt).collect { state ->
                when (state) {
                    is UserDataState.Success -> {
                        logger.info("Purchase verified successfully")
                        success = true
                    }
                    is UserDataState.Error -> {
                        logger.debug("Purchase verification failed: ${state.error}")
                        success = false
                    }
                    else -> {}
                }
            }

            return success
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
                            it.resumeWith(kotlin.Result.failure(WindScribeException("No purchase history found.")))
                        } else {
                            it.resumeWith(kotlin.Result.success(purchasesList))
                        }
                    } else {
                        it.resumeWith(kotlin.Result.failure(WindScribeException(billingResult.debugMessage)))
                    }
                }
            }
    }
