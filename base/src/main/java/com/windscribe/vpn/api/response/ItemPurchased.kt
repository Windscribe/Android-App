/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.api.response

import androidx.annotation.Keep
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

@Keep
class ItemPurchased {
    @SerializedName("orderId")
    @Expose
    val orderId: String? = null

    @SerializedName("packageName")
    @Expose
    val packageName: String? = null

    @SerializedName("productId")
    @Expose
    val productId: String? = null

    @SerializedName("purchaseState")
    @Expose
    val purchaseState: Int? = null

    @SerializedName("purchaseTime")
    @Expose
    val purchaseTime: String? = null

    @SerializedName("purchaseToken")
    @Expose
    val purchaseToken: String? = null
}
