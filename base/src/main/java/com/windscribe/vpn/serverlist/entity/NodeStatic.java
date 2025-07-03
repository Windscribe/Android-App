/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.serverlist.entity;

import androidx.annotation.Keep;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

@Keep
public class NodeStatic {

    @SerializedName("city_name")
    @Expose
    private String cityName;

    @SerializedName("dns_hostname")
    @Expose
    private String dnsHostname;

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

    public NodeStatic(String ip, String ip2, String ip3, String hostname, String dnsHostname, String cityName) {
        this.ip = ip;
        this.ip2 = ip2;
        this.ip3 = ip3;
        this.hostname = hostname;
        this.dnsHostname = dnsHostname;
        this.cityName = cityName;
    }

    public String getCityName() {
        return cityName;
    }

    public void setCityName(String cityName) {
        this.cityName = cityName;
    }

    public String getDnsHostname() {
        return dnsHostname;
    }

    public void setDnsHostname(String dnsHostname) {
        this.dnsHostname = dnsHostname;
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
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
}
