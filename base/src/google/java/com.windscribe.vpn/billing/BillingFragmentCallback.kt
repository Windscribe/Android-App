/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.billing

import com.amazon.device.iap.model.Product
import com.android.billingclient.api.ProductDetails

interface BillingFragmentCallback {
    fun onContinuePlanClick(
        productDetails: ProductDetails,
        selectedIndex: Int,
    )

    fun onContinuePlanClick(selectedSku: Product)

    fun onRestorePurchaseClick()

    fun onTenGbFreeClick()

    fun onTermsClick()

    fun onPolicyClick()
}
