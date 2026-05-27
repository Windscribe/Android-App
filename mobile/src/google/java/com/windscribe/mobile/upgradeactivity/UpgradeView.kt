/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.mobile.upgradeactivity

import com.amazon.device.iap.model.Product
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.QueryProductDetailsParams
import com.windscribe.vpn.billing.WindscribeInAppProduct

interface UpgradeView {

    val billingType: UpgradeActivity.BillingType

    fun getProducts(skuList: List<String>)

    fun goBackToMainActivity()

    fun hideProgressBar()

    fun isBillingProcessFinished(): Boolean

    fun onPurchaseCancelled()

    // Called after onPurchaseUpdate returns successful
    fun onPurchaseSuccessful(purchases: List<Purchase>?)

    fun querySkuDetails(products: List<QueryProductDetailsParams.Product>)

    fun restorePurchase()

    fun setBillingProcessStatus(bProcessFinished: Boolean)

    fun setupPlans(windscribeInAppProduct: WindscribeInAppProduct)

    fun showBillingError(errorMessage: String)

    fun showProgressBar(message: String)

    fun showToast(toastText: String)

    fun startPurchaseFlow(productDetailsParams: List<BillingFlowParams.ProductDetailsParams>, accountID: String?)

    fun startPurchaseFlow(product: Product)

    fun openUrlInBrowser(url: String)

    fun goToSuccessfulUpgrade(isGhostAccount: Boolean)
}
