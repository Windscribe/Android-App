/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.api.response;

import androidx.annotation.NonNull;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class XPressLoginVerifyResponse {

    @SerializedName("session_auth_hash")
    @Expose
    private String sessionAuth;

    public String getSessionAuth() {
        return sessionAuth;
    }

    @NonNull
    @Override
    public String toString() {
        return "XPressLoginVerifyResponse{" +
                "sessionAuth='" + sessionAuth + '\'' +
                '}';
    }
}
