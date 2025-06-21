/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.api.response;

import androidx.annotation.Keep;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.List;


@Keep
public class AccessIpResponse {

    @SerializedName("hosts")
    @Expose
    private final List<String> hosts = null;

    public List<String> getHosts() {
        return hosts;
    }
}
