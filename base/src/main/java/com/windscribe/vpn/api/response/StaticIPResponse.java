/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.api.response;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.windscribe.vpn.serverlist.entity.StaticRegion;

import java.util.List;

@SuppressWarnings("unused")
public class StaticIPResponse {

    @SerializedName("static_ips")
    @Expose
    private List<StaticRegion> staticIpList;

    public List<StaticRegion> getStaticIpList() {
        return staticIpList;
    }

    public void setStaticIpList(List<StaticRegion> staticIpList) {
        this.staticIpList = staticIpList;
    }
}
