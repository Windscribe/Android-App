/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.billing

import android.app.Application
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClient.BillingResponseCode.OK
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ConsumeParams
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.windscribe.vpn.constants.BillingConstants
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import org.slf4j.LoggerFactory

class GoogleBillingManager(
    private val app: Application,
) : PurchasesUpdatedListener,
    DefaultLifecycleObserver {
    /**
     * One-shot purchase events. Backed by a zero-replay SharedFlow so they are not
     * re-delivered on configuration change, mirroring the old SingleLiveEvent behavior.
     */
    private val _onBillingSetUpSuccess = MutableSharedFlow<Int>(replay = 0, extraBufferCapacity = 1)
    val onBillingSetUpSuccess: SharedFlow<Int> = _onBillingSetUpSuccess.asSharedFlow()

    private val _onBillingSetupFailure = MutableSharedFlow<Int>(replay = 0, extraBufferCapacity = 1)
    val onBillingSetupFailure: SharedFlow<Int> = _onBillingSetupFailure.asSharedFlow()

    private val _onProductConsumeFailure = MutableSharedFlow<CustomPurchase>(replay = 0, extraBufferCapacity = 1)
    val onProductConsumeFailure: SharedFlow<CustomPurchase> = _onProductConsumeFailure.asSharedFlow()

    private val _onProductConsumeSuccess = MutableSharedFlow<Purchase>(replay = 0, extraBufferCapacity = 1)
    val onProductConsumeSuccess: SharedFlow<Purchase> = _onProductConsumeSuccess.asSharedFlow()

    private val _purchaseUpdateEvent = MutableSharedFlow<CustomPurchases>(replay = 0, extraBufferCapacity = 1)
    val purchaseUpdateEvent: SharedFlow<CustomPurchases> = _purchaseUpdateEvent.asSharedFlow()

    private val _querySkuDetailEvent = MutableSharedFlow<CustomProductDetails>(replay = 0, extraBufferCapacity = 1)
    val querySkuDetailEvent: SharedFlow<CustomProductDetails> = _querySkuDetailEvent.asSharedFlow()

    private val logger = LoggerFactory.getLogger("billing")

    private lateinit var mBillingClient: BillingClient

    fun InAppConsume(purchase: Purchase) {
        val consumeResponseListener = { billingResult: BillingResult, _: String ->
            if (billingResult.responseCode == OK) {
                _onProductConsumeSuccess.tryEmit(purchase)
            } else {
                _onProductConsumeFailure.tryEmit(CustomPurchase(billingResult.responseCode, purchase))
            }
            Unit
        }
        val consumeParams =
            ConsumeParams
                .newBuilder()
                .setPurchaseToken(purchase.purchaseToken)
                .build()
        mBillingClient.consumeAsync(consumeParams, consumeResponseListener)
    }

    override fun onCreate(owner: LifecycleOwner) {
        // Create a new BillingClient in onCreate().
        // Since the BillingClient can only be used once, we need to create a new instance
        // after ending the previous connection to the Google Play Store in onDestroy().
        mBillingClient =
            BillingClient
                .newBuilder(app)
                .setListener(this)
                .enablePendingPurchases() // Not used for subscriptions.
                .build()
        if (!mBillingClient.isReady) {
            mBillingClient.startConnection(
                object : BillingClientStateListener {
                    override fun onBillingServiceDisconnected() {
                        _onBillingSetupFailure.tryEmit(BillingConstants.PLAY_STORE_UPDATE)
                    }

                    override fun onBillingSetupFinished(billingResult: BillingResult) {
                        val responseCode = billingResult.responseCode
                        if (responseCode == OK) {
                            _onBillingSetUpSuccess.tryEmit(responseCode)
                            getRecentPurchases()
                        } else {
                            _onBillingSetupFailure.tryEmit(responseCode)
                        }
                    }
                },
            )
        }
    }

    override fun onDestroy(owner: LifecycleOwner) {
        if (mBillingClient.isReady) {
            // BillingClient can only be used once.
            // After calling endConnection(), we must create a new BillingClient.
            mBillingClient.endConnection()
        }
    }

    fun launchBillingFlow(
        mActivity: AppCompatActivity,
        params: BillingFlowParams,
    ) {
        mBillingClient.launchBillingFlow(mActivity, params)
    }

    override fun onPurchasesUpdated(
        billingResult: BillingResult,
        purchases: List<Purchase>?,
    ) {
        if (purchases == null) {
            _purchaseUpdateEvent.tryEmit(CustomPurchases(billingResult.responseCode, ArrayList()))
        } else {
            _purchaseUpdateEvent.tryEmit(CustomPurchases(billingResult.responseCode, purchases))
        }
    }

    fun querySkuDetailsAsync(subs: List<QueryProductDetailsParams.Product>) {
        val productDetailsParams =
            QueryProductDetailsParams
                .newBuilder()
                .setProductList(subs)
                .build()
        mBillingClient.queryProductDetailsAsync(productDetailsParams) { billingResult, list ->
            _querySkuDetailEvent.tryEmit(CustomProductDetails(billingResult, list))
        }
    }

    fun subscriptionConsume(purchase: Purchase) {
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            if (!purchase.isAcknowledged) {
                logger.info("Trying to Consume a subscription")
                val acknowledgePurchaseParams =
                    AcknowledgePurchaseParams
                        .newBuilder()
                        .setPurchaseToken(purchase.purchaseToken)
                        .build()
                mBillingClient.acknowledgePurchase(acknowledgePurchaseParams) { billingResult ->
                    if (billingResult.responseCode == OK) {
                        _onProductConsumeSuccess.tryEmit(purchase)
                    } else {
                        _onProductConsumeFailure.tryEmit(CustomPurchase(billingResult.responseCode, purchase))
                    }
                }
            } else {
                logger.info("Already consumed purchase.")
            }
        } else {
            logger.info("Purchase state error: state:" + purchase.purchaseState)
            _onProductConsumeSuccess.tryEmit(purchase)
        }
    }

    private fun getRecentPurchases() {
        mBillingClient.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.SUBS).build(),
        ) { billingResult, purchases ->
            if (billingResult.responseCode == OK) {
                if (purchases.isNotEmpty()) {
                    logger.info("Existing purchase found.")
                    subscriptionConsume(purchases[0])
                } else {
                    logger.info("No subscription history found.")
                }
            }
        }
        mBillingClient.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build(),
        ) { billingResult, purchases ->
            if (billingResult.responseCode == OK) {
                if (purchases.isNotEmpty()) {
                    logger.info("Existing purchase found.")
                    InAppConsume(purchases[0])
                } else {
                    logger.info("No one purchase history found.")
                }
            }
        }
    }
}
