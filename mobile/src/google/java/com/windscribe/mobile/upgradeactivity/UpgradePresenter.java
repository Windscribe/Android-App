/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.mobile.upgradeactivity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.amazon.device.iap.model.Product;
import com.amazon.device.iap.model.PurchaseResponse;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.ProductDetails;
import com.android.billingclient.api.Purchase;
import com.google.common.collect.ImmutableList;
import com.windscribe.vpn.api.response.PushNotificationAction;
import com.windscribe.vpn.billing.AmazonPurchase;
import com.windscribe.vpn.billing.PurchaseState;

import java.util.List;
import java.util.Map;

public interface UpgradePresenter {

    void checkBillingProcessStatus();

    void onAmazonPurchaseHistoryError(String error);

    void onAmazonPurchaseHistorySuccess(List<AmazonPurchase> amazonPurchases);

    void onBillingSetupFailed(int errorCode);

    void onBillingSetupSuccessful();

    void onConsumeFailed(int responseCode, Purchase purchase);

    void onContinueFreeClick();

    void onContinuePlanClick(final Product selectedSku);

    void onDestroy();

    void onMonthlyItemClicked(@Nullable ImmutableList<BillingFlowParams.ProductDetailsParams> productDetailsParams);

    void onProductDataResponse(Map<String, Product> products);

    void onProductResponseFailure();

    void onPurchaseConsumed(Purchase purchase);

    void onPurchaseResponse(PurchaseResponse response);

    void onPurchaseResponseFailure(PurchaseResponse.RequestStatus requestStatus);

    void onPurchaseUpdated(int responseCode, @Nullable List<Purchase> purchases);

    void onSkuDetailsReceived(int responseCode, List<ProductDetails> productDetails);

    void restorePurchase();

    void setLayoutFromApiSession();

    void setPurchaseFlowState(PurchaseState state);

    void setPushNotificationAction(@NonNull PushNotificationAction pushNotificationAction);

    String regionalPlanIfAvailable(String sku);

    void onRegionalPlanSelected(String url);
}
