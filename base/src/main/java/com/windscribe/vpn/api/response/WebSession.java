/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.api.response;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

@Keep
public class WebSession {

    @SerializedName("temp_session")
    @Expose
    private String tempSession;

    public String getTempSession() {
        return tempSession;
    }

    @NonNull
    @Override
    public String toString() {
        return "WebSession{" +
                "tempSession='" + tempSession + '\'' +
                '}';
    }
}
