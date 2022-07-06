/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.localdatabase.tables;


import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@SuppressWarnings("unused")
@Entity(tableName = "Network_Info")
public class NetworkInfo {

    @ColumnInfo(name = "is_auto_secure")
    private boolean isAutoSecureOn;

    @ColumnInfo(name = "is_preferred")
    private boolean isPreferredOn;

    @NonNull
    @PrimaryKey
    private String networkName;

    @ColumnInfo(name = "port")

    private String port;

    @ColumnInfo(name = "protocol")
    private String protocol;

    public NetworkInfo(@NonNull String networkName, boolean isAutoSecureOn, boolean isPreferredOn, String protocol,
            String port) {
        this.networkName = networkName;
        this.isAutoSecureOn = isAutoSecureOn;
        this.isPreferredOn = isPreferredOn;
        this.protocol = protocol;
        this.port = port;
    }

    @NonNull
    public String getNetworkName() {
        return networkName;
    }

    public void setNetworkName(@NonNull String networkName) {
        this.networkName = networkName;
    }

    public String getPort() {
        return port;
    }

    public void setPort(String port) {
        this.port = port;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public boolean isAutoSecureOn() {
        return isAutoSecureOn;
    }

    public void setAutoSecureOn(boolean autoSecureOn) {
        isAutoSecureOn = autoSecureOn;
    }

    public boolean isPreferredOn() {
        return isPreferredOn;
    }

    public void setPreferredOn(boolean preferredOn) {
        isPreferredOn = preferredOn;
    }

    @NonNull
    @Override
    public String toString() {
        return "NetworkInfo{" +
                "networkName='" + networkName + '\'' +
                ", isAutoSecureOn=" + isAutoSecureOn +
                ", isPreferredOn=" + isPreferredOn +
                ", protocol='" + protocol + '\'' +
                ", port='" + port + '\'' +
                '}';
    }
}
