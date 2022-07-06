/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.api.response;

import androidx.annotation.NonNull;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;


public class VerifyExpressLoginResponse {

    @SerializedName("success")
    @Expose
    private int success;

    public boolean isSuccessful() {
        return success == 1;
    }

    @NonNull
    @Override
    public String toString() {
        return "ClaimAccountResponse{" +
                ", success=" + success +
                '}';
    }
}
