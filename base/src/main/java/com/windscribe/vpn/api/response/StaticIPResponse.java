/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.api.response;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.List;

@SuppressWarnings("unused")
public class StaticIPResponse {

    @SerializedName("static_ips")
    @Expose
    private List<StaticIp> staticIpList;

    public List<StaticIp> getStaticIpList() {
        return staticIpList;
    }

    public void setStaticIpList(List<StaticIp> staticIpList) {
        this.staticIpList = staticIpList;
    }
}
