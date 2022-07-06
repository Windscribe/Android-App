/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.billing;

import static com.android.billingclient.api.BillingClient.BillingResponseCode.OK;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;

import com.android.billingclient.api.AcknowledgePurchaseParams;
import com.android.billingclient.api.AcknowledgePurchaseResponseListener;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.ConsumeParams;
import com.android.billingclient.api.ConsumeResponseListener;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.SkuDetailsParams;
import com.android.billingclient.api.SkuDetailsResponseListener;
import com.windscribe.vpn.constants.BillingConstants;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;


public class GoogleBillingManager implements PurchasesUpdatedListener, LifecycleObserver {

    private static volatile GoogleBillingManager INSTANCE;

    /**
     * The purchase event is observable. Only one observer will be notified.
     */
    public final SingleLiveEvent<Integer> onBillingSetUpSuccess = new SingleLiveEvent<>();

    public final SingleLiveEvent<Integer> onBillingSetupFailure = new SingleLiveEvent<>();

    public final SingleLiveEvent<CustomPurchase> onProductConsumeFailure = new SingleLiveEvent<>();

    public final SingleLiveEvent<Purchase> onProductConsumeSuccess = new SingleLiveEvent<>();

    public final SingleLiveEvent<CustomPurchases> purchaseUpdateEvent = new SingleLiveEvent<>();

    public final SingleLiveEvent<CustomSkuDetails> querySkuDetailEvent = new SingleLiveEvent<>();

    private final Application app;

    private final Logger logger = LoggerFactory.getLogger("Billing manager");

    // private BillingManagerResponseListener mListener;
    private BillingClient mBillingClient;

    public GoogleBillingManager(Application app) {
        this.app = app;
    }

    public static GoogleBillingManager getInstance(Application app) {
        if (INSTANCE == null) {
            synchronized (GoogleBillingManager.class) {
                if (INSTANCE == null) {
                    INSTANCE = new GoogleBillingManager(app);
                }
            }
        }
        return INSTANCE;
    }

    public void InAppConsume(Purchase purchase) {
        ConsumeResponseListener consumeResponseListener = (billingResult, s) -> {
            if (billingResult.getResponseCode() == OK) {
                onProductConsumeSuccess.postValue(purchase);
            } else {
                onProductConsumeFailure.postValue(new CustomPurchase(billingResult.getResponseCode(), purchase));
            }
        };
        ConsumeParams consumeParams = ConsumeParams.newBuilder()
                .setPurchaseToken(purchase.getPurchaseToken())
                .build();
        mBillingClient.consumeAsync(consumeParams, consumeResponseListener);
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
    public void create() {
        // Create a new BillingClient in onCreate().
        // Since the BillingClient can only be used once, we need to create a new instance
        // after ending the previous connection to the Google Play Store in onDestroy().
        mBillingClient = BillingClient.newBuilder(app)
                .setListener(this)
                .enablePendingPurchases() // Not used for subscriptions.
                .build();
        if (!mBillingClient.isReady()) {
            mBillingClient.startConnection(new BillingClientStateListener() {
                @Override
                public void onBillingServiceDisconnected() {
                    //Service disconnected
                    onBillingSetupFailure.postValue(BillingConstants.PLAY_STORE_UPDATE);
                }

                @Override
                public void onBillingSetupFinished(@NonNull BillingResult billingResult) {
                    int responseCode = billingResult.getResponseCode();
                    if (responseCode == OK) {
                        onBillingSetUpSuccess.postValue(responseCode);
                        getRecentPurchases();
                    } else {
                        onBillingSetupFailure.postValue(responseCode);
                    }
                }
            });
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    public void destroy() {
        if (mBillingClient.isReady()) {
            // BillingClient can only be used once.
            // After calling endConnection(), we must create a new BillingClient.
            mBillingClient.endConnection();
        }
    }

    public void launchBillingFlow(AppCompatActivity mActivity, BillingFlowParams params) {
        mBillingClient.launchBillingFlow(mActivity, params);
    }

    @Override
    public void onPurchasesUpdated(@NonNull BillingResult billingResult, @Nullable List<Purchase> purchases) {
        purchaseUpdateEvent.postValue(new CustomPurchases(billingResult.getResponseCode(), purchases));
    }

    public void querySkuDetailsAsync(final List<String> subs, boolean isSub) {
        SkuDetailsParams subsDetailsParams = SkuDetailsParams.newBuilder()
                .setSkusList(subs).setType(BillingClient.SkuType.SUBS).build();
        SkuDetailsParams inAppsDetailsParams = SkuDetailsParams.newBuilder()
                .setSkusList(subs).setType(BillingClient.SkuType.INAPP).build();

        SkuDetailsResponseListener skuDetailsResponseListener = (billingResult, list) -> querySkuDetailEvent
                .postValue(new CustomSkuDetails(billingResult, list));
        if (isSub) {
            mBillingClient.querySkuDetailsAsync(subsDetailsParams, skuDetailsResponseListener);
        } else {
            mBillingClient.querySkuDetailsAsync(inAppsDetailsParams, skuDetailsResponseListener);
        }
    }

    public void subscriptionConsume(Purchase purchase) {
        AcknowledgePurchaseResponseListener acknowledgePurchaseResponseListener = billingResult -> {
            if (billingResult.getResponseCode() == OK) {
                onProductConsumeSuccess.postValue(purchase);
            } else {
                onProductConsumeFailure.postValue(new CustomPurchase(billingResult.getResponseCode(), purchase));
            }
        };
        if (purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED) {
            if (!purchase.isAcknowledged()) {
                logger.info("Trying to Consume a subscription");
                AcknowledgePurchaseParams acknowledgePurchaseParams =
                        AcknowledgePurchaseParams.newBuilder()
                                .setPurchaseToken(purchase.getPurchaseToken())
                                .build();
                mBillingClient.acknowledgePurchase(acknowledgePurchaseParams, acknowledgePurchaseResponseListener);
            } else {
                logger.info("Already consumed purchase.");
            }
        } else {
            logger.info("Purchase state error: state:" + purchase.getPurchaseState());
            onProductConsumeSuccess.postValue(purchase);
        }
    }

    private void getRecentPurchases() {
        Purchase.PurchasesResult subsPurchaseResult = mBillingClient.queryPurchases(BillingClient.SkuType.SUBS);
        if (subsPurchaseResult.getResponseCode() == OK) {
            List<Purchase> purchases = subsPurchaseResult.getPurchasesList();
            if (purchases != null && purchases.size() > 0) {
                logger.info("Existing purchase found.");
                subscriptionConsume(purchases.get(0));
            } else {
                logger.info("No subscription history found.");
            }
        }

        Purchase.PurchasesResult inAppPurchaseResult = mBillingClient.queryPurchases(BillingClient.SkuType.INAPP);
        if (subsPurchaseResult.getResponseCode() == OK) {
            List<Purchase> purchases = inAppPurchaseResult.getPurchasesList();
            if (purchases != null && purchases.size() > 0) {
                logger.info("Existing purchase found.");
                InAppConsume(purchases.get(0));
            } else {
                logger.info("No InApp history found.");
            }
        }
    }
}
