/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.api.response;

import androidx.annotation.Keep;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import org.jetbrains.annotations.NotNull;

/**
 * Created by Mustafizur on 2017-11-30.
 */

@Keep
public class ServerCredentialsResponse {

    @SerializedName("password")
    @Expose
    private String passwordEncoded;

    @SerializedName("username")
    @Expose
    private String userNameEncoded;

    public String getPasswordEncoded() {
        return passwordEncoded;
    }

    public void setPasswordEncoded(String passwordEncoded) {
        this.passwordEncoded = passwordEncoded;
    }

    public String getUserNameEncoded() {
        return userNameEncoded;
    }

    public void setUserNameEncoded(String userNameEncoded) {
        this.userNameEncoded = userNameEncoded;
    }

    private static String mask(String value) {
        if (value == null || value.isEmpty()) return "****";
        if (value.length() <= 4) return "****";
        int maskedLength = value.length() - 4;
        return "*".repeat(maskedLength) + value.substring(value.length() - 4);
    }

    @NotNull
    @Override
    public String toString() {
        return "ServerCredentialsResponse{" +
                "userNameEncoded='" + mask(userNameEncoded) + '\'' +
                ", passwordEncoded='" + mask(passwordEncoded) + '\'' +
                '}';
    }
}
