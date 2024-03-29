/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.tv.upgrade

import com.amazon.device.iap.model.Product
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.QueryProductDetailsParams
import com.google.common.collect.ImmutableList
import com.windscribe.vpn.billing.WindscribeInAppProduct

interface UpgradeView {
    val billingType: UpgradeActivity.BillingType
    fun getProducts(skuList: List<String>)
    fun goBackToMainActivity()
    fun goToAddEmail()
    fun goToConfirmEmail()
    fun gotToClaimAccount()
    fun hideProgressBar()
    val isBillingProcessFinished: Boolean
    fun onPurchaseCancelled()
    fun onPurchaseSuccessful(purchases: List<Purchase>)
    fun querySkuDetails(products: List<QueryProductDetailsParams.Product>)
    fun restorePurchase()
    fun setBillingProcessStatus(processFinished: Boolean)
    fun setEmailStatus(isEmailAdded: Boolean, isEmailConfirmed: Boolean)
    fun showBillingDialog(
        windscribeInAppProduct: WindscribeInAppProduct,
        isEmailAdded: Boolean,
        isEmailConfirmed: Boolean
    )
    fun showBillingErrorDialog(errorMessage: String)
    fun showProgressBar(message: String)
    fun showToast(toastText: String)
    fun startPurchaseFlow(
        productDetailsParams: ImmutableList<BillingFlowParams.ProductDetailsParams>,
        accountID: String?
    )

    fun startPurchaseFlow(product: Product)
    fun startSignUpActivity()
    fun startWindscribeActivity()
}
