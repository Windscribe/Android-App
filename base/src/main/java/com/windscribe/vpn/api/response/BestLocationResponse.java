/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.api.response;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * Created by Mustafizur on 2017-09-08.
 */

@Keep
public class BestLocationResponse {

    @SerializedName("city_name")
    @Expose
    private String cityName;

    @SerializedName("country_code")
    @Expose
    private String countryCode;

    @SerializedName("dc_id")
    @Expose
    private String dcId;

    @SerializedName("hostname")
    @Expose
    private String hostname;

    @SerializedName("ip")
    @Expose
    private String ip;

    @SerializedName("ip2")
    @Expose
    private String ip2;

    @SerializedName("ip3")
    @Expose
    private String ip3;

    @SerializedName("location_name")
    @Expose
    private String locationName;

    @SerializedName("server_id")
    @Expose
    private String serverId;

    @SerializedName("short_name")
    @Expose
    private String shortName;

    public String getCityName() {
        return cityName;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public String getDcId() {
        return dcId;
    }

    public String getHostname() {
        return hostname;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getIp2() {
        return ip2;
    }

    public void setIp2(String ip2) {
        this.ip2 = ip2;
    }

    public String getIp3() {
        return ip3;
    }

    public void setIp3(String ip3) {
        this.ip3 = ip3;
    }

    public String getLocationName() {
        return locationName;
    }

    public String getServerId() {
        return serverId;
    }

    public String getShortName() {
        return shortName;
    }

    @NonNull
    @Override
    public String toString() {
        return "BestLocationResponse{" +
                "countryCode='" + countryCode + '\'' +
                ", shortName='" + shortName + '\'' +
                ", locationName='" + locationName + '\'' +
                ", cityName='" + cityName + '\'' +
                ", dcId='" + dcId + '\'' +
                ", serverId='" + serverId + '\'' +
                ", hostname='" + hostname + '\'' +
                ", ip='" + ip + '\'' +
                ", ip2='" + ip2 + '\'' +
                ", ip3='" + ip3 + '\'' +
                '}';
    }
}
