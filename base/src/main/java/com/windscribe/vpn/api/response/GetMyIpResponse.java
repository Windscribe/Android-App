/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.api.response;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

@Keep
public class GetMyIpResponse {

    @SerializedName("user_ip")
    @Expose
    private String userIp;

    public String getUserIp() {
        return userIp;
    }


    @NonNull
    @Override
    public String toString() {
        return "UserIp{" +
                "user_ip=" + userIp +
                '}';
    }
}
