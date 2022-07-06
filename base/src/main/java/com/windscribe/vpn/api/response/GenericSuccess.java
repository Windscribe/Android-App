/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.api.response;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class GenericSuccess {

    @SerializedName("success")
    @Expose
    private int success;

    public boolean isSuccessful() {
        return success == 1;
    }
}
