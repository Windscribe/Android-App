/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.api.response;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.List;

@SuppressWarnings("unused")
public class Sip {

    @SerializedName("update")
    @Expose
    private final List<String> update = null;
    @SerializedName("count")
    @Expose
    private Integer count;

    public Integer getCount() {
        return count;
    }

    public List<String> getUpdate() {
        return update;
    }
}
