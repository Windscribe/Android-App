/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.billing

import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ProductDetails

class CustomProductDetails(
    val billingResult: BillingResult,
    val productDetails: List<ProductDetails>,
)
