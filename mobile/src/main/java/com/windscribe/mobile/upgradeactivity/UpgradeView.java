/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.mobile.upgradeactivity;

import androidx.annotation.Nullable;

import com.amazon.device.iap.model.Product;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.SkuDetails;
import com.windscribe.mobile.upgradeactivity.UpgradeActivity.BillingType;
import com.windscribe.vpn.billing.WindscribeInAppProduct;

import java.util.List;

public interface UpgradeView {

    BillingType getBillingType();

    void getProducts(List<String> skuList);

    void goBackToMainActivity();

    void goToAddEmail();

    void goToConfirmEmail();

    void gotToClaimAccount();

    void hideProgressBar();

    boolean isBillingProcessFinished();

    void onPurchaseCancelled();

    //Called after onPurchaseUpdate returns successful
    void onPurchaseSuccessful(@Nullable List<Purchase> purchases);

    void querySkuDetails(List<String> products, boolean sub);

    void restorePurchase();

    void setBillingProcessStatus(boolean bProcessFinished);

    void setEmailStatus(boolean isEmailAdded, boolean isEmailConfirmed);

    void showBillingDialog(final WindscribeInAppProduct windscribeInAppProduct, boolean isEmailAdded,
            boolean isEmailConfirmed);

    void showBillingErrorDialog(String errorMessage);

    void showProgressBar(String message);

    void showToast(String toastText);

    void startPurchaseFlow(SkuDetails skuDetails, String accountID);

    void startPurchaseFlow(Product product);

    void startSignUpActivity();

    void startWindscribeActivity();

    void openUrlInBrowser(String url);
}
