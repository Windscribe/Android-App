/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.api.response;

import androidx.annotation.NonNull;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;


@SuppressWarnings("unused")
public class ClaimAccountResponse {

    @SerializedName("password_updated")
    @Expose
    private int passwordUpdated;

    @SerializedName("success")
    @Expose
    private int success;

    @SerializedName("username_updated")
    @Expose
    private int usernameUpdated;

    public int getPasswordUpdated() {
        return passwordUpdated;
    }

    public int getUsernameUpdated() {
        return usernameUpdated;
    }

    public boolean isSuccessful() {
        return success == 1;
    }

    @NonNull
    @Override
    public String toString() {
        return "ClaimAccountResponse{" +
                "passwordUpdated=" + passwordUpdated +
                ", usernameUpdated=" + usernameUpdated +
                ", success=" + success +
                '}';
    }
}
