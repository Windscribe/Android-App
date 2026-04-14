/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.api.response;

import androidx.annotation.Keep;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

@Keep
public class GeneratePasswordResponse {

    @SerializedName("password")
    @Expose
    private String password;

    @SerializedName("success")
    @Expose
    private int success;

    public String getPassword() {
        return password;
    }

    public boolean isSuccessful() {
        return success == 1;
    }
}
