/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.api.response;

import androidx.annotation.Nullable;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

@SuppressWarnings("unused")
@Entity
public class StaticIp {

    @SerializedName("city_name")
    @Expose
    private String cityName;
    @SerializedName("country_code")
    @Expose
    private String countryCode;
    @SerializedName("credentials")
    @Expose
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
    @SerializedName("node")
    @Expose
    private StaticIpNode staticIpNode;
    @SerializedName("type")
    @Expose
    private String type;
    @SerializedName("wg_ip")
    @Expose
    private String wgIp;
    @SerializedName("wg_pubkey")
    @Expose
    private String wgPubKey;

    @SerializedName("ping_host")
    @Expose
    private String pingHost;

    @Nullable
    public String getPingHost() {
        return pingHost;
    }

    public void setPingHost(@Nullable String pingHost) {
        this.pingHost = pingHost;
    }

    public String getCityName() {
        return cityName;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public ServerCredentialsResponse getCredentials() {
        return credentials;
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

    public Integer getIpId() {
        return ipId;
    }

    public String getName() {
        return name;
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

    public String getShortName() {
        return shortName;
    }

    public String getStaticIp() {
        return staticIp;
    }

    public StaticIpNode getStaticIpNode() {
        return staticIpNode;
    }

    public String getType() {
        return type;
    }

    public String getWgIp() {
        return wgIp;
    }

    public String getWgPubKey() {
        return wgPubKey;
    }

    public static class StaticIpNode {

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

        public String getCityName() {
            return cityName;
        }

        public String getDnsHostname() {
            return dnsHostname;
        }

        public String getHostname() {
            return hostname;
        }

        public String getIp() {
            return ip;
        }

        public String getIp2() {
            return ip2;
        }

        public String getIp3() {
            return ip3;
        }
    }
}
