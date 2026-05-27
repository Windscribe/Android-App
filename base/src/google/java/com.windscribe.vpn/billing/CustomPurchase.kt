/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.billing

import com.android.billingclient.api.Purchase

class CustomPurchase(val responseCode: Int, val purchase: Purchase)
