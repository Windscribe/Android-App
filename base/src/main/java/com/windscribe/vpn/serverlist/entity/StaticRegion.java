/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.serverlist.entity;

import androidx.annotation.NonNull;
import androidx.room.Embedded;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.windscribe.vpn.api.response.ServerCredentialsResponse;

@Entity(tableName = "StaticRegion")
public class StaticRegion {

    @SerializedName("city_name")
    @Expose
    private String cityName;

    @SerializedName("country_code")
    @Expose
    private String countryCode;

    @SerializedName("credentials")
    @Expose
    @Embedded
    private ServerCredentialsResponse credentials;

    @SerializedName("device_name")
    @Expose
    private String deviceName;

    @PrimaryKey
    @SerializedName("id")
    @Expose
    private Integer id;

    @SerializedName("ip_id")
    @Expose
    private Integer ipId;

    @SerializedName("name")
    @Expose
    private String name;

    @SerializedName("node")
    @Expose
    @Embedded(prefix = "node")
    private NodeStatic nodeStatic;

    @SerializedName("ovpn_x509")
    @Expose
    private String ovpnX509;

    @SerializedName("server_id")
    @Expose
    private Integer serverId;

    @SerializedName("short_name")
    @Expose
    private String shortName;

    @SerializedName("static_ip")
    @Expose
    private String staticIp;

    @SerializedName("type")
    @Expose
    private String type;

    @SerializedName("wg_ip")
    @Expose
    private String wgIp;

    @SerializedName("wg_pubkey")
    @Expose
    private String wgPubKey;

    public String getCityName() {
        return cityName;
    }

    public void setCityName(String cityName) {
        this.cityName = cityName;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }

    public ServerCredentialsResponse getCredentials() {
        return credentials;
    }

    public void setCredentials(ServerCredentialsResponse credentials) {
        this.credentials = credentials;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getIpId() {
        return ipId;
    }

    public void setIpId(Integer ipId) {
        this.ipId = ipId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public NodeStatic getNodeStatic() {
        return nodeStatic;
    }

    public void setNodeStatic(NodeStatic nodeStatic) {
        this.nodeStatic = nodeStatic;
    }

    public String getOvpnX509() {
        return ovpnX509;
    }

    public void setOvpnX509(final String ovpnX509) {
        this.ovpnX509 = ovpnX509;
    }

    public Integer getServerId() {
        return serverId;
    }

    public void setServerId(Integer serverId) {
        this.serverId = serverId;
    }

    public String getShortName() {
        return shortName;
    }

    public void setShortName(String shortName) {
        this.shortName = shortName;
    }

    public String getStaticIp() {
        return staticIp;
    }

    public void setStaticIp(String staticIp) {
        this.staticIp = staticIp;
    }

    public NodeStatic getStaticIpNode() {
        return nodeStatic;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getWgIp() {
        return wgIp;
    }

    public void setWgIp(String wgIp) {
        this.wgIp = wgIp;
    }

    public String getWgPubKey() {
        return wgPubKey;
    }

    public void setWgPubKey(String wgPubKey) {
        this.wgPubKey = wgPubKey;
    }

    @NonNull
    @Override
    public String toString() {
        return "StaticRegion{" +
                "id=" + id +
                ", ipId=" + ipId +
                ", staticIp='" + staticIp + '\'' +
                ", type='" + type + '\'' +
                ", name='" + name + '\'' +
                ", countryCode='" + countryCode + '\'' +
                ", shortName='" + shortName + '\'' +
                ", cityName='" + cityName + '\'' +
                ", serverId=" + serverId +
                ", nodeStatic=" + nodeStatic +
                ", credentials=" + credentials +
                ", deviceName='" + deviceName + '\'' +
                ", OvpnX509='" + ovpnX509 + '\'' +
                '}';
    }

}

