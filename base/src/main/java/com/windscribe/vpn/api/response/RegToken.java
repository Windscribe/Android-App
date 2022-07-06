/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.api.response;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class RegToken {

    @SerializedName("id")
    @Expose
    private String id;

    @SerializedName("signature")
    @Expose
    private String signature;

    @SerializedName("time")
    @Expose
    private long time;

    @SerializedName("token")
    @Expose
    private String token;

    public String getToken() {
        return token;
    }
}
