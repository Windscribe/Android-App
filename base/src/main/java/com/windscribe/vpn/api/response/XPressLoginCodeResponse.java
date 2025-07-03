/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.api.response;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

@Keep
public class XPressLoginCodeResponse {

    @SerializedName("sig")
    @Expose
    private String signature;

    @SerializedName("time")
    @Expose
    private long time;

    @SerializedName("ttl")
    @Expose
    private int ttl;

    @SerializedName("xpress_code")
    @Expose
    private String xPressLoginCode;

    public String getSignature() {
        return signature;
    }

    public void setSignature(final String signature) {
        this.signature = signature;
    }

    public long getTime() {
        return time;
    }

    public void setTime(final long time) {
        this.time = time;
    }

    public int getTtl() {
        return ttl;
    }

    public void setTtl(final int ttl) {
        this.ttl = ttl;
    }

    public String getXPressLoginCode() {
        return xPressLoginCode;
    }

    public void setXPressLoginCode(final String xPressLoginCode) {
        this.xPressLoginCode = xPressLoginCode;
    }

    @NonNull
    @Override
    public String toString() {
        return "XPressLoginCodeResponse{" +
                "xPressLoginCode='" + xPressLoginCode + '\'' +
                ", ttl=" + ttl +
                ", signature='" + signature + '\'' +
                ", time=" + time +
                '}';
    }
}
