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

    @NotNull
    @Override
    public String toString() {
        return "ServerCredentialsResponse{" +
                "userNameEncoded='" + userNameEncoded + '\'' +
                ", passwordEncoded='" + passwordEncoded + '\'' +
                '}';
    }
}
