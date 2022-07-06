/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.tv.upgrade

import com.amazon.device.iap.model.Product
import com.amazon.device.iap.model.PurchaseResponse
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.SkuDetails
import com.windscribe.vpn.api.response.PushNotificationAction
import com.windscribe.vpn.billing.AmazonPurchase
import com.windscribe.vpn.billing.PurchaseState

interface UpgradePresenter {
    fun checkBillingProcessStatus()
    fun onAmazonPurchaseHistoryError(error: String)
    fun onAmazonPurchaseHistorySuccess(amazonPurchases: List<AmazonPurchase>)
    fun onBillingSetupFailed(errorCode: Int)
    fun onBillingSetupSuccessful()
    fun onConsumeFailed(responseCode: Int, purchase: Purchase)
    fun onContinueFreeClick()
    fun onContinuePlanClick(selectedSku: Product)
    fun onDestroy()
    fun onMonthlyItemClicked(monthlySKU: SkuDetails)
    fun onProductDataResponse(products: Map<String, Product>)
    fun onProductResponseFailure()
    fun onPurchaseConsumed(purchase: Purchase)
    fun onPurchaseResponse(response: PurchaseResponse)
    fun onPurchaseResponseFailure(requestStatus: PurchaseResponse.RequestStatus)
    fun onPurchaseUpdated(responseCode: Int, purchases: List<Purchase>)
    fun onSkuDetailsReceived(responseCode: Int, skuDetailsList: List<SkuDetails>)
    fun restorePurchase()
    fun setLayoutFromApiSession()
    fun setPurchaseFlowState(state: PurchaseState)
    fun setPushNotificationAction(pushNotificationAction: PushNotificationAction)
}
