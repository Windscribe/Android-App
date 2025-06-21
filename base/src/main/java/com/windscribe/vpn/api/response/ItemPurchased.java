/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.api.response;


import androidx.annotation.Keep;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

@Keep
public class ItemPurchased {

    @SerializedName("orderId")
    @Expose
    private String orderId;

    @SerializedName("packageName")
    @Expose
    private String packageName;

    @SerializedName("productId")
    @Expose
    private String productId;

    @SerializedName("purchaseState")
    @Expose
    private Integer purchaseState;

    @SerializedName("purchaseTime")
    @Expose
    private String purchaseTime;

    @SerializedName("purchaseToken")
    @Expose
    private String purchaseToken;

    public String getOrderId() {
        return orderId;
    }

    public String getPackageName() {
        return packageName;
    }

    public String getProductId() {
        return productId;
    }

    public Integer getPurchaseState() {
        return purchaseState;
    }

    public String getPurchaseTime() {
        return purchaseTime;
    }

    public String getPurchaseToken() {
        return purchaseToken;
    }
}
